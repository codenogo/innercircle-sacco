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
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private EventOutboxRepository outboxRepository;

    @Mock
    private EventDeadLetterRepository deadLetterRepository;

    @Mock
    private EventSerializer eventSerializer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    void processOutbox_processesPendingEntries() {
        EventOutbox outbox = new EventOutbox();
        outbox.setId(UUID.randomUUID());
        outbox.setEventType("com.innercircle.sacco.common.event.TestEvent");
        outbox.setPayload("{\"test\":true}");
        outbox.setStatus(EventOutboxStatus.PENDING);
        outbox.setCorrelationId(UUID.randomUUID());

        AuditableEvent mockEvent = mock(AuditableEvent.class);
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(
                EventOutboxStatus.PENDING, PageRequest.of(0, 50)))
                .thenReturn(List.of(outbox));
        when(eventSerializer.deserialize(outbox.getPayload(), outbox.getEventType()))
                .thenReturn(mockEvent);

        outboxProcessor.processOutbox();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PROCESSED);
        assertThat(outbox.getProcessedAt()).isNotNull();
        verify(eventPublisher).publishEvent(mockEvent);
        verify(outboxRepository, times(2)).save(outbox);
    }

    @Test
    void processEvent_onFailure_createsDeadLetter() {
        EventOutbox outbox = new EventOutbox();
        outbox.setId(UUID.randomUUID());
        outbox.setEventType("com.innercircle.sacco.common.event.TestEvent");
        outbox.setPayload("{\"test\":true}");
        outbox.setStatus(EventOutboxStatus.PENDING);
        outbox.setCorrelationId(UUID.randomUUID());

        AuditableEvent mockEvent = mock(AuditableEvent.class);
        when(eventSerializer.deserialize(outbox.getPayload(), outbox.getEventType()))
                .thenReturn(mockEvent);
        RuntimeException error = new RuntimeException("publish failed");
        org.mockito.Mockito.doThrow(error).when(eventPublisher).publishEvent(any(Object.class));

        outboxProcessor.processEvent(outbox);

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED);

        ArgumentCaptor<EventDeadLetter> captor = ArgumentCaptor.forClass(EventDeadLetter.class);
        verify(deadLetterRepository).save(captor.capture());
        EventDeadLetter deadLetter = captor.getValue();
        assertThat(deadLetter.getOutboxId()).isEqualTo(outbox.getId());
        assertThat(deadLetter.getEventType()).isEqualTo(outbox.getEventType());
        assertThat(deadLetter.getErrorMessage()).isEqualTo("publish failed");
        assertThat(deadLetter.getRetries()).isZero();
        assertThat(deadLetter.getMaxRetries()).isEqualTo(5);
        assertThat(deadLetter.getStatus()).isEqualTo(EventDeadLetterStatus.PENDING_RETRY);
        assertThat(deadLetter.getNextRetryAt()).isNotNull();
    }

    @Test
    void processOutbox_emptyQueue_doesNothing() {
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(
                EventOutboxStatus.PENDING, PageRequest.of(0, 50)))
                .thenReturn(Collections.emptyList());

        outboxProcessor.processOutbox();

        verify(outboxRepository, never()).save(any());
        verifyNoInteractions(eventSerializer);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(deadLetterRepository);
    }

    @Test
    void processEvent_deserializationError_marksOutboxFailed() {
        EventOutbox outbox = new EventOutbox();
        outbox.setId(UUID.randomUUID());
        outbox.setEventType("com.innercircle.sacco.common.event.NonExistentEvent");
        outbox.setPayload("{\"bad\":true}");
        outbox.setStatus(EventOutboxStatus.PENDING);
        outbox.setCorrelationId(UUID.randomUUID());

        when(eventSerializer.deserialize(outbox.getPayload(), outbox.getEventType()))
                .thenThrow(new IllegalArgumentException("Unknown event type"));

        outboxProcessor.processEvent(outbox);

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED);
        verify(eventPublisher, never()).publishEvent(any());

        ArgumentCaptor<EventDeadLetter> captor = ArgumentCaptor.forClass(EventDeadLetter.class);
        verify(deadLetterRepository).save(captor.capture());
        EventDeadLetter deadLetter = captor.getValue();
        assertThat(deadLetter.getErrorMessage()).isEqualTo("Unknown event type");
    }
}
