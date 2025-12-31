package com.ats.EquipmentAlarm.Entity.alarm;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StackerModeDto {
	

	    private String stacker;
	    private int controlMode;
	    private String modeLabel;

}
