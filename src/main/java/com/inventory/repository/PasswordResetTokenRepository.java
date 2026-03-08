package com.inventory.repository;

import com.inventory.entity.PasswordResetToken;
import com.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByTokenAndIsUsedFalse(String token);
    
    Optional<PasswordResetToken> findByOtpAndUserAndIsUsedFalse(String otp, User user);
    
    List<PasswordResetToken> findByUserAndIsUsedFalseAndTokenType(User user, PasswordResetToken.TokenType tokenType);
    
    List<PasswordResetToken> findByExpiresAtBeforeAndIsUsedFalse(LocalDateTime dateTime);
    
    void deleteByUser(User user);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
