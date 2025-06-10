package com.ats.lumax.controller;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;


import com.ats.lumax.Entity.EquipmentAlarmDetails;
import com.ats.lumax.Entity.EquipmentAlarmHistoryEntity;
import com.ats.lumax.Entity.MasterEquipmentDetailsEntity;
import com.ats.lumax.repo.EquipmentAlaramHistoryrepo;

import com.ats.lumax.repo.EquipmetAlarmDetailsRepo;
import com.ats.lumax.repo.MasterEquipmentRepo;
import com.ats.lumax.service.OpcUaService;
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
	

	private final PredefinedNodeValueService predefinedNodeValueService;
    private final OpcUaService opcUaService;
    
    private final EquipmentAlaramHistoryrepo equipmentHistoryrepo;
    
    @Autowired
    private EquipmetAlarmDetailsRepo equipmentAlarmDetailsRepo;
             
    @Autowired
   private  MasterEquipmentRepo masterEquipmentRepo;

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
    private final Map<String, Boolean> alarmStates = new ConcurrentHashMap<>();


    /**
     * Reads all predefined OPC-UA node values every cycle and processes them to detect alarm conditions.
     * 
     * - If a Boolean node value changes from false to true (i.e., becomes active), it saves a corresponding alarm to the database.
     * - If a Boolean node value changes from true to false (i.e., gets cleared), it updates the alarm status in the database and the in-memory state.
     * 
     * This method uses an in-memory cache (alarmStates) to avoid repeated database writes and reduce load.
     */
    
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void readAndProcessValues() {
        // Retrieve all predefined node IDs and their values (typically configured tags to monitor)
        Map<String, Object> nodeValues = predefinedNodeValueService.getAllValues();
      

        // Load JSON files only once to avoid repetitive I/O in the loop
        List<EquipmentAlarmDetails> alarmDetailsList = loadAlarmDetailsFromJson();
        List<MasterEquipmentDetailsEntity> equipmentDetailsList = loadEquipmentDetailsFromJson();

        // Iterate over each nodeId to check and process its current status
        for (String nodeId : nodeValues.keySet()) {
            try {
                // Read the current value from the OPC UA server for the node
                Optional<DataValue> valueOpt = opcUaService.readValue(nodeId);
                if (valueOpt.isEmpty()) continue;

                // Extract the actual value from the OPC UA variant
                DataValue dataValue = valueOpt.get();
                Object result = dataValue.getValue() != null ? dataValue.getValue().getValue() : null;

                // Determine whether the alarm condition is active (true) or not
                boolean isTrue = result instanceof Boolean && (Boolean) result;

                // Construct Redis key for tracking alarm state
                String redisKey = "alarmState:" + nodeId;

                // Retrieve previous state from Redis cache
                String cachedState = redisTemplate.opsForValue().get(redisKey);
                boolean wasPreviouslyActive = cachedState != null && Boolean.parseBoolean(cachedState);

                // Normalize nodeId (remove quotes)
                String normalizedNodeId = nodeId.replace("\"", "");

                // Case 1: Alarm has just become active
                if (isTrue && !wasPreviouslyActive) {
                    log.info("Alarm activated for nodeId: {}", nodeId);

                    // Update Redis to mark the alarm as active
                    redisTemplate.opsForValue().set(redisKey, "true");

                    // Find alarm details for this nodeId
                    Optional<EquipmentAlarmDetails> alarmOpt = alarmDetailsList.stream()
                            .filter(e -> normalizedNodeId.equals(e.getEquipmentAlarmTag()))
                            .findFirst();

                    if (alarmOpt.isPresent()) {
                        EquipmentAlarmDetails alarmDetail = alarmOpt.get();

                        // Find corresponding equipment information based on equipmentId
                        Optional<MasterEquipmentDetailsEntity> equipmentOpt = equipmentDetailsList.stream()
                                .filter(e -> alarmDetail.getEquipmentId().equals(e.getEquipmentId()))
                                .findFirst();

                        if (equipmentOpt.isPresent()) {
                            MasterEquipmentDetailsEntity equipment = equipmentOpt.get();

                            // Create and populate alarm history record
                            EquipmentAlarmHistoryEntity alarm = new EquipmentAlarmHistoryEntity();
                            alarm.setEquipmentAlarmName(alarmDetail.getEquipmentAlarmName());
                            alarm.setEquipmentAlarmDesc(alarmDetail.getEquipmentAlarmDesc());
                            alarm.setEquipmentAlarmId(alarmDetail.getEquipmentAlarmId());
                            alarm.setEquipmentId(equipment.getEquipmentId());
                            alarm.setEquipmentName(equipment.getEquipmentName());
                            alarm.setEquipmentDesc(equipment.getEquipmentDesc());
                            alarm.setAlarmOccurredDatetime(LocalDateTime.now().toString());
                            alarm.setAlarmResolvedDatetime("NA");
                            alarm.setEquipmentAlarmStatus(true); // true = active

                            // Save the alarm history record to the database
                            equipmentHistoryrepo.save(alarm);
                            log.info("Alarm saved to DB for nodeId: {}", nodeId);
                        }
                    }

                }
                // Case 2: Alarm has just been resolved
                else if (!isTrue && wasPreviouslyActive) {
                    // Update Redis to mark the alarm as inactive
                    redisTemplate.opsForValue().set(redisKey, "false");
                    log.info("Alarm resolved for nodeId: {}", nodeId);

                    // Look up the alarm details from DB using nodeId
                    Optional<EquipmentAlarmDetails> alarmDetailOpt = equipmentAlarmDetailsRepo.findByequipmentAlarmTag(normalizedNodeId);
                    if (alarmDetailOpt.isPresent()) {
                        EquipmentAlarmDetails alarmDetail = alarmDetailOpt.get();

                        // Find existing history record for the active alarm
                        EquipmentAlarmHistoryEntity history = equipmentHistoryrepo.findbyequipmentAlarmId(alarmDetail.getEquipmentAlarmId());

                        if (history != null) {
                            // Mark alarm as resolved and update time
                            history.setAlarmResolvedDatetime(LocalDateTime.now().toString());
                            history.setEquipmentAlarmStatus(false); // false = resolved
                            equipmentHistoryrepo.save(history);
                            log.info("Alarm updated as resolved for: {}", nodeId);
                        }
                    } else {
                        log.warn("No matching alarm found in DB for nodeId: {}", nodeId);
                    }
                }

            } catch (Exception e) {
                // Catch and log any exception to avoid crashing the loop
                log.warn("Error processing nodeId {}: {}", nodeId, e.getMessage());
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
    
