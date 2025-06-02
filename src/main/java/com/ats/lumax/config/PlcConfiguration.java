package com.ats.lumax.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "plc")
public class PlcConfiguration {
    private OpcUa opcUa = new OpcUa();

    @Data
    public static class OpcUa {
        private boolean enabled = true;
        private String serverUrl;
        private String securityPolicy = "None";
        private String username;
        private String password;
        private int connectionTimeout = 5000;
        private int maxReconnectAttempts = 3;
        private long reconnectDelay = 5000;
        private List<Tag> tags;
        private List<Telegram> telegrams;
    }

    @Data
    public static class Tag {
        private String name;
        private String identifier;
        private String dataType;
    }

    @Data
    public static class Telegram {
        private String name;
        private String identifier;
        private String dataType;
        private int length;  // Length of the telegram message
        private String format; // Format specification (if needed)
        private boolean isArray; // Whether this telegram contains array data
    }
} 