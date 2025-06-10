package com.ats.lumax.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ats.lumax.Entity.EquipmentAlarmDetails;
import com.ats.lumax.Entity.EquipmentAlarmHistoryEntity;

public interface EquipmetAlarmDetailsRepo extends JpaRepository<EquipmentAlarmDetails,Integer> {
	

	Optional<EquipmentAlarmDetails> findByequipmentAlarmTag(String equipmentAlarmTag);
	
	
	

}
