package com.ats.lumax.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "infeed_mission_runtime_details")
public class InfeedMissionRuntimeDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "infeed_mission_id")
	private Integer infeedMissionRuntimeDetailsId;

	@Column(name = "pallet_information_id")
	private Integer palletInformationId;

	@Column(name = "pallet_code")
	private String palletCode;

	@Column(name = "product_variant_id")
	private Integer productVariantId;

	@Column(name = "quantity")
	private Integer quantity;

	@Column(name = "rack_id")
	private Integer rackId;

	@Column(name = "position_id")
	private Integer positionId;

	@Column(name = "shift_id")
	private Integer shiftId;

	@Column(name = "pallet_status_id")
	private Integer palletstatusId;

	@Column(name = "cdatetime")
	private LocalDateTime createdDate;

	@Column(name = "infeed_mission_start_datetime")
	private LocalDateTime infeedMissionStartDatetime;

	@Column(name = "infeed_mission_end_datetime")
	private LocalDateTime infeedMissionEndDatetime;

	@Column(name = "infeed_mission_status")
	private String infeedMissionStatus;

	@Column(name = "batch_number")
	private String batchNumber;

	@Column(name = "wms_transfer_order_id")
	private String wmsTransferOrderId;

}
