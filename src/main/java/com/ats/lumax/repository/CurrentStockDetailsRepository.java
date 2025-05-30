package com.ats.lumax.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ats.lumax.entity.CurrentStockDetails;



@Repository
public interface CurrentStockDetailsRepository extends JpaRepository<CurrentStockDetails, Long>{
    
    
    List<CurrentStockDetails> findByPalletInformationId(Long palletInformationId);
}
