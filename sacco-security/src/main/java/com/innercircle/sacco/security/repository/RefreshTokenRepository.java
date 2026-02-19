package com.innercircle.sacco.security.repository;

import com.innercircle.sacco.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("""
            UPDATE RefreshToken r
            SET r.revoked = true,
            r.revokedAt = :revokedAt,
            r.replacedByTokenId = :replacedByTokenId
            WHERE r.id = :id
            AND r.revoked = false
            AND r.expiresAt >= :now
            """)
    int rotateIfActive(
            @Param("id") UUID id,
            @Param("replacedByTokenId") UUID replacedByTokenId,
            @Param("now") Instant now,
            @Param("revokedAt") Instant revokedAt
    );

    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    void deleteByExpiresAtBefore(Instant cutoff);

    @Modifying
    @Query("""
            UPDATE RefreshToken r
            SET r.revoked = true,
            r.revokedAt = CURRENT_TIMESTAMP
            WHERE r.userId = :userId
            AND r.revoked = false
            """)
    void revokeAllByUserId(@Param("userId") UUID userId);
}
