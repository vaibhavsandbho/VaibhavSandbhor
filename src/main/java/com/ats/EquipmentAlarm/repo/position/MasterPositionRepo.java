package com.ats.EquipmentAlarm.repo.position;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ats.EquipmentAlarm.Entity.position.MasterPositionDetailsEntity;
import com.ats.EquipmentAlarm.Entity.position.MasterPositionDto;

public interface MasterPositionRepo extends JpaRepository<MasterPositionDetailsEntity, Integer> {
	


	
	


	
	@Query("SELECT new com.ats.EquipmentAlarm.Entity.position.MasterPositionDto(" +
		       "f.positionId, f.rackId, f.areaId, f.floorId, f.positionName, f.emptyPalletPosition, f.positionIsActive, f.isDataMismatch) " +
		       "FROM MasterPositionDetailsEntity f " +
		       "WHERE f.positionIsActive = 0")
	public	List<MasterPositionDto> findLockPosition();
	
	@Query("SELECT new com.ats.EquipmentAlarm.Entity.position.MasterPositionDto(" +
		       "f.positionId, f.rackId, f.areaId, f.floorId, f.positionName, f.emptyPalletPosition, f.positionIsActive, f.isDataMismatch) " +
		       "FROM MasterPositionDetailsEntity f " +
		       "WHERE f.isDataMismatch = 1")
	public	List<MasterPositionDto> findMismatchPosition();
	
	
	@Query("SELECT COUNT(m) FROM MasterPositionDetailsEntity m WHERE m.positionIsActive = 0 ")
	public	Integer findCountLockPosition();
	
	
	@Query("SELECT COUNT(m) FROM MasterPositionDetailsEntity m WHERE m.isDataMismatch=2")
	int findMismatchPositionCount();
	
}
