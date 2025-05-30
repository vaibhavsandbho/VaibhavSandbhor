package com.ats.lumax;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import com.ats.lumax.controller.MonitorController;
import com.ats.lumax.serviceimpl.PredefinedNodeValueService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MonitorControllerTest {

    @Mock
    private PredefinedNodeValueService predefinedNodeValueService;

    @InjectMocks
    private MonitorController controller;

    
    @Autowired
    private MockMvc mockMvc;

//    @Test
//    void testMonitorMultipleNodesEndpoint() throws Exception {
//        mockMvc.perform(get("/api/monitor-nodes"))
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.status").value("Monitoring started"));
//    }
    

    // These need to be initialized in your controller or made accessible for testing
    private Map<String, Thread> monitoringThreads;
    private Map<String, Object> lastValues;
    @BeforeEach
    void setUp() {
        // Initialize the maps that your controller uses
        monitoringThreads = new ConcurrentHashMap<>();
        lastValues = new ConcurrentHashMap<>();
        
        // If these are private fields in your controller, you might need to use reflection
        // or make them package-private for testing, or provide setter methods
        // controller.setMonitoringThreads(monitoringThreads);
        // controller.setLastValues(lastValues);
    }

    @Test
    void testMonitorMultipleNodes_Success() {
        // Arrange
        Map<String, Object> mockNodeValues = new HashMap<>();
        mockNodeValues.put("node1", " \"ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_PALLET_PRESENT (CH-01)\"\": true,");
        mockNodeValues.put("node2", "\"ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_MATERIAL_CODE (CH-01)\"\": \"MAT1234\",");
        mockNodeValues.put("node3", "\"ns=3;s=\"WMS_TO_PLC\".\"LOADING_STATION_WORK_DONE (CH-01)\"\":true");

        when(predefinedNodeValueService.getAllValues()).thenReturn(mockNodeValues);

        // Act
        ResponseEntity<?> response = controller.monitorMultipleNodes();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

        assertEquals("Monitoring started", responseBody.get("status"));

        @SuppressWarnings("unchecked")
        Map<String, Object> initialValues = (Map<String, Object>) responseBody.get("initialValues");

        assertEquals(mockNodeValues.get("node1"), initialValues.get("node1"));
        assertEquals(mockNodeValues.get("node2"), initialValues.get("node2"));
        assertEquals(mockNodeValues.get("node3"), initialValues.get("node3"));

        verify(predefinedNodeValueService, times(1)).getAllValues();
    }
    
    
    @Test
    void testMonitorMultipleNodes_EmptyNodeList() {
        // Arrange
        Map<String, Object> emptyNodeValues = new HashMap<>();
        when(predefinedNodeValueService.getAllValues()).thenReturn(emptyNodeValues);
        
        // Act
        ResponseEntity<?> response = controller.monitorMultipleNodes();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertNotNull(responseBody);
        assertEquals("Monitoring started", responseBody.get("status"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> initialValues = (Map<String, Object>) responseBody.get("initialValues");
        
        assertTrue(initialValues.isEmpty());
        
        verify(predefinedNodeValueService, times(1)).getAllValues();
    }
    
    
    @Test
    void testMonitorMultipleNodes_NodeValueIsNull() {
        // Arrange
        Map<String, Object> mockNodeValues = new HashMap<>();
        mockNodeValues.put("node1", null);
        mockNodeValues.put("node2", "value2");
        
        when(predefinedNodeValueService.getAllValues()).thenReturn(mockNodeValues);
        
        // Act
        ResponseEntity<?> response = controller.monitorMultipleNodes();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> initialValues = (Map<String, Object>) responseBody.get("initialValues");
        
        assertEquals("Node not found or inaccessible", initialValues.get("node1"));
        assertEquals("value2", initialValues.get("node2"));
        
        verify(predefinedNodeValueService, times(1)).getAllValues();
    }
    
    @Test
    void testMonitorMultipleNodes_ServiceThrowsException() {
        // Arrange
        when(predefinedNodeValueService.getAllValues())
            .thenThrow(new RuntimeException("Service unavailable"));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            controller.monitorMultipleNodes();
        });
        
        verify(predefinedNodeValueService, times(1)).getAllValues();
    }
    @Test
    void testMonitorMultipleNodes_MixedScenarios() {
        // Arrange
        Map<String, Object> mockNodeValues = new HashMap<>();
        mockNodeValues.put("node1", "value1");  // Normal
        mockNodeValues.put("node2", null);      // Inaccessible
        mockNodeValues.put("node3", "value3");  // Already monitored

        // ✅ Simulate node3 is already being monitored (IMPORTANT LINE)
        Thread existingThread = new Thread(() -> {});
        existingThread.start();
        controller.monitoringThreads.put("node3", existingThread); // ✅ Correct: use controller's map

        // Mock service response
        when(predefinedNodeValueService.getAllValues()).thenReturn(mockNodeValues);

        // Act
        ResponseEntity<?> response = controller.monitorMultipleNodes();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> initialValues = (Map<String, Object>) responseBody.get("initialValues");

        assertEquals("value1", initialValues.get("node1"));                           // ✅ Normal
        assertEquals("Node not found or inaccessible", initialValues.get("node2"));   // ✅ Null
        assertEquals("Already monitoring", initialValues.get("node3"));               // ✅ Works now

        verify(predefinedNodeValueService, times(1)).getAllValues();
    }
    
//    @Test
//    void testMonitorMultipleNodes_ThreadCreationAndStorage() throws InterruptedException {
//        // Arrange
//        Map<String, Object> mockNodeValues = new HashMap<>();
//        mockNodeValues.put("node1", "value1");
//        
//        when(predefinedNodeValueService.getAllValues()).thenReturn(mockNodeValues);
//        when(predefinedNodeValueService.getValue("node1")).thenReturn("value1");
//        
//        // Act
//        ResponseEntity<?> response = controller.monitorMultipleNodes();
//        
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        
//        // Wait a bit for thread to start
//        Thread.sleep(100);
//        
//        // Verify thread was created and stored
//        assertTrue(monitoringThreads.containsKey("node1"));
//        Thread monitorThread = monitoringThreads.get("node1");
//        assertNotNull(monitorThread);
//        assertTrue(monitorThread.isAlive());
//        assertTrue(monitorThread.isDaemon());
//        
//        // Verify lastValues was updated
//        assertEquals("value1", lastValues.get("node1"));
//        
//        // Cleanup
//        monitorThread.interrupt();
//        
//        verify(predefinedNodeValueService, times(1)).getAllValues();
//    }
    
    
    @Test
    void testMonitorMultipleNodes_LargeNumberOfNodes() {
        // Arrange
        Map<String, Object> mockNodeValues = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            mockNodeValues.put("node" + i, "value" + i);
        }
        
        when(predefinedNodeValueService.getAllValues()).thenReturn(mockNodeValues);
        
        // Act
        ResponseEntity<?> response = controller.monitorMultipleNodes();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> initialValues = (Map<String, Object>) responseBody.get("initialValues");
        
        assertEquals(100, initialValues.size());
        
        for (int i = 1; i <= 100; i++) {
            assertEquals("value" + i, initialValues.get("node" + i));
        }
        
        verify(predefinedNodeValueService, times(1)).getAllValues();
    }


}
