package com.inventory.service;

import com.inventory.dto.PasswordDTO;
import com.inventory.entity.User;

public interface PasswordService {
    
    /**
     * Send password reset link to user's email
     * @param email User's email
     * @return Response with message and success status
     */
    PasswordDTO.PasswordResponse sendPasswordResetLink(String email);
    
    /**
     * Reset password using reset token
     * @param request Reset password request with token and new password
     * @return Response with message and success status
     */
    PasswordDTO.PasswordResponse resetPassword(PasswordDTO.ResetPasswordRequest request);
    
    /**
     * Request OTP for password change (for logged-in users)
     * @param user Authenticated user
     * @param request Change password request with current password and new password
     * @return Response with OTP sent confirmation
     */
    PasswordDTO.OTPSentResponse requestPasswordChange(User user, PasswordDTO.ChangePasswordRequest request);
    
    /**
     * Verify OTP and change password
     * @param user Authenticated user
     * @param request OTP verification request
     * @return Response with message and success status
     */
    PasswordDTO.PasswordResponse verifyOTPAndChangePassword(User user, PasswordDTO.VerifyOTPRequest request);
    
    /**
     * Clean up expired tokens
     */
    void cleanupExpiredTokens();
}
