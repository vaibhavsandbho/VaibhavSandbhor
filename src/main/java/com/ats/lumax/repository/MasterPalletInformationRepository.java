package com.ats.lumax.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ats.lumax.entity.MasterPalletInformation;

import java.util.List;
import java.time.LocalDateTime;
@Repository
public interface MasterPalletInformationRepository extends JpaRepository<MasterPalletInformation, Long> {
    
    List<MasterPalletInformation> findByTrolleyNumberAndIsInfeedMissionGeneratedAndIsOutfeedMissionGeneratedOrderByPalletInformationIdDesc(
        String trolleyNumber, Boolean isInfeedMissionGenerated, Boolean isOutfeedMissionGenerated);
    
    List<MasterPalletInformation> findByPalletCode(String palletCode);
    
    @Modifying
    @Query("UPDATE MasterPalletInformation m SET m.wmsTransferOrderId = :wmsTransferOrderId, " +
           "m.isInfeedMissionGenerated = :isInfeedMissionGenerated, " +
           "m.isOutfeedMissionGenerated = :isOutfeedMissionGenerated, " +
           "m.isTransferMissionGenerated = :isTransferMissionGenerated " +
           "WHERE m.palletInformationId = :palletInformationId")
    void updatePalletInformationStatus(
        @Param("wmsTransferOrderId") String wmsTransferOrderId,
        @Param("isInfeedMissionGenerated") Boolean isInfeedMissionGenerated,
        @Param("isOutfeedMissionGenerated") Boolean isOutfeedMissionGenerated,
        @Param("isTransferMissionGenerated") Boolean isTransferMissionGenerated,
        @Param("palletInformationId") Integer palletInformationId);
    
    @Modifying
    @Query("UPDATE MasterPalletInformation m SET m.palletCode = :palletCode, m.trolleyNumber = :trolleyNumber, " +
           "m.productVariantId = :productVariantId, " +
           "m.stationId = :stationId, m.quantity = :quantity, " +
           "m.isInfeedMissionGenerated = :isInfeedMissionGenerated, " +
           "m.palletCDateTime = :createdDate, m.isOutfeedMissionGenerated = :isOutfeedMissionGenerated, " +
           "m.wmsTransferOrderId = :wmsTransferOrderId, m.isTransferMissionGenerated = :isTransferMissionGenerated, " +
           "m.customer = :customer, m.status = :status " +
           "WHERE m.palletInformationId = :palletInformationId")
    void updatePalletInformation(
        @Param("palletCode") String palletCode,
        @Param("trolleyNumber") String trolleyNumber,
        @Param("productVariantId") Integer productVariantId,
        @Param("stationId") Integer stationId,
        @Param("quantity") Integer quantity,
        @Param("isInfeedMissionGenerated") Boolean isInfeedMissionGenerated,
        @Param("createdDate") LocalDateTime createdDate,
        @Param("isOutfeedMissionGenerated") Boolean isOutfeedMissionGenerated,
        @Param("wmsTransferOrderId") String wmsTransferOrderId,
        @Param("isTransferMissionGenerated") Boolean isTransferMissionGenerated,
        @Param("customer") String customer,
        @Param("status") Integer status,
        @Param("palletInformationId") Integer palletInformationId);
}
