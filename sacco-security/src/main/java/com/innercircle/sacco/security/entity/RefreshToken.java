package com.innercircle.sacco.security.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

    @Column(nullable = false, name = "user_id")
    private UUID userId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
