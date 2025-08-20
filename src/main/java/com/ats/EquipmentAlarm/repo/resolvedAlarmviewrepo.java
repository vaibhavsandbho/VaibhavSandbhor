package com.ats.EquipmentAlarm.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ats.EquipmentAlarm.Entity.Resolvedequipmentalarms;

public interface resolvedAlarmviewrepo extends JpaRepository<Resolvedequipmentalarms, Integer> {

}
