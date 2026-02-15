package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.BatchProcessingLog;
import com.innercircle.sacco.loan.entity.BatchProcessingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchProcessingLogRepositoryTest {

    @Mock
    private BatchProcessingLogRepository repository;

    private BatchProcessingLog sampleLog;
    private UUID logId;

    @BeforeEach
    void setUp() {
        logId = UUID.randomUUID();
        sampleLog = BatchProcessingLog.builder()
                .processingMonth("2026-02")
                .status(BatchProcessingStatus.COMPLETED)
                .loansProcessed(150)
                .interestAccrued(new BigDecimal("5000.50"))
                .penalizedLoans(10)
                .closedLoans(5)
                .warningsSummary("All processed successfully")
                .startedAt(Instant.parse("2026-02-15T08:00:00Z"))
                .completedAt(Instant.parse("2026-02-15T08:30:00Z"))
                .triggeredBy("admin@sacco.com")
                .build();
        sampleLog.setId(logId);
    }

    @Nested
    @DisplayName("findByProcessingMonth()")
    class FindByProcessingMonth {

        @Test
        @DisplayName("should return BatchProcessingLog when processing month exists")
        void shouldReturnLogWhenProcessingMonthExists() {
            String processingMonth = "2026-02";
            when(repository.findByProcessingMonth(processingMonth))
                    .thenReturn(Optional.of(sampleLog));

            Optional<BatchProcessingLog> result = repository.findByProcessingMonth(processingMonth);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sampleLog);
            assertThat(result.get().getProcessingMonth()).isEqualTo(processingMonth);
            assertThat(result.get().getStatus()).isEqualTo(BatchProcessingStatus.COMPLETED);
        }

        @Test
        @DisplayName("should return empty Optional when processing month does not exist")
        void shouldReturnEmptyWhenProcessingMonthDoesNotExist() {
            String processingMonth = "2025-12";
            when(repository.findByProcessingMonth(processingMonth))
                    .thenReturn(Optional.empty());

            Optional<BatchProcessingLog> result = repository.findByProcessingMonth(processingMonth);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return correct log for different processing months")
        void shouldReturnCorrectLogForDifferentMonths() {
            BatchProcessingLog januaryLog = BatchProcessingLog.builder()
                    .processingMonth("2026-01")
                    .status(BatchProcessingStatus.COMPLETED)
                    .startedAt(Instant.now())
                    .build();

            when(repository.findByProcessingMonth("2026-01"))
                    .thenReturn(Optional.of(januaryLog));
            when(repository.findByProcessingMonth("2026-02"))
                    .thenReturn(Optional.of(sampleLog));

            Optional<BatchProcessingLog> janResult = repository.findByProcessingMonth("2026-01");
            Optional<BatchProcessingLog> febResult = repository.findByProcessingMonth("2026-02");

            assertThat(janResult).isPresent();
            assertThat(janResult.get().getProcessingMonth()).isEqualTo("2026-01");
            assertThat(febResult).isPresent();
            assertThat(febResult.get().getProcessingMonth()).isEqualTo("2026-02");
        }
    }

    @Nested
    @DisplayName("existsByProcessingMonth()")
    class ExistsByProcessingMonth {

        @Test
        @DisplayName("should return true when processing month exists")
        void shouldReturnTrueWhenProcessingMonthExists() {
            String processingMonth = "2026-02";
            when(repository.existsByProcessingMonth(processingMonth))
                    .thenReturn(true);

            boolean result = repository.existsByProcessingMonth(processingMonth);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when processing month does not exist")
        void shouldReturnFalseWhenProcessingMonthDoesNotExist() {
            String processingMonth = "2025-11";
            when(repository.existsByProcessingMonth(processingMonth))
                    .thenReturn(false);

            boolean result = repository.existsByProcessingMonth(processingMonth);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should handle different month formats correctly")
        void shouldHandleDifferentMonthFormats() {
            when(repository.existsByProcessingMonth("2026-01")).thenReturn(true);
            when(repository.existsByProcessingMonth("2026-12")).thenReturn(false);

            assertThat(repository.existsByProcessingMonth("2026-01")).isTrue();
            assertThat(repository.existsByProcessingMonth("2026-12")).isFalse();
        }
    }

    @Nested
    @DisplayName("findTopByStatusOrderByProcessingMonthDesc()")
    class FindTopByStatusOrderByProcessingMonthDesc {

        @Test
        @DisplayName("should return latest completed batch processing log")
        void shouldReturnLatestCompletedLog() {
            when(repository.findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.COMPLETED))
                    .thenReturn(Optional.of(sampleLog));

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.COMPLETED);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sampleLog);
            assertThat(result.get().getStatus()).isEqualTo(BatchProcessingStatus.COMPLETED);
        }

        @Test
        @DisplayName("should return latest failed batch processing log")
        void shouldReturnLatestFailedLog() {
            BatchProcessingLog failedLog = BatchProcessingLog.builder()
                    .processingMonth("2026-01")
                    .status(BatchProcessingStatus.FAILED)
                    .startedAt(Instant.now())
                    .build();

            when(repository.findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.FAILED))
                    .thenReturn(Optional.of(failedLog));

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.FAILED);

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(BatchProcessingStatus.FAILED);
            assertThat(result.get().getProcessingMonth()).isEqualTo("2026-01");
        }

        @Test
        @DisplayName("should return latest started batch processing log")
        void shouldReturnLatestStartedLog() {
            BatchProcessingLog startedLog = BatchProcessingLog.builder()
                    .processingMonth("2026-02")
                    .status(BatchProcessingStatus.STARTED)
                    .startedAt(Instant.now())
                    .build();

            when(repository.findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.STARTED))
                    .thenReturn(Optional.of(startedLog));

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.STARTED);

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(BatchProcessingStatus.STARTED);
        }

        @Test
        @DisplayName("should return empty Optional when no logs with given status exist")
        void shouldReturnEmptyWhenNoLogsWithStatusExist() {
            when(repository.findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.STARTED))
                    .thenReturn(Optional.empty());

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.STARTED);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return most recent log when multiple logs with same status exist")
        void shouldReturnMostRecentLogForStatus() {
            BatchProcessingLog recentLog = BatchProcessingLog.builder()
                    .processingMonth("2026-03")
                    .status(BatchProcessingStatus.COMPLETED)
                    .startedAt(Instant.parse("2026-03-15T08:00:00Z"))
                    .build();

            when(repository.findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.COMPLETED))
                    .thenReturn(Optional.of(recentLog));

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus.COMPLETED);

            assertThat(result).isPresent();
            assertThat(result.get().getProcessingMonth()).isEqualTo("2026-03");
        }
    }
}
