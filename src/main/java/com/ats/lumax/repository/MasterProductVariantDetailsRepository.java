package com.ats.lumax.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ats.lumax.entity.MasterProductVariantDetails;

@Repository
public interface MasterProductVariantDetailsRepository extends JpaRepository<MasterProductVariantDetails, Integer> {
    
    // Find by product variant code
    List<MasterProductVariantDetails> findByProductVariantCode(String productVariantCode);
    
    // Find by product ID
    List<MasterProductVariantDetails> findByProductId(Integer productId);
    
    // Find active product variants
    List<MasterProductVariantDetails> findByProductVarientIsActiveTrue();
    
    // Find non-deleted product variants
    List<MasterProductVariantDetails> findByProductVariantIsDeletedFalse();
    
    // Find active and non-deleted product variants
    List<MasterProductVariantDetails> findByProductVarientIsActiveTrueAndProductVariantIsDeletedFalse();
    
    // Find by trolley type
    List<MasterProductVariantDetails> findByTrolleyType(String trolleyType);
    
    // Custom query to find product variants by capacity range
    @Query("SELECT p FROM MasterProductVariantDetails p WHERE p.capacity BETWEEN :minCapacity AND :maxCapacity")
    List<MasterProductVariantDetails> findByCapacityRange(
        @Param("minCapacity") Integer minCapacity,
        @Param("maxCapacity") Integer maxCapacity
    );
    
    // Custom query to find product variants by ageing days
    @Query("SELECT p FROM MasterProductVariantDetails p WHERE p.ageingDays <= :maxAgeingDays")
    List<MasterProductVariantDetails> findByMaxAgeingDays(@Param("maxAgeingDays") Integer maxAgeingDays);
    
    // Custom query to find product variants by product variant name containing
    @Query("SELECT p FROM MasterProductVariantDetails p WHERE LOWER(p.productVariantName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<MasterProductVariantDetails> searchByProductVariantName(@Param("searchTerm") String searchTerm);
} 