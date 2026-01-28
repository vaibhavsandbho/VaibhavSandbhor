package com.ats.EquipmentAlarm.service;

import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmDetails;
import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmHistoryDto;
import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmHistoryEntity;
import com.ats.EquipmentAlarm.Entity.alarm.MasterEquipmentDetailsEntity;
import com.ats.EquipmentAlarm.config.PlcConfiguration;
import com.ats.EquipmentAlarm.repo.alarm.EquipmentAlaramHistoryrepo;
import com.ats.EquipmentAlarm.repo.alarm.EquipmetAlarmDetailsRepo;
import com.ats.EquipmentAlarm.repo.alarm.MasterEquipmentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.*;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.*;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PLCService {

    private final PlcConfiguration plcConfig;
    private final EquipmentAlaramHistoryrepo historyRepo;
    private final StringRedisTemplate redisTemplate;
    private final PredefinedNodeValueService predefinedNodeValueService;
        private final EquipmentAlaramHistoryrepo equipmentHistoryrepo;
    

    private EquipmetAlarmDetailsRepo equipmentAlarmDetailsRepo;
             
  
   private  MasterEquipmentRepo masterEquipmentRepo;
  

    private OpcUaClient client;
        @Autowired
    private CacheAlarmService cacheAlarmServiceInstance;


        @Autowired
    private ModelMapper modelMapper;

    private Map<String, EquipmentAlarmDetails> alarmDetailsMap = new ConcurrentHashMap<>();
    private  Map<Integer, MasterEquipmentDetailsEntity> equipmentMap = new ConcurrentHashMap<>();

    //         // Thread-safe queues to avoid concurrency issues
        Queue<EquipmentAlarmHistoryEntity> alarmsToInsert = new ConcurrentLinkedQueue<>();
        Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate = new ConcurrentLinkedQueue<>();

    public PLCService(
            PlcConfiguration plcConfig,
            EquipmentAlaramHistoryrepo historyRepo,
            StringRedisTemplate redisTemplate,
            PredefinedNodeValueService predefinedNodeValueService,
            EquipmentAlaramHistoryrepo equipmentHistoryrepo,
            MasterEquipmentRepo masterEquipmentRepo, EquipmentAlaramHistoryrepo equipmentHistoryrepo2
    ) {
        this.plcConfig = plcConfig;
        this.historyRepo = historyRepo;
        this.redisTemplate = redisTemplate;
        this.predefinedNodeValueService = predefinedNodeValueService;
        this.equipmentHistoryrepo = equipmentHistoryrepo2;
    }

    /* ================= INIT ================= */

    @PostConstruct
    public void init() {
        try {
            loadMetadata();
            connect();
            subscribeAllAlarmTags();
        } catch (Exception e) {
            log.error("❌ PLC initialization failed", e);
        }
    }

    /* ================= CONNECTION ================= */

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



    private String getRedisKey(String alarmKey) {
        return "alarmState:" + alarmKey;
    }


        private String extractTagBase(String normalizedTag) {
        // Example input: ns=3;s=8.PLC_TO_WMS_MCP_Alarms.CH06.Alarm14
        // Should return: ns=3;s=8.PLC_TO_WMS_MCP_Alarms.CH06

        int lastDotIndex = normalizedTag.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return normalizedTag.substring(0, lastDotIndex); // remove .AlarmX or .Forward Send Time Out
        }
        return normalizedTag;
    }

    /* ================= SUBSCRIPTIONS ================= */

    private void subscribeAllAlarmTags() throws Exception {

        UaSubscription subscription =
                client.getSubscriptionManager()
                        .createSubscription(250.0).get();

        Set<String> tags =
                extractNodeIds(predefinedNodeValueService.loadAllTagFiles());



        AtomicInteger handle = new AtomicInteger(1);
        List<MonitoredItemCreateRequest> requests = new ArrayList<>();

        for (String tag : tags) {
            NodeId nodeId = NodeId.parse(tag);

            ReadValueId read = new ReadValueId(
                    nodeId,
                    AttributeId.Value.uid(),
                    null,
                    QualifiedName.NULL_VALUE
            );

            MonitoringParameters params =
                    new MonitoringParameters(
                            UInteger.valueOf(handle.getAndIncrement()),
                            250.0,
                            null,
                            UInteger.valueOf(100),
                            true
                    );

            requests.add(new MonitoredItemCreateRequest(
                    read,
                    MonitoringMode.Reporting,
                    params
            ));
               persistAndCacheUpdates(alarmsToInsert, alarmsToUpdate);
        }

        subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                (item, id) -> {
                    if (item.getStatusCode().isGood()) {
                        
                    }

                    item.setValueConsumer((i, v) ->
                            handleValueChange(
                                    i.getReadValueId().getNodeId(),
                                    v
                            )
                    );
                }
        );
    }

    /* ================= VALUE HANDLER ================= */

    private void handleValueChange(NodeId nodeId, DataValue value) {


        if (value == null || value.getValue() == null) return;

        String tag = nodeId.toParseableString().replace("\"", "");
        Object raw = value.getValue().getValue();
if (raw instanceof ExtensionObject) {
        System.out.println("#0.2");
             processWordAlarm(tag, (ExtensionObject) raw, alarmDetailsMap, equipmentMap, alarmsToInsert, alarmsToUpdate);
         }
         else if (raw instanceof Boolean) {
        	
        
           processBooleanAlarm(tag, (Boolean) raw, alarmDetailsMap, equipmentMap, alarmsToInsert, alarmsToUpdate);      } 

    }

       private void processWordAlarm(String normalizedNodeId, ExtensionObject extObj,
                                  Map<String, EquipmentAlarmDetails> alarmDetailsMap,
                                  Map<Integer, MasterEquipmentDetailsEntity> equipmentMap,
                                  Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                                  Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {

        Object body = extObj.getBody();
        
        
        if (body instanceof Structure) {
        	
        	
        	
            Structure struct = (Structure) body;

            // Example: "{Alarm_0=true, Alarm_1=false, ...}"
            String structString = struct.toString();
            String cleaned = structString.replaceAll("[{}]", "");
            String[] parts = cleaned.split(",");

            for (int i = 0; i < parts.length; i++) {
                String[] kv = parts[i].trim().split("=");

                if (kv.length == 2) {
                    boolean active = Boolean.parseBoolean(kv[1].trim());

                    String alarmKey = normalizedNodeId + "_" + i;
                    String redisKey = getRedisKey(alarmKey);

                    EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);



                    if (alarmDetail == null) {
                        log.trace("No alarm detail found for boolean struct key: {}", alarmKey);
                        continue;
                    }

                    MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());
                    if (equipment == null) {
                        log.trace("No equipment detail found for struct key: {}", alarmKey);
                        continue;
                    }

                    handleAlarmChange(alarmDetail, equipment, active,
                                      alarmsToInsert, alarmsToUpdate,redisKey);
                }
            }
        }

        byte[] bytes = ((ByteString) body).bytes();
       
       

        for (int i = 0; i < bytes.length; i++) {
            boolean active = (bytes[i] & 0xFF) != 0;
            String alarmKey = normalizedNodeId + "_" + i;
             String redisKey = getRedisKey(alarmKey);
           
            EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);
            
       

            
        
            if (alarmDetail == null){    
            log.trace("No alarm detail found for boolean array key: {}", alarmKey);
            continue;

            }
            
           
          
            MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());
            if (equipment == null) {
            	 log.trace("No  detail found for boolean array key: {}", alarmKey);
            continue;
            }

            handleAlarmChange(alarmDetail, equipment, active, alarmsToInsert, alarmsToUpdate,redisKey);
        }
    }


        private void processBooleanAlarm(String normalizedNodeId, boolean value,
                                     Map<String, EquipmentAlarmDetails> alarmDetailsMap1,
                                     Map<Integer, MasterEquipmentDetailsEntity> equipmentMap,
                                     Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                                     Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {

     
        String alarmKey = normalizedNodeId + "_0";
         String redisKey = getRedisKey(alarmKey);

         EquipmentAlarmDetails alarmDetail = alarmDetailsMap.get(alarmKey);
   

     
         if (alarmDetail == null) {   log.trace("No alarm detail found for key: {}", alarmKey);return;}

        MasterEquipmentDetailsEntity equipment = equipmentMap.get(alarmDetail.getEquipmentId());

        System.out.println("Equipment Details: " + equipment);
         if (equipment == null) return;

     

        handleAlarmChange(alarmDetail, equipment, value, alarmsToInsert, alarmsToUpdate,redisKey);

       
    }


        private void cacheNewAlarms(Collection<EquipmentAlarmHistoryEntity> newAlarms) {
        List<EquipmentAlarmHistoryDto> cachedDtos =
                (List<EquipmentAlarmHistoryDto>) cacheAlarmServiceInstance.redisTemplate.opsForValue()
                        .get(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY);

        if (cachedDtos == null) cachedDtos = new ArrayList<>();

        Set<Integer> existingIds = cachedDtos.stream()
                .map(EquipmentAlarmHistoryDto::getEquipmentAlarmId)
                .collect(Collectors.toSet());

        List<EquipmentAlarmHistoryDto> newDtos = newAlarms.stream()
                .filter(a -> !existingIds.contains(a.getEquipmentAlarmId()))
                .map(a -> modelMapper.map(a, EquipmentAlarmHistoryDto.class))
                .collect(Collectors.toList());

        cachedDtos.addAll(newDtos);
        cacheAlarmServiceInstance.redisTemplate.opsForValue().set(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY, cachedDtos);
    }


        private void cacheResolvedAlarms(Collection<EquipmentAlarmHistoryEntity> resolvedAlarms) {
        List<EquipmentAlarmHistoryDto> cachedDtos =
                (List<EquipmentAlarmHistoryDto>) cacheAlarmServiceInstance.redisTemplate.opsForValue()
                        .get(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY);

        if (cachedDtos != null) {
            Set<Integer> resolvedIds = resolvedAlarms.stream()
                    .map(EquipmentAlarmHistoryEntity::getEquipmentAlarmId)
                    .collect(Collectors.toSet());

            cachedDtos.removeIf(dto -> resolvedIds.contains(dto.getEquipmentAlarmId()));
            cacheAlarmServiceInstance.redisTemplate.opsForValue().set(cacheAlarmServiceInstance.ACTIVE_ALARMS_KEY, cachedDtos);
        }
    }

    /* ================= ALARM STATE ================= */

    private void handleAlarmChange(
        EquipmentAlarmDetails detail,
        MasterEquipmentDetailsEntity equipment,
        boolean isActive,
        Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
        Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate,
        String redisKey) {

    String now = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    boolean isCached = Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));

    /* ================= ALARM OCCURRED ================= */
    if (isActive) {

        // 🔁 Already active → ignore
        if (isCached) {
            log.debug("🔁 Duplicate alarm ignored: {}", redisKey);
            return;
        }

        EquipmentAlarmHistoryEntity newAlarm =
                createHistoryEntity(detail, equipment, now);

        // ✅ Save ONCE
        equipmentHistoryrepo.save(newAlarm);

        // 🔥 Mark active in Redis
        redisTemplate.opsForValue().set(redisKey, "1");

    }

    /* ================= ALARM RESOLVED ================= */
    else {

        // ❌ Not active → ignore
        if (!isCached) return;

        EquipmentAlarmHistoryEntity history =
                equipmentHistoryrepo
                        .findByEquipmentAlarmIdAndEquipmentAlarmStatusTrue(
                                detail.getEquipmentAlarmId());

        if (history != null) {
            history.setAlarmResolvedDatetime(now);
            history.setEquipmentAlarmStatus(false);

            equipmentHistoryrepo.save(history);

            // 🧹 Remove from Redis
            redisTemplate.delete(redisKey);

       
        }
    }
}


        private EquipmentAlarmHistoryEntity createHistoryEntity(EquipmentAlarmDetails detail, MasterEquipmentDetailsEntity equipment, String now) {
    	
 
        EquipmentAlarmHistoryEntity entity = new EquipmentAlarmHistoryEntity();
        entity.setEquipmentAlarmName(detail.getEquipmentAlarmName());
        entity.setEquipmentAlarmDesc(detail.getEquipmentAlarmDesc());
        entity.setEquipmentAlarmId(detail.getEquipmentAlarmId());
        entity.setEquipmentId(equipment.getEquipmentId());
        entity.setEquipmentName(equipment.getEquipmentName());
        entity.setEquipmentDesc(equipment.getEquipmentDesc());
        entity.setAlarmOccurredDatetime(now);
        entity.setAlarmResolvedDatetime("NA");
        entity.setEquipmentAlarmStatus(true);
        return entity;
    }
    private void persistAndCacheUpdates(Queue<EquipmentAlarmHistoryEntity> alarmsToInsert,
                                        Queue<EquipmentAlarmHistoryEntity> alarmsToUpdate) {

        if (!alarmsToInsert.isEmpty()) {
        EquipmentAlarmHistoryEntity a   = (EquipmentAlarmHistoryEntity) equipmentHistoryrepo.saveAll(alarmsToInsert);

    
        }
        if (!alarmsToUpdate.isEmpty()) {
         
         //   cacheResolvedAlarms(alarmsToUpdate);
        }
    }

    /* ================= METADATA ================= */

    private void loadMetadata() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        try (InputStream a =
                     getClass().getClassLoader()
                             .getResourceAsStream("EquipmentAlarmDetails.json")) {

            List<EquipmentAlarmDetails> alarms =
                    mapper.readValue(a,
                            mapper.getTypeFactory()
                                    .constructCollectionType(List.class, EquipmentAlarmDetails.class));


            

  prepareAlarmDetailsMap(alarms);
        }
      

        try (InputStream e =
                     getClass().getClassLoader()
                             .getResourceAsStream("EquipmentDeatails.json")) {

            List<MasterEquipmentDetailsEntity> eq =
                    mapper.readValue(e,
                            mapper.getTypeFactory()
                                    .constructCollectionType(List.class, MasterEquipmentDetailsEntity.class));

            eq.forEach(x -> equipmentMap.put(x.getEquipmentId(), x));

prepareEquipmentMap(eq);

        }

   


    }


private Map<String, EquipmentAlarmDetails> prepareAlarmDetailsMap(
        List<EquipmentAlarmDetails> alarmDetailsList) {



   alarmDetailsMap =
            alarmDetailsList.stream()
                    .filter(e -> e.getEquipmentAlarmTag() != null)
                    .collect(Collectors.toMap(
                            e -> {
                                String normalizedTag =
                                        e.getEquipmentAlarmTag().replace("\"", "").trim();
                                String baseTag = extractTagBase(normalizedTag);
                                return baseTag + "_" + e.getBitNo();
                            },
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));




    return alarmDetailsMap;
}


        private Map<Integer, MasterEquipmentDetailsEntity> prepareEquipmentMap(List<MasterEquipmentDetailsEntity> equipmentDetailsList) {
                    System.out.println("Preparing equimpent details map...");

                    
        return equipmentDetailsList.stream()
            .collect(Collectors.toMap(MasterEquipmentDetailsEntity::getEquipmentId, Function.identity()));
    }

    private Set<String> extractNodeIds(Map<String, List<String>> tags) {
        return tags.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }




    /* ================= SHUTDOWN ================= */

    @PreDestroy
    public void shutdown() {
        try {
            if (client != null) client.disconnect().get();
        } catch (Exception e) {
            log.error("❌ OPC disconnect failed", e);
        }
    }
}  