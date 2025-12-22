package com.ats.EquipmentAlarm.Entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
@Table(name = "resloved_equipment_alarms_view", schema = "public")
@Entity
@Data
public class Resolvedequipmentalarms {
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int equipmentAlarmHistoryId;

	    @Column(name = "equipment_id")
	    private Integer equipmentId;
	    @Column(name = "equipment_name")
	    private String equipmentName;

	    @Column(name = "equipment_desc")
	    private String equipmentDesc;
	    
	    @Column(name = "equipment_alarm_id")
	    private Integer equipmentAlarmId;

	    @Column(name = "equipment_alarm_status")
	    private Boolean equipmentAlarmStatus;

	  

	    @Column(name = "equipment_alarm_name")
	    private String equipmentAlarmName;

	    @Column(name = "equipment_alarm_desc")
	    private String equipmentAlarmDesc;

	    @Column(name = "equipment_alarm_occurred_datetime")
	    private String alarmOccurredDatetime;

	    @Column(name = "equipment_alarm_resolved_datetime")
	    private String alarmResolvedDatetime;

}
