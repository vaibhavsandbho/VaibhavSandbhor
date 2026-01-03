package com.ats.EquipmentAlarm.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ats.EquipmentAlarm.Entity.alarm.ActiveequipmentalarmsviewEntity;
import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmHistoryDto;
import com.ats.EquipmentAlarm.Entity.alarm.Resolvedequipmentalarms;
import com.ats.EquipmentAlarm.repo.alarm.activateAlaramviewRepo;

@Service
public class CacheAlarmService {
	
	
//	@Autowired
//	
	private activateAlaramviewRepo activateAlaramviewRepoInstance;

	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	public RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.cache.redis.key-prefix}")
	
    private String redisPrefix;
	
	private String activeKey() {
	    return redisPrefix + ":active_alarms_cache";
	}

	private String resolvedKey() {
	    return redisPrefix + ":resolved_alarms_cache";
	}
	
	    public void updatedActiveAlarmCache(List<EquipmentAlarmHistoryDto> e) {
	    	 redisTemplate.opsForValue().set(activeKey(), e);
	        System.out.println("Updated active alarm cache with " + e.size() + " records.");
	    }

	    public void updatedResolvedAlarmCache(List<Resolvedequipmentalarms> resolvedalarmlist) {
	    	  redisTemplate.opsForValue().set(
	    		        resolvedKey(),
	    		        resolvedalarmlist,
	    		        Duration.ofMinutes(1)
	    		    );
	    }
	    @SuppressWarnings("unchecked")
	    public List<EquipmentAlarmHistoryDto> getCachedActivateAlarm() {
	        List<EquipmentAlarmHistoryDto> cache =
	            (List<EquipmentAlarmHistoryDto>) redisTemplate.opsForValue().get(activeKey());

	        return cache != null ? cache : Collections.emptyList();
	    }


public List<EquipmentAlarmHistoryDto> getCachedReslovedAlarm() {
	    List<EquipmentAlarmHistoryDto> cache =
	            (List<EquipmentAlarmHistoryDto>) redisTemplate.opsForValue().get(resolvedKey());

	        return cache != null ? cache : Collections.emptyList();
	    }

}
