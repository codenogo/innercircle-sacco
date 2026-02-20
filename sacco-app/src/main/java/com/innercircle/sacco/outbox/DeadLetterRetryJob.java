package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.outbox.EventDeadLetterRepository;
import com.innercircle.sacco.common.outbox.EventDeadLetterStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class DeadLetterRetryJob {

    private static final int MAX_RETRIES = 5;

    private final EventDeadLetterRepository deadLetterRepository;
    private final DeadLetterRetryExecutor deadLetterRetryExecutor;

    public DeadLetterRetryJob(EventDeadLetterRepository deadLetterRepository,
                              DeadLetterRetryExecutor deadLetterRetryExecutor) {
        this.deadLetterRepository = deadLetterRepository;
        this.deadLetterRetryExecutor = deadLetterRetryExecutor;
    }

    @Scheduled(fixedDelay = 300000)
    public void retryDeadLetters() {
        List<UUID> retryableIds = deadLetterRepository
                .findIdsByStatusAndNextRetryAtBeforeAndRetriesLessThan(
                        EventDeadLetterStatus.PENDING_RETRY, Instant.now(), MAX_RETRIES, PageRequest.of(0, 50));

        for (UUID deadLetterId : retryableIds) {
            try {
                deadLetterRetryExecutor.retryDeadLetter(deadLetterId);
            } catch (Exception e) {
                log.error("Unexpected executor failure for dead letter {}: {}",
                        deadLetterId, e.getMessage(), e);
            }
        }
    }
}
