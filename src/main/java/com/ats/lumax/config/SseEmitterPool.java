package com.ats.lumax.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ats.lumax.Entity.EquipmentAlarmHistoryEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;



import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ats.lumax.Entity.EquipmentAlarmHistoryEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;



import com.ats.lumax.Entity.EquipmentAlarmHistoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterPool {

    // Thread-safe list of all active emitters
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // Cache of currently active alarms to replay to new subscribers
    private final Map<String, EquipmentAlarmHistoryEntity> activeAlarmCache = new ConcurrentHashMap<>();

    /**
     * Subscribes a new SSE client and sends currently active alarms from the cache.
     */
    
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout
        emitters.add(emitter);

        // Remove emitter on completion, timeout, or error
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));

        return emitter;
    }

    /**
     * Sends an update to all active SSE clients.
     */
    public void send(EquipmentAlarmHistoryEntity alarm) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("alarm-update")
                        .data(alarm));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitters.remove(emitter); // Remove broken connection
            }
        }
    }

}

