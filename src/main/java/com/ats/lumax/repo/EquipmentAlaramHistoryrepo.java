package com.ats.lumax.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


	List<EquipmentAlarmHistoryEntity> findByalarmOccurredDatetimeBetween(String startDateTime, String endDateTime);
	
	  Page<EquipmentAlarmHistoryEntity> findByEquipmentAlarmStatusTrue(Pageable pageable);
	  
	  Page<EquipmentAlarmHistoryEntity> findByEquipmentAlarmStatusFalse(Pageable pageable);
	  
	 @Query("select e FROM EquipmentAlarmHistoryEntity e WHERE e.equipmentAlarmStatus=:equipmentAlarmStatus")
	  List<EquipmentAlarmHistoryEntity> findByEquipmentAlarmStatus(@Param ("equipmentAlarmStatus")  String equipmentAlarmStatus);







}
