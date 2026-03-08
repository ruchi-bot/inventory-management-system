package com.inventory.controller;

import com.inventory.dto.AuthDTO;
import com.inventory.dto.PasswordDTO;
import com.inventory.entity.User;
import com.inventory.service.PasswordService;
import com.inventory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    private final PasswordService passwordService;
    
    public AuthController(UserService userService, PasswordService passwordService) {
        this.userService = userService;
        this.passwordService = passwordService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthDTO.LoginResponse> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        AuthDTO.LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
    
    // Forgot Password - Send reset link
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordDTO.PasswordResponse> forgotPassword(
            @Valid @RequestBody PasswordDTO.ForgotPasswordRequest request) {
        PasswordDTO.PasswordResponse response = passwordService.sendPasswordResetLink(request.getEmail());
        return ResponseEntity.ok(response);
    }
    
    // Reset Password using token
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordDTO.PasswordResponse> resetPassword(
            @Valid @RequestBody PasswordDTO.ResetPasswordRequest request) {
        PasswordDTO.PasswordResponse response = passwordService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
    
    // Change Password - Step 1: Request OTP (for logged-in users)
    @PostMapping("/change-password/request")
    public ResponseEntity<PasswordDTO.OTPSentResponse> requestPasswordChange(
            @Valid @RequestBody PasswordDTO.ChangePasswordRequest request) {
        User user = getCurrentUser();
        PasswordDTO.OTPSentResponse response = passwordService.requestPasswordChange(user, request);
        return ResponseEntity.ok(response);
    }
    
    // Change Password - Step 2: Verify OTP and change password
    @PostMapping("/change-password/verify")
    public ResponseEntity<PasswordDTO.PasswordResponse> verifyOTPAndChangePassword(
            @Valid @RequestBody PasswordDTO.VerifyOTPRequest request) {
        User user = getCurrentUser();
        PasswordDTO.PasswordResponse response = passwordService.verifyOTPAndChangePassword(user, request);
        return ResponseEntity.ok(response);
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userService.getUserByEmail(email);
    }
}
