package com.ats.EquipmentAlarm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ats.EquipmentAlarm.service.EquipmentAlarmviewService;
@Component
public class AlarmCacheSchedular {
	
	@Autowired
	private EquipmentAlarmviewService equipmentalarmservice;
	
	@Scheduled(fixedRate = 10*60*100)//for update cache from 10 minures
	public void refreshcache()
	{
		equipmentalarmservice.updateAlarmCaches();
	}

}
