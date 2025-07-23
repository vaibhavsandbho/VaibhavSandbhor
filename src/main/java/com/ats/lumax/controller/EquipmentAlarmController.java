package com.ats.lumax.controller;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.*;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.hibernate.internal.build.AllowSysOut;
import org.modelmapper.ModelMapper;

import com.ats.lumax.Entity.EquipmentAlarmDetails;
import com.ats.lumax.Entity.EquipmentAlarmHistoryDto;
import com.ats.lumax.Entity.EquipmentAlarmHistoryEntity;
import com.ats.lumax.Entity.MasterEquipmentDetailsEntity;

import com.ats.lumax.repo.EquipmentAlaramHistoryrepo;

import com.ats.lumax.repo.EquipmetAlarmDetailsRepo;
import com.ats.lumax.repo.MasterEquipmentRepo;
import com.ats.lumax.service.CacheAlarmService;
import com.ats.lumax.service.OpcUaService;
import com.ats.lumax.service.OpcUaValueConverter;
import com.ats.lumax.service.PredefinedNodeValueService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/plc/EquipmentAlarm")
@RequiredArgsConstructor
@Slf4j
public class EquipmentAlarmController {


    private final OpcUaValueConverter opcUaValueConverter;

	
	private final PredefinedNodeValueService predefinedNodeValueService;

    private final OpcUaService opcUaService;
    
    private final EquipmentAlaramHistoryrepo equipmentHistoryrepo;
    
    @Autowired
    private EquipmetAlarmDetailsRepo equipmentAlarmDetailsRepo;
             
    @Autowired
   private  MasterEquipmentRepo masterEquipmentRepo;
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private CacheAlarmService cacheAlarmServiceInstance;
    
    @Autowired
    private ModelMapper modelMapper;
    
    private final Map<String, Boolean> alarmStates = new ConcurrentHashMap<>();
//
   

    @GetMapping("/status")
    public ResponseEntity<String> getConnectionStatus() {
        boolean isConnected = opcUaService.isConnected();
        return ResponseEntity.ok(isConnected ? "Connected" : "Disconnected");
    }

    @GetMapping("/browse")
    public ResponseEntity<List<String>> browseTags(
            @RequestParam(required = false) String startingNode) {
        try {
            List<String> tags = opcUaService.browseTags(startingNode);
            
            ObjectMapper mapper=new ObjectMapper();
            
            String filePath = "src/main/resources/BrowseTag.json";
            
            
            
            File file=new File(filePath);
            // Create directories if they don't exist
            file.getParentFile().mkdirs();
            // Write or overwrite the JSON file with the new tags
            mapper.writeValue(file, tags);
            
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            log.error("Error browsing tags", e);
            return ResponseEntity.internalServerError().build();
        }
    }

   
    
    private volatile boolean running = true;
    @PostConstruct
    public void startMonitoringThread() {
        Thread thread = new Thread(() -> {
            while (running) {
                try {
                	readAndProcessWordAlarmsFromWordTags();
                    Thread.sleep(1000); // Run every 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace(); // Log any other error
                }
            }
        });

        thread.setDaemon(true); // Optional: stops when app stops
        thread.start();
    }
 


    /**
     * Reads all predefined OPC-UA node values every cycle and processes them to detect alarm conditions.
     * 
     * - If a Boolean node value changes from false to true (i.e., becomes active), it saves a corresponding alarm to the database.
     * - If a Boolean node value changes from true to false (i.e., gets cleared), it updates the alarm status in the database and the in-memory state.
     * 
     * This method uses an in-memory cache (alarmStates) to avoid repeated database writes and reduce load.
     */
    public void readAndProcessWordAlarmsFromWordTags() {
        log.info("Starting alarm processing from word tags");

        Map<String, List<String>> allTags = predefinedNodeValueService.loadAllTagFiles();
        Set<String> allNodeIds = allTags.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toSet());
        

        List<EquipmentAlarmDetails> alarmDetailsList = loadAlarmDetailsFromJson();
        List<MasterEquipmentDetailsEntity> equipmentDetailsList = loadEquipmentDetailsFromJson();

        log.info("Loaded {} alarm details and {} equipment details for processing",
            alarmDetailsList.size(), equipmentDetailsList.size());

        // Fixed map creation - use base tag consistently
        Map<String, EquipmentAlarmDetails> alarmDetailsMap = alarmDetailsList.stream()
            .filter(e -> e.getEquipmentAlarmTag() != null)
            .collect(Collectors.toMap(
            	    e -> {
            	        String normalizedTag = e.getEquipmentAlarmTag().replace("\"", "").trim();
            	        String baseTag = extractTagBase(normalizedTag);
            	        String key = baseTag + "_" + e.getBitNo();
            	    
            	       
            	        return key;
            	        
            	       
            	    },
            	    Function.identity(),
            	    (existing, replacement) -> {
            	        log.warn("Duplicate alarm key found, keeping existing: {}", existing.getEquipmentAlarmName());
            	        return existing;
            	    }
            	));

        
    

        // Debug: Print all keys in the map
        log.info("Created alarm details map with {} entries", alarmDetailsMap.size());
        alarmDetailsMap.keySet().forEach(key -> log.debug("Map key: {}", key));

        Map<Integer, MasterEquipmentDetailsEntity> equipmentMap = equipmentDetailsList.stream()
            .collect(Collectors.toMap(MasterEquipmentDetailsEntity::getEquipmentId, Function.identity()));

        List<EquipmentAlarmHistoryEntity> alarmsToInsert = Collections.synchronizedList(new ArrayList<>());
        List<EquipmentAlarmHistoryEntity> alarmsToUpdate = Collections.synchronizedList(new ArrayList<>());

        log.info("Processing {} OPC UA nodes for alarm state changes", allNodeIds.size());

        try {
        	
        
            allNodeIds.parallelStream().forEach(nodeId -> {
                try {
                	
                   
                    Optional<DataValue> alarmWordOpt = opcUaService.readValue(nodeId);
                    
                    if (!alarmWordOpt.isPresent()) {
                        log.debug("No data value present for node: {}", nodeId);
                        return;
                    }
              
                    Object alarmWordObj = alarmWordOpt.get().getValue().getValue();
                    
                    if (!(alarmWordObj instanceof ExtensionObject)) {
                        log.debug("Node {} does not contain ExtensionObject, skipping", nodeId);
                        return;
                    }
                    
                    System.out.println("#0.4");
                    ExtensionObject extObj = (ExtensionObject) alarmWordObj;
                    Object body = extObj.getBody();
                    

                    if (!(body instanceof ByteString)) {
                        log.debug("ExtensionObject body is not ByteString for node: {}", nodeId);
                        return;
                    }
                    
                    System.out.println("#0.5");
                    byte[] bytes = ((ByteString) body).bytes();
                    log.debug("Processing {} bytes from node: {}", bytes.length, nodeId);
                    
                    for (int i = 0; i < bytes.length; i++) {
                        byte b = bytes[i];
                        
                        
                      
                      
                       
                        // Apply the SAME extraction logic as in map creation
                        String normalizedNodeId = nodeId.replace("\"", "");;
                        
                       
//                        String baseTag = extractTagBase(normalizedNodeId); // Use same method!
            
                        // Use the same key construction logic as in map creation
                        
                      
                        String alarmKey = normalizedNodeId + "_" + i;
                        String redisKey = "alarmState:" + alarmKey;
                        
                   
                        
                     
                        log.debug("Looking for alarm key: {} (from nodeId: {}, bitIndex: {})", alarmKey, nodeId, i);
                        
                      

                        EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);
                        
                        
                        if(i==6)
                        {
                        	
                        	System.out.println("alarmkey"+alarmKey);
                        	System.out.println("alarmKey"+alarmDetail);
                        }
                        if (alarmDetail == null) {
                            log.trace("No alarm detail found for key: {}", alarmKey);
                            continue;
                        }
                        
                        MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());
                        if (equipment == null) {
                            log.warn("No equipment found for ID: {} (alarm: {})", 
                                alarmDetail.getEquipmentId(), alarmDetail.getEquipmentAlarmName());
                            continue;
                        }

                        // Get cached state to check if alarm was previously active
                        String cachedState = redisTemplate.opsForValue().get(redisKey);
                        boolean wasPreviouslyActive = cachedState != null && Boolean.parseBoolean(cachedState);
                        
                        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        
                        if ((b & 0xFF) != 0) {
                            System.out.println("ALARM ON - Bit index: " + i + ", AlarmKey: " + alarmKey);
                        }
             
                        if ((b & 0xFF) != 0) { // Alarm is currently ACTIVE
                            System.out.println("#0.8");
                            if (!wasPreviouslyActive) {
                                EquipmentAlarmHistoryEntity newAlarm = new EquipmentAlarmHistoryEntity();
                                newAlarm.setEquipmentAlarmName(alarmDetail.getEquipmentAlarmName());
                                newAlarm.setEquipmentAlarmDesc(alarmDetail.getEquipmentAlarmDesc());
                                newAlarm.setEquipmentAlarmId(alarmDetail.getEquipmentAlarmId());
                                newAlarm.setEquipmentId(equipment.getEquipmentId());
                                newAlarm.setEquipmentName(equipment.getEquipmentName());
                                newAlarm.setEquipmentDesc(equipment.getEquipmentDesc());
                                newAlarm.setAlarmOccurredDatetime(now);
                                newAlarm.setAlarmResolvedDatetime("NA");
                                newAlarm.setEquipmentAlarmStatus(true);
                                System.out.println("New alarm created for alarmkey: " + alarmKey);
                                
                                // Update cache to mark as active
                                redisTemplate.opsForValue().set(redisKey, "true");
                                alarmsToInsert.add(newAlarm);
                                
                                log.info("New alarm activated - Equipment: {}, Alarm: {}, Time: {}", 
                                    equipment.getEquipmentName(), alarmDetail.getEquipmentAlarmName(), now);
                            } else {
                                log.debug("Alarm already active (duplicate prevented) - Equipment: {}, Alarm: {}", 
                                    equipment.getEquipmentName(), alarmDetail.getEquipmentAlarmName());
                            }
                            
                        } else if ((b & 0xFF) == 0) { // Alarm is currently RESOLVED
                            System.out.println("#0.9");
                            if (wasPreviouslyActive) {
                                EquipmentAlarmHistoryEntity history = 
                                    equipmentHistoryrepo.findByEquipmentAlarmIdAndEquipmentAlarmStatusTrue(alarmDetail.getEquipmentAlarmId());
                                
                                if (history != null) {
                                    System.out.println("#0.10" + history);
                                    history.setAlarmResolvedDatetime(now);
                                    history.setEquipmentAlarmStatus(false);
                                    
                                    // Update cache to mark as resolved
                                    redisTemplate.opsForValue().set(redisKey, "false");
                                    alarmsToUpdate.add(history);
                                    
                                    log.info("Alarm resolved - Equipment: {}, Alarm: {}, Duration: {} to {}", 
                                        equipment.getEquipmentName(), alarmDetail.getEquipmentAlarmName(),
                                        history.getAlarmOccurredDatetime(), now);
                                } else {
                                    log.warn("No active alarm history found for resolution - Equipment: {}, Alarm: {}", 
                                        equipment.getEquipmentName(), alarmDetail.getEquipmentAlarmName());
                                }
                            } else {
                                log.trace("Alarm already resolved or never was active - Equipment: {}, Alarm: {}", 
                                    equipment.getEquipmentName(), alarmDetail.getEquipmentAlarmName());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing alarm data for node: {} - Error: {}", nodeId, e.getMessage(), e);
                }
            });
            
        } finally {
            try {
                if (!alarmsToInsert.isEmpty()) {
                    System.out.println("#0.11");
                    equipmentHistoryrepo.saveAll(alarmsToInsert);
                    log.info("Successfully inserted {} new alarm records into database", alarmsToInsert.size());
                    updateActiveAlarmCache(alarmsToInsert, true);
                }

                if (!alarmsToUpdate.isEmpty()) {
                    System.out.println("#0.12");
                    equipmentHistoryrepo.saveAll(alarmsToUpdate);
                    log.info("Successfully updated {} resolved alarm records in database", alarmsToUpdate.size());
                    updateActiveAlarmCache(alarmsToUpdate, false);
                }

                log.info("Alarm processing completed successfully - New alarms: {}, Resolved alarms: {}", 
                    alarmsToInsert.size(), alarmsToUpdate.size());

            } catch (Exception ex) {
                log.error("Critical error during database or Redis operations: {}", ex.getMessage(), ex);
                throw new RuntimeException("Failed to persist alarm data", ex);
            }
        }
    }

    private String extractTagBase(String normalizedTag) {
        // Example input: ns=3;s=8.PLC_TO_WMS_MCP_Alarms.CH06.Alarm14
        // Should return: ns=3;s=8.PLC_TO_WMS_MCP_Alarms.CH06

        int lastDotIndex = normalizedTag.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return normalizedTag.substring(0, lastDotIndex); // remove .AlarmX or .Forward Send Time Out
        }
        return normalizedTag;
    }
 // Loads alarm detail definitions from JSON file (used to map nodeId to alarm metadata)
    private List<EquipmentAlarmDetails> loadAlarmDetailsFromJson() {
        try {
            String path = System.getProperty("user.dir") + "/EquipmentAlarmDetails.json";
            return new ObjectMapper().readValue(new File(path), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to load EquipmentAlarmDetails.json", e);
            return Collections.emptyList();
        }
    }

    // Loads master equipment definitions from JSON file (used to enrich alarm data)
    private List<MasterEquipmentDetailsEntity> loadEquipmentDetailsFromJson() {
        try {
            String path = System.getProperty("user.dir") + "/EquipmentDeatails.json";
            return new ObjectMapper().readValue(new File(path), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to load EquipmentDeatails.json", e);
            return Collections.emptyList();
        }
    }
//    private String extractTagBase(String tag) {
//        if (tag == null || tag.isEmpty()) return tag;
//
//        // Remove suffix after last dot if it starts with "Alarm" followed by a number
//        int lastDotIndex = tag.lastIndexOf('.');
//        if (lastDotIndex != -1 && lastDotIndex < tag.length() - 1) {
//            String suffix = tag.substring(lastDotIndex + 1);
//            if (suffix.matches("Alarm\\d+")) {
//                return tag.substring(0, lastDotIndex);
//            }
//        }
//        return tag;
//    }
    private void updateActiveAlarmCache(List<EquipmentAlarmHistoryEntity> alarmsToUpdate, boolean isActive) {
        try {
            String redisKey = "alarmState:" + alarmsToUpdate;
            redisTemplate.opsForValue().set(redisKey, String.valueOf(isActive));
            log.debug("Updated Redis cache for key: {} to state: {}", redisKey, isActive);
        } catch (Exception e) {
            log.error("Failed to update Redis cache for alarmKey: {}, Error: {}", alarmsToUpdate, e.getMessage(), e);
        }
    }

}
    
