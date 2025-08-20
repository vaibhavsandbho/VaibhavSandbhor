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

    @GetMapping("/browse")
    public ResponseEntity<List<String>> browseTags(
            @RequestParam(required = false) String startingNode) {
        try {
            List<String> tags = opcUaService.browseTags(startingNode);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            log.error("Error browsing tags", e);
            return ResponseEntity.internalServerError().build();
        }
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

    @PostMapping("/write")
    public ResponseEntity<?> writeValue(@RequestBody WriteRequest request) {
        try {
            boolean success = opcUaService.writeValue(request.getNodeId(), request.getValue());
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "status", "StatusCode{name=Good, value=0x00000000, quality=good}",
                    "value", request.getValue(),
                    "timestamp", ZonedDateTime.now().toString()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "StatusCode{name=Bad, value=0x80000000, quality=bad}",
                    "value", request.getValue(),
                    "timestamp", ZonedDateTime.now().toString()
                ));
            }
        } catch (Exception e) {
            log.error("Error writing value for nodeId: {}", request.getNodeId(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "StatusCode{name=Bad, value=0x80000000, quality=bad}",
                "value", request.getValue(),
                "timestamp", ZonedDateTime.now().toString()
            ));
        }
    }

    @GetMapping("/read-node")
    public ResponseEntity<?> readValuesUnderNode(@RequestParam String nodeId) {
        try {
            Map<String, DataValue> values = opcUaService.readValuesUnderNode(nodeId);
            
            log.error("values {}",values.toString());
            
            // Transform the results into a more API-friendly format
            Map<String, Object> response = new HashMap<>();
            values.forEach((tag, dataValue) -> {
                Variant variant = dataValue.getValue();
                
                
                response.put(tag, Map.of(
                    "value", variant != null ? variant.getValue() : null,
                    "timestamp", dataValue.getSourceTime().getJavaDate(),
                    "status", dataValue.getStatusCode().toString()
                ));
            });
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reading values under nodeId: {}", nodeId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
//    @PostConstruct
//    public ResponseEntity<String> changeValue() {
//    	
//    	
//    	
//    	Thread monitorThread = null;
//        if (monitorThread != null && monitorThread.isAlive()) {
//            return ResponseEntity.ok("Monitor thread already running.");
//        }
//
//        monitorThread = new Thread(() -> {
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    Optional<DataValue> value = opcUaService.readValue("ns=3;s=\"PLC_To_WMS\".\"STKR1_Heart Bit\"");
//
//                    if (value.isPresent()) {
//                        Object result = value.get().getValue().getValue();
//                        
//
//                        boolean writeSuccess;
//                        if (result instanceof Boolean && (Boolean) result) {
//                           ;
//                            writeSuccess = opcUaService.writeValue("ns=3;s=\"WMS_TO_PLC\".\"STKR1_Heart Bit\"", "true");
//                        } else {
//                           
//                            writeSuccess = opcUaService.writeValue("ns=3;s=\"WMS_TO_PLC\".\"STKR1_Heart Bit\"", "false");
//                        }
//
//               
//                    } else {
//                        log.warn("No value present for the source tag.");
//                    }
//
//                    Thread.sleep(1000); // Wait 1 second before next read
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt(); // Restore interrupt flag
//                    log.info("Monitor thread interrupted, stopping.");
//                    break;
//                } catch (Exception e) {
//                    log.error("Error in monitor thread: {}", e.getMessage());
//                }
//            }
//        });
//
//        monitorThread.setDaemon(true);
//        monitorThread.start();
//
//        return ResponseEntity.ok("Monitor thread started.");
//    }

    @Data
    static class WriteRequest {
        private String nodeId;
        private String value;
    }
} 