package com.ats.lumax.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ats.lumax.Entity.EquipmentAlarmHistoryEntity;

public interface EquipmentAlaramHistoryrepo extends JpaRepository<EquipmentAlarmHistoryEntity,Integer> {
	
//	@Query("SELECT s FROM EquipmentAlarmHistoryEntity s WHERE s.equipmentAlarmName = :equipmentAlarmName")
//	EquipmentAlarmHistoryEntity findByequipmentAlarmName(@Param("equipmentAlarmName") String equipmentAlarmName);
//	
	@Query("SELECT e FROM EquipmentAlarmHistoryEntity e WHERE e.equipmentAlarmName = :alarmName AND e.equipmentAlarmStatus = true")
	EquipmentAlarmHistoryEntity findActiveAlarmByName(@Param("alarmName") String alarmName);
	
	
	@Query("SELECT e FROM EquipmentAlarmHistoryEntity e WHERE e.equipmentAlarmId = :equipmentAlarmId AND e.equipmentAlarmStatus = true")
	EquipmentAlarmHistoryEntity findbyequipmentAlarmId(@Param("equipmentAlarmId") Integer equipmentAlarmId);





}
