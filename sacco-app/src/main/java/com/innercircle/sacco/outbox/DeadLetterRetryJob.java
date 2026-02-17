package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.event.AuditableEvent;
import com.innercircle.sacco.common.outbox.EventDeadLetter;
import com.innercircle.sacco.common.outbox.EventDeadLetterRepository;
import com.innercircle.sacco.common.outbox.EventDeadLetterStatus;
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
public class DeadLetterRetryJob {

    private static final Duration MAX_BACKOFF = Duration.ofHours(4);

    private final EventDeadLetterRepository deadLetterRepository;
    private final EventSerializer eventSerializer;
    private final ApplicationEventPublisher eventPublisher;

    public DeadLetterRetryJob(EventDeadLetterRepository deadLetterRepository,
                              EventSerializer eventSerializer,
                              ApplicationEventPublisher eventPublisher) {
        this.deadLetterRepository = deadLetterRepository;
        this.eventSerializer = eventSerializer;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 300000)
    public void retryDeadLetters() {
        List<EventDeadLetter> retryable = deadLetterRepository
                .findByStatusAndNextRetryAtBeforeAndRetriesLessThan(
                        EventDeadLetterStatus.PENDING_RETRY, Instant.now(), 5, PageRequest.of(0, 50));

        for (EventDeadLetter deadLetter : retryable) {
            retryEvent(deadLetter);
        }
    }

    @Transactional
    public void retryEvent(EventDeadLetter deadLetter) {
        try {
            deadLetter.setStatus(EventDeadLetterStatus.RETRYING);
            deadLetterRepository.save(deadLetter);

            AuditableEvent event = eventSerializer.deserialize(deadLetter.getPayload(), deadLetter.getEventType());
            eventPublisher.publishEvent(event);

            deadLetter.setStatus(EventDeadLetterStatus.RESOLVED);
            deadLetter.setLastRetryAt(Instant.now());
            deadLetterRepository.save(deadLetter);
        } catch (Exception e) {
            int newRetries = deadLetter.getRetries() + 1;
            deadLetter.setRetries(newRetries);
            deadLetter.setLastRetryAt(Instant.now());
            deadLetter.setErrorMessage(e.getMessage());

            if (newRetries >= deadLetter.getMaxRetries()) {
                deadLetter.setStatus(EventDeadLetterStatus.FAILED);
                log.warn("Dead letter event {} has exceeded max retries ({}). Event type: {}, correlation: {}",
                        deadLetter.getId(), deadLetter.getMaxRetries(),
                        deadLetter.getEventType(), deadLetter.getCorrelationId());
            } else {
                long backoffMinutes = (long) (5 * Math.pow(3, newRetries));
                Duration backoff = Duration.ofMinutes(Math.min(backoffMinutes, MAX_BACKOFF.toMinutes()));
                deadLetter.setNextRetryAt(Instant.now().plus(backoff));
                deadLetter.setStatus(EventDeadLetterStatus.PENDING_RETRY);
            }
            deadLetterRepository.save(deadLetter);
        }
    }
}
