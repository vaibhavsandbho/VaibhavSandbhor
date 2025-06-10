package com.ats.lumax.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("BrowseTag.json")) {
            if (is == null) {
                System.err.println("❌ BrowseTag not found in classpath");
                throw new FileNotFoundException("BrowseTag not found in classpath");
            }

            // Step 1: Read as List
            List<String> tagList = mapper.readValue(is, new TypeReference<List<String>>() {});
            System.out.println("✅ Loaded tag list: " + tagList.size());

            // Step 2: Convert to Map<String, Object>
            for (String tag : tagList) {
                nodeValues.put(tag, null); // Or use a default value like false/0/etc.
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to load BrowseTag: " + e.getMessage());
            throw new RuntimeException("Failed to load BrowseTag", e);
        }
    }

    public Object getValue(String nodeId) {
        return nodeValues.get(nodeId);
    }

    public Map<String, Object> getAllValues() {
        return Collections.unmodifiableMap(nodeValues);
    }
}

