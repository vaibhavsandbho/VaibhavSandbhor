package com.ats.EquipmentAlarm.controller;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ats.EquipmentAlarm.service.OpcUaService;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.ZonedDateTime;
import java.util.HashMap;

@RestController
@RequestMapping("/api/plc/heartBeat")
@RequiredArgsConstructor
@Slf4j
public class PlcController {
	
	@Autowired
    private final OpcUaService opcUaService;

    @GetMapping("/status")
    public ResponseEntity<String> getConnectionStatus() {
        boolean isConnected = opcUaService.isConnected();
        return ResponseEntity.ok(isConnected ? "Connected" : "Disconnected");
    }

  
    @GetMapping("/read")
    public ResponseEntity<?> readValue(@RequestParam String nodeId) {
        try {
            Optional<DataValue> value = opcUaService.readValue(nodeId);
            if (value.isPresent()) {
                DataValue dataValue = value.get();
                Variant variant = dataValue.getValue();
                Object result = variant != null ? variant.getValue() : null;
                return ResponseEntity.ok(Map.of(
                    "value", result,
                    "timestamp", dataValue.getSourceTime().getJavaDate(),
                    "status", dataValue.getStatusCode().toString()
                ));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error reading value for nodeId: {}", nodeId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

  
    @Data
    static class WriteRequest {
        private String nodeId;
        private String value;
    }
} 