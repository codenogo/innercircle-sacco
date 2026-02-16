package com.innercircle.sacco.common.outbox;

import com.innercircle.sacco.common.event.AuditableEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class EventOutboxWriter {

    private final EventOutboxRepository eventOutboxRepository;
    private final EventSerializer eventSerializer;

    public EventOutboxWriter(EventOutboxRepository eventOutboxRepository, EventSerializer eventSerializer) {
        this.eventOutboxRepository = eventOutboxRepository;
        this.eventSerializer = eventSerializer;
    }

    public void write(AuditableEvent event, String aggregateType, UUID aggregateId) {
        String payload = eventSerializer.serialize(event);
        String idempotencyKey = event.getEventType() + ":" + aggregateId + ":" + Instant.now().toEpochMilli();

        EventOutbox outbox = new EventOutbox();
        outbox.setEventType(event.getClass().getName());
        outbox.setAggregateType(aggregateType);
        outbox.setAggregateId(aggregateId);
        outbox.setCorrelationId(event.getCorrelationId());
        outbox.setIdempotencyKey(idempotencyKey);
        outbox.setPayload(payload);
        outbox.setStatus(EventOutboxStatus.PENDING);

        eventOutboxRepository.save(outbox);
    }
}
