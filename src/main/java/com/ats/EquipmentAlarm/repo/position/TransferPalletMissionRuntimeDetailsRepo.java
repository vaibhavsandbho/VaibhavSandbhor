package com.ats.EquipmentAlarm.repo.position;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ats.EquipmentAlarm.Entity.position.TransferPalletMissionRuntimeDetailsEntity;

public interface TransferPalletMissionRuntimeDetailsRepo extends JpaRepository<TransferPalletMissionRuntimeDetailsEntity, Integer> {

	
	@Query("select m from TransferPalletMissionRuntimeDetailsEntity m where m.transferMissionStatus='IN_PROGRESS'  ")
	List<TransferPalletMissionRuntimeDetailsEntity> findBytransferMissionStatus();
}
