package com.ats.EquipmentAlarm.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.mapping.Collection;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ats.EquipmentAlarm.Entity.ActiveequipmentalarmsviewEntity;
import com.ats.EquipmentAlarm.Entity.EquipmentAlarmHistoryDto;
import com.ats.EquipmentAlarm.Entity.EquipmentAlarmHistoryEntity;
import com.ats.EquipmentAlarm.Entity.Resolvedequipmentalarms;
import com.ats.EquipmentAlarm.repo.EquipmentAlaramHistoryrepo;
import com.ats.EquipmentAlarm.repo.activateAlaramviewRepo;
import com.ats.EquipmentAlarm.repo.resolvedAlarmviewrepo;
@Service
public class CacheAlarmService {
	@Autowired
	 private EquipmentAlaramHistoryrepo aalrm;
	@Autowired
	
	private activateAlaramviewRepo activateAlaramviewRepoInstance;
	
	@Autowired
	private resolvedAlarmviewrepo resolvedAlarmviewrepoInstance;
	
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
	        redisTemplate.opsForValue().set(RESOLVED_ALARMS_KEY, resolvedalarmlist);
	        System.out.println("Updated resolved alarm cache with " + resolvedalarmlist.size() + " records.");
	    } 

	    public List<EquipmentAlarmHistoryDto> getCachedActivateAlarm() {
	    	
	        List<EquipmentAlarmHistoryDto> cache =
	                (List<EquipmentAlarmHistoryDto>) redisTemplate.opsForValue().get(ACTIVE_ALARMS_KEY);

	        if (cache != null && !cache.isEmpty()) {
	            System.out.println("Returning cached active alarms: " + cache.size());
	            return cache;
	        }
	        
	        
	    //    List<EquipmentAlarmHistoryEntity> history=aalrm.findAllActiveAlarms();

//	        List<ActiveequipmentalarmsviewEntity> fresh = activateAlaramviewRepoInstance.findAll();

       // List<EquipmentAlarmHistoryDto> dtoList = history.stream()
	        //        .map(entity -> modelMapper.map(entity, EquipmentAlarmHistoryDto.class))
	       //         .collect(Collectors.toList());

//	        updatedActiveAlarmCache(dtoList);
//	        System.out.println("Fetched fresh active alarms from DB: " + dtoList.size());

	        return Collections.EMPTY_LIST;
	    }

	    public List<Resolvedequipmentalarms> getCachedReslovedAlarm() {
	        List<Resolvedequipmentalarms> cache =
	                (List<Resolvedequipmentalarms>) redisTemplate.opsForValue().get(RESOLVED_ALARMS_KEY);

	        if (cache != null && !cache.isEmpty()) {
	            System.out.println("Returning cached resolved alarms: " + cache.size());
	            return cache;
	        }

//	        List<Resolvedequipmentalarms> freshData = resolvedAlarmviewrepoInstance.findAll();
//	        updatedResolvedAlarmCache(freshData);
	      

	        return Collections.EMPTY_LIST;
	    }
}
