package com.ats.EquipmentAlarm.service;

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


    public Map<String, Object> getAllValues() {
        return Collections.unmodifiableMap(nodeValues);
    }

    public Map<String, List<String>> loadAllTagFiles() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> allTags = new HashMap<>();

        // Define all tag file keys (can be extended)
        List<String> keys = List.of("Zone2AlarmsTag","Zone1AlarmsTag");

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

