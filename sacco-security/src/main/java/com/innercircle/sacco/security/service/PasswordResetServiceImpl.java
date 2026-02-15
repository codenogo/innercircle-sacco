package com.innercircle.sacco.security.service;

import com.innercircle.sacco.common.service.EmailService;
import com.innercircle.sacco.security.entity.PasswordResetToken;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.PasswordResetTokenRepository;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final long TOKEN_EXPIRY_HOURS = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        // Find user by email (silently fail if not found to prevent email enumeration)
        var userOptional = userAccountRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return; // Don't reveal that email doesn't exist
        }

        UserAccount user = userOptional.get();

        // Generate cryptographically secure token
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Create password reset token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send email
        emailService.sendPasswordResetEmail(email, token);

        log.info("Password reset token generated for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Find token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        // Validate token
        if (resetToken.getUsed()) {
            throw new IllegalArgumentException("Token has already been used");
        }

        if (Instant.now().isAfter(resetToken.getExpiresAt())) {
            throw new IllegalArgumentException("Token has expired");
        }

        // Validate password complexity
        validatePasswordComplexity(newPassword);

        // Find user
        UserAccount user = userAccountRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userAccountRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getUsername());
    }

    @Override
    public void cleanupExpiredTokens() {
        int deleted = passwordResetTokenRepository.deleteByExpiresAtBeforeOrUsedTrue(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired/used password reset tokens", deleted);
        }
    }

    private void validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}
