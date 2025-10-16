package com.ats.EquipmentAlarm.repo.position;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ats.EquipmentAlarm.Entity.position.OutfeedMissionRuntimeDetailsEntity;
import com.ats.EquipmentAlarm.Entity.position.TransferPalletMissionRuntimeDetailsEntity;

public interface OutfeedMissionRuntimeDetailsRepo extends JpaRepository<OutfeedMissionRuntimeDetailsEntity, Integer> {

	
	@Query("select m from OutfeedMissionRuntimeDetailsEntity m where m.outfeedMissionStatus='IN_PROGRESS'")
	List<OutfeedMissionRuntimeDetailsEntity> findByoutfeedMissionStatus();
}

