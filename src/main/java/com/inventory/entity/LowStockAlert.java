package com.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "low_stock_alerts")
public class LowStockAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String sku;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;
    
    @Column(name = "threshold", nullable = false)
    private Integer threshold;
    
    @Column(name = "alert_sent_at", nullable = false)
    private LocalDateTime alertSentAt;
    
    @Column(name = "is_resolved")
    private boolean isResolved = false;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "email_recipients")
    private String emailRecipients; // Comma-separated list
    
    @PrePersist
    protected void onCreate() {
        alertSentAt = LocalDateTime.now();
    }
    
    // Constructors
    public LowStockAlert() {}
    
    public LowStockAlert(String sku, String productName, Integer currentQuantity, 
                        Integer threshold, String emailRecipients) {
        this.sku = sku;
        this.productName = productName;
        this.currentQuantity = currentQuantity;
        this.threshold = threshold;
        this.emailRecipients = emailRecipients;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Integer getCurrentQuantity() {
        return currentQuantity;
    }
    
    public void setCurrentQuantity(Integer currentQuantity) {
        this.currentQuantity = currentQuantity;
    }
    
    public Integer getThreshold() {
        return threshold;
    }
    
    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }
    
    public LocalDateTime getAlertSentAt() {
        return alertSentAt;
    }
    
    public void setAlertSentAt(LocalDateTime alertSentAt) {
        this.alertSentAt = alertSentAt;
    }
    
    public boolean isResolved() {
        return isResolved;
    }
    
    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }
    
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    
    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    public String getEmailRecipients() {
        return emailRecipients;
    }
    
    public void setEmailRecipients(String emailRecipients) {
        this.emailRecipients = emailRecipients;
    }
}
