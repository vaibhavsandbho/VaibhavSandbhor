package com.ats.EquipmentAlarm.service;

import lombok.extern.slf4j.Slf4j;

import org.apache.juli.logging.Log;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.ats.EquipmentAlarm.Entity.EquipmentAlarmDetails;
import com.ats.EquipmentAlarm.Entity.EquipmentAlarmHistoryEntity;
import com.ats.EquipmentAlarm.Entity.MasterEquipmentDetailsEntity;
import com.ats.EquipmentAlarm.config.KeyStoreLoader;
import com.ats.EquipmentAlarm.config.PlcConfiguration;
import com.ats.EquipmentAlarm.repo.EquipmentAlaramHistoryrepo;
import com.ats.EquipmentAlarm.repo.EquipmetAlarmDetailsRepo;
import com.ats.EquipmentAlarm.repo.MasterEquipmentRepo;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;


@Service
@Slf4j
public class OpcUaService {
    private final PlcConfiguration plcConfig;
    
    
    private EquipmentAlaramHistoryrepo equpmentalarmHistoryrepo;
  
    private OpcUaClient client;
    @Autowired
    private EquipmetAlarmDetailsRepo equipmentAlarmDetailsRepo;
    @Autowired
    private  MasterEquipmentRepo masterEquipmentRepo;
    private final Map<String, DataValue> tagValues = new ConcurrentHashMap<>();
//    private final KafkaBrowseService kafkaBrowseService;
    private final OpcUaValueConverter valueConverter;

    @Autowired
    public OpcUaService(PlcConfiguration plcConfig, 
//                       KafkaBrowseService kafkaBrowseService,
                       OpcUaValueConverter valueConverter
                        ) {
        this.plcConfig = plcConfig;
//        this.kafkaBrowseService = kafkaBrowseService;
        
     
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
        System.out.println("Starting OPC UA connection...");

        SecurityPolicy securityPolicy = SecurityPolicy.valueOf(plcConfig.getOpcUa().getSecurityPolicy());
        MessageSecurityMode securityMode = MessageSecurityMode.valueOf(plcConfig.getOpcUa().getSecurityMode());

        System.out.println("SecurityPolicy: " + securityPolicy + ", SecurityMode: " + securityMode);

        Path securityDir = Paths.get("security");
        Files.createDirectories(securityDir);

        // Load or create the client certificate
        KeyStoreLoader loader = new KeyStoreLoader().load(securityDir);
        System.out.println("Client certificate loaded. ApplicationUri: " + loader.getApplicationUri());

        // Build OPC UA client
        client = OpcUaClient.create(
            plcConfig.getOpcUa().getServerUrl(),
            endpoints -> endpoints.stream()
                .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getUri()))
                .filter(e -> e.getSecurityMode().equals(securityMode))
                .findFirst(),
            configBuilder -> configBuilder
                .setApplicationName(LocalizedText.english("PLC Integration Client"))
                .setApplicationUri(loader.getApplicationUri())  // MUST match certificate
                .setKeyPair(loader.getClientKeyPair())
                .setCertificate(loader.getClientCertificate())
                .setCertificateChain(loader.getClientCertificateChain())
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


//    private IdentityProvider createIdentityProvider() {
//        String username = plcConfig.getOpcUa().getUsername();
//        return (username != null && !username.isEmpty()) 
//            ? new UsernameProvider(username, plcConfig.getOpcUa().getPassword())
//            : new AnonymousProvider();
//    }

    private void connectWithRetry() throws Exception {
        int attempts = 0;
        while (attempts < plcConfig.getOpcUa().getMaxReconnectAttempts()) {
            try {
                client.connect().get();
                System.out.println("✅ Connected to PLC: " + plcConfig.getOpcUa().getServerUrl());
                return;
            } catch (Exception ex) {
                attempts++;
                System.err.println("❌ Connection failed (attempt " + attempts + "): " + ex.getMessage());
                if (attempts >= plcConfig.getOpcUa().getMaxReconnectAttempts()) throw ex;
                Thread.sleep(plcConfig.getOpcUa().getReconnectDelay());
            }
        }
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
                String nodeId = value.getSourceTime().toString(); // You might want to modify this based on your needs
                tagValues.put(nodeId, value);
                
                Variant variant = value.getValue();
                log.debug("Value updated: NodeId={}, Value={}", nodeId, variant.getValue());
                
                // Publish to Kafka when value changes
                publishToBrowseData(nodeId, value);
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

            // For each tag found, read its value and publish to Kafka
            tags.forEach(tag -> {
                Optional<DataValue> value = readValue(tag);
                value.ifPresent(dataValue -> publishToBrowseData(tag, dataValue));
            });

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
            
            // First read the current value to determine its data type
            Optional<DataValue> currentValue = readValue(identifier);
            if (currentValue.isEmpty()) {
                log.error("Could not read current value to determine data type for nodeId={}", nodeId);
                return false;
            }

            // Convert the input string to the correct data type
            Variant variant = convertToTargetType(value, currentValue.get().getValue());
            DataValue dataValue = new DataValue(variant, null, null);
            
            // Create lists for batch write operation
            List<NodeId> nodeIds = ImmutableList.of(nodeId);
            List<DataValue> dataValues = ImmutableList.of(dataValue);
            
            // Write values and wait for result
            List<StatusCode> statusCodes = client.writeValues(nodeIds, dataValues).get();
            StatusCode status = statusCodes.get(0);
            
            if (status.isGood()) {
               
                
                // Read back the value to verify and publish to Kafka
                Optional<DataValue> readBack = readValue(identifier);
                readBack.ifPresent(readValue -> publishToBrowseData(identifier, readValue));
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
     private void publishToBrowseData(String identifier, DataValue dataValue) {
        try {
            Map<String, Object> browseData = new HashMap<>();
            browseData.put("nodeId", identifier);
            browseData.put("value", valueConverter.convertValue(dataValue.getValue()));
            browseData.put("status", dataValue.getStatusCode().toString());
            browseData.put("timestamp", Instant.now().toString());
            browseData.put("sourceTimestamp", dataValue.getSourceTime() != null ? 
                dataValue.getSourceTime().getJavaTime() : null);
            browseData.put("serverTimestamp", dataValue.getServerTime() != null ? 
                dataValue.getServerTime().getJavaTime() : null);
            
//            kafkaBrowseService.processBrowseData(identifier, browseData);
        } catch (Exception e) {
            log.error("Error publishing browse data for identifier: {}", identifier, e);
        }
    }

    public Map<String, DataValue> readValuesUnderNode(String startingNode) {
        Map<String, DataValue> results = new HashMap<>();
        try {
            List<String> tags = browseTags(startingNode);
            List<NodeId> nodeIds = tags.stream()
                .map(NodeId::parse)
                .toList();                 
            
            if (!nodeIds.isEmpty()) {
                List<DataValue> values = client.readValues(0.0, TimestampsToReturn.Both, nodeIds).get();
                
                for (int i = 0; i < tags.size(); i++) {
                    DataValue originalValue = values.get(i);
                    DataValue convertedValue = valueConverter.convertDataValue(originalValue);
                    results.put(tags.get(i), convertedValue);
                    publishToBrowseData(tags.get(i), convertedValue);
                }
            }
            
            return results;
        } catch (Exception e) {
            log.error("Error reading values under node: {}", startingNode, e);
            return results;
        }
    }
    
 

    public void saveDataFormDb() {
        try {
            List<EquipmentAlarmDetails> list = equipmentAlarmDetailsRepo.findAll();
            ObjectMapper mapper = new ObjectMapper();

            // Save file outside the JAR (in current working dir "data" folder)
            String filePath = System.getProperty("user.dir") + "/data/EquipmentAlarmDetails.json";
            File file = new File(filePath);

            // Ensure parent dirs exist
            file.getParentFile().mkdirs();

            mapper.writeValue(file, list);
            System.out.println("Data successfully written to: " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    public void saveEquipmentDetails() throws StreamWriteException, DatabindException, IOException
    {
    	List<EquipmentAlarmDetails> list = equipmentAlarmDetailsRepo.findAll();
        ObjectMapper mapper = new ObjectMapper();
    	    
        // Save file outside the JAR (in current working dir "data" folder)
        String filePath = System.getProperty("user.dir") + "/data/EquipmentDetails.json";
        File file = new File(filePath);

        // Ensure parent dirs exist
        file.getParentFile().mkdirs();
    	   
    	    
    	    mapper.writeValue(file,list);
    	    System.out.println("Data successfully written to: " + file.getAbsolutePath());
    	     
    }
    
    
    public void RetriveDatafromDb()
    {
    	List<EquipmentAlarmHistoryEntity> alarm=equpmentalarmHistoryrepo.findAllActiveAlarms();
    }

} 