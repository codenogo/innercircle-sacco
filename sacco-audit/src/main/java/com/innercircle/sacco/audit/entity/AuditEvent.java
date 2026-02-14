package com.innercircle.sacco.audit.entity;

import com.innercircle.sacco.common.util.UuidGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable, append-only audit event entity.
 * Records all significant data mutations in the system with before/after snapshots.
 * Uses UUID v7 for time-ordered primary keys.
 */
@Entity
@Table(
    name = "audit_events",
    indexes = {
        @Index(name = "idx_audit_entity_type_id", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_actor", columnList = "actor"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
    }
)
@Immutable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 100)
    private String actor;

    @Column(updatable = false, length = 200)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 50)
    private AuditAction action;

    @Column(nullable = false, updatable = false, length = 100)
    private String entityType;

    @Column(nullable = false, updatable = false)
    private UUID entityId;

    @Column(updatable = false, columnDefinition = "TEXT")
    private String beforeSnapshot;

    @Column(updatable = false, columnDefinition = "TEXT")
    private String afterSnapshot;

    @Column(updatable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UuidGenerator.generateV7();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
