package com.inventory.service.impl;

import com.inventory.dto.AuthDTO;
import com.inventory.dto.UserDTO;
import com.inventory.entity.User;
import com.inventory.entity.Role;
import com.inventory.entity.UserStatus;
import com.inventory.entity.AuditLog;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.ValidationException;
import com.inventory.repository.UserRepository;
import com.inventory.repository.AuditLogRepository;
import com.inventory.service.EmailService;
import com.inventory.service.UserService;
import com.inventory.util.PasswordGenerator;
import com.inventory.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final EmailService emailService;
    private final HttpServletRequest request;
    
    public UserServiceImpl(UserRepository userRepository,
                          AuditLogRepository auditLogRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenUtil jwtTokenUtil,
                          EmailService emailService,
                          HttpServletRequest request) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.emailService = emailService;
        this.request = request;
    }
    
    @Override
    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", loginRequest.getEmail()));
        
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        
        if (user.isDeleted()) {
            throw new UnauthorizedException("Account has been deleted");
        }
        
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }
        
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        String token = jwtTokenUtil.generateToken(user.getEmail(), user.getRole().name());
        
        logAudit(user.getId(), user.getEmail(), "LOGIN", "USER", user.getId(), 
                "User logged in successfully", getClientIp());
        
        return new AuthDTO.LoginResponse(
                token,
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName()
        );
    }
    
    @Override
    public UserDTO.UserResponse createUser(UserDTO.CreateUserRequest request, Long createdBy) {
        if (userRepository.existsByEmailAndIsDeleted(request.getEmail(), false)) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role specified. Allowed roles: MASTER_ADMIN, ADMIN, EMPLOYEE");
        }
        
        // Validate role creation permissions
        if (role == Role.MASTER_ADMIN) {
            throw new UnauthorizedException("Cannot create MASTER_ADMIN user");
        }
        
        String tempPassword = PasswordGenerator.generateRandomPassword();
        String hashedPassword = passwordEncoder.encode(tempPassword);
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(hashedPassword);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);
        
        User savedUser = userRepository.save(user);
        
        // Send email with credentials
        emailService.sendUserCreationEmail(
                request.getEmail(),
                role.name(),
                tempPassword
        );
        
        logAudit(createdBy, getCurrentUserEmail(createdBy), "CREATE_USER", "USER", 
                savedUser.getId(), "Created new user with role: " + role, getClientIp());
        
        return mapToUserResponse(savedUser);
    }
    
    @Override
    public void deleteUser(Long userId, Long deletedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        if (user.getRole() == Role.MASTER_ADMIN) {
            throw new UnauthorizedException("Cannot delete MASTER_ADMIN");
        }
        
        if (user.isDeleted()) {
            throw new ValidationException("User is already deleted");
        }
        
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        
        logAudit(deletedBy, getCurrentUserEmail(deletedBy), "DELETE_USER", "USER", 
                userId, "Soft deleted user", getClientIp());
    }
    
    @Override
    public void restoreUser(Long userId, Long restoredBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        if (!user.isDeleted()) {
            throw new ValidationException("User is not deleted");
        }
        
        // Check if deleted within 30 days
        if (user.getDeletedAt() != null && user.getDeletedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new ValidationException("Cannot restore user after 30 days");
        }
        
        user.setDeleted(false);
        user.setDeletedAt(null);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        
        logAudit(restoredBy, getCurrentUserEmail(restoredBy), "RESTORE_USER", "USER", 
                userId, "Restored deleted user", getClientIp());
    }
    
    @Override
    public void permanentDeleteUser(Long userId, Long deletedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        if (user.getRole() == Role.MASTER_ADMIN) {
            throw new UnauthorizedException("Cannot delete MASTER_ADMIN");
        }
        
        if (!user.isDeleted()) {
            throw new ValidationException("User must be soft-deleted before permanent deletion");
        }
        
        logAudit(deletedBy, getCurrentUserEmail(deletedBy), "PERMANENT_DELETE_USER", "USER", 
                userId, "Permanently deleted user: " + user.getEmail(), getClientIp());
        
        userRepository.delete(user);
    }
    
    @Override
    public List<UserDTO.UserResponse> getActiveUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UserDTO.UserResponse> getDeletedUsers() {
        return userRepository.findAll().stream()
                .filter(User::isDeleted)
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void cleanupOldDeletedUsers() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<User> oldDeletedUsers = userRepository.findAll().stream()
                .filter(user -> user.isDeleted() && 
                               user.getDeletedAt() != null && 
                               user.getDeletedAt().isBefore(cutoffDate))
                .collect(Collectors.toList());
        
        for (User user : oldDeletedUsers) {
            if (user.getRole() != Role.MASTER_ADMIN) {
                logAudit(1L, "system", "AUTO_DELETE_USER", "USER", 
                        user.getId(), "Automatically deleted user after 30 days: " + user.getEmail(), "system");
                userRepository.delete(user);
            }
        }
    }
    
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
    
    @Override
    public List<UserDTO.UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public UserDTO.UserSummary getUserSummary() {
        UserDTO.UserSummary summary = new UserDTO.UserSummary();
        
        // Count users by role
        summary.setMasterAdmins(userRepository.countActiveByRole(Role.MASTER_ADMIN));
        summary.setAdmins(userRepository.countActiveByRole(Role.ADMIN));
        summary.setEmployees(userRepository.countActiveByRole(Role.EMPLOYEE));
        
        // Count total users (all non-deleted users)
        List<User> allUsers = userRepository.findAll();
        summary.setTotalUsers(allUsers.stream()
                .filter(user -> !user.isDeleted())
                .count());
        
        // Count active users (excluding deleted)
        summary.setActiveUsers(allUsers.stream()
                .filter(user -> !user.isDeleted() && user.getStatus() == UserStatus.ACTIVE)
                .count());
        
        // Count deleted users
        summary.setDeletedUsers(userRepository.findByIsDeleted(true).size());
        
        return summary;
    }
    
    @Override
    public List<UserDTO.UserResponse> getUsersByRole(String role) {
        Role userRole;
        try {
            userRole = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role specified. Allowed roles: MASTER_ADMIN, ADMIN, EMPLOYEE");
        }
        
        return userRepository.findByRole(userRole).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    private UserDTO.UserResponse mapToUserResponse(User user) {
        UserDTO.UserResponse response = new UserDTO.UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole().name());
        response.setStatus(user.getStatus().name());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLogin(user.getLastLogin());
        response.setDeletedAt(user.getDeletedAt());
        response.setDeleted(user.isDeleted());
        return response;
    }
    
    private void logAudit(Long userId, String userEmail, String action, 
                         String entityType, Long entityId, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog(userId, userEmail, action, entityType, entityId, details, ipAddress);
        auditLogRepository.save(auditLog);
    }
    
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    private String getCurrentUserEmail(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElse("Unknown");
    }
}