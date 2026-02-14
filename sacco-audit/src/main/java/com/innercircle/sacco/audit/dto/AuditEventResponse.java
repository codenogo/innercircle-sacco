package com.innercircle.sacco.audit.dto;

import com.innercircle.sacco.audit.entity.AuditAction;
import com.innercircle.sacco.audit.entity.AuditEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for audit events.
 * Represents an audit trail entry in API responses.
 */
@Getter
@Builder
@AllArgsConstructor
public class AuditEventResponse {

    private UUID id;
    private String actor;
    private String actorName;
    private AuditAction action;
    private String entityType;
    private UUID entityId;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String ipAddress;
    private Instant timestamp;

    /**
     * Convert an AuditEvent entity to a response DTO.
     */
    public static AuditEventResponse from(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .actor(event.getActor())
                .actorName(event.getActorName())
                .action(event.getAction())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .beforeSnapshot(event.getBeforeSnapshot())
                .afterSnapshot(event.getAfterSnapshot())
                .ipAddress(event.getIpAddress())
                .timestamp(event.getTimestamp())
                .build();
    }
}
