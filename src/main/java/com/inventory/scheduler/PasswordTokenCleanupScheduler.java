package com.inventory.scheduler;

import com.inventory.service.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PasswordTokenCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordTokenCleanupScheduler.class);
    
    private final PasswordService passwordService;
    
    public PasswordTokenCleanupScheduler(PasswordService passwordService) {
        this.passwordService = passwordService;
    }
    
    /**
     * Clean up expired password reset tokens every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        logger.info("Starting password reset token cleanup job");
        try {
            passwordService.cleanupExpiredTokens();
            logger.info("Password reset token cleanup job completed successfully");
        } catch (Exception e) {
            logger.error("Error during password reset token cleanup: {}", e.getMessage(), e);
        }
    }
}
