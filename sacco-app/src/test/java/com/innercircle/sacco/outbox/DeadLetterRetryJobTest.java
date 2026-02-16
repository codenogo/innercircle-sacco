package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.event.AuditableEvent;
import com.innercircle.sacco.common.outbox.EventDeadLetter;
import com.innercircle.sacco.common.outbox.EventDeadLetterRepository;
import com.innercircle.sacco.common.outbox.EventDeadLetterStatus;
import com.innercircle.sacco.common.outbox.EventSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterRetryJobTest {

    @Mock
    private EventDeadLetterRepository deadLetterRepository;

    @Mock
    private EventSerializer eventSerializer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeadLetterRetryJob deadLetterRetryJob;

    @Test
    void retryEvent_onSuccess_marksResolved() {
        EventDeadLetter deadLetter = new EventDeadLetter();
        deadLetter.setId(UUID.randomUUID());
        deadLetter.setEventType("com.innercircle.sacco.common.event.TestEvent");
        deadLetter.setPayload("{\"test\":true}");
        deadLetter.setStatus(EventDeadLetterStatus.PENDING_RETRY);
        deadLetter.setRetries(1);
        deadLetter.setMaxRetries(5);
        deadLetter.setCorrelationId(UUID.randomUUID());

        AuditableEvent mockEvent = mock(AuditableEvent.class);
        when(eventSerializer.deserialize(deadLetter.getPayload(), deadLetter.getEventType()))
                .thenReturn(mockEvent);

        deadLetterRetryJob.retryEvent(deadLetter);

        assertThat(deadLetter.getStatus()).isEqualTo(EventDeadLetterStatus.RESOLVED);
        assertThat(deadLetter.getLastRetryAt()).isNotNull();
        verify(eventPublisher).publishEvent(mockEvent);
        verify(deadLetterRepository, org.mockito.Mockito.times(2)).save(deadLetter);
    }

    @Test
    void retryEvent_onFailure_incrementsRetries() {
        EventDeadLetter deadLetter = new EventDeadLetter();
        deadLetter.setId(UUID.randomUUID());
        deadLetter.setEventType("com.innercircle.sacco.common.event.TestEvent");
        deadLetter.setPayload("{\"test\":true}");
        deadLetter.setStatus(EventDeadLetterStatus.PENDING_RETRY);
        deadLetter.setRetries(1);
        deadLetter.setMaxRetries(5);
        deadLetter.setCorrelationId(UUID.randomUUID());

        AuditableEvent mockEvent = mock(AuditableEvent.class);
        when(eventSerializer.deserialize(deadLetter.getPayload(), deadLetter.getEventType()))
                .thenReturn(mockEvent);
        doThrow(new RuntimeException("retry failed")).when(eventPublisher).publishEvent(any(Object.class));

        deadLetterRetryJob.retryEvent(deadLetter);

        assertThat(deadLetter.getRetries()).isEqualTo(2);
        assertThat(deadLetter.getStatus()).isEqualTo(EventDeadLetterStatus.PENDING_RETRY);
        assertThat(deadLetter.getLastRetryAt()).isNotNull();
        assertThat(deadLetter.getNextRetryAt()).isNotNull();
        assertThat(deadLetter.getErrorMessage()).isEqualTo("retry failed");
        verify(deadLetterRepository, org.mockito.Mockito.times(2)).save(deadLetter);
    }

    @Test
    void retryEvent_maxRetriesExceeded_marksFailed() {
        EventDeadLetter deadLetter = new EventDeadLetter();
        deadLetter.setId(UUID.randomUUID());
        deadLetter.setEventType("com.innercircle.sacco.common.event.TestEvent");
        deadLetter.setPayload("{\"test\":true}");
        deadLetter.setStatus(EventDeadLetterStatus.PENDING_RETRY);
        deadLetter.setRetries(4); // maxRetries - 1
        deadLetter.setMaxRetries(5);
        deadLetter.setCorrelationId(UUID.randomUUID());

        AuditableEvent mockEvent = mock(AuditableEvent.class);
        when(eventSerializer.deserialize(deadLetter.getPayload(), deadLetter.getEventType()))
                .thenReturn(mockEvent);
        doThrow(new RuntimeException("final failure")).when(eventPublisher).publishEvent(any(Object.class));

        deadLetterRetryJob.retryEvent(deadLetter);

        assertThat(deadLetter.getRetries()).isEqualTo(5);
        assertThat(deadLetter.getStatus()).isEqualTo(EventDeadLetterStatus.FAILED);
        assertThat(deadLetter.getLastRetryAt()).isNotNull();
        assertThat(deadLetter.getErrorMessage()).isEqualTo("final failure");
        verify(deadLetterRepository, org.mockito.Mockito.times(2)).save(deadLetter);
    }

    @Test
    void retryDeadLetters_passesCorrectQueryParameters() {
        ArgumentCaptor<EventDeadLetterStatus> statusCaptor = ArgumentCaptor.forClass(EventDeadLetterStatus.class);
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Integer> retriesCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);

        when(deadLetterRepository.findByStatusAndNextRetryAtBeforeAndRetriesLessThan(
                statusCaptor.capture(), instantCaptor.capture(), retriesCaptor.capture(), pageCaptor.capture()))
                .thenReturn(java.util.Collections.emptyList());

        Instant before = Instant.now();
        deadLetterRetryJob.retryDeadLetters();
        Instant after = Instant.now();

        assertThat(statusCaptor.getValue()).isEqualTo(EventDeadLetterStatus.PENDING_RETRY);
        assertThat(instantCaptor.getValue()).isBetween(before, after);
        assertThat(retriesCaptor.getValue()).isEqualTo(5);
        assertThat(pageCaptor.getValue()).isEqualTo(PageRequest.of(0, 50));
    }
}
