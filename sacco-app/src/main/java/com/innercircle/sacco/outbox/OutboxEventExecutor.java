package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.event.AuditableEvent;
import com.innercircle.sacco.common.outbox.EventDeadLetter;
import com.innercircle.sacco.common.outbox.EventDeadLetterRepository;
import com.innercircle.sacco.common.outbox.EventDeadLetterStatus;
import com.innercircle.sacco.common.outbox.EventOutbox;
import com.innercircle.sacco.common.outbox.EventOutboxRepository;
import com.innercircle.sacco.common.outbox.EventOutboxStatus;
import com.innercircle.sacco.common.outbox.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventExecutor {

    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofMinutes(5);

    private final EventOutboxRepository outboxRepository;
    private final EventDeadLetterRepository deadLetterRepository;
    private final EventSerializer eventSerializer;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEvent(UUID outboxId) {
        Optional<EventOutbox> maybeOutbox = outboxRepository.findByIdForUpdate(outboxId);
        if (maybeOutbox.isEmpty()) {
            return;
        }

        EventOutbox outbox = maybeOutbox.get();
        if (outbox.getStatus() != EventOutboxStatus.PENDING) {
            return;
        }

        try {
            outbox.setStatus(EventOutboxStatus.PROCESSING);
            outboxRepository.saveAndFlush(outbox);

            AuditableEvent event = eventSerializer.deserialize(outbox.getPayload(), outbox.getEventType());
            eventPublisher.publishEvent(event);

            outbox.setStatus(EventOutboxStatus.PROCESSED);
            outbox.setProcessedAt(Instant.now());
            outboxRepository.saveAndFlush(outbox);

            markDeadLetterResolved(outbox.getId());
        } catch (Exception e) {
            log.error("Failed to process outbox event {}: {}", outbox.getId(), e.getMessage(), e);

            outbox.setStatus(EventOutboxStatus.FAILED);
            outboxRepository.saveAndFlush(outbox);

            upsertDeadLetter(outbox, e);
        }
    }

    private void markDeadLetterResolved(UUID outboxId) {
        deadLetterRepository.findByOutboxId(outboxId).ifPresent(deadLetter -> {
            deadLetter.setStatus(EventDeadLetterStatus.RESOLVED);
            deadLetter.setLastRetryAt(Instant.now());
            deadLetterRepository.saveAndFlush(deadLetter);
        });
    }

    private void upsertDeadLetter(EventOutbox outbox, Exception e) {
        EventDeadLetter deadLetter = deadLetterRepository.findByOutboxId(outbox.getId())
                .orElseGet(EventDeadLetter::new);

        if (deadLetter.getId() == null) {
            deadLetter.setOutboxId(outbox.getId());
            deadLetter.setRetries(0);
            deadLetter.setMaxRetries(DEFAULT_MAX_RETRIES);
        }

        deadLetter.setEventType(outbox.getEventType());
        deadLetter.setCorrelationId(outbox.getCorrelationId());
        deadLetter.setPayload(outbox.getPayload());
        deadLetter.setErrorMessage(e.getMessage());
        deadLetter.setStatus(EventDeadLetterStatus.PENDING_RETRY);
        deadLetter.setNextRetryAt(Instant.now().plus(INITIAL_RETRY_DELAY));
        deadLetterRepository.saveAndFlush(deadLetter);
    }
}
