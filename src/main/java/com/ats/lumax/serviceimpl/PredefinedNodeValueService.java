package com.ats.lumax.serviceimpl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class PredefinedNodeValueService {

    private final Map<String, Object> nodeValues = new HashMap<>();
    @PostConstruct
    public void init() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("node_values.json")) {
            if (is == null) {
                System.err.println("❌ node_values.json not found in classpath");
                throw new FileNotFoundException("node_values.json not found in classpath");
            }
            Map<String, Object> values = mapper.readValue(is, new TypeReference<>() {});
            System.err.println("values"+values);
            nodeValues.putAll(values);
            
        } catch (IOException e) {
            System.err.println("❌ Failed to load node_values.json: " + e.getMessage());
            throw new RuntimeException("Failed to load node_values.json", e);
        }
    }

    public Object getValue(String nodeId) {
        return nodeValues.get(nodeId);
    }

    public Map<String, Object> getAllValues() {
        return Collections.unmodifiableMap(nodeValues);
    }
}

