package com.inventory.scheduler;

import com.inventory.entity.Product;
import com.inventory.repository.ProductRepository;
import com.inventory.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LowStockAlertScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(LowStockAlertScheduler.class);
    
    private final ProductRepository productRepository;
    private final AlertService alertService;
    
    public LowStockAlertScheduler(ProductRepository productRepository, AlertService alertService) {
        this.productRepository = productRepository;
        this.alertService = alertService;
    }
    
    // Run every 6 hours
    @Scheduled(cron = "0 0 0/6 * * *")
    public void checkLowStockProducts() {
        logger.info("Starting scheduled low stock check...");
        
        try {
            List<Product> activeProducts = productRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .collect(java.util.stream.Collectors.toList());
            
            int checkedCount = 0;
            int alertsCreated = 0;
            
            for (Product product : activeProducts) {
                try {
                    int threshold = product.getMinStockThreshold() != null ? 
                                  product.getMinStockThreshold() : 10;
                    
                    if (product.getQuantity() <= threshold) {
                        alertService.checkAndCreateLowStockAlert(product);
                        alertsCreated++;
                    }
                    checkedCount++;
                } catch (Exception e) {
                    logger.error("Error checking product {}: {}", product.getSku(), e.getMessage());
                }
            }
            
            logger.info("Low stock check completed. Checked {} products, created/updated {} alerts", 
                       checkedCount, alertsCreated);
            
        } catch (Exception e) {
            logger.error("Error during low stock check: {}", e.getMessage(), e);
        }
    }
    
    // Run at 9 AM every day
    @Scheduled(cron = "0 0 9 * * *")
    public void dailyLowStockReport() {
        logger.info("Generating daily low stock report...");
        
        try {
            List<Product> activeProducts = productRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .collect(java.util.stream.Collectors.toList());
            
            long lowStockCount = activeProducts.stream()
                .filter(p -> {
                    int threshold = p.getMinStockThreshold() != null ? p.getMinStockThreshold() : 10;
                    return p.getQuantity() <= threshold;
                })
                .count();
            
            logger.info("Daily Report: {} products currently have low stock", lowStockCount);
            
        } catch (Exception e) {
            logger.error("Error generating daily report: {}", e.getMessage(), e);
        }
    }
}
