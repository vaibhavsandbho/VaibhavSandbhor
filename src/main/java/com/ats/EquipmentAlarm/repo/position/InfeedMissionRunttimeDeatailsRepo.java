package com.ats.EquipmentAlarm.repo.position;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ats.EquipmentAlarm.Entity.position.InfeedMissionRuntimeDetailsEntity;
import com.ats.EquipmentAlarm.Entity.position.OutfeedMissionRuntimeDetailsEntity;

public interface InfeedMissionRunttimeDeatailsRepo extends JpaRepository<InfeedMissionRuntimeDetailsEntity, Integer> {

	
	@Query("select m from InfeedMissionRuntimeDetailsEntity m where m.infeedMissionStatus='IN_PROGRESS'")
	List<InfeedMissionRuntimeDetailsEntity> findByinfeedMissionStatus();
}
