package com.ats.EquipmentAlarm.repo.alarm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmDetails;
import com.ats.EquipmentAlarm.Entity.alarm.EquipmentAlarmHistoryEntity;

public interface EquipmetAlarmDetailsRepo extends JpaRepository<EquipmentAlarmDetails,Integer> {
	

	Optional<EquipmentAlarmDetails> findByequipmentAlarmTag(String equipmentAlarmTag);
	
	
	

}
