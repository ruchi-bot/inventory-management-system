package com.inventory.service.impl;

import com.inventory.dto.PasswordDTO;
import com.inventory.entity.PasswordResetToken;
import com.inventory.entity.User;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.exception.ValidationException;
import com.inventory.repository.PasswordResetTokenRepository;
import com.inventory.repository.UserRepository;
import com.inventory.service.EmailService;
import com.inventory.service.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PasswordServiceImpl implements PasswordService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordServiceImpl.class);
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom random = new SecureRandom();
    
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    public PasswordServiceImpl(UserRepository userRepository,
                              PasswordResetTokenRepository tokenRepository,
                              PasswordEncoder passwordEncoder,
                              EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    
    @Override
    public PasswordDTO.PasswordResponse sendPasswordResetLink(String email) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // Check if user is deleted or inactive
        if (user.isDeleted()) {
            throw new ValidationException("Account is deleted. Please contact administrator.");
        }
        
        // Invalidate any existing reset tokens for this user
        List<PasswordResetToken> existingTokens = tokenRepository
                .findByUserAndIsUsedFalseAndTokenType(user, PasswordResetToken.TokenType.RESET_PASSWORD);
        existingTokens.forEach(PasswordResetToken::markAsUsed);
        tokenRepository.saveAll(existingTokens);
        
        // Generate new reset token
        String resetToken = UUID.randomUUID().toString();
        
        PasswordResetToken token = new PasswordResetToken(
                user,
                resetToken,
                null,
                PasswordResetToken.TokenType.RESET_PASSWORD,
                RESET_TOKEN_EXPIRY_MINUTES
        );
        
        tokenRepository.save(token);
        
        // Send email with reset link
        String userName = user.getFirstName() + " " + user.getLastName();
        emailService.sendPasswordResetLink(user.getEmail(), userName, resetToken, RESET_TOKEN_EXPIRY_MINUTES);
        
        logger.info("Password reset link sent to: {}", email);
        
        return new PasswordDTO.PasswordResponse(
                "Password reset link has been sent to your email. Please check your inbox.",
                true
        );
    }
    
    @Override
    public PasswordDTO.PasswordResponse resetPassword(PasswordDTO.ResetPasswordRequest request) {
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }
        
        // Find and validate token
        PasswordResetToken token = tokenRepository.findByTokenAndIsUsedFalse(request.getToken())
                .orElseThrow(() -> new ValidationException("Invalid or expired reset link"));
        
        if (token.getTokenType() != PasswordResetToken.TokenType.RESET_PASSWORD) {
            throw new ValidationException("Invalid token type");
        }
        
        if (!token.isValid()) {
            throw new ValidationException("Reset link has expired or already been used");
        }
        
        User user = token.getUser();
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Mark token as used
        token.markAsUsed();
        tokenRepository.save(token);
        
        // Send confirmation email
        String userName = user.getFirstName() + " " + user.getLastName();
        emailService.sendPasswordChangedConfirmation(user.getEmail(), userName);
        
        logger.info("Password reset successful for user: {}", user.getEmail());
        
        return new PasswordDTO.PasswordResponse(
                "Password has been reset successfully. You can now login with your new password.",
                true
        );
    }
    
    @Override
    public PasswordDTO.OTPSentResponse requestPasswordChange(User user, PasswordDTO.ChangePasswordRequest request) {
        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ValidationException("Current password is incorrect");
        }
        
        // Validate new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("New passwords do not match");
        }
        
        // Validate new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ValidationException("New password must be different from current password");
        }
        
        // Invalidate any existing OTP tokens for this user
        List<PasswordResetToken> existingTokens = tokenRepository
                .findByUserAndIsUsedFalseAndTokenType(user, PasswordResetToken.TokenType.CHANGE_PASSWORD);
        existingTokens.forEach(PasswordResetToken::markAsUsed);
        tokenRepository.saveAll(existingTokens);
        
        // Generate OTP
        String otp = generateOTP();
        
        // Store new password temporarily in token (we'll use token field for this)
        String tempPasswordHash = passwordEncoder.encode(request.getNewPassword());
        
        PasswordResetToken token = new PasswordResetToken(
                user,
                tempPasswordHash, // Store hashed new password temporarily
                otp,
                PasswordResetToken.TokenType.CHANGE_PASSWORD,
                OTP_EXPIRY_MINUTES
        );
        
        tokenRepository.save(token);
        
        // Send OTP email
        String userName = user.getFirstName() + " " + user.getLastName();
        emailService.sendOTPEmail(user.getEmail(), userName, otp, OTP_EXPIRY_MINUTES, "Password Change");
        
        logger.info("OTP sent for password change to: {}", user.getEmail());
        
        return new PasswordDTO.OTPSentResponse(
                "OTP has been sent to your email. Please verify to complete password change.",
                true,
                maskEmail(user.getEmail()),
                OTP_EXPIRY_MINUTES
        );
    }
    
    @Override
    public PasswordDTO.PasswordResponse verifyOTPAndChangePassword(User user, PasswordDTO.VerifyOTPRequest request) {
        // Find valid OTP token for this user
        PasswordResetToken token = tokenRepository.findByOtpAndUserAndIsUsedFalse(request.getOtp(), user)
                .orElseThrow(() -> new ValidationException("Invalid or expired OTP"));
        
        if (token.getTokenType() != PasswordResetToken.TokenType.CHANGE_PASSWORD) {
            throw new ValidationException("Invalid OTP type");
        }
        
        if (!token.isValid()) {
            throw new ValidationException("OTP has expired or already been used");
        }
        
        // Update password with the one stored in token field
        user.setPassword(token.getToken()); // This contains the hashed new password
        userRepository.save(user);
        
        // Mark token as used
        token.markAsUsed();
        tokenRepository.save(token);
        
        // Send confirmation email
        String userName = user.getFirstName() + " " + user.getLastName();
        emailService.sendPasswordChangedConfirmation(user.getEmail(), userName);
        
        logger.info("Password changed successfully for user: {}", user.getEmail());
        
        return new PasswordDTO.PasswordResponse(
                "Password has been changed successfully.",
                true
        );
    }
    
    @Override
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.deleteByExpiresAtBefore(now);
        logger.info("Cleaned up expired password reset tokens");
    }
    
    private String generateOTP() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***@" + domain;
        }
        
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + domain;
    }
}
