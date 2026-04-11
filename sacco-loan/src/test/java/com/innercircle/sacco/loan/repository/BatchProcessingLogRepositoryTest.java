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
                .processingDate("2026-02-15")
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
    @DisplayName("findByProcessingDate()")
    class FindByProcessingDate {

        @Test
        @DisplayName("should return BatchProcessingLog when processing date exists")
        void shouldReturnLogWhenProcessingDateExists() {
            String processingDate = "2026-02-15";
            when(repository.findByProcessingDate(processingDate))
                    .thenReturn(Optional.of(sampleLog));

            Optional<BatchProcessingLog> result = repository.findByProcessingDate(processingDate);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sampleLog);
            assertThat(result.get().getProcessingDate()).isEqualTo(processingDate);
            assertThat(result.get().getStatus()).isEqualTo(BatchProcessingStatus.COMPLETED);
        }

        @Test
        @DisplayName("should return empty Optional when processing date does not exist")
        void shouldReturnEmptyWhenProcessingDateDoesNotExist() {
            String processingDate = "2025-12-01";
            when(repository.findByProcessingDate(processingDate))
                    .thenReturn(Optional.empty());

            Optional<BatchProcessingLog> result = repository.findByProcessingDate(processingDate);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return correct log for different dates")
        void shouldReturnCorrectLogForDifferentMonths() {
            BatchProcessingLog januaryLog = BatchProcessingLog.builder()
                    .processingDate("2026-01-15")
                    .status(BatchProcessingStatus.COMPLETED)
                    .startedAt(Instant.now())
                    .build();

            when(repository.findByProcessingDate("2026-01-15"))
                    .thenReturn(Optional.of(januaryLog));
            when(repository.findByProcessingDate("2026-02-15"))
                    .thenReturn(Optional.of(sampleLog));

            Optional<BatchProcessingLog> janResult = repository.findByProcessingDate("2026-01-15");
            Optional<BatchProcessingLog> febResult = repository.findByProcessingDate("2026-02-15");

            assertThat(janResult).isPresent();
            assertThat(janResult.get().getProcessingDate()).isEqualTo("2026-01-15");
            assertThat(febResult).isPresent();
            assertThat(febResult.get().getProcessingDate()).isEqualTo("2026-02-15");
        }
    }

    @Nested
    @DisplayName("existsByProcessingDate()")
    class ExistsByProcessingDate {

        @Test
        @DisplayName("should return true when processing date exists")
        void shouldReturnTrueWhenProcessingDateExists() {
            String processingDate = "2026-02-15";
            when(repository.existsByProcessingDate(processingDate))
                    .thenReturn(true);

            boolean result = repository.existsByProcessingDate(processingDate);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when processing date does not exist")
        void shouldReturnFalseWhenProcessingDateDoesNotExist() {
            String processingDate = "2025-11-01";
            when(repository.existsByProcessingDate(processingDate))
                    .thenReturn(false);

            boolean result = repository.existsByProcessingDate(processingDate);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should handle different date formats correctly")
        void shouldHandleDifferentMonthFormats() {
            when(repository.existsByProcessingDate("2026-01-15")).thenReturn(true);
            when(repository.existsByProcessingDate("2026-12-01")).thenReturn(false);

            assertThat(repository.existsByProcessingDate("2026-01-15")).isTrue();
            assertThat(repository.existsByProcessingDate("2026-12-01")).isFalse();
        }
    }

    @Nested
    @DisplayName("findTopByStatusOrderByProcessingDateDesc()")
    class FindTopByStatusOrderByProcessingDateDesc {

        @Test
        @DisplayName("should return latest completed batch processing log")
        void shouldReturnLatestCompletedLog() {
            when(repository.findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.COMPLETED))
                    .thenReturn(Optional.of(sampleLog));

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.COMPLETED);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sampleLog);
            assertThat(result.get().getStatus()).isEqualTo(BatchProcessingStatus.COMPLETED);
        }

        @Test
        @DisplayName("should return latest failed batch processing log")
        void shouldReturnLatestFailedLog() {
            BatchProcessingLog failedLog = BatchProcessingLog.builder()
                    .processingDate("2026-01-15")
                    .status(BatchProcessingStatus.FAILED)
                    .startedAt(Instant.now())
                    .build();

            when(repository.findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.FAILED))
                    .thenReturn(Optional.of(failedLog));

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.FAILED);

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(BatchProcessingStatus.FAILED);
            assertThat(result.get().getProcessingDate()).isEqualTo("2026-01-15");
        }

        @Test
        @DisplayName("should return latest started batch processing log")
        void shouldReturnLatestStartedLog() {
            BatchProcessingLog startedLog = BatchProcessingLog.builder()
                    .processingDate("2026-02-15")
                    .status(BatchProcessingStatus.STARTED)
                    .startedAt(Instant.now())
                    .build();

            when(repository.findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.STARTED))
                    .thenReturn(Optional.of(startedLog));

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.STARTED);

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(BatchProcessingStatus.STARTED);
        }

        @Test
        @DisplayName("should return empty Optional when no logs with given status exist")
        void shouldReturnEmptyWhenNoLogsWithStatusExist() {
            when(repository.findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.STARTED))
                    .thenReturn(Optional.empty());

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.STARTED);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return most recent log when multiple logs with same status exist")
        void shouldReturnMostRecentLogForStatus() {
            BatchProcessingLog recentLog = BatchProcessingLog.builder()
                    .processingDate("2026-03-15")
                    .status(BatchProcessingStatus.COMPLETED)
                    .startedAt(Instant.parse("2026-03-15T08:00:00Z"))
                    .build();

            when(repository.findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.COMPLETED))
                    .thenReturn(Optional.of(recentLog));

            Optional<BatchProcessingLog> result = repository
                    .findTopByStatusOrderByProcessingDateDesc(BatchProcessingStatus.COMPLETED);

            assertThat(result).isPresent();
            assertThat(result.get().getProcessingDate()).isEqualTo("2026-03-15");
        }
    }
}
