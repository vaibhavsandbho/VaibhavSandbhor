package com.ats.lumax.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ats.lumax.Entity.ActiveequipmentalarmsviewEntity;
import com.ats.lumax.Entity.Resolvedequipmentalarms;
import com.ats.lumax.config.SseEmitterPool;
import com.ats.lumax.repo.activateAlaramviewRepo;
import com.ats.lumax.repo.resolvedAlarmviewrepo;
import com.ats.lumax.service.EquipmentAlarmviewService;

@RestController

@RequestMapping("/SSE")
@CrossOrigin("*")
public class AlarmSseController {

	@Autowired
    private  SseEmitterPool sseEmitterPool;
	@Autowired
	private EquipmentAlarmviewService euipmentAlarmService;
	



    @GetMapping("/alarm-stream")
    public SseEmitter streamAlarms() {
    	
    	

        return sseEmitterPool.subscribe();  // Sends existing alarms too
    }
    
    @GetMapping("/alarms/active")
    public ResponseEntity<List<ActiveequipmentalarmsviewEntity>> getActiveAlarms() {
        return ResponseEntity.ok(euipmentAlarmService.getActiveAlarmsFromCache());
    }

    @GetMapping("/alarms/resolved")
    public ResponseEntity<List<Resolvedequipmentalarms>> getResolvedAlarms() {
        return ResponseEntity.ok(euipmentAlarmService.getResolvedAlarmsFromCache());
    }  
    
    
   
}
    
    
    
 
