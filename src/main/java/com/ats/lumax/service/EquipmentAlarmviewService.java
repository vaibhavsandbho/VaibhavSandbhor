package com.ats.lumax.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.ats.lumax.Entity.ActiveequipmentalarmsviewEntity;
import com.ats.lumax.Entity.Resolvedequipmentalarms;
import com.ats.lumax.repo.activateAlaramviewRepo;
import com.ats.lumax.repo.resolvedAlarmviewrepo;
@Service
public class EquipmentAlarmviewService {

    @Autowired
    private activateAlaramviewRepo activateAlaramviewRepoInstance;

    @Autowired
    private resolvedAlarmviewrepo resolvedAlarmviewrepoInstance;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_ACTIVE_ALARMS_KEY = "ACTIVE_ALARMS_CACHE";
    private static final String REDIS_RESOLVED_ALARMS_KEY = "RESOLVED_ALARMS_CACHE";

    public void updateAlarmCaches() {
        // Fetch from DB
        List<ActiveequipmentalarmsviewEntity> activeList = activateAlaramviewRepoInstance.findAll();
        List<Resolvedequipmentalarms> resolvedList = resolvedAlarmviewrepoInstance.findAll();

        // Store in Redis
        redisTemplate.opsForValue().set(REDIS_ACTIVE_ALARMS_KEY, activeList);
        redisTemplate.opsForValue().set(REDIS_RESOLVED_ALARMS_KEY, resolvedList);
    }

    @SuppressWarnings("unchecked")
    public List<ActiveequipmentalarmsviewEntity> getActiveAlarmsFromCache() {
        return (List<ActiveequipmentalarmsviewEntity>) redisTemplate.opsForValue().get(REDIS_ACTIVE_ALARMS_KEY);
    }

    @SuppressWarnings("unchecked")
    public List<Resolvedequipmentalarms> getResolvedAlarmsFromCache() {
        return (List<Resolvedequipmentalarms>) redisTemplate.opsForValue().get(REDIS_RESOLVED_ALARMS_KEY);
    }
}

