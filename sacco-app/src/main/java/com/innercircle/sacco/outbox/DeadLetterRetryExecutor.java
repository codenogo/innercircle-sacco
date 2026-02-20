package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.event.AuditableEvent;
import com.innercircle.sacco.common.outbox.EventDeadLetter;
import com.innercircle.sacco.common.outbox.EventDeadLetterRepository;
import com.innercircle.sacco.common.outbox.EventDeadLetterStatus;
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
public class DeadLetterRetryExecutor {

    private static final Duration MAX_BACKOFF = Duration.ofHours(4);

    private final EventDeadLetterRepository deadLetterRepository;
    private final EventSerializer eventSerializer;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryDeadLetter(UUID deadLetterId) {
        Optional<EventDeadLetter> maybeDeadLetter = deadLetterRepository.findByIdForUpdate(deadLetterId);
        if (maybeDeadLetter.isEmpty()) {
            return;
        }

        EventDeadLetter deadLetter = maybeDeadLetter.get();
        if (deadLetter.getStatus() != EventDeadLetterStatus.PENDING_RETRY) {
            return;
        }

        try {
            deadLetter.setStatus(EventDeadLetterStatus.RETRYING);
            deadLetterRepository.saveAndFlush(deadLetter);

            AuditableEvent event = eventSerializer.deserialize(deadLetter.getPayload(), deadLetter.getEventType());
            eventPublisher.publishEvent(event);

            deadLetter.setStatus(EventDeadLetterStatus.RESOLVED);
            deadLetter.setLastRetryAt(Instant.now());
            deadLetter.setNextRetryAt(null);
            deadLetterRepository.saveAndFlush(deadLetter);
        } catch (Exception e) {
            int newRetries = deadLetter.getRetries() + 1;
            deadLetter.setRetries(newRetries);
            deadLetter.setLastRetryAt(Instant.now());
            deadLetter.setErrorMessage(e.getMessage());

            if (newRetries >= deadLetter.getMaxRetries()) {
                deadLetter.setStatus(EventDeadLetterStatus.FAILED);
                deadLetter.setNextRetryAt(null);
                log.warn("Dead letter {} exceeded max retries ({}). Event type: {}, correlation: {}",
                        deadLetter.getId(), deadLetter.getMaxRetries(),
                        deadLetter.getEventType(), deadLetter.getCorrelationId());
            } else {
                long backoffMinutes = (long) (5 * Math.pow(3, newRetries));
                Duration backoff = Duration.ofMinutes(Math.min(backoffMinutes, MAX_BACKOFF.toMinutes()));
                deadLetter.setNextRetryAt(Instant.now().plus(backoff));
                deadLetter.setStatus(EventDeadLetterStatus.PENDING_RETRY);
            }
            deadLetterRepository.saveAndFlush(deadLetter);
        }
    }
}
