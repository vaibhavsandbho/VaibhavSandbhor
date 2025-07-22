package com.ats.lumax.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
	@Qualifier(value="activeAlarmRedisTemplate")
	
	private RedisTemplate<String, List<ActiveequipmentalarmsviewEntity>> redisTemplate;
	@Autowired
	@Qualifier(value="resolvedAlarmRedisTemplate")
	private RedisTemplate<String,  List<Resolvedequipmentalarms>> redisTemplate1;


	private static final String REDIS_ACTIVE_ALARMS_KEY = "ACTIVE_ALARMS_CACHE";
	private static final String REDIS_RESOLVED_ALARMS_KEY = "RESOLVED_ALARMS_CACHE";

	public void updateAlarmCaches() {
	    // Fetch from DB
	    List<ActiveequipmentalarmsviewEntity> activeList = activateAlaramviewRepoInstance.findAll();
	    List<Resolvedequipmentalarms> resolvedList = resolvedAlarmviewrepoInstance.findAll();

	    // Push to Redis
	    redisTemplate.opsForValue().set(REDIS_ACTIVE_ALARMS_KEY, activeList);
	    redisTemplate1.opsForValue().set(REDIS_RESOLVED_ALARMS_KEY, resolvedList);
	}
	
	public List<ActiveequipmentalarmsviewEntity> getActiveAlarmsFromCache() {
	    return redisTemplate.opsForValue().get(REDIS_ACTIVE_ALARMS_KEY);
	}

	public List<Resolvedequipmentalarms> getResolvedAlarmsFromCache() {
	    return redisTemplate1.opsForValue().get(REDIS_RESOLVED_ALARMS_KEY);
	}

}
