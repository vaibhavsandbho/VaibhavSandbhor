package com.ats.lumax;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import com.ats.lumax.controller.OpcUaSubscriptionController;
import com.ats.lumax.service.OpcUaService;

import java.util.List;
import java.util.Map;

public class OpcUaSubscriptionControllerBrowseMethodTest {

	 @InjectMocks
	    private OpcUaSubscriptionController controller;

	    @Mock
	    private OpcUaService opcUaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBrowseNodes_Success() throws Exception {
        String startingNode = "node1";
        List<String> mockNodes = List.of("nodeA", "nodeB");

        when(opcUaService.browseTags(startingNode)).thenReturn(mockNodes);

        ResponseEntity<?> response = controller.browseNodes(startingNode);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo("Success");
        assertThat(body.get("nodes")).isEqualTo(mockNodes);
        assertThat(body.get("count")).isEqualTo(mockNodes.size());

        verify(opcUaService).browseTags(startingNode);
    }

    @Test
    void testBrowseNodes_Error() throws Exception {
        String startingNode = "node1";
        String errorMessage = "Service failure";

        when(opcUaService.browseTags(startingNode)).thenThrow(new RuntimeException(errorMessage));

        ResponseEntity<?> response = controller.browseNodes(startingNode);

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo("Error");
        assertThat(body.get("message")).isEqualTo("Failed to browse nodes: " + errorMessage);

        verify(opcUaService).browseTags(startingNode);
    }
}
