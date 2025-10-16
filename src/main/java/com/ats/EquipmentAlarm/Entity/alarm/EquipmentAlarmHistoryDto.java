package com.ats.EquipmentAlarm.Entity.alarm;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentAlarmHistoryDto {
	private int equipmentAlarmHistoryId;

   
    private Integer equipmentId;
    
    private String equipmentName;

    private String equipmentDesc;
    
    
    private Integer equipmentAlarmId;

   
    private Boolean equipmentAlarmStatus;

  

   
    private String equipmentAlarmName;

  
    private String equipmentAlarmDesc;

  
    private String alarmOccurredDatetime;

    
    private String alarmResolvedDatetime;
	  
}
