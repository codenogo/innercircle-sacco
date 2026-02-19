package com.innercircle.sacco.security.service;

import com.innercircle.sacco.common.util.UuidGenerator;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 3600L;
    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 7 * 24 * 60 * 60L;
    private static final long REFRESH_RETRY_GRACE_SECONDS = 5L;

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
                .expiresAt(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("Authentication successful for user '{}'", request.username());

        // Return login response
        return new LoginResponse(
                accessToken,
                refreshTokenString,
                "Bearer",
                ACCESS_TOKEN_EXPIRY_SECONDS
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

        Instant now = Instant.now();

        // Validate: not expired
        if (refreshToken.getExpiresAt().isBefore(now)) {
            log.warn("Refresh token has expired: userId={}", refreshToken.getUserId());
            throw new BadCredentialsException("Refresh token expired");
        }

        RefreshToken refreshTokenToIssue = rotateOrResolveRefreshToken(refreshToken, now);

        // Load UserAccount by userId
        UserAccount userAccount = userAccountRepository.findById(refreshTokenToIssue.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found for refresh token: userId={}", refreshTokenToIssue.getUserId());
                    return new BadCredentialsException("Invalid refresh token");
                });

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(userAccount);

        log.info("Token refresh successful for user '{}'", userAccount.getUsername());

        // Return new login response
        return new LoginResponse(
                newAccessToken,
                refreshTokenToIssue.getToken(),
                "Bearer",
                ACCESS_TOKEN_EXPIRY_SECONDS
        );
    }

    private RefreshToken rotateOrResolveRefreshToken(RefreshToken refreshToken, Instant now) {
        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            return resolveConcurrentRefreshRetry(refreshToken, now)
                    .orElseThrow(() -> {
                        log.warn("Refresh token has been revoked: userId={}", refreshToken.getUserId());
                        return new BadCredentialsException("Invalid refresh token");
                    });
        }

        String newRefreshTokenString = jwtService.generateRefreshToken();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(refreshToken.getUserId())
                .token(newRefreshTokenString)
                .expiresAt(now.plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS))
                .revoked(false)
                .build();
        newRefreshToken.setId(UuidGenerator.generateV7());
        RefreshToken savedNewRefreshToken = refreshTokenRepository.save(newRefreshToken);

        // Atomically rotate old token. Only one request can claim this token.
        int updatedRows = refreshTokenRepository.rotateIfActive(
                refreshToken.getId(),
                savedNewRefreshToken.getId(),
                now,
                now
        );

        if (updatedRows == 1) {
            return savedNewRefreshToken;
        }

        // This request lost the race; remove its unused successor token and serve the winner's token if within grace.
        refreshTokenRepository.deleteById(savedNewRefreshToken.getId());
        RefreshToken latestTokenState = refreshTokenRepository.findById(refreshToken.getId()).orElse(refreshToken);
        return resolveConcurrentRefreshRetry(latestTokenState, now)
                .orElseThrow(() -> {
                    log.warn("Refresh token was already consumed by another request: userId={}", refreshToken.getUserId());
                    return new BadCredentialsException("Invalid refresh token");
                });
    }

    private Optional<RefreshToken> resolveConcurrentRefreshRetry(RefreshToken refreshToken, Instant now) {
        Instant revokedAt = refreshToken.getRevokedAt();
        UUID replacementTokenId = refreshToken.getReplacedByTokenId();

        if (!Boolean.TRUE.equals(refreshToken.getRevoked()) || revokedAt == null || replacementTokenId == null) {
            return Optional.empty();
        }

        Instant earliestAllowedReplay = now.minusSeconds(REFRESH_RETRY_GRACE_SECONDS);
        if (revokedAt.isBefore(earliestAllowedReplay)) {
            return Optional.empty();
        }

        return refreshTokenRepository.findById(replacementTokenId)
                .filter(token -> !Boolean.TRUE.equals(token.getRevoked()))
                .filter(token -> !token.getExpiresAt().isBefore(now));
    }
}
