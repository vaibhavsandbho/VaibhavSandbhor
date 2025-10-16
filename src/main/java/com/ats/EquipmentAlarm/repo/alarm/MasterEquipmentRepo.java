package com.ats.EquipmentAlarm.repo.alarm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ats.EquipmentAlarm.Entity.alarm.MasterEquipmentDetailsEntity;

public interface MasterEquipmentRepo extends JpaRepository<MasterEquipmentDetailsEntity, Integer> {
	@Query("select e from MasterEquipmentDetailsEntity e where e.equipmentId=equipmentId")
	MasterEquipmentDetailsEntity	findbyequipmentId(@Param("equipmentId") int equipmentId);
	
	

 
}
