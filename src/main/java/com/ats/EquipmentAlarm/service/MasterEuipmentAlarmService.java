package com.ats.EquipmentAlarm.service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmHistoryEntity;
import com.ats.EquipmentAlarm.repo.alarm.EquipmentAlaramHistoryrepo;
import com.google.common.base.Predicate;

@Service
public class MasterEuipmentAlarmService {
	@Autowired
	private EquipmentAlaramHistoryrepo equipmentAlaramHistoryrepoInstance;
	
	List<EquipmentAlarmHistoryEntity> list=new ArrayList();
	
	
	
	public List<EquipmentAlarmHistoryEntity> findbyFilter(String startdate, String enddate,String status) {
	   
	    if (!(startdate.equals("NA")) && !(enddate.equals("NA"))) {
			// Removinf "T" from dataetime format
			 
			// ::"+infeedMissionStartCdatetime.toString().replace("T", " "));
			String startDateTime = startdate.toString().replace("T", " ");
			// System.out.println("endDateTime::"+
			// infeedMissionEndCdatetime.toString().replace("T", " "));
			String endDateTime = enddate.toString().replace("T", " ");

			list = equipmentAlaramHistoryrepoInstance.findByalarmOccurredDatetimeBetween(startDateTime,
					endDateTime);
	
	    

		} else {
			
			list = equipmentAlaramHistoryrepoInstance.findAll();
			System.out.println("Without Date");
			System.out.println("list size"+list.size());
		}
	    
	    if (status.contentEquals("true")) {
	        list = list.stream()
	                   .filter(EquipmentAlarmHistoryEntity::getEquipmentAlarmStatus)
	                   .collect(Collectors.toList());
	    } else if (status.contentEquals("false")) {
	        list = list.stream()
	                   .filter(e -> !e.getEquipmentAlarmStatus())
	                   .collect(Collectors.toList());
	    }
	    return list;

}
	 public Page<EquipmentAlarmHistoryEntity> findActiveAlarms(int offset, int limit) {
	        int page = offset / limit; // Convert offset to page number
	        return equipmentAlaramHistoryrepoInstance.findByEquipmentAlarmStatusTrue(PageRequest.of(page, limit));
	    }
	
	 
	 public Page<EquipmentAlarmHistoryEntity> findInActiveAlarms(int offset, int limit) {
	        int page = offset / limit; // Convert offset to page number
	        return equipmentAlaramHistoryrepoInstance.findByEquipmentAlarmStatusFalse(PageRequest.of(page, limit));
	    }
}
