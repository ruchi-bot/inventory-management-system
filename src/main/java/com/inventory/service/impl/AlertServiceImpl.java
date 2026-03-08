package com.inventory.service.impl;

import com.inventory.dto.AlertDTO;
import com.inventory.entity.LowStockAlert;
import com.inventory.entity.Product;
import com.inventory.entity.Role;
import com.inventory.entity.User;
import com.inventory.entity.UserStatus;
import com.inventory.repository.LowStockAlertRepository;
import com.inventory.repository.UserRepository;
import com.inventory.service.AlertService;
import com.inventory.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertServiceImpl implements AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertServiceImpl.class);
    
    private final LowStockAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    public AlertServiceImpl(LowStockAlertRepository alertRepository,
                           UserRepository userRepository,
                           EmailService emailService) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    @Override
    public void checkAndCreateLowStockAlert(Product product) {
        // Get the threshold for this product, use default if not set
        int threshold = product.getMinStockThreshold() != null ? 
                       product.getMinStockThreshold() : 10;
        
        // Check if quantity is below threshold
        if (product.getQuantity() >= threshold) {
            // Stock is sufficient, resolve any existing alert
            resolveAlertBySku(product.getSku());
            return;
        }
        
        // Check if there's already an active alert for this product
        Optional<LowStockAlert> existingAlert = alertRepository.findBySkuAndIsResolvedFalse(product.getSku());
        
        if (existingAlert.isPresent()) {
            // Update the existing alert with new quantity
            LowStockAlert alert = existingAlert.get();
            alert.setCurrentQuantity(product.getQuantity());
            alert.setThreshold(threshold);
            alertRepository.save(alert);
            logger.info("Updated existing low stock alert for SKU: {}", product.getSku());
            return;
        }
        
        // Create new alert
        List<String> adminEmails = getAdminEmails();
        
        if (adminEmails.isEmpty()) {
            logger.warn("No admin emails found to send low stock alert for SKU: {}", product.getSku());
            return;
        }
        
        String recipients = String.join(",", adminEmails);
        
        LowStockAlert alert = new LowStockAlert(
            product.getSku(),
            product.getProductName(),
            product.getQuantity(),
            threshold,
            recipients
        );
        
        alertRepository.save(alert);
        
        // Send email notifications
        emailService.sendLowStockAlert(
            product.getProductName(),
            product.getSku(),
            product.getQuantity(),
            threshold,
            adminEmails
        );
        
        logger.info("Created and sent low stock alert for product: {} (SKU: {})", 
                   product.getProductName(), product.getSku());
    }
    
    @Override
    public void resolveAlert(Long alertId) {
        Optional<LowStockAlert> alertOpt = alertRepository.findById(alertId);
        
        if (alertOpt.isPresent()) {
            LowStockAlert alert = alertOpt.get();
            alert.setResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
            logger.info("Resolved alert ID: {} for SKU: {}", alertId, alert.getSku());
        }
    }
    
    @Override
    public void resolveAlertBySku(String sku) {
        Optional<LowStockAlert> alertOpt = alertRepository.findBySkuAndIsResolvedFalse(sku);
        
        if (alertOpt.isPresent()) {
            LowStockAlert alert = alertOpt.get();
            alert.setResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
            logger.info("Resolved alert for SKU: {}", sku);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AlertDTO.LowStockAlertResponse> getActiveAlerts() {
        List<LowStockAlert> alerts = alertRepository.findByIsResolvedFalseOrderByAlertSentAtDesc();
        
        return alerts.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public AlertDTO.AlertSummary getAlertSummary() {
        long totalActiveAlerts = alertRepository.countByIsResolvedFalse();
        
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        List<LowStockAlert> todayAlerts = alertRepository.findByAlertSentAtAfter(todayStart);
        long todayCount = todayAlerts.stream().filter(a -> !a.isResolved()).count();
        
        List<AlertDTO.LowStockAlertResponse> recentAlerts = alertRepository
            .findByIsResolvedFalseOrderByAlertSentAtDesc()
            .stream()
            .limit(5)
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new AlertDTO.AlertSummary(totalActiveAlerts, todayCount, recentAlerts);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getAdminEmails() {
        List<User> admins = userRepository.findAll().stream()
            .filter(user -> !user.isDeleted())
            .filter(user -> user.getStatus() == UserStatus.ACTIVE)
            .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MASTER_ADMIN)
            .collect(Collectors.toList());
        
        return admins.stream()
            .map(User::getEmail)
            .collect(Collectors.toList());
    }
    
    @Override
    public void sendThresholdUpdateNotification(String productName, String sku, Integer oldThreshold, Integer newThreshold, Long updatedBy) {
        List<String> adminEmails = getAdminEmails();
        
        if (adminEmails.isEmpty()) {
            logger.warn("No admin emails found to send threshold update notification for SKU: {}", sku);
            return;
        }
        
        String updatedByEmail = userRepository.findById(updatedBy)
            .map(user -> user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ")")
            .orElse("Unknown");
        
        emailService.sendThresholdUpdateAlert(
            productName,
            sku,
            oldThreshold,
            newThreshold,
            updatedByEmail,
            adminEmails
        );
        
        logger.info("Sent threshold update notification for product: {} (SKU: {}) to {} admins", 
                   productName, sku, adminEmails.size());
    }
    
    private AlertDTO.LowStockAlertResponse convertToDTO(LowStockAlert alert) {
        List<String> recipients = alert.getEmailRecipients() != null ?
            Arrays.asList(alert.getEmailRecipients().split(",")) :
            List.of();
        
        return new AlertDTO.LowStockAlertResponse(
            alert.getId(),
            alert.getSku(),
            alert.getProductName(),
            alert.getCurrentQuantity(),
            alert.getThreshold(),
            alert.getAlertSentAt(),
            alert.isResolved(),
            alert.getResolvedAt(),
            recipients
        );
    }
}
