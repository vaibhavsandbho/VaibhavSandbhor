package com.ats.lumax.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class KafkaBrowseService {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final Map<String, Map<String, Object>> browseDataStore = new ConcurrentHashMap<>();

    @Value("${plc.kafka.browse-topic}")
    private String browseTopic;

    public KafkaBrowseService(KafkaTemplate<String, Map<String, Object>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void processBrowseData(String nodeId, Map<String, Object> browseData) {
        try {
            Map<String, Object> previous = browseDataStore.get(nodeId);
            
            // Store the new data
            browseDataStore.put(nodeId, browseData);
            
            // If data has changed, publish to Kafka
            if (hasChanged(previous, browseData)) {
                kafkaTemplate.send(browseTopic, nodeId, browseData);
                log.info("Published update for nodeId: {}", nodeId);
            }
        } catch (Exception e) {
            log.error("Error processing browse data: {}", e.getMessage(), e);
        }
    }

    private boolean hasChanged(Map<String, Object> previous, Map<String, Object> current) {
        if (previous == null) return true;
        return !previous.equals(current);
    }

    // Get all stored browse data
    public Map<String, Map<String, Object>> getAllBrowseData() {
        return new ConcurrentHashMap<>(browseDataStore);
    }

    // Get specific node data
    public Map<String, Object> getBrowseData(String nodeId) {
        return browseDataStore.get(nodeId);
    }
} 