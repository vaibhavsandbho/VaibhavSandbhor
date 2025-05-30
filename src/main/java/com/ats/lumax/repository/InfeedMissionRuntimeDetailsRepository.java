package com.ats.lumax.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ats.lumax.entity.InfeedMissionRuntimeDetails;

import java.util.List;
@Repository
public interface InfeedMissionRuntimeDetailsRepository extends JpaRepository<InfeedMissionRuntimeDetails, Long>{
	 @Query("SELECT i FROM InfeedMissionRuntimeDetails i WHERE i.palletInformationId = :palletInformationId " +
	           "AND (i.infeedMissionStatus = :status1 OR i.infeedMissionStatus = :status2)")
	    List<InfeedMissionRuntimeDetails> findByInfeedMissionStatusAndPalletInformationId(
	        @Param("status1") String status1,
	        @Param("status2") String status2,
	        @Param("palletInformationId") Long palletInformationId);
}
