package com.ats.lumax.serviceimpl;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ats.lumax.entity.CurrentStockDetails;
import com.ats.lumax.entity.InfeedMissionRuntimeDetails;
import com.ats.lumax.entity.MasterPalletInformation;
import com.ats.lumax.entity.MasterProductVariantDetails;
import com.ats.lumax.entity.MasterStationTagDetails;
import com.ats.lumax.repository.CurrentStockDetailsRepository;
import com.ats.lumax.repository.InfeedMissionRuntimeDetailsRepository;
import com.ats.lumax.repository.MasterPalletInformationRepository;
import com.ats.lumax.repository.MasterProductVariantDetailsRepository;
import com.ats.lumax.repository.MasterStationTagDetailsRepository;
import com.ats.lumax.service.OpcUaService;
import com.ats.lumax.service.WmsStationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WmsStationServiceImpl implements WmsStationService {
	  @Autowired
	    private MasterStationTagDetailsRepository stationTagDetailsRepository;
	    
	    @Autowired
	    private MasterProductVariantDetailsRepository productVariantDetailsRepository;
	    
	    @Autowired
	    private MasterPalletInformationRepository palletInformationRepository;
	    
	    @Autowired
	    private InfeedMissionRuntimeDetailsRepository infeedMissionRuntimeDetailsRepository;
	    
	    @Autowired
	    private CurrentStockDetailsRepository currentStockDetailsRepository;
	    
	    @Autowired
	    private OpcUaService opcUaService;

	    @Override
	    @Transactional
	    public void processPalletPresence(Integer stationId) {
	        try {
	        	
	        	
	            //Optional<DataValue> palletPresentValue = opcUaService.readValue("ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_PALLET_PRESENT (CH-01)\"");
	             Optional<DataValue> palletPresentValue = opcUaService.readValue("ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_PALLET_PRESENT (CH-01)\"");
	            String palletPresent = palletPresentValue
	                .map(value -> value.getValue().getValue().toString())
	                .orElse("False");
	            log.debug("Pallet present status: {}", palletPresent);

	            if ("True".equals(palletPresent)) {
	                processPalletPresent(stationId);
	            } else {
	                processPalletNotPresent(stationId);
	            }
	        } catch (Exception e) {
	            log.error("Error processing pallet presence: {}", e.getMessage(), e);
	        }
	    }

	    private void processPalletPresent(Integer stationId) {
	        try {
	            List<MasterStationTagDetails> palletCodeDetails = getStationTagDetails("PC", stationId);
	            if (palletCodeDetails != null && !palletCodeDetails.isEmpty()) {
	                Optional<DataValue> barcodeValue = opcUaService.readValue("ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_MATERIAL_CODE (CH-01)\"");
	                String barcodeFormat = barcodeValue
	                    .map(value -> value.getValue().getValue().toString().trim())
	                    .orElse("");
	                processBarcode(barcodeFormat, stationId);
	            }
	        } catch (Exception e) {
	            log.error("Error processing pallet present: {}", e.getMessage(), e);
	        }
	    }

	    private void processBarcode(String barcodeFormat, Integer stationId) {
	        try {
	            String[] barcode = barcodeFormat.split(",");
	            if (barcode.length >= 5) {
	                String trolleyNumber = barcode[0];
	                String materialCode = barcode[1];
	                String materialDescription = barcode[2];
	                String customer = barcode[3];
	                int quantity = Integer.parseInt(barcode[4]);

	                if (!"NA".equals(trolleyNumber)) {
	                    processTrolleyNumber(trolleyNumber, materialCode, customer, quantity, stationId, barcodeFormat);
	                }
	            }
	        } catch (Exception e) {
	            log.error("Error processing barcode: {}", e.getMessage(), e);
	        }
	    }

	    private void processTrolleyNumber(String trolleyNumber, String materialCode, String customer, 
	                                    int quantity, Integer stationId, String barcodeFormat) {
	        try {
	            List<MasterProductVariantDetails> productDetails = getProductVariantDetails(materialCode);
	            if (productDetails != null && !productDetails.isEmpty()) {
	                String wmsTransferOrderId = generateWmsTransferOrderId(trolleyNumber);
	                processPalletInformation(trolleyNumber, productDetails.get(0), customer, quantity, 
	                                       stationId, wmsTransferOrderId, barcodeFormat);
	            }
	        } catch (Exception e) {
	            log.error("Error processing trolley number: {}", e.getMessage(), e);
	        }
	    }

	    @Override
	    public String generateWmsTransferOrderId(String trolleyNumber) {
	        LocalDateTime now = LocalDateTime.now();
	        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
	        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
	        return trolleyNumber + "_" + now.format(dateFormatter) + "_" + now.format(timeFormatter);
	    }

	    @Override
	    public void processPalletInformation(String trolleyNumber, MasterProductVariantDetails productDetails,
	                                        String customer, int quantity, Integer stationId, 
	                                        String wmsTransferOrderId, String barcodeFormat) {
	        try {
	            List<MasterPalletInformation> existingPallets = getPalletInformation(trolleyNumber, 0, 0);
	            if (existingPallets != null && !existingPallets.isEmpty()) {
	                log.info("Pallet information already exists for trolley number: {}. Skipping new entry.", trolleyNumber);
	            } else {
	                // Only create new pallet if it doesn't exist
	                createNewPalletInformation(trolleyNumber, productDetails, customer, quantity, 
	                                         stationId, wmsTransferOrderId, barcodeFormat);
	            }
	            
	            // Always call giveWorkDone after processing, regardless of whether pallet was new or existing
	            giveWorkDone(stationId);
	            
	        } catch (Exception e) {
	            log.error("Error processing pallet information: {}", e.getMessage(), e);
	        }
	    }

	    private void processExistingPallet(MasterPalletInformation existingPallet, String wmsTransferOrderId) {
	        try {
	            List<InfeedMissionRuntimeDetails> missionDetails = infeedMissionRuntimeDetailsRepository
	                .findByInfeedMissionStatusAndPalletInformationId("READY", "IN_PROGRESS", 
	                                                               existingPallet.getPalletInformationId().longValue());
	            
	            if (missionDetails.isEmpty()) {
	                List<CurrentStockDetails> stockDetails = currentStockDetailsRepository
	                    .findByPalletInformationId(existingPallet.getPalletInformationId().longValue());
	                
	                if (stockDetails.isEmpty()) {
	                    updatePalletInformationStatus(wmsTransferOrderId, 0, 0, 0, 
	                                                existingPallet.getPalletInformationId().longValue());
	                }
	            }
	        } catch (Exception e) {
	            log.error("Error processing existing pallet: {}", e.getMessage(), e);
	        }
	    }

	    private void createNewPalletInformation(String trolleyNumber, MasterProductVariantDetails productDetails,
	                                          String customer, int quantity, Integer stationId, 
	                                          String wmsTransferOrderId, String barcodeFormat) {
	        try {
	            MasterPalletInformation newPallet = new MasterPalletInformation();
	            newPallet.setPalletCode(barcodeFormat);
	            newPallet.setTrolleyNumber(trolleyNumber);
	            newPallet.setProductVariantId(productDetails.getProductVariantId());
	            newPallet.setStationId(stationId);
	            newPallet.setQuantity(quantity);
	            newPallet.setCustomer(customer);
	            newPallet.setWmsTransferOrderId(wmsTransferOrderId);
	            newPallet.setPalletCDateTime(LocalDateTime.now());
	            newPallet.setStatus(1); // Assuming 1 represents "FULL" status
	            newPallet.setIsOutfeedMissionGenerated(false);
	            newPallet.setIsTransferMissionGenerated(false);
	            newPallet.setIsInfeedMissionGenerated(false);
	            newPallet.setPalletInformationIsDeleted(false);
	            newPallet.setLoadingStationWorkdone(false);
	            newPallet.setUnloadingStationWorkdone(false);
				newPallet.setTrolleyHeight("NA");
				newPallet.setIsTrolleyDoorClosed(false);
				newPallet.setBatchNumber("NA");
				newPallet.setUserId(1);

	            savePalletInformation(newPallet);

	        } catch (Exception e) {
	            log.error("Error creating new pallet information: {}", e.getMessage(), e);
	        }
	    }

	    private void processPalletNotPresent(Integer stationId) {
	        try {
	            List<MasterStationTagDetails> palletPresentDetails = getStationTagDetails("PP", stationId);
	            if (palletPresentDetails != null && !palletPresentDetails.isEmpty() && 
	                !"NA".equals(palletPresentDetails.get(0).getCurrentValue())) {
	                resetStationDetails(stationId);
	            }
	        } catch (Exception e) {
	            log.error("Error processing pallet not present: {}", e.getMessage(), e);
	        }
	    }


		private void giveWorkDone(Integer stationId) {
		    try {
		        String nodeId = "ns=3;s=\"WMS_TO_PLC\".\"LOADING_STATION_WORK_DONE (CH-01)\"";
		        
		        // Try to write to OPC UA
		        LocalDateTime now = LocalDateTime.now();
		        opcUaService.writeValue(nodeId, "true");
		        log.info("Set LOADING_STATION_WORK_DONE (CH-01) to True at {}", now);
		        
		    } catch (Exception e) {
		        log.error("Error setting work done to true via OPC UA: {}", e.getMessage(), e);
		    }
		    
		    // Always update the database flag as backup
		    try {
		        // Update station tag details to reflect work done
		        updateStationTagDetails("True", stationId, "WD"); // Assuming "WD" is work done tag type
		        log.info("Updated database work done flag for station: {}", stationId);
		    } catch (Exception e) {
		        log.error("Error updating database work done flag: {}", e.getMessage(), e);
		    }
		}

	    private void resetStationDetails(Integer stationId) {
	        try {
	            Optional<DataValue> palletPresentValue = opcUaService.readValue("ns=3;s=\"PLC_To_WMS\".\"LOADING_STATION_PALLET_PRESENT (CH-01)\"");
	            String palletPresent = palletPresentValue
	                .map(value -> value.getValue().getValue().toString())
	                .orElse("False");
	            
	            if ("False".equals(palletPresent)) {
	                LocalDateTime now = LocalDateTime.now();
	                String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	                String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
	                
	                updateStationDetails("NA", 0, currentDate + " " + currentTime, "NA", stationId);
	            }
	        } catch (Exception e) {
	            log.error("Error resetting station details: {}", e.getMessage(), e);
	        }
	    }

	    @Override
	    public void updateStationTagDetails(String currentValue, Integer stationId, String plcTagType) {
	        stationTagDetailsRepository.updateCurrentValueByStationIdAndPlcTagType(currentValue, stationId, plcTagType);
	    }

	    @Override
	    public void updateStationDetails(String currentValue, Integer ccAcknowledgement, String cDatetime,
	                                   String wmsTransferOrderId, Integer stationId) {
	        stationTagDetailsRepository.updateStationDetails(currentValue, ccAcknowledgement, cDatetime, stationId);
	    }

	    @Override
	    public List<MasterStationTagDetails> getStationTagDetails(String plcTagType, Integer stationId) {
	        return stationTagDetailsRepository.findByPlcTagTypeAndStationId(plcTagType, stationId);
	    }

	    @Override
	    public List<MasterProductVariantDetails> getProductVariantDetails(String productVariantCode) {
	        return productVariantDetailsRepository.findByProductVariantCode(productVariantCode);
	    }

	    @Override
	    public List<MasterPalletInformation> getPalletInformation(String trolleyNumber,
	                                                            Integer isInfeedMissionGenerated,
	                                                            Integer isOutfeedMissionGenerated) {
	        return palletInformationRepository
	            .findByTrolleyNumberAndIsInfeedMissionGeneratedAndIsOutfeedMissionGeneratedOrderByPalletInformationIdDesc(
	                trolleyNumber, isInfeedMissionGenerated == 1, isOutfeedMissionGenerated == 1);
	    }

	    @Override
	    public void updatePalletInformationStatus(String wmsTransferOrderId,
	                                            Integer isInfeedMissionGenerated,
	                                            Integer isOutfeedMissionGenerated,
	                                            Integer isTransferMissionGenerated,
	                                            Long palletInformationId) {
	        palletInformationRepository.updatePalletInformationStatus(wmsTransferOrderId,
			isInfeedMissionGenerated == 1,
	                                                                isOutfeedMissionGenerated == 1,
	                                                                isTransferMissionGenerated == 1,
	                                                                palletInformationId.intValue());
	    }

	    @Override
	    public void updatePalletInformation(String palletCode, String trolleyNumber, String productId,
	                                      String productName, String productVariantId, String productVariantCode,
	                                      String productVariantName, Integer stationId, Integer capacity,
	                                      Integer isInfeedMissionGenerated, String createdDate,
	                                      Integer isOutfeedMissionGenerated, String wmsTransferOrderId,
	                                      Integer isTransferMissionGenerated, String customer,
	                                      Integer quantity, String status, Long palletInformationId) {
	        palletInformationRepository.updatePalletInformation(palletCode, trolleyNumber,
	                                                          Integer.parseInt(productVariantId),
	                                                          stationId, quantity,
	                                                          isInfeedMissionGenerated == 1,
	                                                          LocalDateTime.parse(createdDate),
	                                                          isOutfeedMissionGenerated == 1,
	                                                          wmsTransferOrderId,
	                                                          isTransferMissionGenerated == 1,
	                                                          customer,
	                                                          Integer.parseInt(status),
	                                                          palletInformationId.intValue());
	    }

	    @Override
	    public MasterPalletInformation savePalletInformation(MasterPalletInformation palletInformation) {
	        return palletInformationRepository.save(palletInformation);
	    }

	    @Override
	    public boolean isPalletInCurrentStock(Long palletInformationId) {
	        List<CurrentStockDetails> stockDetails = currentStockDetailsRepository.findByPalletInformationId(palletInformationId);
	        return stockDetails != null && !stockDetails.isEmpty();
	    }



	} 