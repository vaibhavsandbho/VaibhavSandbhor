package com.ats.lumax.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.ats.lumax.service.OpcUaService;
import com.ats.lumax.serviceimpl.PredefinedNodeValueService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/opcua/subscription")
@CrossOrigin(origins = "*")
public class OpcUaSubscriptionController {


    private OpcUaService opcUaService;
    
    private final PredefinedNodeValueService predefinedNodeValueService;
    
    public OpcUaSubscriptionController(PredefinedNodeValueService predefinedNodeValueService,OpcUaService opcUaService) {
        this.predefinedNodeValueService = predefinedNodeValueService;
        this.opcUaService = opcUaService;
    }
 
    
    private final Map<String, Thread> monitoringThreads = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> monitoringFlags = new ConcurrentHashMap<>();

//    @Autowired
//    private OpcUaNodeConfig opcUaNodeConfig;

    // Store the last known values for each node
    private final Map<String, DataValue> lastValues = new ConcurrentHashMap<>();

    // Add this field at the class level
    
    private final Map<String, Thread> monitorThreads = new ConcurrentHashMap<>();

    @PostMapping("/create")
    public ResponseEntity<?> createSubscription(@RequestParam String nodeId) {
        try {
            log.info("Creating subscription for node: {}", nodeId);
            
            // Read the current value to verify the node exists
            Optional<DataValue> value = opcUaService.readValue(nodeId);
            if (value.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "status", "Error",
                        "message", "Node not found or not accessible: " + nodeId
                    ));
            }

            // Store initial value
            lastValues.put(nodeId, value.get());
            log.info("Initial value for node {}: {}", nodeId, value.get().getValue().getValue());

            // The subscription will be handled by the OpcUaService's internal subscription mechanism
            // We just need to verify the node is accessible
            return ResponseEntity.ok(Map.of(
                "status", "Success",
                "message", "Node is accessible and will be monitored",
                "nodeId", nodeId,
                "currentValue", value.get().getValue().getValue()
            ));

        } catch (Exception e) {
            log.error("Error creating subscription: {}");
            e.printStackTrace();
            e.getMessage();
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "Error",
                    "message", "Failed to create subscription: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/read/{nodeId}")
    public ResponseEntity<?> readSubscribedValue(@PathVariable String nodeId) {
        try {
            log.info("Reading value for node: {}", nodeId);
            
            Optional<DataValue> value = opcUaService.readValue(nodeId);
            
            log.info("value {}",value.getClass());
            if (value.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Check if value has changed
            DataValue lastValue = lastValues.get(nodeId);
            if (lastValue == null || !lastValue.getValue().equals(value.get().getValue())) {
                log.info("Value changed for node {}: {} -> {}", 
                    nodeId, 
                    lastValue != null ? lastValue.getValue().getValue() : "null",
                    value.get().getValue().getValue());
                lastValues.put(nodeId, value.get());
            }

            return ResponseEntity.ok(Map.of(
                "status", "Success",
                "nodeId", nodeId,
                "value", value.get().getValue().getValue(),
                "timestamp", value.get().getSourceTime().getJavaTime(),
                "statusCode", value.get().getStatusCode().toString()
            ));

        } catch (Exception e) {
            log.error("Error reading subscribed value: {}",nodeId);
            e.printStackTrace();
            e.getMessage();
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "Error",
                    "message", "Failed to read value: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/browse")
    public ResponseEntity<?> browseNodes(@RequestParam(required = false) String startingNode) {
        try {
            log.info("Browsing nodes starting from: {}", startingNode);
            
            List<String> nodes = opcUaService.browseTags(startingNode);
            
            return ResponseEntity.ok(Map.of(
                "status", "Success",
                "nodes", nodes,
                "count", nodes.size()
            ));

        } catch (Exception e) {
            log.error("Error browsing nodes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "Error",
                    "message", "Failed to browse nodes: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getConnectionStatus() {
        try {
            boolean isConnected = opcUaService.isConnected();
            
            return ResponseEntity.ok(Map.of(
                "status", "Success",
                "connected", isConnected,
                "message", isConnected ? "Connected to OPC UA server" : "Disconnected from OPC UA server"
            ));

        } catch (Exception e) {
            log.error("Error getting connection status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "Error",
                    "message", "Failed to get connection status: " + e.getMessage()
                ));
        }
    }
    
    @GetMapping("/monitor/{nodeId}")
    public ResponseEntity<?> monitorValueChanges(@PathVariable String nodeId) {
        try {
            log.info("Starting value change monitoring for node: {}", nodeId);
            
            // Initial read
            Optional<DataValue> value = opcUaService.readValue(nodeId);
            if (value.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "status", "Error",
                        "message", "Node not found or not accessible: " + nodeId
                    ));
            }

            // Store initial value
            lastValues.put(nodeId, value.get());
            log.info("Initial value for node {}: {}", nodeId, value.get().getValue().getValue());

            // Start monitoring in a separate thread
            Thread monitorThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Optional<DataValue> currentValue = opcUaService.readValue(nodeId);
                        if (currentValue.isPresent()) {
                            DataValue lastValue = lastValues.get(nodeId);
                           
                            if (lastValue == null || !lastValue.getValue().equals(currentValue.get().getValue())) {
                                log.info("Value changed for node {}: {} -> {}", 
                                    nodeId, 
                                    lastValue != null ? lastValue.getValue().getValue() : "null",
                                    currentValue.get().getValue().getValue());
                                lastValues.put(nodeId, currentValue.get());
                                
                                
                                
                                
                            }
                        }
                        Thread.sleep(100); // Check every second
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Error monitoring value changes: {}", e.getMessage());
                    }
                }
            });
            monitorThread.setDaemon(true);
            monitorThread.start();

            return ResponseEntity.ok(Map.of(
                "status", "Success",
                "message", "Started monitoring value changes for node: " + nodeId,
                "initialValue", value.get().getValue().getValue()
            ));

        } catch (Exception e) {
            log.error("Error starting value monitoring: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "Error",
                    "message", "Failed to start value monitoring: " + e.getMessage()
                ));
        }
    }


    @PostMapping("/monitor")
    public ResponseEntity<?> monitorMultipleNodes() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> initialValues = new HashMap<>();
        
        // Fetch all node IDs from the predefined service
        Map<String, Object> allNodeValues = predefinedNodeValueService.getAllValues();
        

        for (String nodeId : allNodeValues.keySet()) {
            try {
                if (monitoringThreads.containsKey(nodeId)) {
                    initialValues.put(nodeId, "Already monitoring");
                    continue;
                }

                Optional<DataValue> value = opcUaService.readValue(nodeId);
                if (value.isEmpty()) {
                    initialValues.put(nodeId, "Node not found or inaccessible");
                    continue;
                }

                lastValues.put(nodeId, value.get());
                initialValues.put(nodeId, value.get().getValue().getValue());

                Thread monitorThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Optional<DataValue> currentValue = opcUaService.readValue(nodeId);
                            if (currentValue.isPresent()) {
                                DataValue lastValue = lastValues.get(nodeId);

                                if (lastValue == null || !lastValue.getValue().equals(currentValue.get().getValue())) {
                                    log.info("Node {} value changed: {} -> {}",
                                        nodeId,
                                        lastValue != null ? lastValue.getValue().getValue() : "null",
                                        currentValue.get().getValue().getValue());
                                    lastValues.put(nodeId, currentValue.get());
                                }
                            }
                            Thread.sleep(1000); // 1-second interval
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            log.error("Error monitoring node {}: {}", nodeId, e.getMessage());
                        }
                    }
                });

                monitorThread.setDaemon(true);
                monitorThread.start();
                monitoringThreads.put(nodeId, monitorThread);

            } catch (Exception e) {
                log.error("Error setting up monitoring for {}: {}", nodeId);
                 e.getMessage();
                 e.printStackTrace();
                initialValues.put(nodeId, "Error: " + e.getMessage());
            }
        }

        response.put("status", "Monitoring started");
        response.put("initialValues", initialValues);
        return ResponseEntity.ok(response);
    }
    
    

}