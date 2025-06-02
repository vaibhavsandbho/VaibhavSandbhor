package com.ats.lumax.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class PlcWriterService {
    
    private final OpcUaService opcUaService;
    private final Random random = new Random();
    
    // Create separate loggers for general info and errors
    private static final Logger log = LoggerFactory.getLogger(PlcWriterService.class);
    private static final Logger errorLog = LoggerFactory.getLogger("plcWriterErrors");
    
    @Value("${plc.writer.node-id}")
    private String nodeId;
    
    @Value("${plc.writer.enabled:false}")
    private boolean enabled;
    
    public PlcWriterService(OpcUaService opcUaService) {
        this.opcUaService = opcUaService;
    }

    @Scheduled(fixedRateString = "${plc.writer.interval:5000}")
    public void writeRandomValue() {
        if (!enabled) {
            return;
        }
        
        try {
            String randomValue = generateRandomAlphanumeric(6);
            log.info("Attempting to write value: {}", randomValue);
            
            boolean success = opcUaService.writeValue(nodeId, randomValue);
            
            if (success) {
                log.info("Successfully wrote value: {}", randomValue);
            } else {
                String errorMsg = String.format("Failed to write value: %s to node: %s", randomValue, nodeId);
                errorLog.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = String.format("Error in PLC writer: %s", e.getMessage());
            errorLog.error(errorMsg, e);
        }
    }

    private String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
} 