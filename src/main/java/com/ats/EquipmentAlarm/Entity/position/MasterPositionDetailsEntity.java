package com.ats.EquipmentAlarm.Entity.position;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ats_wms_master_position_details")
public class MasterPositionDetailsEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "POSITION_ID")
	private int positionId;

	@Column(name = "RACK_ID")
	private int rackId;

	@Column(name = "AREA_ID")
	private int areaId;

	@Column(name = "FLOOR_ID")
	private int floorId;

	@Column(name = "POSITION_NAME")
	private String positionName;

	@Column(name = "POSITION_DESC")
	private String positionDesc;

	@Column(name = "POSITION_NUMBER_IN_RACK")
	private int positionNumberInRack;
	
	
	@Column(name = "IS_MATERIAL_LOADED")
	private int isMaterialLoaded;

	@Column(name = "POSITION_IS_ALLOCATED")
	private int positionIsAllocated;
	

	@Column(name = "POSITION_IS_EMPTY")
	private int emptyPalletPosition;
	

	@Column(name = "CDATETIME")
	private String cDateTime;

	@Column(name = "USER_ID")
	private int userId;

	@Column(name = "USER_NAME")
	private String userName;

	@Column(name = "POSITION_IS_ACTIVE")
	private int positionIsActive;

	@Column(name = "POSITION_IS_DELETED")
	private int positionIsDeleted;

	@Column(name = "IS_MANUAL_DISPATCH")
	private int isManualDispatch;
	
//	@Column(name="IS_DATA_MISMATCH")
//	private int isDataMismatch;

}
