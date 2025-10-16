package com.ats.EquipmentAlarm.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmHistoryEntity;
import com.ats.EquipmentAlarm.repo.alarm.EquipmentAlaramHistoryrepo;
import com.ats.EquipmentAlarm.repo.alarm.EquipmetAlarmDetailsRepo;
import com.ats.EquipmentAlarm.service.MasterEuipmentAlarmService;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin("*")
public class DashBoardController {
	
	
	@Autowired
	private MasterEuipmentAlarmService masterAlarmServiceInstance;
	

	@GetMapping("/fetchEquipmentAlarmByAllFilters/{equipmentAlarmStatus}/{alarmOccurredDatetime}/{alarmOccurredDatetime1}")
	public List<EquipmentAlarmHistoryEntity> fetchEquipmentAlaramByAllFilters(
	    @PathVariable String equipmentAlarmStatus,
	    @PathVariable String alarmOccurredDatetime, 
	    @PathVariable String alarmOccurredDatetime1) {

		
		System.out.println("#557668");
	    System.out.println("In Filter method"+equipmentAlarmStatus);

	    // Handle empty or null values
//	    if ("NA".equals(alarmOccurredDatetime)) {
//	        alarmOccurredDatetime = null;
//	    }
//	    if ("NA".equals(alarmOccurredDatetime1)) {
//	        alarmOccurredDatetime1 = null;
//	    }
////	    if ("NA".equals(equipmentAlarmStatus)) {
//	        equipmentAlarmStatus = null;
//	    }

	    return masterAlarmServiceInstance.findbyFilter(alarmOccurredDatetime, alarmOccurredDatetime1, equipmentAlarmStatus);
	}

	
	@GetMapping("/fetchActivateAlarm")
	public List<EquipmentAlarmHistoryEntity> fetchbyEquipmentAlarmStatus(  @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit)
	{
		System.out.println("In controller");
		 Page<EquipmentAlarmHistoryEntity> page = masterAlarmServiceInstance.findActiveAlarms(offset, limit);
		 
		 System.out.println("ssk"+page.getContent());
	        return page.getContent();
	}
	
	@GetMapping("/fecthInActivateAlarm")
	public List<EquipmentAlarmHistoryEntity> fetchInActivateAlarm(  @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit)
	{
		System.out.println("In controller");
		 Page<EquipmentAlarmHistoryEntity> page = masterAlarmServiceInstance.findActiveAlarms(offset, limit);
		 
		 System.out.println("ssk"+page.getContent());
	        return page.getContent();
		
		
	}

	
	
	
	

}