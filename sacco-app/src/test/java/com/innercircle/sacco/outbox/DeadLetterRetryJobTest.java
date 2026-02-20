package com.innercircle.sacco.outbox;

import com.innercircle.sacco.common.outbox.EventDeadLetterRepository;
import com.innercircle.sacco.common.outbox.EventDeadLetterStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterRetryJobTest {

    @Mock
    private EventDeadLetterRepository deadLetterRepository;

    @Mock
    private DeadLetterRetryExecutor deadLetterRetryExecutor;

    @InjectMocks
    private DeadLetterRetryJob deadLetterRetryJob;

    @Test
    void retryDeadLetters_shouldDelegateRetryToExecutor() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(deadLetterRepository.findIdsByStatusAndNextRetryAtBeforeAndRetriesLessThan(
                org.mockito.ArgumentMatchers.eq(EventDeadLetterStatus.PENDING_RETRY),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 50))))
                .thenReturn(List.of(first, second));

        deadLetterRetryJob.retryDeadLetters();

        verify(deadLetterRetryExecutor).retryDeadLetter(first);
        verify(deadLetterRetryExecutor).retryDeadLetter(second);
    }

    @Test
    void retryDeadLetters_whenExecutorFails_shouldContinue() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(deadLetterRepository.findIdsByStatusAndNextRetryAtBeforeAndRetriesLessThan(
                org.mockito.ArgumentMatchers.eq(EventDeadLetterStatus.PENDING_RETRY),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 50))))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("fail")).when(deadLetterRetryExecutor).retryDeadLetter(first);

        deadLetterRetryJob.retryDeadLetters();

        verify(deadLetterRetryExecutor).retryDeadLetter(first);
        verify(deadLetterRetryExecutor).retryDeadLetter(second);
    }

    @Test
    void retryDeadLetters_whenNoRetryableEntries_shouldDoNothing() {
        when(deadLetterRepository.findIdsByStatusAndNextRetryAtBeforeAndRetriesLessThan(
                org.mockito.ArgumentMatchers.eq(EventDeadLetterStatus.PENDING_RETRY),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 50))))
                .thenReturn(Collections.emptyList());

        deadLetterRetryJob.retryDeadLetters();

        verifyNoInteractions(deadLetterRetryExecutor);
    }
}
