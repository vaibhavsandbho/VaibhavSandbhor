package com.ats.EquipmentAlarm.Entity.alarm;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Table(name = "active_equipment_alarms_view", schema = "public")
@Entity
@Data
public class ActiveequipmentalarmsviewEntity {
	
	
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
