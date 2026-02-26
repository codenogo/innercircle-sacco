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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${security.auth.access-token-expiry-seconds:3600}")
    private long accessTokenExpirySeconds = 3600L;

    @Value("${security.auth.refresh-token-expiry-seconds:604800}")
    private long refreshTokenExpirySeconds = 7 * 24 * 60 * 60L;

    @Value("${security.auth.refresh-retry-grace-seconds:5}")
    private long refreshRetryGraceSeconds = 5L;

    @Value("${security.auth.refresh-chain-max-depth:10}")
    private int refreshChainMaxDepth = 10;

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
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpirySeconds))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("Authentication successful for user '{}'", request.username());

        // Return login response
        return new LoginResponse(
                accessToken,
                refreshTokenString,
                "Bearer",
                accessTokenExpirySeconds
        );
    }

    @Transactional
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> {
                    log.warn("Refresh token rejected: reason=NOT_FOUND");
                    return new BadCredentialsException("Invalid refresh token");
                });

        Instant now = Instant.now();

        // Validate: not expired
        if (refreshToken.getExpiresAt().isBefore(now)) {
            log.warn("Refresh token rejected: reason=EXPIRED userId={} tokenId={}",
                    refreshToken.getUserId(), refreshToken.getId());
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
                accessTokenExpirySeconds
        );
    }

    private RefreshToken rotateOrResolveRefreshToken(RefreshToken refreshToken, Instant now) {
        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            return resolveConcurrentRefreshRetry(refreshToken, now)
                    .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        }

        String newRefreshTokenString = jwtService.generateRefreshToken();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(refreshToken.getUserId())
                .token(newRefreshTokenString)
                .expiresAt(now.plusSeconds(refreshTokenExpirySeconds))
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
        if (!Boolean.TRUE.equals(refreshToken.getRevoked())) {
            return Optional.empty();
        }

        RefreshToken cursor = refreshToken;
        Set<UUID> visitedTokenIds = new HashSet<>();

        for (int depth = 0; depth < refreshChainMaxDepth; depth++) {
            if (cursor.getId() != null && !visitedTokenIds.add(cursor.getId())) {
                log.warn("Refresh token rejected: reason=CHAIN_BROKEN userId={} tokenId={} detail=rotation-cycle",
                        cursor.getUserId(), cursor.getId());
                return Optional.empty();
            }

            Instant revokedAt = cursor.getRevokedAt();
            UUID replacementTokenId = cursor.getReplacedByTokenId();
            if (revokedAt == null || replacementTokenId == null) {
                log.warn("Refresh token rejected: reason=CHAIN_BROKEN userId={} tokenId={} detail=missing-rotation-metadata",
                        cursor.getUserId(), cursor.getId());
                return Optional.empty();
            }

            Instant earliestAllowedReplay = now.minusSeconds(refreshRetryGraceSeconds);
            if (revokedAt.isBefore(earliestAllowedReplay)) {
                log.warn("Refresh token rejected: reason=REVOKED_OUTSIDE_GRACE userId={} tokenId={}",
                        cursor.getUserId(), cursor.getId());
                return Optional.empty();
            }

            Optional<RefreshToken> replacement = refreshTokenRepository.findById(replacementTokenId);
            if (replacement.isEmpty()) {
                log.warn("Refresh token rejected: reason=CHAIN_BROKEN userId={} tokenId={} detail=missing-replacement",
                        cursor.getUserId(), cursor.getId());
                return Optional.empty();
            }

            RefreshToken candidate = replacement.get();
            if (!Boolean.TRUE.equals(candidate.getRevoked())) {
                if (candidate.getExpiresAt().isBefore(now)) {
                    log.warn("Refresh token rejected: reason=EXPIRED userId={} tokenId={} detail=replacement-expired",
                            candidate.getUserId(), candidate.getId());
                    return Optional.empty();
                }
                return Optional.of(candidate);
            }

            cursor = candidate;
        }

        log.warn("Refresh token rejected: reason=CHAIN_BROKEN userId={} tokenId={} detail=chain-depth-exceeded depth={}",
                refreshToken.getUserId(), refreshToken.getId(), refreshChainMaxDepth);
        return Optional.empty();
    }
}
