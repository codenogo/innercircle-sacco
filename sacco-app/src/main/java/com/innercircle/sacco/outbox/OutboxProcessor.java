package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.outbox.EventOutboxRepository;
import com.innercircle.sacco.common.outbox.EventOutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class OutboxProcessor {

    private final EventOutboxRepository outboxRepository;
    private final OutboxEventExecutor outboxEventExecutor;

    public OutboxProcessor(EventOutboxRepository outboxRepository,
                           OutboxEventExecutor outboxEventExecutor) {
        this.outboxRepository = outboxRepository;
        this.outboxEventExecutor = outboxEventExecutor;
    }

    @Scheduled(fixedDelayString = "${sacco.outbox.processing.fixed-delay-ms:1000}")
    public void processOutbox() {
        List<UUID> pendingEventIds = outboxRepository
                .findIdsByStatusOrderByCreatedAtAsc(EventOutboxStatus.PENDING, PageRequest.of(0, 50));

        for (UUID outboxId : pendingEventIds) {
            try {
                outboxEventExecutor.processEvent(outboxId);
            } catch (Exception e) {
                log.error("Unexpected executor failure for outbox event {}: {}", outboxId, e.getMessage(), e);
            }
        }
    }
}
