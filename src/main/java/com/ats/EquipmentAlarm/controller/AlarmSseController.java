package com.ats.EquipmentAlarm.controller;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ats.EquipmentAlarm.Entity.EquipmentAlarmHistoryDto;
import com.ats.EquipmentAlarm.Entity.Resolvedequipmentalarms;
import com.ats.EquipmentAlarm.service.CacheAlarmService;


@RestController

@RequestMapping("/alarm")
@CrossOrigin("*")
public class AlarmSseController {

    


	@Autowired
	private CacheAlarmService euipmentAlarmService;
	



   
//    @GetMapping("/alarms/active")
//    public ResponseEntity<List<ActiveequipmentalarmsviewEntity>> getActiveAlarms() {
//        return ResponseEntity.ok(euipmentAlarmService.getCachedActivateAlarm());
//    }
//
//    @GetMapping("/alarms/resolved")
//    public ResponseEntity<List<Resolvedequipmentalarms>> getResolvedAlarms() {
//        return ResponseEntity.ok(euipmentAlarmService.getCachedReslovedAlarm());
//    }  
//    
	@GetMapping("/stream")
	public SseEmitter streamAlarm() {
	    SseEmitter sseEmitter = new SseEmitter(0L); // No timeout

	    Executors.newSingleThreadExecutor().submit(() -> {
	        try {
	            // Initial connection
	            sseEmitter.send(SseEmitter.event().name("init").data("connected"));

	            // Keep previous state to detect changes
	            List<EquipmentAlarmHistoryDto> previousActive = new ArrayList<>();
	            List<Resolvedequipmentalarms> previousResolved = new ArrayList<>();

	            while (true) {
	                List<EquipmentAlarmHistoryDto> currentActive = euipmentAlarmService.getCachedActivateAlarm();
	                List<Resolvedequipmentalarms> currentResolved = euipmentAlarmService.getCachedReslovedAlarm();

	                boolean changed = !currentActive.equals(previousActive) || !currentResolved.equals(previousResolved);

	                if (changed) {
	                    Map<String, Object> data = new HashMap<>();
	                    data.put("activelist", currentActive);
	                    data.put("resolvedlist", currentResolved);

	                    try {
	                        sseEmitter.send(SseEmitter.event().name("alarm-update").data(data));
	                    } catch (IOException sendException) {
	                        sseEmitter.completeWithError(sendException);
	                        break;
	                    }

	                    // Update previous state
	                    previousActive = new ArrayList<>(currentActive);
	                    previousResolved = new ArrayList<>(currentResolved);
	                }

	                // Sleep for a short interval (1-2 sec) to check for updates faster
	                Thread.sleep(2000);
	            }

	        } catch (Exception e) {
	            sseEmitter.completeWithError(e);
	        }
	    });

	    return sseEmitter;
	}

}
    
    
    
 
