package com.ats.EquipmentAlarm.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmDetails;
import com.ats.EquipmentAlarm.Entity.alarm.MasterEquipmentDetailsEntity;
import com.ats.EquipmentAlarm.config.PlcConfiguration;
import com.ats.EquipmentAlarm.repo.alarm.EquipmetAlarmDetailsRepo;
import com.ats.EquipmentAlarm.repo.alarm.MasterEquipmentRepo;
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

  

   

  
    public Optional<DataValue> readValue(String identifier) {
    	 try {
    	        
         	 DataValue value = client.readValue(0.0, TimestampsToReturn.Both, NodeId.parse(identifier)).get();
         	
             return Optional.ofNullable(value);
         } catch (Exception e) {
             log.error("Error reading value for identifier: {}", identifier, e);
             return Optional.empty();
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

    
    
   
  
   
    public void saveDataFormDb() {
        try {
            List<EquipmentAlarmDetails> list = equipmentAlarmDetailsRepo.findAll();

            ObjectMapper mapper = new ObjectMapper();

            String filePath = System.getProperty("user.dir") + "/src/main/resources/EquipmentAlarmDetails.json";
            File file = new File(filePath);

            mapper.writeValue(file, list);
            System.out.println("Data successfully written to: " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
  
    public void saveEquipmentDetails() throws StreamWriteException, DatabindException, IOException
    {
    	 
    	    List<MasterEquipmentDetailsEntity> listEquipment=masterEquipmentRepo.findAll();
    	    
    	    ObjectMapper mapper = new ObjectMapper();
    	    
    	    String filePath=System.getProperty("user.dir")+"/src/main/resources/EquipmentDetails.json";
    	    
    	    File file=new File(filePath);
    	    
    	    mapper.writeValue(file,listEquipment);
    	    System.out.println("Data successfully written to: " + file.getAbsolutePath());
    	     
    }

} 