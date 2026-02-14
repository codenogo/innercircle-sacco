package com.innercircle.sacco.common.event;

/**
 * Marker interface for domain events that should be captured in the audit trail.
 * All domain events implement this so the audit module can listen for them generically.
 */
public interface AuditableEvent {

    String getEventType();

    String getActor();
}
