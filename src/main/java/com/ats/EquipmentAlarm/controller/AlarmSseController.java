package com.ats.EquipmentAlarm.controller;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
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
import com.ats.EquipmentAlarm.service.AlarmSseService;
import com.ats.EquipmentAlarm.service.CacheAlarmService;


@RestController

@RequestMapping("/alarm")
@CrossOrigin("*")
public class AlarmSseController {

    


	@Autowired
	private CacheAlarmService euipmentAlarmService;
	
	@Autowired
    private  AlarmSseService sseService;


   
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
//	@GetMapping("/stream")
//	public SseEmitter streamAlarm() throws IOException {
//	    SseEmitter sseEmitter = new SseEmitter(0L);
//	    
//
//	    Executors.newSingleThreadExecutor().submit(() -> {
//	        try {
//	        	
//	            while (true) {
//	            	sseEmitter.send(SseEmitter.event().name("init").data("connected"));// No timeout
//	                // Fetch active and resolved alarms
//	                List<EquipmentAlarmHistoryDto> activelist = euipmentAlarmService.getCachedActivateAlarm();
//	                List<Resolvedequipmentalarms> resolvedlist = euipmentAlarmService.getCachedReslovedAlarm();
//
//	                Map<String, Object> data = new HashMap<>();
//                  data.put("resolvedlist", resolvedlist);
//	                data.put("activelist", activelist);
//
//	                SseEmitter.SseEventBuilder event = SseEmitter.event()
//	                        .name("alarm-update")
//	                        .data(data);
//
//	                try {
//	                    sseEmitter.send(event);
//	                } catch (IOException sendException) {
//	                    // Client disconnected or network issue
//	                    sseEmitter.completeWithError(sendException);
//	                    break;
//	                }
//
//	                Thread.sleep(10000);
//	            }
//	        } catch (Exception e) {
//	            sseEmitter.completeWithError(e);
//	        }
//	    });
//
//	    return sseEmitter;
//	}

	


    @GetMapping("/stream")
    public SseEmitter stream() {
        return sseService.subscribe();
    }
}
    
    
    
 
