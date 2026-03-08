package com.inventory.scheduler;

import com.inventory.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(UserCleanupScheduler.class);
    private final UserService userService;
    
    public UserCleanupScheduler(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Runs daily at 2:00 AM to clean up users deleted more than 30 days ago
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldDeletedUsers() {
        logger.info("Starting automatic cleanup of users deleted more than 30 days ago");
        try {
            userService.cleanupOldDeletedUsers();
            logger.info("Completed automatic cleanup of old deleted users");
        } catch (Exception e) {
            logger.error("Error during automatic cleanup of old deleted users", e);
        }
    }
}
