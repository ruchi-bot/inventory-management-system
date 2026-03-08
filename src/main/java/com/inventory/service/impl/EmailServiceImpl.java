package com.inventory.service.impl;

import com.inventory.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${app.email.from:inventory@company.com}")
    private String fromEmail;
    
    @Value("${spring.mail.username}")
    private String username;
    
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Override
    public void sendUserCreationEmail(String toEmail, String role, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to Inventory Management System - Account Created");
            
            String emailBody = String.format("""
                Dear User,
                
                Your account has been created successfully in the Inventory Management System.
                
                Account Details:
                - Email: %s
                - Role: %s
                - Temporary Password: %s
                
                Login Instructions:
                1. Go to the login page
                2. Use your email and temporary password
                3. You will be prompted to change your password on first login
                
                For security reasons, please change your password immediately after login.
                
                Best regards,
                Inventory Management Team
                """, toEmail, role, tempPassword);
            
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("User creation email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send user creation email to {}: {}", toEmail, e.getMessage(), e);
            // Don't throw exception - email failure shouldn't fail user creation
        }
    }
    
    @Override
    public void sendLowStockAlert(String productName, String sku, int currentQuantity, int threshold, java.util.List<String> recipients) {
        try {
            if (recipients == null || recipients.isEmpty()) {
                logger.warn("No recipients provided for low stock alert. Skipping email for SKU: {}", sku);
                return;
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipients.toArray(new String[0]));
            message.setSubject("🚨 LOW STOCK ALERT: " + productName);
            
            String emailBody = String.format("""
                URGENT: LOW STOCK ALERT
                
                Product Details:
                - Product Name: %s
                - SKU: %s
                - Current Quantity: %d units
                - Minimum Threshold: %d units
                - Status: BELOW THRESHOLD
                
                Action Required: Please restock this product immediately to avoid stockout.
                
                This is an automated alert from the Inventory Management System.
                Login to the system to view detailed inventory information.
                
                Best regards,
                Inventory Management System
                """, productName, sku, currentQuantity, threshold);
            
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Low stock alert email sent for product: {} (SKU: {}) to {} recipients", 
                       productName, sku, recipients.size());
            
        } catch (Exception e) {
            logger.error("Failed to send low stock alert for product {}: {}", productName, e.getMessage(), e);
            // Don't throw exception - continue with normal operation
        }
    }
    
    @Override
    public void sendThresholdUpdateAlert(String productName, String sku, int oldThreshold, int newThreshold, String updatedByEmail, java.util.List<String> recipients) {
        try {
            if (recipients == null || recipients.isEmpty()) {
                logger.warn("No recipients provided for threshold update alert. Skipping email for SKU: {}", sku);
                return;
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipients.toArray(new String[0]));
            message.setSubject("🔔 THRESHOLD UPDATE ALERT: " + productName);
            
            String emailBody = String.format("""
                NOTIFICATION: STOCK THRESHOLD UPDATED
                
                An employee has updated the minimum stock threshold for a product.
                
                Product Details:
                - Product Name: %s
                - SKU: %s
                - Previous Threshold: %d units
                - New Threshold: %d units
                - Updated By: %s
                
                The system will now send low stock alerts when inventory falls below %d units.
                
                This is an automated notification from the Inventory Management System.
                Login to the system to view detailed inventory information.
                
                Best regards,
                Inventory Management System
                """, productName, sku, oldThreshold, newThreshold, updatedByEmail, newThreshold);
            
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Threshold update alert email sent for product: {} (SKU: {}) to {} recipients", 
                       productName, sku, recipients.size());
            
        } catch (Exception e) {
            logger.error("Failed to send threshold update alert for product {}: {}", productName, e.getMessage(), e);
            // Don't throw exception - continue with normal operation
        }
    }
    
    @Override
    public void sendPasswordResetLink(String toEmail, String userName, String resetToken, int expiryMinutes) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔐 Password Reset Request - Inventory Management System");
            
            String resetLink = "http://localhost:3000/reset-password?token=" + resetToken;
            
            String emailBody = String.format("""
                Dear %s,
                
                We received a request to reset your password for your Inventory Management System account.
                
                Click the link below to reset your password:
                %s
                
                This link will expire in %d minutes for security reasons.
                
                If you didn't request this password reset, please ignore this email or contact your administrator.
                Your password will remain unchanged unless you access the link above and create a new one.
                
                For security reasons:
                - Do not share this link with anyone
                - The link can only be used once
                - Create a strong password with at least 8 characters including uppercase, lowercase, numbers, and special characters
                
                Best regards,
                Inventory Management Team
                """, userName, resetLink, expiryMinutes);
            
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Password reset link sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send password reset link to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email. Please try again later.");
        }
    }
    
    @Override
    public void sendOTPEmail(String toEmail, String userName, String otp, int expiryMinutes, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔐 Your OTP for " + purpose + " - Inventory Management System");
            
            String emailBody = String.format("""
                Dear %s,
                
                Your One-Time Password (OTP) for %s is:
                
                                    %s
                
                This OTP will expire in %d minutes.
                
                Security Tips:
                - Do not share this OTP with anyone
                - Our team will never ask for your OTP
                - If you didn't request this, please contact your administrator immediately
                
                Best regards,
                Inventory Management Team
                """, userName, purpose, otp, expiryMinutes);
            
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("OTP sent successfully to: {} for purpose: {}", toEmail, purpose);
            
        } catch (Exception e) {
            logger.error("Failed to send OTP to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email. Please try again later.");
        }
    }
    
    @Override
    public void sendPasswordChangedConfirmation(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("✅ Password Changed Successfully - Inventory Management System");
            
            String emailBody = String.format("""
                Dear %s,
                
                Your password has been changed successfully.
                
                If you made this change, no further action is required.
                
                If you did not make this change:
                - Contact your administrator immediately
                - Your account may have been compromised
                
                Changed on: %s
                
                Best regards,
                Inventory Management Team
                """, userName, java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Password changed confirmation sent to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send password changed confirmation to {}: {}", toEmail, e.getMessage(), e);
            // Don't throw exception - password already changed, just log the error
        }
    }
}
