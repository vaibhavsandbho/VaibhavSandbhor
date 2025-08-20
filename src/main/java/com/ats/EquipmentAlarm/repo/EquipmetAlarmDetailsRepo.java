package com.ats.EquipmentAlarm.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ats.EquipmentAlarm.Entity.EquipmentAlarmDetails;
import com.ats.EquipmentAlarm.Entity.EquipmentAlarmHistoryEntity;

public interface EquipmetAlarmDetailsRepo extends JpaRepository<EquipmentAlarmDetails,Integer> {
	

	Optional<EquipmentAlarmDetails> findByequipmentAlarmTag(String equipmentAlarmTag);
	
	
	

}
