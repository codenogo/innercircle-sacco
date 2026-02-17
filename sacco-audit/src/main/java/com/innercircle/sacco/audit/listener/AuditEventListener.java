package com.innercircle.sacco.audit.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innercircle.sacco.audit.entity.AuditAction;
import com.innercircle.sacco.audit.service.AuditService;
import com.innercircle.sacco.common.event.AuditableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener that captures all AuditableEvent subtypes and persists them to the audit trail.
 * Uses @EventListener to process audit events synchronously within the outbox transaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Listen for all AuditableEvent subtypes and create audit entries.
     * Fires synchronously as events are now published via outbox pattern.
     *
     * @param event The auditable domain event
     */
    @EventListener
    public void handleAuditableEvent(AuditableEvent event) {
        try {
            String actor = event.getActor();
            String eventType = event.getEventType();

            // Extract entity information from the event
            Map<String, Object> eventData = extractEventData(event);

            String entityType = (String) eventData.get("entityType");
            UUID entityId = (UUID) eventData.get("entityId");
            Object beforeState = eventData.get("beforeState");
            Object afterState = eventData.get("afterState");

            // Determine audit action from event type
            AuditAction action = mapEventTypeToAction(eventType);

            // Serialize snapshots to JSON
            String beforeSnapshot = beforeState != null ? serializeToJson(beforeState) : null;
            String afterSnapshot = afterState != null ? serializeToJson(afterState) : null;

            // Get actor name (can be enhanced with user service lookup)
            String actorName = actor;

            // IP address can be extracted from request context (future enhancement)
            String ipAddress = null;

            auditService.logEvent(
                    actor,
                    actorName,
                    action,
                    entityType,
                    entityId,
                    beforeSnapshot,
                    afterSnapshot,
                    ipAddress
            );

            log.debug("Audited event: type={}, actor={}, entityType={}, entityId={}",
                    eventType, actor, entityType, entityId);

        } catch (Exception e) {
            // Don't fail the business transaction if audit logging fails
            log.error("Failed to log audit event for: {}", event.getEventType(), e);
        }
    }

    /**
     * Extract relevant data from the event using reflection.
     * This allows us to handle any AuditableEvent subtype generically.
     */
    private Map<String, Object> extractEventData(AuditableEvent event) {
        Map<String, Object> data = new HashMap<>();

        try {
            Class<?> eventClass = event.getClass();

            // Try to extract common fields using reflection
            data.put("entityType", extractField(event, "entityType", eventClass.getSimpleName()));
            data.put("entityId", extractField(event, "entityId", null));
            data.put("beforeState", extractField(event, "beforeState", null));
            data.put("afterState", extractField(event, "afterState", null));

        } catch (Exception e) {
            log.warn("Could not extract all event data from: {}", event.getClass().getName(), e);
        }

        return data;
    }

    /**
     * Extract a field value from the event using reflection.
     */
    private Object extractField(Object event, String fieldName, Object defaultValue) {
        try {
            Field field = findField(event.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(event);
                return value != null ? value : defaultValue;
            }
        } catch (Exception e) {
            log.trace("Field {} not found in {}", fieldName, event.getClass().getName());
        }
        return defaultValue;
    }

    /**
     * Find a field in the class hierarchy.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Map event type string to AuditAction enum.
     */
    private AuditAction mapEventTypeToAction(String eventType) {
        if (eventType == null) {
            return AuditAction.UPDATE;
        }

        String upperEventType = eventType.toUpperCase();

        if (upperEventType.contains("CREATE")) {
            return AuditAction.CREATE;
        } else if (upperEventType.contains("UPDATE")) {
            return AuditAction.UPDATE;
        } else if (upperEventType.contains("DELETE")) {
            return AuditAction.DELETE;
        } else if (upperEventType.contains("APPROVE")) {
            return AuditAction.APPROVE;
        } else if (upperEventType.contains("REJECT")) {
            return AuditAction.REJECT;
        } else if (upperEventType.contains("DISBURSE")) {
            return AuditAction.DISBURSE;
        } else if (upperEventType.contains("SUSPEND")) {
            return AuditAction.SUSPEND;
        } else if (upperEventType.contains("REACTIVATE")) {
            return AuditAction.REACTIVATE;
        } else if (upperEventType.contains("LOGIN")) {
            return AuditAction.LOGIN;
        } else if (upperEventType.contains("LOGOUT")) {
            return AuditAction.LOGOUT;
        } else if (upperEventType.contains("CONFIG")) {
            return AuditAction.CONFIG_CHANGE;
        } else if (upperEventType.contains("REPAID") || upperEventType.contains("REPAY")) {
            return AuditAction.REPAID;
        }

        return AuditAction.UPDATE;
    }

    /**
     * Serialize an object to JSON string.
     */
    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", object.getClass().getName(), e);
            return object.toString();
        }
    }
}
