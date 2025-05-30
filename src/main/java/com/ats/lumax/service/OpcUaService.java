package com.ats.lumax.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.ats.lumax.plc.config.PlcConfiguration;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OpcUaService {
    private final PlcConfiguration plcConfig;
    private OpcUaClient client;
    private final Map<String, DataValue> tagValues = new ConcurrentHashMap<>();
    private final OpcUaValueConverter valueConverter;
    private final Map<String, OpcUaSubscription> activeSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, OpcUaMonitoredItem> monitoredItems = new ConcurrentHashMap<>();
    private final Map<String, Thread> monitoringThreads = new ConcurrentHashMap<>();

    @Autowired
    public OpcUaService(PlcConfiguration plcConfig, 
                       OpcUaValueConverter valueConverter) {
        this.plcConfig = plcConfig;
        this.valueConverter = valueConverter;
    }

    @PostConstruct
    public void init() {
        if (plcConfig.getOpcUa().isEnabled()) {
            try {
                connect();
                subscribeToData();
            } catch (Exception e) {
                log.error("Failed to initialize OPC UA connection", e);
            }
        }
    }

    private void connect() throws Exception {
        SecurityPolicy securityPolicy = SecurityPolicy.valueOf(plcConfig.getOpcUa().getSecurityPolicy());
        
        client = OpcUaClient.create(
            plcConfig.getOpcUa().getServerUrl(),
            endpoints -> endpoints.stream()
                .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getUri()))
                .findFirst(),
            configBuilder -> configBuilder
                .setApplicationName(LocalizedText.english("PLC Integration Client"))
                .setApplicationUri("urn:plc:client")
                .setRequestTimeout(UInteger.valueOf(plcConfig.getOpcUa().getConnectionTimeout()))
                .setIdentityProvider(createIdentityProvider())
                .build()
        );

        connectWithRetry();
    }

    private IdentityProvider createIdentityProvider() {
        String username = plcConfig.getOpcUa().getUsername();
        return (username != null && !username.isEmpty()) 
            ? new UsernameProvider(username, plcConfig.getOpcUa().getPassword())
            : new AnonymousProvider();
    }

    private void connectWithRetry() {
        int attempts = 0;
        while (attempts < plcConfig.getOpcUa().getMaxReconnectAttempts()) {
            try {
                client.connect().get();
                log.info("Connected to OPC UA server at {}", plcConfig.getOpcUa().getServerUrl());
                return;
            } catch (Exception e) {
                attempts++;
                log.error("Connection attempt {} failed", attempts, e);
                if (attempts < plcConfig.getOpcUa().getMaxReconnectAttempts()) {
                    try {
                        Thread.sleep(plcConfig.getOpcUa().getReconnectDelay());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to connect after " + attempts + " attempts");
    }

    private void subscribeToData() {
        try {
            double publishingInterval = 100.0;
            List<ReadValueId> readValueIds = new ArrayList<>();
            
            // Add tags to subscription
            if (plcConfig.getOpcUa().getTags() != null) {
                plcConfig.getOpcUa().getTags().forEach(tag -> 
                    readValueIds.add(new ReadValueId(
                        NodeId.parse(tag.getIdentifier()),
                        AttributeId.Value.uid(),
                        null,
                        QualifiedName.NULL_VALUE
                    ))
                );
            }
            
            // Add telegrams to subscription
            if (plcConfig.getOpcUa().getTelegrams() != null) {
                plcConfig.getOpcUa().getTelegrams().forEach(telegram -> 
                    readValueIds.add(new ReadValueId(
                        NodeId.parse(telegram.getIdentifier()),
                        AttributeId.Value.uid(),
                        null,
                        QualifiedName.NULL_VALUE
                    ))
                );
            }

            if (!readValueIds.isEmpty()) {
                createSubscription(publishingInterval, readValueIds);
            }
        } catch (Exception e) {
            log.error("Error setting up subscriptions", e);
        }
    }

    private void createSubscription(double publishingInterval, List<ReadValueId> readValueIds) {
        client.getSubscriptionManager().createSubscription(publishingInterval).thenAccept(subscription -> {
            List<MonitoredItemCreateRequest> requests = readValueIds.stream()
                .map(readValueId -> new MonitoredItemCreateRequest(
                    readValueId,
                    MonitoringMode.Reporting,
                    new MonitoringParameters(
                        UInteger.valueOf(readValueIds.indexOf(readValueId)),
                        publishingInterval,
                        null,
                        UInteger.valueOf(10),
                        true
                    )
                ))
                .toList();

            subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                (item, id) -> item.setValueConsumer(this::handleValueChange)
            );
            
            log.info("Successfully subscribed to {} items", readValueIds.size());
        });
    }

    private void handleValueChange(DataValue value) {
        try {
            if (value != null && value.getValue() != null) {
                String nodeId = value.getSourceTime().toString();
                tagValues.put(nodeId, value);
                
                Variant variant = value.getValue();
                log.debug("Value updated: NodeId={}, Value={}", nodeId, variant.getValue());
            }
        } catch (Exception e) {
            log.error("Error handling value change", e);
        }
    }

    public Optional<DataValue> readValue(String identifier) {
        try {
        
            DataValue value = client.readValue(0.0, TimestampsToReturn.Both, NodeId.parse(identifier)).get();
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Error reading value for identifier: {}", identifier, e);
            return Optional.empty();
        }
    }

    public List<String> browseTags(String startingNode) {
        try {
            NodeId nodeId = startingNode != null ? 
                NodeId.parse(startingNode) : 
                Identifiers.ObjectsFolder;

            List<String> tags = client.getAddressSpace().browse(nodeId).stream()
                .map(ref -> ref.getNodeId().toParseableString())
                .toList();

            return tags;
        } catch (Exception e) {
            log.error("Error browsing tags", e);
            return Collections.emptyList();
        }
    }

    public boolean isConnected() {
        try {
            return client != null && 
                   client.readValue(0.0, TimestampsToReturn.Both, 
                       Identifiers.Server_ServerStatus_CurrentTime).get() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void disconnect() {
        if (client != null) {
            try {
                client.disconnect().get();
                log.info("Disconnected from OPC UA server");
            } catch (Exception e) {
                log.error("Error disconnecting from OPC UA server", e);
            }
        }
    }

    public boolean writeValue(String identifier, String value) {
        try {
            NodeId nodeId = NodeId.parse(identifier);
            
            Optional<DataValue> currentValue = readValue(identifier);
            if (currentValue.isEmpty()) {
                log.error("Could not read current value to determine data type for nodeId={}", nodeId);
                return false;
            }

            Variant variant = convertToTargetType(value, currentValue.get().getValue());
            DataValue dataValue = new DataValue(variant, null, null);
            
            List<NodeId> nodeIds = ImmutableList.of(nodeId);
            List<DataValue> dataValues = ImmutableList.of(dataValue);
            
            List<StatusCode> statusCodes = client.writeValues(nodeIds, dataValues).get();
            StatusCode status = statusCodes.get(0);
            
            if (status.isGood()) {
                log.info("Successfully wrote '{}' to nodeId={}", value, nodeId);
                return true;
            } else {
                log.error("Failed to write value. StatusCode={}", status);
                return false;
            }
        } catch (Exception e) {
            log.error("Error writing value for identifier: {}", identifier, e);
            return false;
        }
    }

    private Variant convertToTargetType(String value, Variant currentValue) {
        Object currentObj = currentValue.getValue();
        try {
            if (currentObj instanceof Boolean) {
                return new Variant(Boolean.parseBoolean(value));
            } else if (currentObj instanceof Integer) {
                return new Variant(Integer.parseInt(value));
            } else if (currentObj instanceof Long) {
                return new Variant(Long.parseLong(value));
            } else if (currentObj instanceof Float) {
                return new Variant(Float.parseFloat(value));
            } else if (currentObj instanceof Double) {
                return new Variant(Double.parseDouble(value));
            } else if (currentObj instanceof Short) {
                return new Variant(Short.parseShort(value));
            } else if (currentObj instanceof Byte) {
                return new Variant(Byte.parseByte(value));
            } else {
                // Default to string if type is not recognized
                return new Variant(value);
            }
        } catch (NumberFormatException e) {
            log.error("Error converting value '{}' to type {}", value, currentObj.getClass().getSimpleName());
            throw e;
        }
    }

    public boolean writeValues(List<String> identifiers, List<String> values) {
        try {
            List<NodeId> nodeIds = new ArrayList<>();
            List<DataValue> dataValues = new ArrayList<>();
            
            // Process each value with its correct type
            for (int i = 0; i < identifiers.size(); i++) {
                NodeId nodeId = NodeId.parse(identifiers.get(i));
                Optional<DataValue> currentValue = readValue(identifiers.get(i));
                
                if (currentValue.isPresent()) {
                    Variant variant = convertToTargetType(values.get(i), currentValue.get().getValue());
                    dataValues.add(new DataValue(variant, null, null));
                    nodeIds.add(nodeId);
                } else {
                    log.error("Could not read current value for nodeId={}", nodeId);
                    return false;
                }
            }
            
            List<StatusCode> results = client.writeValues(nodeIds, dataValues).get();
            boolean allSuccess = results.stream().allMatch(StatusCode::isGood);
            
            if (allSuccess) {
                log.info("Successfully wrote batch values");
            } else {
                log.error("Some batch writes failed");
            }
            
            return allSuccess;
        } catch (Exception e) {
            log.error("Error writing multiple values", e);
            return false;
        }
    }

    public Map<String, DataValue> readValuesUnderNode(String startingNode) {
        Map<String, DataValue> results = new HashMap<>();
        try {
            NodeId nodeId = NodeId.parse(startingNode);
            List<String> tags = client.getAddressSpace().browse(nodeId).stream()
                .map(ref -> ref.getNodeId().toParseableString())
                .toList();

            if (!tags.isEmpty()) {
                List<NodeId> nodeIds = tags.stream()
                    .map(NodeId::parse)
                    .toList();

                List<DataValue> values = client.readValues(0.0, TimestampsToReturn.Both, nodeIds).get();

                for (int i = 0; i < tags.size(); i++) {
                    DataValue value = values.get(i);
                    if (value != null && value.getValue() != null) {
                        results.put(tags.get(i), value);
                    }
                }
            }
            
            return results;
        } catch (Exception e) {
            log.error("Error reading values under node: {}", startingNode, e);
            return results;
        }
    }
    
    public void stopMonitoring(String nodeId) throws Exception {
        try {
            // Remove monitored item
            OpcUaMonitoredItem monitoredItem = monitoredItems.remove(nodeId);
            OpcUaSubscription subscription = activeSubscriptions.get(nodeId);
            log.info("In service {}", nodeId);

            if (monitoredItem != null && subscription != null) {
                // Delete the monitored item
                subscription.deleteMonitoredItems(List.of(monitoredItem)).get();
              
                log.info("#1.1");
                // Remove subscription from map if it's no longer needed
                List<UaMonitoredItem> remainingItems = subscription.getMonitoredItems();
                boolean isEmpty = remainingItems.stream()
                    .noneMatch(item -> monitoredItems.containsValue(item));
                
                log.debug("boolean {}", isEmpty);

                if (isEmpty) {
                    client.getSubscriptionManager()
                        .deleteSubscription(subscription.getSubscriptionId())
                        .get();
                    log.info("#1.2");
                    // Remove all nodeIds pointing to this subscription
                    activeSubscriptions.entrySet().removeIf(entry ->
                        entry.getValue().getSubscriptionId().equals(subscription.getSubscriptionId())
                    );
                    log.info("Deleted subscription with ID: {}", subscription.getSubscriptionId());
                }

            } else {
                log.warn("No monitored item or subscription found for nodeId: {}", nodeId);
            }

            // Stop background thread if it exists
            Thread thread = monitoringThreads.remove(nodeId);
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                log.info("Monitoring thread interrupted for nodeId: {}", nodeId);
            }

        } catch (Exception e) {
            log.error("Failed to stop monitoring for nodeId={}", nodeId, e);
            throw new Exception("Error stopping monitoring for nodeId=" + nodeId, e);
        }
    }
    

    
} 