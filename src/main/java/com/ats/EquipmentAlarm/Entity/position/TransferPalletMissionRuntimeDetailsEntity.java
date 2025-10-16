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

@Entity
@Table(name = "ats_wms_transfer_pallet_mission_runtime_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferPalletMissionRuntimeDetailsEntity {
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	
	@Column(name = "TRANSFER_PALLET_MISSION_RUNTIME_DETAILS_ID")
	private int transferPalletMissionRuntimeDetailsId;
	
	@Column(name="PALLET_INFORMTION_ID")
	private int palletInformationId;
	
	@Column(name="PALLET_CODE")
	private String palletCode;
	
	@Column(name="PRODUCT_VARIANT_CODE")
	private String productvariantCode;
	
	@Column(name="PRODUCT_VARIANT_NAME")
	private String productvariantName;
	
	@Column(name="MODEL_NUMBER")
	private String modelNumber;
	
	@Column(name="PREVIOUS_POSITION_ID")
	private int previousePositionId;
	
	@Column(name="PREVIOUS_POSITION_NAME")
	private String previousPositionName;
	
	@Column(name="TRANSFER_POSITION_ID")
	private int transferPositionId;
	
	@Column(name="TRANSFER_POSITION_NAME")
	private String transferPositionName;
	
	@Column(name="FLOOR_ID")
	private int floorId;
	
	@Column(name="FLOOR_NAME")
	private String floorName;
	
	@Column(name="AREA_ID")
	private int areaId;
	
	@Column(name="AREA_NAME")
	private String areaName;
	
	@Column(name="RACK_ID")
	private int rackId;
	
	@Column(name="RACK_NAME")
	private String rackName;
	
	@Column(name="RACK_SIDE")
	private String rackSide;
	
	@Column(name="RACK_COLUMN")
	private int rackColumn;
	
	@Column(name="POSITION_NUMBER_IN_RACK")
	private int positionNumberInRack;
	
	@Column(name="CDATETIME")
	private String cdatetime;
	
	@Column(name="QUANTITY")
	private int quantity;
	
	@Column(name="TRANSFER_MISSION_START_DATETIME")
	private String transferMissionStartDatetime;
	
	@Column(name="TRANSFER_MISSION_END_DATETIME")
	private String transferMissionEndDatetime;
	
	@Column(name="TRANSFER_MISSION_STATUS")
	private String transferMissionStatus;
	
	@Column(name="SHIFT_ID")
	private int shiftId;
	
	@Column(name="SHIFT_NAME")
	private String shiftName;
	
	@Column(name="USER_ID")
	private int userId;
	
	@Column(name="USER_NAME")
	private String userName;
	
	@Column(name="TRANSFER_MISSION_IS_DELETED")
	private int transferMissionIsDeleted;

	

}
