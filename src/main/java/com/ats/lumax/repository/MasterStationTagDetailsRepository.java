package com.ats.lumax.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ats.lumax.entity.MasterStationTagDetails;

@Repository
public interface MasterStationTagDetailsRepository extends JpaRepository<MasterStationTagDetails, Long> {
    
    List<MasterStationTagDetails> findByPlcTagTypeAndStationId(String plcTagType, Integer stationId);
    
    @Modifying
    @Query("UPDATE MasterStationTagDetails m SET m.currentValue = :currentValue WHERE m.stationId = :stationId AND m.plcTagType = :plcTagType")
    void updateCurrentValueByStationIdAndPlcTagType(@Param("currentValue") String currentValue, 
                                                   @Param("stationId") Integer stationId, 
                                                   @Param("plcTagType") String plcTagType);
    
    @Modifying
    @Query("UPDATE MasterStationTagDetails m SET m.currentValue = :currentValue, m.ccAcknowledgement = :ccAcknowledgement, " +
           "m.cDatetime = :cDatetime WHERE m.stationId = :stationId")
    void updateStationDetails(@Param("currentValue") String currentValue,
                            @Param("ccAcknowledgement") Integer ccAcknowledgement,
                            @Param("cDatetime") String cDatetime,
                            @Param("stationId") Integer stationId);
}
