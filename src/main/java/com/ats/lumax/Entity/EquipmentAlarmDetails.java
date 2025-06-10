package com.ats.lumax.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

@Entity

@Getter
@Setter
@ToString // Optional, for logging

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "master_equipment_alarm_details")
public class EquipmentAlarmDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "equipment_alarm_seq")
    @SequenceGenerator(name = "equipment_alarm_seq", sequenceName = "master_equipment_alarm_details_equipment_alarm_id_seq", allocationSize = 1)
    @Column(name = "equipment_alarm_id", nullable = false)
    private Integer equipmentAlarmId;

    @Column(name = "equipment_id")
    private Integer equipmentId;

    @Column(name = "equipment_alarm_tag", length = 200)
    private String equipmentAlarmTag;

    @Column(name = "equipment_alarm_cdatetime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime equipmentAlarmCdatetime;

    @Column(name = "equipment_alarm_name", length = 200)
    private String equipmentAlarmName;

    @Column(name = "equipment_alarm_desc", length = 500)
    private String equipmentAlarmDesc;

    @Column(name = "equipment_alarm_status")
    private Boolean equipmentAlarmStatus;

    @Column(name = "equipment_alarm_is_deleted")
    private Boolean equipmentAlarmIsDeleted;

    
}
