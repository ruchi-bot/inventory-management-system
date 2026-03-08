package com.inventory.repository;

import com.inventory.entity.LowStockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {
    
    List<LowStockAlert> findByIsResolvedFalseOrderByAlertSentAtDesc();
    
    Optional<LowStockAlert> findBySkuAndIsResolvedFalse(String sku);
    
    List<LowStockAlert> findBySkuOrderByAlertSentAtDesc(String sku);
    
    List<LowStockAlert> findByAlertSentAtAfter(LocalDateTime dateTime);
    
    long countByIsResolvedFalse();
}
