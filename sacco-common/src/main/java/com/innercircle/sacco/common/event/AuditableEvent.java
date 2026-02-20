package com.innercircle.sacco.common.event;

import java.util.UUID;

/**
 * Marker interface for domain events that should be captured in the audit trail.
 * All domain events implement this so the audit module can listen for them generically.
 */
public interface AuditableEvent {

    String getEventType();

    String getActor();

    UUID getCorrelationId();

    default UUID getEntityId() {
        return null;
    }

    default String getEntityType() {
        return getClass().getSimpleName();
    }

    default Object getBeforeState() {
        return null;
    }

    default Object getAfterState() {
        return null;
    }
}
