package com.ats.EquipmentAlarm.Entity.alarm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyUtil {

    @Value("${app.redis.prefix}")
    private String prefix;

    public String key(String baseKey) {
        return prefix + ":" + baseKey;
    }
}
