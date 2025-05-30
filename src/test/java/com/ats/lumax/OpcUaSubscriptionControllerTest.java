package com.ats.lumax;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.mockito.Mockito.when;


import com.ats.lumax.controller.OpcUaSubscriptionController;
import com.ats.lumax.service.OpcUaService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class OpcUaSubscriptionControllerTest {
	

    @InjectMocks
    private OpcUaSubscriptionController controller;

    @Mock
    private OpcUaService opcUaService;


    private Map<String, DataValue> lastValues;

    @BeforeEach
    void setUp() {
        // Replace the field 'lastValues' with a new map using reflection
        lastValues = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(controller, "lastValues", lastValues);
    }
    @Test
    void testReadSubscribedValue_ValuePresentAndChanged() {
    	String nodeId = "ns=3;s=PLC_To_WMS.LOADING_STATION_MATERIAL_CODE (CH-01)";

        
 

        // Create old and new DataValue instances
        DataValue oldValue = new DataValue(new Variant(20.0));
        DataValue newValue = new DataValue(new Variant(25.0), StatusCode.GOOD, DateTime.now());


        // Put old value in lastValues map (simulate cache)
        lastValues.put(nodeId, oldValue);

        // Mock the opcUaService to return the new value wrapped in Optional
        when(opcUaService.readValue(nodeId)).thenReturn(Optional.of(newValue));

        // Call your controller method
        ResponseEntity<?> response = controller.readSubscribedValue(nodeId);

        // Assert HTTP status 200 OK
        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        // Assert body contains expected status and new value
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Success", body.get("status"));
        assertEquals(nodeId, body.get("nodeId"));
        assertEquals(newValue.getValue().getValue(), body.get("value"));

        // You can add timestamp and statusCode checks similarly
    }
    
    @Test
    public void testGetConnectionStatus_Connected() {
        // Arrange
        when(opcUaService.isConnected()).thenReturn(true);

        // Act
        ResponseEntity<?> response = controller.getConnectionStatus();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body)
            .containsEntry("status", "Success")
            .containsEntry("connected", true)
            .containsEntry("message", "Connected to OPC UA server");
    }

    @Test
    public void testGetConnectionStatus_Disconnected() {
        // Arrange
        when(opcUaService.isConnected()).thenReturn(false);

        // Act
        ResponseEntity<?> response = controller.getConnectionStatus();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body)
            .containsEntry("status", "Success")
            .containsEntry("connected", false)
            .containsEntry("message", "Disconnected from OPC UA server");
    }

    @Test
    public void testGetConnectionStatus_Exception() {
        // Arrange
        when(opcUaService.isConnected()).thenThrow(new RuntimeException("Connection error"));

        // Act
        ResponseEntity<?> response = controller.getConnectionStatus();

        // Assert
        assertEquals(500, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body)
            .containsEntry("status", "Error")
            .containsEntry("message", "Failed to get connection status: Connection error");
    }


}
