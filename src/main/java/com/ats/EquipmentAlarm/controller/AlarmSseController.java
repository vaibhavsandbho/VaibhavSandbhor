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

import com.ats.EquipmentAlarm.service.CacheAlarmService;




@RestController

@RequestMapping("/alarm")
@CrossOrigin("*")
public class AlarmSseController {

	@Autowired
	private StackerService stackerService;


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
	    // 30 minutes timeout (0L is infinite)
	    SseEmitter sseEmitter = new SseEmitter(30 * 60 * 1000L);

	    Executors.newSingleThreadExecutor().submit(() -> {
	        try {
	            while (true) {
	                // Heartbeat to keep the connection alive
	                try {
	                    sseEmitter.send(SseEmitter.event().name("heartbeat").data("ping"));
	                } catch (IOException e) {
	                    sseEmitter.complete(); // client disconnected
	                    break;
	                }

	                // Fetch active and resolved alarms
	                List<com.ats.EquipmentAlarm.Entity.EquipmentAlarmHistoryDto> activeList = euipmentAlarmService.getCachedActivateAlarm();
	                List<com.ats.EquipmentAlarm.Entity.Resolvedequipmentalarms> resolvedList = euipmentAlarmService.getCachedReslovedAlarm();
	                
	                      
	                       
//	                       ResponseEntity<?> r=stackerhealth.readMultipleStackerModes();

	                Map<String, Object> data = new HashMap<>();
	                data.put("activelist", activeList);
	                data.put("resolvedlist", resolvedList);
//	                data.put("lockpositoncount", lockpositoncount);
//	                data.put("misMatchCount", misMatchCount);
	                // ✅ stacker name + control mode
	                data.put("stackerModes", stackerService.getStackerModes());
	             
	                try {
	                    sseEmitter.send(SseEmitter.event().name("alarm-update").data(data));
	                } catch (IOException e) {
	                    sseEmitter.complete(); // client disconnected
	                    break;
	                }

	                Thread.sleep(10000); // 10s interval
	            }
	        } catch (Exception ex) {
	            sseEmitter.completeWithError(ex);
	        }
	    });

	    return sseEmitter;
	}

}
    
    
    
 