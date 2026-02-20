package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.outbox.EventOutboxRepository;
import com.innercircle.sacco.common.outbox.EventOutboxStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private EventOutboxRepository outboxRepository;

    @Mock
    private OutboxEventExecutor outboxEventExecutor;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    void processOutbox_shouldDelegateEachPendingIdToExecutor() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(outboxRepository.findIdsByStatusOrderByCreatedAtAsc(
                EventOutboxStatus.PENDING, PageRequest.of(0, 50)))
                .thenReturn(List.of(first, second));

        outboxProcessor.processOutbox();

        verify(outboxEventExecutor).processEvent(first);
        verify(outboxEventExecutor).processEvent(second);
    }

    @Test
    void processOutbox_whenExecutorFails_shouldContinueRemainingIds() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(outboxRepository.findIdsByStatusOrderByCreatedAtAsc(
                EventOutboxStatus.PENDING, PageRequest.of(0, 50)))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("boom")).when(outboxEventExecutor).processEvent(first);

        outboxProcessor.processOutbox();

        verify(outboxEventExecutor).processEvent(first);
        verify(outboxEventExecutor).processEvent(second);
    }

    @Test
    void processOutbox_whenNoPendingIds_shouldDoNothing() {
        when(outboxRepository.findIdsByStatusOrderByCreatedAtAsc(
                EventOutboxStatus.PENDING, PageRequest.of(0, 50)))
                .thenReturn(Collections.emptyList());

        outboxProcessor.processOutbox();

        verify(outboxRepository, times(1))
                .findIdsByStatusOrderByCreatedAtAsc(EventOutboxStatus.PENDING, PageRequest.of(0, 50));
        verifyNoInteractions(outboxEventExecutor);
    }
}
