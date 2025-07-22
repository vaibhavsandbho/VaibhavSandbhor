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

    public Map<String, List<String>> loadAllTagFiles() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> allTags = new HashMap<>();

        // Define all tag file keys (can be extended)
        List<String> keys = List.of("BrowseTag");

        for (String key : keys) {
            String fileName = key + ".json";

            try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (is == null) {
                    System.err.println("⚠️ File not found: " + fileName);
                    allTags.put(key, List.of());
                    continue;
                }

                List<String> tagList = mapper.readValue(is, new TypeReference<List<String>>() {});
                allTags.put(key, tagList);
                System.out.println("✅ Loaded " + tagList.size() + " tags from " + fileName);

            } catch (IOException e) {
                System.err.println("❌ Error reading file: " + fileName);
                e.printStackTrace();
                allTags.put(key, List.of()); // Fallback to empty list on error
            }
        }

        return allTags;
    }

}

