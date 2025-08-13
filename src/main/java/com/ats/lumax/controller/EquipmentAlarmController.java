package com.ats.lumax.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.*;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
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
     * @throws IOException 
     * @throws DatabindException 
     * @throws StreamReadException 
     */
    public void readAndProcessWordAlarmsFromWordTags() throws StreamReadException, DatabindException, IOException {
        log.info("Starting alarm processing from word tags");

        // Load and prepare mappings
        Map<String, List<String>> allTags = predefinedNodeValueService.loadAllTagFiles();
        Set<String> allNodeIds = extractNodeIds(allTags);
        
        System.out.println("#0.1");

        List<EquipmentAlarmDetails> alarmDetailsList = loadAlarmDetailsFromJson();
        List<MasterEquipmentDetailsEntity> equipmentDetailsList = loadEquipmentDetailsFromJson();

        log.info("Loaded {} alarm details and {} equipment details", 
                 alarmDetailsList.size(), equipmentDetailsList.size());

        Map<String, EquipmentAlarmDetails> alarmDetailsMap = prepareAlarmDetailsMap(alarmDetailsList);
        Map<Integer, MasterEquipmentDetailsEntity> equipmentMap = prepareEquipmentMap(equipmentDetailsList);

        // Thread-safe queues to avoid concurrency issues
        Queue<EquipmentAlarmHistoryEntity> alarmsToInsert = new ConcurrentLinkedQueue<>();
        Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate = new ConcurrentLinkedQueue<>();
        
    
    
        // Process in parallel
        log.info("Processing {} OPC UA nodes for alarm state changes", allNodeIds.size());
        allNodeIds.parallelStream().forEach(nodeId -> {
            try {
            	System.out.println("#0.2");
                processNode(nodeId, alarmDetailsMap, equipmentMap, alarmsToInsert, alarmsToUpdate);
            } catch (Exception e) {
                log.error("Error processing node {}: {}", nodeId, e.getMessage(), e);
            }
        });

        
               // Persist results and update cache
        persistAndCacheUpdates(alarmsToInsert, alarmsToUpdate);

        log.info("Alarm processing completed - New: {}, Resolved: {}", 
                 alarmsToInsert.size(), alarmsToUpdate.size());
    }

    private Set<String> extractNodeIds(Map<String, List<String>> allTags) {
        return allTags.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private Map<String, EquipmentAlarmDetails> prepareAlarmDetailsMap(List<EquipmentAlarmDetails> alarmDetailsList) {
        return alarmDetailsList.stream()
            .filter(e -> e.getEquipmentAlarmTag() != null)
            .collect(Collectors.toMap(
                e -> {
                    String normalizedTag = e.getEquipmentAlarmTag().replace("\"", "").trim();
                    String baseTag = extractTagBase(normalizedTag);
                    return baseTag + "_" + e.getBitNo();
                },
                Function.identity(),
                (existing, replacement) -> existing
            ));
    }

    private Map<Integer, MasterEquipmentDetailsEntity> prepareEquipmentMap(List<MasterEquipmentDetailsEntity> equipmentDetailsList) {
        return equipmentDetailsList.stream()
            .collect(Collectors.toMap(MasterEquipmentDetailsEntity::getEquipmentId, Function.identity()));
    }

    private void processNode(String nodeId, 
                             Map<String, EquipmentAlarmDetails> alarmDetailsMap, 
                             Map<Integer, MasterEquipmentDetailsEntity> equipmentMap,
                             Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                             Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {
    	
    	
    	System.out.println("0.333");

        Optional<DataValue> alarmWordOpt = opcUaService.readValue(nodeId);
        if (!alarmWordOpt.isPresent()) {
            log.trace("No value for node: {}", nodeId);
            return;
        }
        System.out.println("1.1");

        Object alarmWordObj = alarmWordOpt.get().getValue().getValue();
        String normalizedNodeId = nodeId.replace("\"", "");

        if (alarmWordObj instanceof Boolean) {
            processBooleanAlarm(normalizedNodeId, (Boolean) alarmWordObj, alarmDetailsMap, equipmentMap, alarmsToInsert, alarmsToUpdate);
        } else if (alarmWordObj instanceof ExtensionObject) {
        	
        	System.out.println("#1.2");
            processWordAlarm(normalizedNodeId, (ExtensionObject) alarmWordObj, alarmDetailsMap, equipmentMap, alarmsToInsert, alarmsToUpdate);
        } else {
            log.trace("Unsupported data type for node: {}", nodeId);
        }
    }

    private void processBooleanAlarm(String normalizedNodeId, boolean value,
                                     Map<String, EquipmentAlarmDetails> alarmDetailsMap,
                                     Map<Integer, MasterEquipmentDetailsEntity> equipmentMap,
                                     Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                                     Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {
    	
    	System.out.println("#5.1");

        String alarmKey = normalizedNodeId + "_0";
        String redisKey = getRedisKey(alarmKey);

        EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);
        System.out.println("alarmKey"+alarmKey);
        System.out.println("alarmDetail"+alarmDetail);
     
        
     
        if (alarmDetail == null) return;

        MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());
        if (equipment == null) return;

        handleAlarmChange(alarmDetail, equipment, value, redisKey, alarmsToInsert, alarmsToUpdate);
    }

    private void processWordAlarm(String normalizedNodeId, ExtensionObject extObj,
                                  Map<String, EquipmentAlarmDetails> alarmDetailsMap,
                                  Map<Integer, MasterEquipmentDetailsEntity> equipmentMap,
                                  Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                                  Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {
System.out.println("2.1");
        Object body = extObj.getBody();
        
        System.out.println("body"+body);
        if (!(body instanceof ByteString)) return;

        byte[] bytes = ((ByteString) body).bytes();
        
        System.out.println("bytes"+bytes);

        for (int i = 0; i < bytes.length; i++) {
            boolean active = (bytes[i] & 0xFF) != 0;
            String alarmKey = normalizedNodeId + "_" + i;
            String redisKey = getRedisKey(alarmKey);
           
            EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);
            System.out.println("alarmKey"+alarmKey);
            System.out.println("alarmDetail"+alarmDetail);
        
            if (alarmDetail == null) continue;


          
            MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());
            if (equipment == null) continue;

            handleAlarmChange(alarmDetail, equipment, active, redisKey, alarmsToInsert, alarmsToUpdate);
        }
    }

    private void handleAlarmChange(EquipmentAlarmDetails detail, MasterEquipmentDetailsEntity equipment,
                                   boolean isActive, String redisKey,
                                   Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                                   Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {

        boolean wasActive = Boolean.parseBoolean(redisTemplate.opsForValue().get(redisKey));
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
System.out.println("3.1");
        if (isActive && !wasActive) {
            EquipmentAlarmHistoryEntity newAlarm = createHistoryEntity(detail, equipment, now);
            redisTemplate.opsForValue().set(redisKey, "true");
            alarmsToInsert.add(newAlarm);
        } else if (!isActive && wasActive) {
            EquipmentAlarmHistoryEntity history =
                    equipmentHistoryrepo.findByEquipmentAlarmIdAndEquipmentAlarmStatusTrue(detail.getEquipmentAlarmId());
            if (history != null) {
                history.setAlarmResolvedDatetime(now);
                history.setEquipmentAlarmStatus(false);
                redisTemplate.opsForValue().set(redisKey, "false");
                alarmsToUpdate.add(history);
            }
        }
    }

    private EquipmentAlarmHistoryEntity createHistoryEntity(EquipmentAlarmDetails detail, MasterEquipmentDetailsEntity equipment, String now) {
    	
    	System.out.println("4.1");
        EquipmentAlarmHistoryEntity entity = new EquipmentAlarmHistoryEntity();
        entity.setEquipmentAlarmName(detail.getEquipmentAlarmName());
        entity.setEquipmentAlarmDesc(detail.getEquipmentAlarmDesc());
        entity.setEquipmentAlarmId(detail.getEquipmentAlarmId());
        entity.setEquipmentId(equipment.getEquipmentId());
        entity.setEquipmentName(equipment.getEquipmentName());
        entity.setEquipmentDesc(equipment.getEquipmentDesc());
        entity.setAlarmOccurredDatetime(now);
        entity.setAlarmResolvedDatetime("NA");
        entity.setEquipmentAlarmStatus(true);
        return entity;
    }

    private void persistAndCacheUpdates(Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                                        Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {

        if (!alarmsToInsert.isEmpty()) {
            equipmentHistoryrepo.saveAll(alarmsToInsert);
            cacheNewAlarms(alarmsToInsert);
        }
        if (!alarmsToUpdate.isEmpty()) {
            equipmentHistoryrepo.saveAll(alarmsToUpdate);
            cacheResolvedAlarms(alarmsToUpdate);
        }
    }

    private void cacheNewAlarms(Collection<EquipmentAlarmHistoryEntity> newAlarms) {
        List<EquipmentAlarmHistoryDto> cachedDtos =
                (List<EquipmentAlarmHistoryDto>) cacheAlarmServiceInstance.redisTemplate.opsForValue()
                        .get(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY);

        if (cachedDtos == null) cachedDtos = new ArrayList<>();

        Set<Integer> existingIds = cachedDtos.stream()
                .map(EquipmentAlarmHistoryDto::getEquipmentAlarmId)
                .collect(Collectors.toSet());

        List<EquipmentAlarmHistoryDto> newDtos = newAlarms.stream()
                .filter(a -> !existingIds.contains(a.getEquipmentAlarmId()))
                .map(a -> modelMapper.map(a, EquipmentAlarmHistoryDto.class))
                .collect(Collectors.toList());

        cachedDtos.addAll(newDtos);
        cacheAlarmServiceInstance.redisTemplate.opsForValue().set(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY, cachedDtos);
    }

    private void cacheResolvedAlarms(Collection<EquipmentAlarmHistoryEntity> resolvedAlarms) {
        List<EquipmentAlarmHistoryDto> cachedDtos =
                (List<EquipmentAlarmHistoryDto>) cacheAlarmServiceInstance.redisTemplate.opsForValue()
                        .get(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY);

        if (cachedDtos != null) {
            Set<Integer> resolvedIds = resolvedAlarms.stream()
                    .map(EquipmentAlarmHistoryEntity::getEquipmentAlarmId)
                    .collect(Collectors.toSet());

            cachedDtos.removeIf(dto -> resolvedIds.contains(dto.getEquipmentAlarmId()));
            cacheAlarmServiceInstance.redisTemplate.opsForValue().set(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY, cachedDtos);
        }
    }

    private String getRedisKey(String alarmKey) {
        return "alarmState:" + alarmKey;
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
    private List<EquipmentAlarmDetails> loadAlarmDetailsFromJson() throws StreamReadException, DatabindException, IOException {
    	  try (InputStream is = getClass().getClassLoader().getResourceAsStream("EquipmentAlarmDetails.json")) {
    	        if (is == null) {
    	            log.error("EquipmentAlarmDetails.json resource not found!");
    	            return Collections.emptyList();
    	        }
    	        return new ObjectMapper().readValue(is, new TypeReference<List<EquipmentAlarmDetails>>() {});
    }
    }

    // Loads master equipment definitions from JSON file (used to enrich alarm data)
    private List<MasterEquipmentDetailsEntity> loadEquipmentDetailsFromJson() throws StreamReadException, DatabindException, IOException {
    	try (InputStream is = getClass().getClassLoader().getResourceAsStream("EquipmentDeatails.json")) {
	        if (is == null) {
	            log.error("EquipmentAlarmDetails.json resource not found!");
	            return Collections.emptyList();
	        }
	        return new ObjectMapper().readValue(is, new TypeReference<List<MasterEquipmentDetailsEntity>>() {});
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
   

}
    
