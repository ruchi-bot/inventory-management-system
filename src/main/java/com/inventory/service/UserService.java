package com.inventory.service;

import com.inventory.dto.AuthDTO;
import com.inventory.dto.UserDTO;
import com.inventory.entity.User;
import java.util.List;

public interface UserService {
    AuthDTO.LoginResponse login(AuthDTO.LoginRequest request);
    UserDTO.UserResponse createUser(UserDTO.CreateUserRequest request, Long createdBy);
    void deleteUser(Long userId, Long deletedBy);
    void restoreUser(Long userId, Long restoredBy);
    void permanentDeleteUser(Long userId, Long deletedBy);
    List<UserDTO.UserResponse> getAllUsers();
    List<UserDTO.UserResponse> getActiveUsers();
    List<UserDTO.UserResponse> getDeletedUsers();
    UserDTO.UserSummary getUserSummary();
    List<UserDTO.UserResponse> getUsersByRole(String role);
    void cleanupOldDeletedUsers();
    User getUserByEmail(String email);
}