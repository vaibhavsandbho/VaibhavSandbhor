package com.ats.EquipmentAlarm.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "master_equipment_details", schema = "public")
public class MasterEquipmentDetailsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipment_id")
    private Integer equipmentId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "cdatetime")
    private String cdatetime;

    @Column(name = "equipment_name", length = 200)
    private String equipmentName;

    @Column(name = "equipment_desc", length = 500)
    private String equipmentDesc;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "equipment_is_active")
    private Boolean equipmentIsActive;

    @Column(name = "equipment_is_deleted")
    private Boolean equipmentIsDeleted;

    @Column(name = "equipment_cdatetime")
    private String equipmentCdatetime;
}
