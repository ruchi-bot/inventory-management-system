package com.inventory.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AlertDTO {
    
    public static class LowStockAlertResponse {
        private Long id;
        private String sku;
        private String productName;
        private Integer currentQuantity;
        private Integer threshold;
        private LocalDateTime alertSentAt;
        private boolean isResolved;
        private LocalDateTime resolvedAt;
        private List<String> emailRecipients;
        
        // Constructors
        public LowStockAlertResponse() {}
        
        public LowStockAlertResponse(Long id, String sku, String productName, Integer currentQuantity, 
                                    Integer threshold, LocalDateTime alertSentAt, boolean isResolved, 
                                    LocalDateTime resolvedAt, List<String> emailRecipients) {
            this.id = id;
            this.sku = sku;
            this.productName = productName;
            this.currentQuantity = currentQuantity;
            this.threshold = threshold;
            this.alertSentAt = alertSentAt;
            this.isResolved = isResolved;
            this.resolvedAt = resolvedAt;
            this.emailRecipients = emailRecipients;
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public Integer getCurrentQuantity() { return currentQuantity; }
        public void setCurrentQuantity(Integer currentQuantity) { this.currentQuantity = currentQuantity; }
        
        public Integer getThreshold() { return threshold; }
        public void setThreshold(Integer threshold) { this.threshold = threshold; }
        
        public LocalDateTime getAlertSentAt() { return alertSentAt; }
        public void setAlertSentAt(LocalDateTime alertSentAt) { this.alertSentAt = alertSentAt; }
        
        public boolean isResolved() { return isResolved; }
        public void setResolved(boolean resolved) { isResolved = resolved; }
        
        public LocalDateTime getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
        
        public List<String> getEmailRecipients() { return emailRecipients; }
        public void setEmailRecipients(List<String> emailRecipients) { this.emailRecipients = emailRecipients; }
    }
    
    public static class AlertSummary {
        private long totalActiveAlerts;
        private long todayAlerts;
        private List<LowStockAlertResponse> recentAlerts;
        
        // Constructors
        public AlertSummary() {}
        
        public AlertSummary(long totalActiveAlerts, long todayAlerts, List<LowStockAlertResponse> recentAlerts) {
            this.totalActiveAlerts = totalActiveAlerts;
            this.todayAlerts = todayAlerts;
            this.recentAlerts = recentAlerts;
        }
        
        // Getters and Setters
        public long getTotalActiveAlerts() { return totalActiveAlerts; }
        public void setTotalActiveAlerts(long totalActiveAlerts) { this.totalActiveAlerts = totalActiveAlerts; }
        
        public long getTodayAlerts() { return todayAlerts; }
        public void setTodayAlerts(long todayAlerts) { this.todayAlerts = todayAlerts; }
        
        public List<LowStockAlertResponse> getRecentAlerts() { return recentAlerts; }
        public void setRecentAlerts(List<LowStockAlertResponse> recentAlerts) { this.recentAlerts = recentAlerts; }
    }
    
    public static class UpdateThresholdRequest {
        private Integer minStockThreshold;
        
        // Constructors
        public UpdateThresholdRequest() {}
        
        public UpdateThresholdRequest(Integer minStockThreshold) {
            this.minStockThreshold = minStockThreshold;
        }
        
        // Getters and Setters
        public Integer getMinStockThreshold() { return minStockThreshold; }
        public void setMinStockThreshold(Integer minStockThreshold) { this.minStockThreshold = minStockThreshold; }
    }
}
