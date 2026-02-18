package com.innercircle.sacco.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innercircle.sacco.common.event.AuditableEvent;
import org.springframework.stereotype.Component;

@Component
public class EventSerializer {

    private final ObjectMapper objectMapper;

    public EventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(AuditableEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event: " + event.getEventType(), e);
        }
    }

    public AuditableEvent deserialize(String json, String eventType) {
        try {
            Class<?> eventClass = Class.forName(eventType);
            return (AuditableEvent) objectMapper.readValue(json, eventClass);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize event of type: " + eventType, e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown event type: " + eventType, e);
        }
    }
}
