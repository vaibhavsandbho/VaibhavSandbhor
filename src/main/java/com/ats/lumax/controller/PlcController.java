package com.ats.lumax.controller;


import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ats.lumax.service.OpcUaService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/plc")
@RequiredArgsConstructor
@Slf4j
public class PlcController {
	private final OpcUaService opcUaService;

	@GetMapping("/status")
	public ResponseEntity<String> getConnectionStatus() {
		boolean isConnected = opcUaService.isConnected();
		return ResponseEntity.ok(isConnected ? "Connected" : "Disconnected");
	}

	@GetMapping("/browse")
	public ResponseEntity<List<String>> browseTags(@RequestParam(required = false) String startingNode) {
		try {
			List<String> tags = opcUaService.browseTags(startingNode);
			return ResponseEntity.ok(tags);
		} catch (Exception e) {
			log.error("Error browsing tags", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@GetMapping("/read")
	public ResponseEntity<?> readValue(@RequestParam String nodeId) {
		try {
			Optional<DataValue> value = opcUaService.readValue(nodeId);
			if (value.isPresent()) {
				DataValue dataValue = value.get();
				Variant variant = dataValue.getValue();
				Object result = variant != null ? variant.getValue() : null;
				return ResponseEntity.ok(Map.of("value", result, "timestamp", dataValue.getSourceTime().getJavaDate(),
						"status", dataValue.getStatusCode().toString()));
			}
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			log.error("Error reading value for nodeId: {}", nodeId, e);
			return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/write")
	public ResponseEntity<?> writeValue(@RequestBody WriteRequest request) {
		try {
			boolean success = opcUaService.writeValue(request.getNodeId(), request.getValue());
			if (success) {
				return ResponseEntity.ok(Map.of("status", "StatusCode{name=Good, value=0x00000000, quality=good}",
						"value", request.getValue(), "timestamp", ZonedDateTime.now().toString()));
			} else {
				return ResponseEntity.badRequest()
						.body(Map.of("status", "StatusCode{name=Bad, value=0x80000000, quality=bad}", "value",
								request.getValue(), "timestamp", ZonedDateTime.now().toString()));
			}
		} catch (Exception e) {
			log.error("Error writing value for nodeId: {}", request.getNodeId(), e);
			return ResponseEntity.internalServerError()
					.body(Map.of("status", "StatusCode{name=Bad, value=0x80000000, quality=bad}", "value",
							request.getValue(), "timestamp", ZonedDateTime.now().toString()));
		}
	}

	// @GetMapping("/read-node")
	// public ResponseEntity<?> readValuesUnderNode(@RequestParam String nodeId) {
	// try {
	// Map<String, DataValue> values = opcUaService.readValuesUnderNode(nodeId);

	// // Transform the results into a more API-friendly format
	// Map<String, Object> response = new HashMap<>();
	// values.forEach((tag, dataValue) -> {
	// Variant variant = dataValue.getValue();
	// response.put(tag, Map.of(
	// "value", variant != null ? variant.getValue() : null,
	// "timestamp", dataValue.getSourceTime().getJavaDate(),
	// "status", dataValue.getStatusCode().toString()
	// ));
	// });

	// return ResponseEntity.ok(response);
	// } catch (Exception e) {
	// log.error("Error reading values under nodeId: {}", nodeId, e);
	// return ResponseEntity.internalServerError().body(Map.of("error",
	// e.getMessage()));
	// }
	// }

	@GetMapping("/read-node")
	public ResponseEntity<?> readValuesUnderNode(@RequestParam String nodeId) {
		try {
			Map<String, DataValue> values = opcUaService.readValuesUnderNode(nodeId);

			// Transform the results into a more API-friendly format
			Map<String, Object> response = new HashMap<>();
			values.forEach((tag, dataValue) -> {
				Variant variant = dataValue.getValue();
				response.put(tag,
						Map.of("value", variant != null ? variant.getValue() : null, "timestamp",
								dataValue.getSourceTime() != null ? dataValue.getSourceTime().getJavaDate()
										: new Date(),
								"status",
								dataValue.getStatusCode() != null ? dataValue.getStatusCode().toString() : "Unknown",
								"error", false, "trace", null, "message", "Success", "path", tag));
			});

			// If no values were found, return a default response
			if (response.isEmpty()) {
				return ResponseEntity.ok(Map.of("value", null, "timestamp", new Date(), "status", "NoData", "error",
						false, "trace", null, "message", "No values found", "path", nodeId));
			}

			// Return the first value found (assuming single value is expected)
			Map<String, Object> firstValue = (Map<String, Object>) response.values().iterator().next();
			return ResponseEntity.ok(firstValue);

		} catch (Exception e) {
			log.error("Error reading values under nodeId: {}", nodeId);
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of("value", null, "timestamp", new Date(), "status",
					"Error", "error", true, "trace", e.getStackTrace(), "message", e.getMessage(), "path", nodeId));
		}
	}

	@Data
	static class WriteRequest {
		private String nodeId;
		private String value;
	}
}