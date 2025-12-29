package com.ats.EquipmentAlarm.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFutureAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ats.EquipmentAlarm.Entity.position.InfeedMissionRuntimeDetailsEntity;
import com.ats.EquipmentAlarm.Entity.position.OutfeedMissionRuntimeDetailsEntity;
import com.ats.EquipmentAlarm.Entity.position.TransferPalletMissionRuntimeDetailsEntity;
import com.ats.EquipmentAlarm.repo.position.InfeedMissionRunttimeDeatailsRepo;
import com.ats.EquipmentAlarm.repo.position.OutfeedMissionRuntimeDetailsRepo;
import com.ats.EquipmentAlarm.repo.position.TransferPalletMissionRuntimeDetailsRepo;
import com.ats.EquipmentAlarm.service.OpcUaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/plc")
@RequiredArgsConstructor
@Slf4j
public class StackerHealthController {
	@Autowired
    private final OpcUaService opcUaService;
	@Autowired
	OutfeedMissionRuntimeDetailsRepo outfeedMissionRuntimeDetailsRepoInstance;
	@Autowired
	InfeedMissionRunttimeDeatailsRepo InfeedMissionRunttimeDeatailsRepoInstnace;
	@Autowired
	TransferPalletMissionRuntimeDetailsRepo  TransferPalletMissionRuntimeDetailsRepoInstance;
 
    @GetMapping("/status")
    public ResponseEntity<String> getConnectionStatus() {
        boolean isConnected = opcUaService.isConnected();
        return ResponseEntity.ok(isConnected ? "Connected" : "Disconnected");
    }
        @GetMapping("/read")
        public ResponseEntity<?> readMultipleStackerModes() {
            try {
                List<String> logs = new ArrayList<>();
                List<Map<String, Object>> stackerResults = new ArrayList<>();

                // Define node IDs for each stacker
                Map<String, String> stackerNodes = Map.of(
                        "STKR1", "ns=3;s=\"0.PLC_TO_WMS\".\"STKR1_Control mode\"",
                        "STKR2", "ns=3;s=\"0.PLC_TO_WMS\".\"STKR2_Control mode\""
//                        "STKR3", "ns=3;s=\"0.PLC_TO_WMS\".\"STKR3_Control mode\""
                );

                // Loop over each stacker and read its control mode
                for (Map.Entry<String, String> entry : stackerNodes.entrySet()) {
                    String stackerName = entry.getKey();
                    String nodeId = entry.getValue();

                    Optional<DataValue> valueOpt = opcUaService.readValue(nodeId);
                    if (valueOpt.isPresent()) {
                        DataValue dataValue = valueOpt.get();
                        Variant variant = dataValue.getValue();
                        Object result = variant != null ? variant.getValue() : null;

                        int controlMode = (result instanceof Number) ? ((Number) result).intValue() : -1;
                        String modeLabel = switch (controlMode) {
                            case 1 -> "Manual";
                            case 2 -> "Semiauto";
                            case 3 -> "Auto";
                            default -> "Unknown";
                        };

                        logs.add(String.format("%s → Control Mode: %s (%d)", stackerName, modeLabel, controlMode));

                        // If control mode = 2 (Semiauto), fetch mission data
                        if (controlMode == 2 ||controlMode ==1) {
                            List<OutfeedMissionRuntimeDetailsEntity> outfeedList =
                                    outfeedMissionRuntimeDetailsRepoInstance.findByoutfeedMissionStatus();

                            List<InfeedMissionRuntimeDetailsEntity> infeedList =
                                    InfeedMissionRunttimeDeatailsRepoInstnace.findByinfeedMissionStatus();

                            List<TransferPalletMissionRuntimeDetailsEntity> transferList =
                                    TransferPalletMissionRuntimeDetailsRepoInstance.findBytransferMissionStatus();

                            // Log missions
                            for (OutfeedMissionRuntimeDetailsEntity o : outfeedList) {
                                String msg = String.format(
                                        "%s | Mode: Semiauto | MissionId: %s | PositionId: %s | Source: Outfeed | positioname",
                                        stackerName, o.getOutfeedMissionId(), o.getPositionId(),o.getPositionName());
                                log.info(msg);
                                logs.add(msg);
                            }

                            for (InfeedMissionRuntimeDetailsEntity i : infeedList) {
                                String msg = String.format(
                                        "%s | Mode: Semiauto | MissionId: %s | PositionId: %s | Source: Infeed",
                                        stackerName, i.getInfeedMissionId(), i.getPositionId());
                                log.info(msg);
                                logs.add(msg);
                            }

                            for (TransferPalletMissionRuntimeDetailsEntity t : transferList) {
                                String msg = String.format(
                                        "%s | Mode: Semiauto | MissionId: %s | PositionId: %s | Source: Transfer",
                                        stackerName, t.getPreviousePositionId(), t.getTransferPositionId());
                                log.info(msg);
                                logs.add(msg);
                            }

                            if (outfeedList.isEmpty() && infeedList.isEmpty() && transferList.isEmpty()) {
                                String msg = String.format("%s | Mode: Semiauto | No active missions found.", stackerName);
                                log.info(msg);
                                logs.add(msg);
                            }
                        }

                        // Collect individual stacker info for response
                        stackerResults.add(Map.of(
                                "stacker", stackerName,
                                "controlMode", controlMode,
                                "modeLabel", modeLabel,
                                "timestamp", dataValue.getSourceTime().getJavaDate(),
                                "status", dataValue.getStatusCode().toString()
                        ));
                    } else {
                        logs.add(stackerName + " → Failed to read value or node not found.");
                    }
                }

                // ✅ Return combined result and logs to Postman
                return ResponseEntity.ok(Map.of(
                        "stackerResults", stackerResults,
                        "logs", logs
                ));

            } catch (Exception e) {
                log.error("Error reading control modes for stackers", e);
                return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
            }
        }


    }





