package com.ats.lumax.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "current_stock_details")
public class CurrentStockDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "current_stock_details_id")
	private Integer currentStockDetailsId;

	@Column(name = "pallet_information_id")
	private Integer palletInformationId;

	@Column(name = "position_id")
	private Integer positionId;

	@Column(name = "load_datetime")
	private LocalDateTime loadDate;
}
