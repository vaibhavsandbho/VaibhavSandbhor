package com.ats.EquipmentAlarm.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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
import org.eclipse.milo.opcua.stack.core.types.structured.Structure;
import org.hibernate.internal.build.AllowSysOut;
import org.modelmapper.ModelMapper;

import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmDetails;
import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmHistoryDto;
import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmHistoryEntity;
import com.ats.EquipmentAlarm.Entity.alarm.MasterEquipmentDetailsEntity;
import com.ats.EquipmentAlarm.repo.alarm.EquipmentAlaramHistoryrepo;
import com.ats.EquipmentAlarm.repo.alarm.EquipmetAlarmDetailsRepo;
import com.ats.EquipmentAlarm.repo.alarm.MasterEquipmentRepo;
import com.ats.EquipmentAlarm.service.CacheAlarmService;
import com.ats.EquipmentAlarm.service.OpcUaService;
import com.ats.EquipmentAlarm.service.OpcUaValueConverter;
import com.ats.EquipmentAlarm.service.PredefinedNodeValueService;
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
    
 
    private EquipmetAlarmDetailsRepo equipmentAlarmDetailsRepo;
             
    @Autowired
   private  MasterEquipmentRepo masterEquipmentRepo;
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private CacheAlarmService cacheAlarmServiceInstance;
    
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ResourceLoader resourceLoader;
    
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
                    Thread.sleep(15000); // Run every 15 second
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
            	log.debug("Processing OPC UA node: {}", nodeId);
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
    	
    	
    	log.trace("Starting node processing for: {}", nodeId);

        Optional<DataValue> alarmWordOpt = opcUaService.readValue(nodeId);
        if (!alarmWordOpt.isPresent()) {
            log.trace("No value for node: {}", nodeId);
            return;
        }
        log.trace("Successfully read value for node: {}", nodeId);

        Object alarmWordObj = alarmWordOpt.get().getValue().getValue();
        String normalizedNodeId = nodeId.replace("\"", "");

        if (alarmWordObj instanceof Boolean) {
        	
        	
            processBooleanAlarm(normalizedNodeId, (Boolean) alarmWordObj, alarmDetailsMap, equipmentMap, alarmsToInsert, alarmsToUpdate);
        } else if (alarmWordObj instanceof ExtensionObject) {
        	
            processWordAlarm(normalizedNodeId, (ExtensionObject) alarmWordObj, alarmDetailsMap, equipmentMap, alarmsToInsert, alarmsToUpdate);
        }
        else if (alarmWordObj instanceof Boolean[]) {
        
            processWordAlarmBooleanArray(normalizedNodeId, (Boolean[]) alarmWordObj, alarmDetailsMap, equipmentMap, alarmsToInsert, alarmsToUpdate);
        }else {
            log.trace("Unsupported data type for node: {}", nodeId);
        }
    }

    private void processBooleanAlarm(String normalizedNodeId, boolean value,
                                     Map<String, EquipmentAlarmDetails> alarmDetailsMap,
                                     Map<Integer, MasterEquipmentDetailsEntity> equipmentMap,
                                     Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                                     Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {
    	
    	log.debug("Processing boolean alarm for node: {}", normalizedNodeId);

        String alarmKey = normalizedNodeId + "_0";
        String redisKey = getRedisKey(alarmKey);

        EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);
    
      

     
        if (alarmDetail == null) {   log.trace("No alarm detail found for key: {}", alarmKey);return;}

        MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());
        if (equipment == null) return;

        handleAlarmChange(alarmDetail, equipment, value, redisKey, alarmsToInsert, alarmsToUpdate);
    }

    private void processWordAlarm(String normalizedNodeId, ExtensionObject extObj,
                                  Map<String, EquipmentAlarmDetails> alarmDetailsMap,
                                  Map<Integer, MasterEquipmentDetailsEntity> equipmentMap,
                                  Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                                  Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {

        Object body = extObj.getBody();
        
        
        if (body instanceof Structure) {
        	
        	
        	
            Structure struct = (Structure) body;

            // Example: "{Alarm_0=true, Alarm_1=false, ...}"
            String structString = struct.toString();
            String cleaned = structString.replaceAll("[{}]", "");
            String[] parts = cleaned.split(",");

            for (int i = 0; i < parts.length; i++) {
                String[] kv = parts[i].trim().split("=");

                if (kv.length == 2) {
                    boolean active = Boolean.parseBoolean(kv[1].trim());

                    String alarmKey = normalizedNodeId + "_" + i;
                    String redisKey = getRedisKey(alarmKey);

                    EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);

                   

                    if (alarmDetail == null) {
                        log.trace("No alarm detail found for boolean struct key: {}", alarmKey);
                        continue;
                    }

                    MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());
                    if (equipment == null) {
                        log.trace("No equipment detail found for struct key: {}", alarmKey);
                        continue;
                    }

                    handleAlarmChange(alarmDetail, equipment, active, redisKey,
                                      alarmsToInsert, alarmsToUpdate);
                }
            }
        }

        byte[] bytes = ((ByteString) body).bytes();
       
       

        for (int i = 0; i < bytes.length; i++) {
            boolean active = (bytes[i] & 0xFF) != 0;
            String alarmKey = normalizedNodeId + "_" + i;
            String redisKey = getRedisKey(alarmKey);
           
            EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);
            
            
            
        
            if (alarmDetail == null){    
            log.trace("No alarm detail found for boolean array key: {}", alarmKey);
            continue;

            }
          
            MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());
            if (equipment == null) {
            	 log.trace("No  detail found for boolean array key: {}", alarmKey);
            continue;
            }

            handleAlarmChange(alarmDetail, equipment, active, redisKey, alarmsToInsert, alarmsToUpdate);
        }
    }
    
    
    private void processWordAlarmBooleanArray(String normalizedNodeId, Boolean[] extObj,
            Map<String, EquipmentAlarmDetails> alarmDetailsMap,
            Map<Integer, MasterEquipmentDetailsEntity> equipmentMap,
            Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
            Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {

  for (int i = 0; i < extObj.length; i++) {
  boolean active = extObj[i];
  String alarmKey = normalizedNodeId + "_" + i;
  String redisKey = getRedisKey(alarmKey);
  

  EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);
 

if (alarmDetail == null) {  log.trace("No alarm detail found for boolean array key: {}", alarmKey); continue;}



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
        
     
//        
        
       
        if (isActive && !wasActive) {
        	
            EquipmentAlarmHistoryEntity newAlarm = createHistoryEntity(detail, equipment, now);
            redisTemplate.opsForValue().set(redisKey, "true");
            alarmsToInsert.add(newAlarm);
            log.info("NEW ALARM TRIGGERED - Equipment: {}, Alarm: {}, Time: {}", 
                    equipment.getEquipmentName(), detail.getEquipmentAlarmName(), now);
        } else if (!isActive && wasActive) {
        	   EquipmentAlarmHistoryEntity history =
        	              equipmentHistoryrepo.findByEquipmentAlarmIdAndEquipmentAlarmStatusTrue(detail.getEquipmentAlarmId());
            if (history != null) {
                history.setAlarmResolvedDatetime(now);
                history.setEquipmentAlarmStatus(false);
                redisTemplate.opsForValue().set(redisKey, "false");
                alarmsToUpdate.add(history);
                log.info("ALARM RESOLVED - Equipment: {}, Alarm: {}, Time: {}", 
                        equipment.getEquipmentName(), detail.getEquipmentAlarmName(), now);
            }
        }
    }

    private EquipmentAlarmHistoryEntity createHistoryEntity(EquipmentAlarmDetails detail, MasterEquipmentDetailsEntity equipment, String now) {
    	
    	log.debug("Creating new alarm history entity for equipment: {} with alarm: {}", 
    	          equipment.getEquipmentName(), detail.getEquipmentAlarmName());
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
 //  // Loads alarm detail definitions from JSON file (used to map nodeId to alarm metadata)
    private List<EquipmentAlarmDetails> loadAlarmDetailsFromJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Resource resource = resourceLoader.getResource("classpath:EquipmentAlarmDetails.json");


        if (!resource.exists()) {

            throw new FileNotFoundException("EquipmentAlarmDetails.json not found in classpath");
        }

        try (InputStream is = resource.getInputStream()) {
            return mapper.readValue(
                is,
                new TypeReference<List<EquipmentAlarmDetails>>() {}
            );
        }
    }
        // First check external data folder
       // String externalPath = System.getProperty("user.dir") + "/data/EquipmentAlarmDetails.json";
        
//        String basePath = new File(System.getProperty("user.dir")).getAbsolutePath();
//        File externalFile = new File(basePath,"/data/EquipmentAlarmDetails.json");

//        if (externalFile.exists()) {
//            try (InputStream is = new FileInputStream(externalFile)) {
//                return mapper.readValue(is, new TypeReference<List<EquipmentAlarmDetails>>() {});
//            }
//        }
 

    private List<MasterEquipmentDetailsEntity> loadEquipmentDetailsFromJson() throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        Resource resource = resourceLoader.getResource("classpath:EquipmentDetails.json");

        if (!resource.exists()) {
            throw new FileNotFoundException("EquipmentDetails.json not found in classpath");
        }

        try (InputStream is = resource.getInputStream()) {
            return mapper.readValue(
                is,
                new TypeReference<List<MasterEquipmentDetailsEntity>>() {}
            );
        }
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
   


    