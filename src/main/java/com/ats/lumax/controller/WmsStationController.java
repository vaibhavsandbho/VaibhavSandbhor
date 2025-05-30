package com.ats.lumax.controller;



import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ats.lumax.entity.MasterPalletInformation;
import com.ats.lumax.entity.MasterProductVariantDetails;
import com.ats.lumax.service.OpcUaService;
import com.ats.lumax.service.WmsStationService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/wms-station")
@CrossOrigin(origins = "*") // Enable CORS for testing
public class WmsStationController {
	@Autowired
	private WmsStationService wmsStationService;

	@Autowired
	private OpcUaService opcUaService;

	@GetMapping("/process-pallet-presence/{stationId}")
	public ResponseEntity<String> processPalletPresenceGet(@PathVariable Integer stationId) {
		try {
			wmsStationService.processPalletPresence(stationId);
			return ResponseEntity.ok("Pallet presence processed for station: " + stationId);
		} catch (Exception e) {
			log.error("Error processing pallet presence: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
		}
	}

	@PostMapping("/process-pallet-presence/{stationId}")
	public ResponseEntity<String> processPalletPresencePost(@PathVariable Integer stationId) {
		try {
			wmsStationService.processPalletPresence(stationId);
			return ResponseEntity.ok("Pallet presence processed for station: " + stationId);
		} catch (Exception e) {
			log.error("Error processing pallet presence: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
		}
	}

	@GetMapping("/test")
	public ResponseEntity<String> test() {
		return ResponseEntity.ok("API is working!");
	}

	@GetMapping("/read-trolley-if-pallet-present/{stationId}")
	public ResponseEntity<String> readTrolleyIfPalletPresent(@PathVariable Integer stationId) {
		try {
			log.info("Starting process for station ID: {}", stationId);

			// 1. Check pallet presence
			Optional<DataValue> palletPresenceValue = opcUaService
					.readValue("ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_PALLET_PRESENT (CH-01)\"");
			boolean isPalletPresent = palletPresenceValue.map(value -> Boolean.TRUE.equals(value.getValue().getValue()))
					.orElse(false);

			log.debug("Pallet presence status for station {}: {}", stationId, isPalletPresent);

			if (!isPalletPresent) {
				log.info("No pallet present at station {}", stationId);
				return ResponseEntity.ok("No pallet present at station " + stationId);
			}

			// 2. Read trolley code
			Optional<DataValue> barcodeValue = opcUaService
					.readValue("ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_MATERIAL_CODE (CH-01)\"");
			String barcode = barcodeValue.map(value -> value.getValue().getValue().toString().trim()).orElse("");

			log.debug("Raw barcode read for station {}: {}", stationId, barcode);

			// 3. Validate barcode length and format
			if (barcode.isEmpty() || barcode.split(",").length < 5) {
				log.warn("Invalid barcode format for station {}: {}", stationId, barcode);
				return ResponseEntity.ok("Invalid barcode format");
			}

			// 4. Split barcode (guaranteed to have at least 5 parts now)
			String[] barcodeParts = barcode.split(",");
			String trolleyNumber = barcodeParts[0];
			String productVariantCode = barcodeParts[1];
			log.info("Extracted trolley number: {}, product variant code: {}", trolleyNumber, productVariantCode);

			// 5. Check product variant in master table
			List<MasterProductVariantDetails> productDetails = wmsStationService
					.getProductVariantDetails(productVariantCode);
			if (productDetails == null || productDetails.isEmpty()) {
				log.warn("Product variant not found: {}", productVariantCode);
				return ResponseEntity.ok("Product variant not found: " + productVariantCode);
			}

			// 6. Check existing pallet information
			List<MasterPalletInformation> existingPallets = wmsStationService.getPalletInformation(trolleyNumber, 0, 0);

			if (existingPallets != null && !existingPallets.isEmpty()) {
				MasterPalletInformation palletInfo = existingPallets.get(0);
				// Interlock: Check if pallet is in ASRS (current stock)
				boolean isInAsrs = wmsStationService.isPalletInCurrentStock(palletInfo.getPalletInformationId().longValue());
				if (isInAsrs) {
					log.warn("Pallet {} already present in ASRS. Outfeed required before new infeed.", palletInfo.getPalletCode());
					return ResponseEntity.ok("Pallet already present in ASRS. Please outfeed it first.");
				}
				log.info("Pallet information already exists for trolley: {}", trolleyNumber);
				return ResponseEntity.ok("Pallet information already exists");
			}

			// 7. Create new pallet information
			String materialDescription = barcodeParts[2];
			String customer = barcodeParts[3];
			int quantity = Integer.parseInt(barcodeParts[4]);

			wmsStationService.processPalletInformation(trolleyNumber, productDetails.get(0), customer, quantity,
					stationId, wmsStationService.generateWmsTransferOrderId(trolleyNumber), barcode);

           // 8. Set work done after pallet information is successfully processed
          setWorkDone(stationId);
					
			log.info("Successfully processed new pallet information for trolley: {}", trolleyNumber);
			return ResponseEntity.ok("Successfully processed pallet information");

		} catch (Exception e) {
			log.error("Error processing pallet information for station {}: {}", stationId, e.getMessage(), e);
			return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
		}
	}

	@GetMapping("/read-trolley-barcode/{stationId}")
	public ResponseEntity<String> readTrolleyBarcode(@PathVariable Integer stationId) {
		try {
			Optional<DataValue> barcodeValue = opcUaService
					.readValue("ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_MATERIAL_CODE (CH-01)\"");
			String barcode = barcodeValue.map(value -> value.getValue().getValue().toString().trim()).orElse("");

			if (barcode.isEmpty() || barcode.split(",").length < 5) {
				return ResponseEntity.ok("No barcode found");
			}

			// Return only the trolley number (first part)
			String[] parts = barcode.split(",");
			String trolleyCode = parts[0];

			return ResponseEntity.ok(trolleyCode);
		} catch (Exception e) {
			log.error("Error reading trolley barcode for station {}: {}", stationId, e.getMessage(), e);
			return ResponseEntity.internalServerError().body("Error reading trolley barcode");
		}
	}

	@GetMapping("/read-full-barcode/{stationId}")
	public ResponseEntity<String> readFullBarcode(@PathVariable Integer stationId) {
		try {
			// Read the barcode value from OPC UA
			Optional<DataValue> barcodeValue = opcUaService
					.readValue("ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_MATERIAL_CODE (CH-01)\"");
			String barcode = barcodeValue.map(value -> value.getValue().getValue().toString().trim()).orElse("");

			if (barcode.isEmpty()) {
				return ResponseEntity.ok("No barcode found");
			}
			if (barcode.split(",").length < 5) {
				return ResponseEntity.ok("Invalid barcode format");
			}

			return ResponseEntity.ok(barcode);
		} catch (Exception e) {
			log.error("Error reading full barcode for station {}: {}", stationId, e.getMessage(), e);
			return ResponseEntity.internalServerError().body("Error reading full barcode");
		}
	}

	@GetMapping("/check-pallet-in-asrs/{trolleyNumber}")
	public ResponseEntity<String> checkPalletInAsrs(@PathVariable String trolleyNumber) {
		try {
			log.info("Checking if pallet with trolley number {} exists in ASRS", trolleyNumber);
			
			// 1. Check for existing pallet with this trolley number
			List<MasterPalletInformation> existingPallets = wmsStationService.getPalletInformation(trolleyNumber, 0, 0);
			
			if (existingPallets == null || existingPallets.isEmpty()) {
				log.info("No pallet found with trolley number: {}", trolleyNumber);
				return ResponseEntity.ok("No pallet information found");
			}
			
			// 2. Get the first pallet (most recent one due to OrderByPalletInformationIdDesc in the repository method)
			MasterPalletInformation palletInfo = existingPallets.get(0);
			
			// 3. Check if pallet is in ASRS (current stock)
			boolean isInAsrs = wmsStationService.isPalletInCurrentStock(palletInfo.getPalletInformationId().longValue());
			
			if (isInAsrs) {
				log.warn("Pallet {} already present in ASRS. Outfeed required before new infeed.", palletInfo.getPalletCode());
				return ResponseEntity.ok("Pallet already present in ASRS. Please outfeed it first.");
			}
			
			return ResponseEntity.ok("Pallet not in ASRS");
			
		} catch (Exception e) {
			log.error("Error checking pallet in ASRS for trolley number {}: {}", trolleyNumber, e.getMessage(), e);
			return ResponseEntity.internalServerError().body("Error checking pallet in ASRS: " + e.getMessage());
		}
	}

	@PostMapping("/give-work-done-to-plc/{stationId}")
	public ResponseEntity<?> setWorkDone(@PathVariable Integer stationId) {
		try {
			log.info("Setting work done for station: {}", stationId);
			
			String nodeId = "ns=3;s=\"WMS_TO_PLC\".\"LOADING_STATION_WORK_DONE (CH-01)\"";
			Boolean writeValue = null;
			// Write to OPC UA
			opcUaService.writeValue(nodeId, "true");
			log.info("",writeValue);
			log.info("Successfully set work done for station: {}", stationId);
			
			return ResponseEntity.ok(Map.of(
				"status", "Success",
				"value", "true",
				"timestamp", ZonedDateTime.now().toString(),
				"message", "Work done successfully set for station: " + stationId
			));
			
		} catch (Exception e) {
			log.error("Error setting work done for station {}: {}", stationId, e.getMessage(), e);
			return ResponseEntity.internalServerError()
				.body(Map.of(
					"status", "Error",
					"value", "true",
					"timestamp", ZonedDateTime.now().toString(),
					"error", "Failed to set work done: " + e.getMessage()
				));
		}
	}



}
