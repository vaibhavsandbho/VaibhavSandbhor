package com.ats.lumax.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.internal.build.AllowSysOut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ats.lumax.serviceimpl.PredefinedNodeValueService;


import jakarta.annotation.PostConstruct;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/new")
@Slf4j
public class MonitorController {

    private final PredefinedNodeValueService predefinedNodeValueService;

    // Assuming you have these maps to manage monitoring threads and last values as in your original code
    public final Map<String, Thread> monitoringThreads = new ConcurrentHashMap<>();
    private final Map<String, Object> lastValues = new ConcurrentHashMap<>();
    
 
    private final Map<String, Thread> monitoringThreads1 = new ConcurrentHashMap<>();

    public MonitorController(PredefinedNodeValueService predefinedNodeValueService) {
        this.predefinedNodeValueService = predefinedNodeValueService;
    }

    
    @PostConstruct
    public void initMonitoringOnStartup() {
        monitorMultipleNodes();
    }

@PostMapping("/monitor")
    public ResponseEntity<?> monitorMultipleNodes() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> initialValues = new HashMap<>();

        // Fetch all node IDs from the predefined service
        Map<String, Object> allNodeValues = predefinedNodeValueService.getAllValues();
        
        System.out.println("IN Controller"+allNodeValues);

        for (String nodeId : allNodeValues.keySet()) {
            try {
                if (monitoringThreads.containsKey(nodeId)) {
                    initialValues.put(nodeId, "Already monitoring");
                    continue;
                }

                Object value = allNodeValues.get(nodeId);
                if (value == null) {
                    initialValues.put(nodeId, "Node not found or inaccessible");
                    continue;
                }

                lastValues.put(nodeId, value);
                initialValues.put(nodeId, value);

                // Create a thread to simulate monitoring that checks the "value" periodically
                Thread monitorThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            //  re-fetch the current value, 
                            // but since values are static in file, simulate no change or add to logic
                            Object currentValue = predefinedNodeValueService.getValue(nodeId);

                            Object lastValue = lastValues.get(nodeId);

                            if (lastValue == null || !lastValue.equals(currentValue)) {
                                System.out.printf("Node %s value changed: %s -> %s%n", nodeId, lastValue, currentValue);
                                lastValues.put(nodeId, currentValue);
                            }

                            Thread.sleep(1000); // 1-second interval
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                        	 // ✅ Only custom short message in logs
                            log.error("Error reading value for identifier:{}",nodeId);

                            // ✅ Full exception to console (optional)
                            e.printStackTrace(); // This prints full trace to console only, NOT to the log file
                           
                        }
                    }
                });

                monitorThread.setDaemon(true);
                monitorThread.start();
                monitoringThreads.put(nodeId, monitorThread);

            } catch (Exception e) {
                System.err.printf("Error setting up monitoring for %s: %s%n", nodeId);
                e.printStackTrace();
                initialValues.put(nodeId, "Error: " + e.getMessage());
            }
        }

        response.put("status", "Monitoring started");
        response.put("initialValues", initialValues);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/monitoring/stop-all")
    public ResponseEntity<?> stopMonitoring() {
        Map<String, Object> response = new HashMap<>();

        // Stop threads from monitoringThreads
        for (Map.Entry<String, Thread> entry : monitoringThreads.entrySet()) {
            Thread thread = entry.getValue();
            if (thread != null && thread.isAlive()) thread.interrupt();
        }
        monitoringThreads.clear();

        // Stop threads from monitoringThreads1
        for (Map.Entry<String, Thread> entry : monitoringThreads1.entrySet()) {
            Thread thread = entry.getValue();
            if (thread != null && thread.isAlive()) thread.interrupt();
        }
        monitoringThreads1.clear();

        lastValues.clear();
      
        response.put("status", "Monitoring stopped for all nodes");
        return ResponseEntity.ok(response);
    }


}
