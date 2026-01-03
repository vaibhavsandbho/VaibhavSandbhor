package com.ats.EquipmentAlarm.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ats.EquipmentAlarm.Entity.alarm.ActiveequipmentalarmsviewEntity;
import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmHistoryDto;
import com.ats.EquipmentAlarm.Entity.alarm.Resolvedequipmentalarms;
import com.ats.EquipmentAlarm.repo.alarm.activateAlaramviewRepo;
@Service
public class CacheAlarmService {

    @Autowired
	public RedisTemplate<String, Object> redisTemplate;

    // ✅ CELL PROJECT KEYS (UNIQUE)
    public static final String CELL_ACTIVE_ALARMS_KEY =
            "CELL_PROJECT:active_alarms_cache";

    public static final String CELL_RESOLVED_ALARMS_KEY =
            "CELL_PROJECT:resolved_alarms_cache";

    public void updatedActiveAlarmCache(List<EquipmentAlarmHistoryDto> alarms) {
        redisTemplate.opsForValue().set(CELL_ACTIVE_ALARMS_KEY, alarms);
        System.out.println("CELL → Updated active alarm cache: " + alarms.size());
    }

    public void updatedResolvedAlarmCache(List<Resolvedequipmentalarms> alarms) {
        redisTemplate.opsForValue().set(
                CELL_RESOLVED_ALARMS_KEY,
                alarms,
                Duration.ofMinutes(1)
        );
        System.out.println("CELL → Updated resolved alarm cache: " + alarms.size());
    }

    @SuppressWarnings("unchecked")
    public List<EquipmentAlarmHistoryDto> getCachedActivateAlarm() {
        List<EquipmentAlarmHistoryDto> cache =
                (List<EquipmentAlarmHistoryDto>)
                        redisTemplate.opsForValue().get(CELL_ACTIVE_ALARMS_KEY);

        return cache != null ? cache : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<Resolvedequipmentalarms> getCachedReslovedAlarm() {
        List<Resolvedequipmentalarms> cache =
                (List<Resolvedequipmentalarms>)
                        redisTemplate.opsForValue().get(CELL_RESOLVED_ALARMS_KEY);

        return cache != null ? cache : Collections.emptyList();
    }
}
