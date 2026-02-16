package com.innercircle.sacco.common.event;

import java.util.UUID;

/**
 * Marker interface for domain events that should be captured in the audit trail.
 * All domain events implement this so the audit module can listen for them generically.
 */
public interface AuditableEvent {

    String getEventType();

    String getActor();

    default UUID getCorrelationId() {
        return null;
    }
}
