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
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
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
                    readAndProcessValues();
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
    
    public void readAndProcessValues() {
        Map<String, Object> nodeValues = predefinedNodeValueService.getAllValues();

        List<EquipmentAlarmDetails> alarmDetailsList = loadAlarmDetailsFromJson();
        List<MasterEquipmentDetailsEntity> equipmentDetailsList = loadEquipmentDetailsFromJson();

        Map<String, EquipmentAlarmDetails> alarmDetailsMap = alarmDetailsList.stream()
            .collect(Collectors.toMap(e -> e.getEquipmentAlarmTag().replace("\"", ""), Function.identity()));

        Map<Integer, MasterEquipmentDetailsEntity> equipmentMap = equipmentDetailsList.stream()
            .collect(Collectors.toMap(MasterEquipmentDetailsEntity::getEquipmentId, Function.identity()));

        List<EquipmentAlarmHistoryEntity> alarmsToInsert = Collections.synchronizedList(new ArrayList<>());
        List<EquipmentAlarmHistoryEntity> alarmsToUpdate = Collections.synchronizedList(new ArrayList<>());

        try {
            nodeValues.keySet().parallelStream().forEach(nodeId -> {
                try {
                    Optional<DataValue> valueOpt = opcUaService.readValue(nodeId);
                    if (valueOpt.isEmpty()) return;

                    DataValue dataValue = valueOpt.get();
                    Object result = dataValue.getValue() != null ? dataValue.getValue().getValue() : null;
                    boolean isTrue = result instanceof Boolean && (Boolean) result;

                    String normalizedNodeId = nodeId.replace("\"", "");
                    String redisKey = "alarmState:" + normalizedNodeId;

                    String cachedState = redisTemplate.opsForValue().get(redisKey);
                    boolean wasPreviouslyActive = cachedState != null && Boolean.parseBoolean(cachedState);

                    String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    if (isTrue && !wasPreviouslyActive) {
                        log.info("Alarm activated for nodeId: {}", nodeId);
                        redisTemplate.opsForValue().set(redisKey, "true");

                        EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(normalizedNodeId);
                        if (alarmDetail == null) return;

                        MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());
                        if (equipment == null) return;

                        EquipmentAlarmHistoryEntity alarm = new EquipmentAlarmHistoryEntity();
                        alarm.setEquipmentAlarmName(alarmDetail.getEquipmentAlarmName());
                        alarm.setEquipmentAlarmDesc(alarmDetail.getEquipmentAlarmDesc());
                        alarm.setEquipmentAlarmId(alarmDetail.getEquipmentAlarmId());
                        alarm.setEquipmentId(equipment.getEquipmentId());
                        alarm.setEquipmentName(equipment.getEquipmentName());
                        alarm.setEquipmentDesc(equipment.getEquipmentDesc());
                        alarm.setAlarmOccurredDatetime(now);
                        alarm.setAlarmResolvedDatetime("NA");
                        alarm.setEquipmentAlarmStatus(true);

                        alarmsToInsert.add(alarm);

                    } else if (!isTrue && wasPreviouslyActive) {
                        log.info("Alarm resolved for nodeId: {}", nodeId);
                        redisTemplate.opsForValue().set(redisKey, "false");

                        EquipmentAlarmDetails alarmDetails = alarmDetailsMap.get(normalizedNodeId);
                        if (alarmDetails == null) return;

                        EquipmentAlarmHistoryEntity history = equipmentHistoryrepo.findbyequipmentAlarmId(alarmDetails.getEquipmentAlarmId());
                        if (history == null || !history.getEquipmentAlarmStatus()) return;

                        history.setAlarmResolvedDatetime(now);
                        history.setEquipmentAlarmStatus(false);

                        alarmsToUpdate.add(history);
                    }

                } catch (Exception e) {
                    log.warn("Error processing nodeId {}: {}", nodeId, e.getMessage());
                }
            });
        } finally {
            try {
                if (!alarmsToInsert.isEmpty()) {
                    equipmentHistoryrepo.saveAll(alarmsToInsert);
                    log.info("Inserted {} new alarms", alarmsToInsert.size());

                    List<EquipmentAlarmHistoryDto> cachedDtos =
                        (List<EquipmentAlarmHistoryDto>) cacheAlarmServiceInstance.redisTemplate.opsForValue().get(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY);

                    if (cachedDtos == null) {
                        cachedDtos = new ArrayList<>();
                    }

                    Set<Integer> existingAlarmIds = cachedDtos.stream()
                        .map(EquipmentAlarmHistoryDto::getEquipmentAlarmId)
                        .collect(Collectors.toSet());

                    List<EquipmentAlarmHistoryDto> newDtos = alarmsToInsert.stream()
                        .filter(a -> !existingAlarmIds.contains(a.getEquipmentAlarmId()))
                        .map(a -> modelMapper.map(a, EquipmentAlarmHistoryDto.class))
                        .collect(Collectors.toList());

                    cachedDtos.addAll(newDtos);

                    cacheAlarmServiceInstance.redisTemplate.opsForValue().set(
                        cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY,
                        cachedDtos,
                        Duration.ofMinutes(1)
                    );
                }

                if (!alarmsToUpdate.isEmpty()) {
                    equipmentHistoryrepo.saveAll(alarmsToUpdate);
                    log.info("Updated {} resolved alarms", alarmsToUpdate.size());

                    List<EquipmentAlarmHistoryDto> cachedDtos =
                        (List<EquipmentAlarmHistoryDto>) cacheAlarmServiceInstance.redisTemplate.opsForValue().get(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY);

                    if (cachedDtos != null) {
                        Set<Integer> resolvedAlarmIds = alarmsToUpdate.stream()
                            .map(EquipmentAlarmHistoryEntity::getEquipmentAlarmId)
                            .collect(Collectors.toSet());

                        cachedDtos.removeIf(dto -> resolvedAlarmIds.contains(dto.getEquipmentAlarmId()));

                        cacheAlarmServiceInstance.redisTemplate.opsForValue().set(
                            cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY,
                            cachedDtos,
                            Duration.ofMinutes(1)
                        );
                    }
                }
            } catch (Exception ex) {
                log.error("Error during DB or Redis update: {}", ex.getMessage(), ex);
            }
        }
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

}
    
