package com.ats.lumax.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "master_pallet_information")
public class MasterPalletInformation {
	@Id

	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "pallet_information_id")
	private Integer palletInformationId;

	@Column(name = "pallet_code")
	private String palletCode;

	@Column(name = "trolley_number")
	private String trolleyNumber;

	@Column(name = "customer")
	private String customer;

	@Column(name = "product_variant_id")
	private Integer productVariantId;

	@Column(name = "batch_number")
	private String batchNumber;

	@Column(name = "station_id")
	private Integer stationId;

	@Column(name = "quantity")
	private Integer quantity;

	@Column(name = "pallet_status_id")
	private Integer status;

	@Column(name = "is_transfer_mission_generated")
	private Boolean isTransferMissionGenerated;

	@Column(name = "pallet_cdatetime")
	private LocalDateTime palletCDateTime;

	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "pallet_information_is_deleted")
	private Boolean palletInformationIsDeleted;

	@Column(name = "trolley_height")
	private String trolleyHeight;

	@Column(name = "is_trolley_door_closed")
	private Boolean isTrolleyDoorClosed;

	@Column(name = "loading_station_workdone")
	private Boolean loadingStationWorkdone;

	@Column(name = "wms_transfer_order_id")
	private String wmsTransferOrderId;

	@Column(name = "is_infeed_mission_generated")
	private Boolean isInfeedMissionGenerated;

	@Column(name = "is_outfeed_mission_generated")
	private Boolean isOutfeedMissionGenerated;

	@Column(name = "unloading_station_workdone")
	private Boolean unloadingStationWorkdone;

}
