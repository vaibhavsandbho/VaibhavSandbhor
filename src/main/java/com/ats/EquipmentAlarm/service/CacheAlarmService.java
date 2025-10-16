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
	
	
//	@Autowired
//	
	private activateAlaramviewRepo activateAlaramviewRepoInstance;

	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	public RedisTemplate<String, Object> redisTemplate;
	 public static final String ACTIVE_ALARMS_KEY = "active_alarms_cache";
	    public static final String RESOLVED_ALARMS_KEY = "resolved_alarms_cache";
	    public void updatedActiveAlarmCache(List<EquipmentAlarmHistoryDto> e) {
	        redisTemplate.opsForValue().set(ACTIVE_ALARMS_KEY, e);
	        System.out.println("Updated active alarm cache with " + e.size() + " records.");
	    }

	    public void updatedResolvedAlarmCache(List<Resolvedequipmentalarms> resolvedalarmlist) {
	        redisTemplate.opsForValue().set(RESOLVED_ALARMS_KEY, resolvedalarmlist, Duration.ofMinutes(1));
	        System.out.println("Updated resolved alarm cache with " + resolvedalarmlist.size() + " records.");
	    }

	    public List<EquipmentAlarmHistoryDto> getCachedActivateAlarm() {
	        try {
	            // Refresh cache expiration
	            if (redisTemplate.hasKey(ACTIVE_ALARMS_KEY)) {
	               // redisTemplate.expire(ACTIVE_ALARMS_KEY, Duration.ofMinutes(1));
	            }

	            // Try to get cached alarms
	            List<EquipmentAlarmHistoryDto> cache =
	                    (List<EquipmentAlarmHistoryDto>) redisTemplate.opsForValue().get(ACTIVE_ALARMS_KEY);

	            if (cache != null && !cache.isEmpty()) {
	                System.out.println("Returning cached active alarms: " + cache.size());
	                return cache;
	            }

//	            // Fetch fresh data from DB
//	            List<ActiveequipmentalarmsviewEntity> fresh = activateAlaramviewRepoInstance.findAll();
//	            if (fresh == null || fresh.isEmpty()) {
//	                return Collections.emptyList(); // Return empty list if DB returns null or empty
//	            }
//
//	            // Map entities to DTO
//	            List<EquipmentAlarmHistoryDto> dtoList = fresh.stream()
//	                    .map(entity -> modelMapper.map(entity, EquipmentAlarmHistoryDto.class))
//	                    .collect(Collectors.toList());
//
//	            // Update cache
	            updatedActiveAlarmCache(cache);

	            return cache;

	        } catch (Exception e) {
	            // Log error and return empty list
	            System.err.println("Error fetching active alarms: " + e.getMessage());
	            e.printStackTrace();
	            return Collections.emptyList();
	        }
	    }

//
	    public List<Resolvedequipmentalarms> getCachedReslovedAlarm() {
	        List<Resolvedequipmentalarms> cache =
	                (List<Resolvedequipmentalarms>) redisTemplate.opsForValue().get(RESOLVED_ALARMS_KEY);

	        if (cache != null && !cache.isEmpty()) {
	            System.out.println("Returning cached resolved alarms: " + cache.size());
	            return cache;
	        }
//
//	        List<Resolvedequipmentalarms> freshData = resolvedAlarmviewrepoInstance.findAll();
//	        updatedResolvedAlarmCache(freshData);
//	        System.out.println("Fetched fresh resolved alarms from DB: " + freshData.size());

	        return cache;
	    }
}
