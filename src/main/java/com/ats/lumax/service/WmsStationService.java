package com.ats.lumax.service;


import java.util.List;

import com.ats.lumax.entity.MasterPalletInformation;
import com.ats.lumax.entity.MasterProductVariantDetails;
import com.ats.lumax.entity.MasterStationTagDetails;
public interface WmsStationService {
    void processPalletPresence(Integer stationId);
    
    void updateStationTagDetails(String currentValue, Integer stationId, String plcTagType);
    
    void updateStationDetails(String currentValue, Integer ccAcknowledgement, String cDatetime, 
                            String wmsTransferOrderId, Integer stationId);
    
    List<MasterStationTagDetails> getStationTagDetails(String plcTagType, Integer stationId);
    
    
    
    List<MasterPalletInformation> getPalletInformation(String trolleyNumber, Integer loadingStationIsInfeedMissionGenerated, 
                                                      Integer isOutfeedMissionGenerated);
    
    void updatePalletInformationStatus(String wmsTransferOrderId, Integer loadingStationIsInfeedMissionGenerated,
                                     Integer isOutfeedMissionGenerated, Integer isTransferMissionGenerated,
                                     Long palletInformationId);
    
    void updatePalletInformation(String palletCode, String trolleyNumber, String productId, String productName,
                                String productVariantId, String productVariantCode, String productVariantName,
                                Integer stationId, Integer capacity, Integer loadingStationIsInfeedMissionGenerated,
                                String createdDate, Integer isOutfeedMissionGenerated, String wmsTransferOrderId,
                                Integer isTransferMissionGenerated, String customer, Integer quantity,
                                String status, Long palletInformationId);
    
    MasterPalletInformation savePalletInformation(MasterPalletInformation palletInformation);
    
    List<MasterProductVariantDetails> getProductVariantDetails(String productVariantCode);
    
    String generateWmsTransferOrderId(String trolleyNumber);
    
    void processPalletInformation(String trolleyNumber, MasterProductVariantDetails productDetails,
                                String customer, int quantity, Integer stationId, 
                                String wmsTransferOrderId, String barcodeFormat);
    
    /**
     * Checks if a pallet is present in current stock (ASRS) by its pallet information ID.
     * @param palletInformationId the ID of the pallet information
     * @return true if present in ASRS, false otherwise
     */
    boolean isPalletInCurrentStock(Long palletInformationId);
}
