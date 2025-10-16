package com.ats.EquipmentAlarm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ats.EquipmentAlarm.Entity.position.MasterPositionDto;
import com.ats.EquipmentAlarm.service.MasterPositionService;

@RestController
@CrossOrigin
@RequestMapping("/masterpositionDetails")
public class MasterPositionController {
	
	
	@Autowired
	MasterPositionService masterPositionService;
	
	@GetMapping("/lockedpositionDetails")
	public  List<MasterPositionDto> getLockedPosition()
	{
		
	//	System.out.println("positionList"+masterPositionService.getAllLockedPosition());
		return masterPositionService.getAllLockedPosition();
	}
	
	
	@GetMapping("/positionMismatchDetails")
	public List<MasterPositionDto> getDataMismatchPositon()
	{
		return masterPositionService.getAllMismatchPositon();
	}
	
	
	
	}


