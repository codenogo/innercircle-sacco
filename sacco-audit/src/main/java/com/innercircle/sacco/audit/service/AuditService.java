package com.innercircle.sacco.audit.service;

import com.innercircle.sacco.audit.entity.AuditAction;
import com.innercircle.sacco.audit.entity.AuditEvent;
import com.innercircle.sacco.common.dto.CursorPage;

import java.time.Instant;
import java.util.UUID;

/**
 * Service interface for audit event operations.
 * Provides methods for logging audit events and querying the audit trail.
 */
public interface AuditService {

    /**
     * Log an audit event.
     *
     * @param actor The user or system component performing the action
     * @param actorName Human-readable name of the actor
     * @param action The type of action being performed
     * @param entityType The type of entity being acted upon
     * @param entityId The ID of the entity being acted upon
     * @param beforeSnapshot JSON snapshot of the entity state before the action
     * @param afterSnapshot JSON snapshot of the entity state after the action
     * @param ipAddress IP address of the actor (optional)
     * @return The created audit event
     */
    AuditEvent logEvent(
            String actor,
            String actorName,
            AuditAction action,
            String entityType,
            UUID entityId,
            String beforeSnapshot,
            String afterSnapshot,
            String ipAddress
    );

    /**
     * Query audit events with filters and cursor pagination.
     *
     * @param cursor Cursor for pagination (UUID of last item from previous page)
     * @param entityType Filter by entity type (optional)
     * @param entityId Filter by entity ID (optional)
     * @param actor Filter by actor (optional)
     * @param action Filter by action (optional)
     * @param startDate Filter by start date (optional)
     * @param endDate Filter by end date (optional)
     * @param limit Maximum number of results to return
     * @return Cursor-paginated audit events
     */
    CursorPage<AuditEvent> queryEvents(
            UUID cursor,
            String entityType,
            UUID entityId,
            String actor,
            AuditAction action,
            Instant startDate,
            Instant endDate,
            int limit
    );

    /**
     * Get the complete audit trail for a specific entity.
     *
     * @param entityType The type of entity
     * @param entityId The ID of the entity
     * @param cursor Cursor for pagination (optional)
     * @param limit Maximum number of results to return
     * @return Cursor-paginated audit events for the entity
     */
    CursorPage<AuditEvent> getEntityTrail(
            String entityType,
            UUID entityId,
            UUID cursor,
            int limit
    );
}
