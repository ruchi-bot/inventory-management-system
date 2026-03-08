package com.inventory.service;

import com.inventory.dto.AlertDTO;
import com.inventory.entity.Product;

import java.util.List;

public interface AlertService {
    
    void checkAndCreateLowStockAlert(Product product);
    
    void resolveAlert(Long alertId);
    
    void resolveAlertBySku(String sku);
    
    List<AlertDTO.LowStockAlertResponse> getActiveAlerts();
    
    AlertDTO.AlertSummary getAlertSummary();
    
    List<String> getAdminEmails();
    
    void sendThresholdUpdateNotification(String productName, String sku, Integer oldThreshold, Integer newThreshold, Long updatedBy);
}
