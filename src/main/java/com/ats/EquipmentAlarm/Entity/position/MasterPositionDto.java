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

public class MasterPositionDto {
	
	
	@Id
	private int positionId;

	
	private int rackId;


	private int areaId;


	private int floorId;

	
	private String positionName;

	private int emptyPalletPosition;
	private int positionIsActive;
private int isDataMismatch;

}

