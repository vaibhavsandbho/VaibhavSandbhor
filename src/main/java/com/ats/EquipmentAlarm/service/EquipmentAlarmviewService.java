package com.ats.EquipmentAlarm.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.ats.EquipmentAlarm.Entity.alarm.ActiveequipmentalarmsviewEntity;
import com.ats.EquipmentAlarm.Entity.alarm.Resolvedequipmentalarms;

@Service
public class EquipmentAlarmviewService {
	


	@Autowired
	@Qualifier(value="activeAlarmRedisTemplate")
	
	private RedisTemplate<String, List<ActiveequipmentalarmsviewEntity>> redisTemplate;
	@Autowired
	@Qualifier(value="resolvedAlarmRedisTemplate")
	private RedisTemplate<String,  List<Resolvedequipmentalarms>> redisTemplate1;


	private static final String REDIS_ACTIVE_ALARMS_KEY = "ACTIVE_ALARMS_CACHE";
	private static final String REDIS_RESOLVED_ALARMS_KEY = "RESOLVED_ALARMS_CACHE";

//
	
	public List<ActiveequipmentalarmsviewEntity> getActiveAlarmsFromCache() {
	    return redisTemplate.opsForValue().get(REDIS_ACTIVE_ALARMS_KEY);
	}

	public List<Resolvedequipmentalarms> getResolvedAlarmsFromCache() {
	    return redisTemplate1.opsForValue().get(REDIS_RESOLVED_ALARMS_KEY);
	}

}
