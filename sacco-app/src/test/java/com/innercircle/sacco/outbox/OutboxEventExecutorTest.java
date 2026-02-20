package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.event.AuditableEvent;
import com.innercircle.sacco.common.outbox.EventDeadLetter;
import com.innercircle.sacco.common.outbox.EventDeadLetterRepository;
import com.innercircle.sacco.common.outbox.EventDeadLetterStatus;
import com.innercircle.sacco.common.outbox.EventOutbox;
import com.innercircle.sacco.common.outbox.EventOutboxRepository;
import com.innercircle.sacco.common.outbox.EventOutboxStatus;
import com.innercircle.sacco.common.outbox.EventSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventExecutorTest {

    @Mock
    private EventOutboxRepository outboxRepository;

    @Mock
    private EventDeadLetterRepository deadLetterRepository;

    @Mock
    private EventSerializer eventSerializer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OutboxEventExecutor outboxEventExecutor;

    @Test
    void processEvent_shouldProcessPendingEventAndResolveDeadLetter() {
        UUID outboxId = UUID.randomUUID();
        EventOutbox outbox = buildPendingOutbox(outboxId);
        EventDeadLetter deadLetter = new EventDeadLetter();
        deadLetter.setId(UUID.randomUUID());
        deadLetter.setOutboxId(outboxId);
        deadLetter.setStatus(EventDeadLetterStatus.PENDING_RETRY);
        AuditableEvent event = mock(AuditableEvent.class);

        when(outboxRepository.findByIdForUpdate(outboxId)).thenReturn(Optional.of(outbox));
        when(eventSerializer.deserialize(outbox.getPayload(), outbox.getEventType())).thenReturn(event);
        when(deadLetterRepository.findByOutboxId(outboxId)).thenReturn(Optional.of(deadLetter));

        outboxEventExecutor.processEvent(outboxId);

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PROCESSED);
        assertThat(outbox.getProcessedAt()).isNotNull();
        assertThat(deadLetter.getStatus()).isEqualTo(EventDeadLetterStatus.RESOLVED);
        assertThat(deadLetter.getLastRetryAt()).isNotNull();
        verify(eventPublisher).publishEvent(event);
        verify(outboxRepository, times(2)).saveAndFlush(outbox);
        verify(deadLetterRepository).saveAndFlush(deadLetter);
    }

    @Test
    void processEvent_whenPublishFails_shouldMarkFailedAndUpsertDeadLetter() {
        UUID outboxId = UUID.randomUUID();
        EventOutbox outbox = buildPendingOutbox(outboxId);
        AuditableEvent event = mock(AuditableEvent.class);

        when(outboxRepository.findByIdForUpdate(outboxId)).thenReturn(Optional.of(outbox));
        when(eventSerializer.deserialize(outbox.getPayload(), outbox.getEventType())).thenReturn(event);
        doThrow(new RuntimeException("publish failed")).when(eventPublisher).publishEvent(any(Object.class));
        when(deadLetterRepository.findByOutboxId(outboxId)).thenReturn(Optional.empty());

        outboxEventExecutor.processEvent(outboxId);

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED);
        verify(outboxRepository, times(2)).saveAndFlush(outbox);

        ArgumentCaptor<EventDeadLetter> captor = ArgumentCaptor.forClass(EventDeadLetter.class);
        verify(deadLetterRepository).saveAndFlush(captor.capture());
        EventDeadLetter captured = captor.getValue();
        assertThat(captured.getOutboxId()).isEqualTo(outboxId);
        assertThat(captured.getStatus()).isEqualTo(EventDeadLetterStatus.PENDING_RETRY);
        assertThat(captured.getRetries()).isZero();
        assertThat(captured.getMaxRetries()).isEqualTo(5);
        assertThat(captured.getErrorMessage()).isEqualTo("publish failed");
        assertThat(captured.getNextRetryAt()).isNotNull();
    }

    @Test
    void processEvent_whenOutboxMissing_shouldDoNothing() {
        UUID outboxId = UUID.randomUUID();
        when(outboxRepository.findByIdForUpdate(outboxId)).thenReturn(Optional.empty());

        outboxEventExecutor.processEvent(outboxId);

        verify(eventSerializer, never()).deserialize(any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(deadLetterRepository, never()).saveAndFlush(any());
    }

    @Test
    void processEvent_whenStatusNotPending_shouldDoNothing() {
        UUID outboxId = UUID.randomUUID();
        EventOutbox outbox = buildPendingOutbox(outboxId);
        outbox.setStatus(EventOutboxStatus.PROCESSED);
        when(outboxRepository.findByIdForUpdate(outboxId)).thenReturn(Optional.of(outbox));

        outboxEventExecutor.processEvent(outboxId);

        verify(eventSerializer, never()).deserialize(any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(outboxRepository, never()).saveAndFlush(any());
    }

    private EventOutbox buildPendingOutbox(UUID id) {
        EventOutbox outbox = new EventOutbox();
        outbox.setId(id);
        outbox.setEventType("com.innercircle.sacco.common.event.TestEvent");
        outbox.setPayload("{\"ok\":true}");
        outbox.setStatus(EventOutboxStatus.PENDING);
        outbox.setCorrelationId(UUID.randomUUID());
        return outbox;
    }
}
