package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.event.AuditableEvent;
import com.innercircle.sacco.common.outbox.EventDeadLetter;
import com.innercircle.sacco.common.outbox.EventDeadLetterRepository;
import com.innercircle.sacco.common.outbox.EventDeadLetterStatus;
import com.innercircle.sacco.common.outbox.EventOutbox;
import com.innercircle.sacco.common.outbox.EventOutboxRepository;
import com.innercircle.sacco.common.outbox.EventOutboxStatus;
import com.innercircle.sacco.common.outbox.EventSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
public class OutboxProcessor {

    private final EventOutboxRepository outboxRepository;
    private final EventDeadLetterRepository deadLetterRepository;
    private final EventSerializer eventSerializer;
    private final ApplicationEventPublisher eventPublisher;

    public OutboxProcessor(EventOutboxRepository outboxRepository,
                           EventDeadLetterRepository deadLetterRepository,
                           EventSerializer eventSerializer,
                           ApplicationEventPublisher eventPublisher) {
        this.outboxRepository = outboxRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.eventSerializer = eventSerializer;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {
        List<EventOutbox> pendingEvents = outboxRepository
                .findByStatusOrderByCreatedAtAsc(EventOutboxStatus.PENDING, PageRequest.of(0, 50));

        for (EventOutbox outbox : pendingEvents) {
            processEvent(outbox);
        }
    }

    @Transactional
    public void processEvent(EventOutbox outbox) {
        try {
            outbox.setStatus(EventOutboxStatus.PROCESSING);
            outboxRepository.save(outbox);

            AuditableEvent event = eventSerializer.deserialize(outbox.getPayload(), outbox.getEventType());
            eventPublisher.publishEvent(event);

            outbox.setStatus(EventOutboxStatus.PROCESSED);
            outbox.setProcessedAt(Instant.now());
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to process outbox event {}: {}", outbox.getId(), e.getMessage(), e);
            outbox.setStatus(EventOutboxStatus.FAILED);
            outboxRepository.save(outbox);

            EventDeadLetter deadLetter = new EventDeadLetter();
            deadLetter.setOutboxId(outbox.getId());
            deadLetter.setEventType(outbox.getEventType());
            deadLetter.setCorrelationId(outbox.getCorrelationId());
            deadLetter.setPayload(outbox.getPayload());
            deadLetter.setErrorMessage(e.getMessage());
            deadLetter.setRetries(0);
            deadLetter.setMaxRetries(5);
            deadLetter.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(5)));
            deadLetter.setStatus(EventDeadLetterStatus.PENDING_RETRY);
            deadLetterRepository.save(deadLetter);
        }
    }
}
