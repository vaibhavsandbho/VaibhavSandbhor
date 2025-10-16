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
@Table(name = "ats_wms_infeed_mission_runtime_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfeedMissionRuntimeDetailsEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "INFEED_MISSION_ID")
	private int infeedMissionId;

	@Column(name = "WMS_TRANSFER_ORDER_ID")
	private String wmsTransferOrderId;

	@Column(name = "PALLET_INFORMATION_ID")
	private int palletInformationId;

	@Column(name = "PALLET_CODE")
	private String palletCode;

	@Column(name = "FILLED_PERCENTAGE")
	private int filledPercentage;

	@Column(name = "QUANTITY")
	private int quantity;

	@Column(name = "AREA_ID")
	private int areaId;

	@Column(name = "AREA_NAME")
	private String areaName;

	@Column(name = "FLOOR_ID")
	private int floorId;

	@Column(name = "FLOOR_NAME")
	private String floorName;

	@Column(name = "RACK_ID")
	private int rackId;

	@Column(name = "RACK_NAME")
	private String rackName;

	@Column(name = "RACK_SIDE")
	private String rackSide;

	@Column(name = "RACK_COLUMN")
	private int rackColumn;

	@Column(name = "POSITION_ID")
	private int positionId;

	@Column(name = "POSITION_NAME")
	private String positionName;

	@Column(name = "POSITION_NUMBER_IN_RACK")
	private int positionNumberInRack;

	@Column(name = "PALLET_STATUS_ID")
	private int palletStatusId;

	@Column(name = "PALLET_STATUS_NAME")
	private String palletStatusName;

	@Column(name = "INFEED_MISSION_CDATETIME")
	private String createdDatetime;

	@Column(name = "INFEED_MISSION_START_DATETIME")
	private String infeedMissionStartDateTime;

	@Column(name = "INFEED_MISSION_END_DATETIME")
	private String infeedMissionEndDateTime;

	@Column(name = "INFEED_MISSION_STATUS")
	private String infeedMissionStatus;

	@Column(name = "INFEED_MISSION_IS_DELETED")
	private int infeedMissionIsDeleted;
	
	@Column(name="SHIFT_ID")
	private int shiftId;
	
	@Column(name="SHIFT_NAME")
	private String shiftName;

	@Column(name = "PRODUCT_VARIANT_NAME")
	private String productVariantName;
	
	@Column(name = "PRODUCT_VARIENT_CODE")
	private String productVariantCode;
	
	@Column(name = "PRODUCT_NAME")
	private String productName;
}