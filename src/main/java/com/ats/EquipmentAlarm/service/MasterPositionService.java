package com.ats.EquipmentAlarm.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ats.EquipmentAlarm.Entity.position.MasterPositionDto;
import com.ats.EquipmentAlarm.repo.position.MasterPositionRepo;

@Service
public class MasterPositionService {
	
	@Autowired
	
	MasterPositionRepo masterPositionRepo;
	
	
	
	public List<MasterPositionDto> getAllLockedPosition()
	{
		List<MasterPositionDto> list= masterPositionRepo.findLockPosition();
		
		if(!list.isEmpty())
		{
			return list;
		}
		return null;
	}
	
	public List<MasterPositionDto> getAllMismatchPositon()
	{
		return masterPositionRepo.findMismatchPosition();
	}
	
	
	public Integer getLockPositionCount()
	{
		return masterPositionRepo.findCountLockPosition();
	}
	
	public Integer getMismatchPositioncount()
	{
		return masterPositionRepo.findMismatchPositionCount();
	}

}
