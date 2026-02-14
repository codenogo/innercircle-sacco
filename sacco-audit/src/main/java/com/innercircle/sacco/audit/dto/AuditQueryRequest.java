package com.innercircle.sacco.audit.dto;

import com.innercircle.sacco.audit.entity.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for querying audit events with optional filters.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditQueryRequest {

    private UUID cursor;
    private String entityType;
    private UUID entityId;
    private String actor;
    private AuditAction action;
    private Instant startDate;
    private Instant endDate;

    @Builder.Default
    private Integer limit = 50;

    /**
     * Validate and set reasonable defaults for the query.
     */
    public void validate() {
        if (limit == null || limit <= 0) {
            limit = 50;
        }
        if (limit > 1000) {
            limit = 1000; // Max limit
        }
    }
}
