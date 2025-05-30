package com.ats.lumax.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "master_station_tag_details")
public class MasterStationTagDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "station_tag_details_id")
	private Integer stationTagDetailsId;

	@Column(name = "station_id")
	private Integer stationId;

	@Column(name = "plc_tag_name", length = 100)
	private String plcTagName;

	@Column(name = "plc_tag_type")
	private String plcTagType;

	@Column(name = "current_value")
	private String currentValue;

	@Column(name = "cc_aknowledgement")
	private Boolean ccAcknowledgement;

	@Column(name = "cdatetime")
	private LocalDateTime cDatetime;

}