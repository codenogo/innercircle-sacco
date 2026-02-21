package com.innercircle.sacco.loan.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.loan.dto.BatchProcessingResult;
import com.innercircle.sacco.loan.service.LoanBatchService;
import com.innercircle.sacco.loan.service.LoanReversalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanBatchController")
class LoanBatchControllerTest {

    @Mock
    private LoanBatchService batchService;

    @Mock
    private LoanReversalService reversalService;

    @InjectMocks
    private LoanBatchController controller;

    @Nested
    @DisplayName("processBatch")
    class ProcessBatch {

        @Test
        @DisplayName("should call processOutstandingLoans when targetDate is null")
        void shouldProcessOutstandingLoansWhenNoTargetMonth() {
            // Given
            BatchProcessingResult expectedResult = BatchProcessingResult.builder()
                    .processedLoans(5)
                    .penalizedLoans(1)
                    .closedLoans(0)
                    .interestAccruedLoans(4)
                    .totalInterestAccrued(new BigDecimal("5000.00"))
                    .processedAt(Instant.now())
                    .message("Processed 5 loans")
                    .build();

            when(batchService.processOutstandingLoans()).thenReturn(expectedResult);

            // When
            ApiResponse<BatchProcessingResult> response = controller.processBatch(null);

            // Then
            verify(batchService).processOutstandingLoans();
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(expectedResult);
            assertThat(response.getMessage()).isEqualTo("Processed 5 loans");
        }

        @Test
        @DisplayName("should call processDailyLoans when targetDate is provided")
        void shouldProcessDailyLoansWhenTargetDateProvided() {
            // Given
            LocalDate targetDate = LocalDate.of(2026, 3, 1);
            BatchProcessingResult expectedResult = BatchProcessingResult.builder()
                    .processedLoans(8)
                    .penalizedLoans(2)
                    .closedLoans(1)
                    .interestAccruedLoans(5)
                    .totalInterestAccrued(new BigDecimal("8500.00"))
                    .processedAt(Instant.now())
                    .message("Processed 8 loans for 2026-03-01")
                    .processingDate("2026-03-01")
                    .build();

            when(batchService.processDailyLoans(eq(targetDate), any(String.class)))
                    .thenReturn(expectedResult);

            // When
            ApiResponse<BatchProcessingResult> response = controller.processBatch(targetDate);

            // Then
            verify(batchService).processDailyLoans(eq(targetDate), any(String.class));
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(expectedResult);
            assertThat(response.getMessage()).isEqualTo("Processed 8 loans for 2026-03-01");
        }

        @Test
        @DisplayName("should include warnings in response when present")
        void shouldIncludeWarningsInResponse() {
            // Given
            List<String> warnings = List.of(
                    "1 unpaid loan detected",
                    "2 loans are overdue by more than 30 days"
            );

            BatchProcessingResult resultWithWarnings = BatchProcessingResult.builder()
                    .processedLoans(5)
                    .penalizedLoans(1)
                    .closedLoans(0)
                    .interestAccruedLoans(4)
                    .totalInterestAccrued(new BigDecimal("5000.00"))
                    .processedAt(Instant.now())
                    .message("Processed 5 loans")
                    .warnings(warnings)
                    .build();

            when(batchService.processOutstandingLoans()).thenReturn(resultWithWarnings);

            // When
            ApiResponse<BatchProcessingResult> response = controller.processBatch(null);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getWarnings()).isNotNull();
            assertThat(response.getData().getWarnings()).hasSize(2);
            assertThat(response.getData().getWarnings()).containsExactlyInAnyOrder(
                    "1 unpaid loan detected",
                    "2 loans are overdue by more than 30 days"
            );
        }

        @Test
        @DisplayName("should include processingDate in response when present")
        void shouldIncludeProcessingDateInResponse() {
            // Given
            LocalDate targetDate = LocalDate.of(2026, 3, 1);
            BatchProcessingResult resultWithDate = BatchProcessingResult.builder()
                    .processedLoans(5)
                    .penalizedLoans(1)
                    .closedLoans(0)
                    .interestAccruedLoans(4)
                    .totalInterestAccrued(new BigDecimal("5000.00"))
                    .processedAt(Instant.now())
                    .message("Processed 5 loans for 2026-03-01")
                    .processingDate("2026-03-01")
                    .build();

            when(batchService.processDailyLoans(eq(targetDate), any(String.class)))
                    .thenReturn(resultWithDate);

            // When
            ApiResponse<BatchProcessingResult> response = controller.processBatch(targetDate);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getProcessingDate()).isNotNull();
            assertThat(response.getData().getProcessingDate()).isEqualTo("2026-03-01");
        }

        @Test
        @DisplayName("should handle zero processed loans")
        void shouldHandleZeroProcessedLoans() {
            // Given
            BatchProcessingResult emptyResult = BatchProcessingResult.builder()
                    .processedLoans(0)
                    .penalizedLoans(0)
                    .closedLoans(0)
                    .interestAccruedLoans(0)
                    .totalInterestAccrued(BigDecimal.ZERO)
                    .processedAt(Instant.now())
                    .message("No loans to process")
                    .build();

            when(batchService.processOutstandingLoans()).thenReturn(emptyResult);

            // When
            ApiResponse<BatchProcessingResult> response = controller.processBatch(null);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getProcessedLoans()).isZero();
            assertThat(response.getMessage()).isEqualTo("No loans to process");
        }
    }

    @Nested
    @DisplayName("handleIllegalState")
    class HandleIllegalState {

        @Test
        @DisplayName("should return 409 CONFLICT with error message")
        void shouldReturn409ConflictWithErrorMessage() {
            // Given
            IllegalStateException exception = new IllegalStateException("Date already processed: 2026-01-01");

            // When
            ResponseEntity<ApiResponse<Void>> response = controller.handleIllegalState(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).isEqualTo("Date already processed: 2026-01-01");
            assertThat(response.getBody().getData()).isNull();
        }

        @Test
        @DisplayName("should handle different error messages")
        void shouldHandleDifferentErrorMessages() {
            // Given
            IllegalStateException exception = new IllegalStateException("Batch processing already in progress");

            // When
            ResponseEntity<ApiResponse<Void>> response = controller.handleIllegalState(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Batch processing already in progress");
        }

        @Test
        @DisplayName("should handle empty error message")
        void shouldHandleEmptyErrorMessage() {
            // Given
            IllegalStateException exception = new IllegalStateException("");

            // When
            ResponseEntity<ApiResponse<Void>> response = controller.handleIllegalState(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Integration scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("should handle comprehensive batch result with all fields populated")
        void shouldHandleComprehensiveBatchResult() {
            // Given
            LocalDate targetDate = LocalDate.of(2026, 3, 1);
            BatchProcessingResult comprehensiveResult = BatchProcessingResult.builder()
                    .processedLoans(15)
                    .penalizedLoans(3)
                    .closedLoans(2)
                    .interestAccruedLoans(10)
                    .totalInterestAccrued(new BigDecimal("15750.50"))
                    .processedAt(Instant.now())
                    .message("Successfully processed 2026-03-01 batch")
                    .warnings(List.of(
                            "3 loans penalized for late payment",
                            "2 loans fully paid and closed"
                    ))
                    .processingDate("2026-03-01")
                    .build();

            when(batchService.processDailyLoans(eq(targetDate), any(String.class)))
                    .thenReturn(comprehensiveResult);

            // When
            ApiResponse<BatchProcessingResult> response = controller.processBatch(targetDate);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();

            BatchProcessingResult result = response.getData();
            assertThat(result.getProcessedLoans()).isEqualTo(15);
            assertThat(result.getPenalizedLoans()).isEqualTo(3);
            assertThat(result.getClosedLoans()).isEqualTo(2);
            assertThat(result.getInterestAccruedLoans()).isEqualTo(10);
            assertThat(result.getTotalInterestAccrued()).isEqualByComparingTo("15750.50");
            assertThat(result.getProcessedAt()).isNotNull();
            assertThat(result.getMessage()).isEqualTo("Successfully processed 2026-03-01 batch");
            assertThat(result.getWarnings()).hasSize(2);
            assertThat(result.getProcessingDate()).isEqualTo("2026-03-01");
        }

        @Test
        @DisplayName("should handle different target dates correctly")
        void shouldHandleDifferentTargetDates() {
            // Given
            LocalDate januaryDate = LocalDate.of(2026, 1, 1);

            BatchProcessingResult januaryResult = BatchProcessingResult.builder()
                    .processedLoans(10)
                    .penalizedLoans(1)
                    .closedLoans(0)
                    .interestAccruedLoans(9)
                    .totalInterestAccrued(new BigDecimal("10000.00"))
                    .processedAt(Instant.now())
                    .message("Processed 2026-01-01")
                    .processingDate("2026-01-01")
                    .build();

            when(batchService.processDailyLoans(eq(januaryDate), any(String.class)))
                    .thenReturn(januaryResult);

            // When
            ApiResponse<BatchProcessingResult> response = controller.processBatch(januaryDate);

            // Then
            verify(batchService).processDailyLoans(eq(januaryDate), any(String.class));
            assertThat(response.getData().getProcessingDate()).isEqualTo("2026-01-01");
        }
    }
}
