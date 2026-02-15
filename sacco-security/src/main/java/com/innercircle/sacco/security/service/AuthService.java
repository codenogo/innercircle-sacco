package com.innercircle.sacco.security.service;

import com.innercircle.sacco.security.dto.LoginRequest;
import com.innercircle.sacco.security.dto.LoginResponse;
import com.innercircle.sacco.security.dto.RefreshTokenRequest;
import com.innercircle.sacco.security.entity.RefreshToken;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.RefreshTokenRepository;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final SaccoUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResponse authenticate(LoginRequest request) {
        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

        // Check if account is enabled
        if (!userDetails.isEnabled()) {
            log.warn("Authentication failed: Account disabled for user '{}'", request.username());
            throw new DisabledException("Account is disabled");
        }

        // Check if account is not locked
        if (!userDetails.isAccountNonLocked()) {
            log.warn("Authentication failed: Account locked for user '{}'", request.username());
            throw new LockedException("Account is locked");
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            log.warn("Authentication failed: Invalid password for user '{}'", request.username());
            throw new BadCredentialsException("Invalid username or password");
        }

        // Load UserAccount entity
        UserAccount userAccount = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        // Generate access token
        String accessToken = jwtService.generateAccessToken(userAccount);

        // Generate refresh token
        String refreshTokenString = jwtService.generateRefreshToken();

        // Create and persist refresh token entity
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userAccount.getId())
                .token(refreshTokenString)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60)) // 7 days
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("Authentication successful for user '{}'", request.username());

        // Return login response
        return new LoginResponse(
                accessToken,
                refreshTokenString,
                "Bearer",
                3600L
        );
    }

    @Transactional
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> {
                    log.warn("Refresh token not found");
                    return new BadCredentialsException("Invalid refresh token");
                });

        // Validate: not revoked
        if (refreshToken.getRevoked()) {
            log.warn("Refresh token has been revoked: userId={}", refreshToken.getUserId());
            throw new BadCredentialsException("Invalid refresh token");
        }

        // Validate: not expired
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token has expired: userId={}", refreshToken.getUserId());
            throw new BadCredentialsException("Refresh token expired");
        }

        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Load UserAccount by userId
        UserAccount userAccount = userAccountRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found for refresh token: userId={}", refreshToken.getUserId());
                    return new BadCredentialsException("Invalid refresh token");
                });

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(userAccount);

        // Generate new refresh token
        String newRefreshTokenString = jwtService.generateRefreshToken();

        // Create and persist new refresh token entity
        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(userAccount.getId())
                .token(newRefreshTokenString)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60)) // 7 days
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        log.info("Token refresh successful for user '{}'", userAccount.getUsername());

        // Return new login response
        return new LoginResponse(
                newAccessToken,
                newRefreshTokenString,
                "Bearer",
                3600L
        );
    }
}
