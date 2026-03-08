package com.inventory.service;

import java.util.List;

public interface EmailService {
    void sendUserCreationEmail(String toEmail, String role, String tempPassword);
    void sendLowStockAlert(String productName, String sku, int currentQuantity, int threshold, List<String> recipients);
    void sendThresholdUpdateAlert(String productName, String sku, int oldThreshold, int newThreshold, String updatedByEmail, List<String> recipients);
    void sendPasswordResetLink(String toEmail, String userName, String resetToken, int expiryMinutes);
    void sendOTPEmail(String toEmail, String userName, String otp, int expiryMinutes, String purpose);
    void sendPasswordChangedConfirmation(String toEmail, String userName);
}
