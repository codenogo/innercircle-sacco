package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.event.AuditableEvent;
import com.innercircle.sacco.common.outbox.EventDeadLetter;
import com.innercircle.sacco.common.outbox.EventDeadLetterRepository;
import com.innercircle.sacco.common.outbox.EventDeadLetterStatus;
import com.innercircle.sacco.common.outbox.EventSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
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
class DeadLetterRetryExecutorTest {

    @Mock
    private EventDeadLetterRepository deadLetterRepository;

    @Mock
    private EventSerializer eventSerializer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeadLetterRetryExecutor deadLetterRetryExecutor;

    @Test
    void retryDeadLetter_whenPublishSucceeds_shouldResolveDeadLetter() {
        UUID id = UUID.randomUUID();
        EventDeadLetter deadLetter = buildPendingDeadLetter(id, 1, 5);
        AuditableEvent event = mock(AuditableEvent.class);
        when(deadLetterRepository.findByIdForUpdate(id)).thenReturn(Optional.of(deadLetter));
        when(eventSerializer.deserialize(deadLetter.getPayload(), deadLetter.getEventType())).thenReturn(event);

        deadLetterRetryExecutor.retryDeadLetter(id);

        assertThat(deadLetter.getStatus()).isEqualTo(EventDeadLetterStatus.RESOLVED);
        assertThat(deadLetter.getLastRetryAt()).isNotNull();
        assertThat(deadLetter.getNextRetryAt()).isNull();
        verify(eventPublisher).publishEvent(event);
        verify(deadLetterRepository, times(2)).saveAndFlush(deadLetter);
    }

    @Test
    void retryDeadLetter_whenPublishFailsAndRetriesRemain_shouldReschedule() {
        UUID id = UUID.randomUUID();
        EventDeadLetter deadLetter = buildPendingDeadLetter(id, 1, 5);
        AuditableEvent event = mock(AuditableEvent.class);
        when(deadLetterRepository.findByIdForUpdate(id)).thenReturn(Optional.of(deadLetter));
        when(eventSerializer.deserialize(deadLetter.getPayload(), deadLetter.getEventType())).thenReturn(event);
        doThrow(new RuntimeException("retry failed")).when(eventPublisher).publishEvent(any(Object.class));

        Instant before = Instant.now();
        deadLetterRetryExecutor.retryDeadLetter(id);
        Instant after = Instant.now();

        assertThat(deadLetter.getStatus()).isEqualTo(EventDeadLetterStatus.PENDING_RETRY);
        assertThat(deadLetter.getRetries()).isEqualTo(2);
        assertThat(deadLetter.getLastRetryAt()).isNotNull();
        assertThat(deadLetter.getErrorMessage()).isEqualTo("retry failed");
        assertThat(deadLetter.getNextRetryAt()).isAfter(before).isAfter(after);
        verify(deadLetterRepository, times(2)).saveAndFlush(deadLetter);
    }

    @Test
    void retryDeadLetter_whenPublishFailsAndRetriesExhausted_shouldMarkFailed() {
        UUID id = UUID.randomUUID();
        EventDeadLetter deadLetter = buildPendingDeadLetter(id, 4, 5);
        AuditableEvent event = mock(AuditableEvent.class);
        when(deadLetterRepository.findByIdForUpdate(id)).thenReturn(Optional.of(deadLetter));
        when(eventSerializer.deserialize(deadLetter.getPayload(), deadLetter.getEventType())).thenReturn(event);
        doThrow(new RuntimeException("final failure")).when(eventPublisher).publishEvent(any(Object.class));

        deadLetterRetryExecutor.retryDeadLetter(id);

        assertThat(deadLetter.getStatus()).isEqualTo(EventDeadLetterStatus.FAILED);
        assertThat(deadLetter.getRetries()).isEqualTo(5);
        assertThat(deadLetter.getNextRetryAt()).isNull();
        verify(deadLetterRepository, times(2)).saveAndFlush(deadLetter);
    }

    @Test
    void retryDeadLetter_whenNotFound_shouldDoNothing() {
        UUID id = UUID.randomUUID();
        when(deadLetterRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        deadLetterRetryExecutor.retryDeadLetter(id);

        verify(deadLetterRepository, never()).saveAndFlush(any(EventDeadLetter.class));
        verify(eventSerializer, never()).deserialize(any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void retryDeadLetter_whenStatusIsNotPendingRetry_shouldDoNothing() {
        UUID id = UUID.randomUUID();
        EventDeadLetter deadLetter = buildPendingDeadLetter(id, 1, 5);
        deadLetter.setStatus(EventDeadLetterStatus.RESOLVED);
        when(deadLetterRepository.findByIdForUpdate(id)).thenReturn(Optional.of(deadLetter));

        deadLetterRetryExecutor.retryDeadLetter(id);

        verify(deadLetterRepository, never()).saveAndFlush(any(EventDeadLetter.class));
        verify(eventSerializer, never()).deserialize(any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private EventDeadLetter buildPendingDeadLetter(UUID id, int retries, int maxRetries) {
        EventDeadLetter deadLetter = new EventDeadLetter();
        deadLetter.setId(id);
        deadLetter.setEventType("com.innercircle.sacco.common.event.TestEvent");
        deadLetter.setPayload("{\"ok\":true}");
        deadLetter.setStatus(EventDeadLetterStatus.PENDING_RETRY);
        deadLetter.setRetries(retries);
        deadLetter.setMaxRetries(maxRetries);
        deadLetter.setCorrelationId(UUID.randomUUID());
        return deadLetter;
    }
}
