package com.innercircle.sacco.loan.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.loan.dto.BatchProcessingResult;
import com.innercircle.sacco.loan.dto.ReversalRequest;
import com.innercircle.sacco.loan.dto.ReversalResponse;
import com.innercircle.sacco.loan.service.LoanBatchService;
import com.innercircle.sacco.loan.service.LoanReversalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanBatchControllerTest {

    @Mock
    private LoanBatchService batchService;

    @Mock
    private LoanReversalService reversalService;

    @InjectMocks
    private LoanBatchController batchController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // processBatch
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("processBatch")
    class ProcessBatch {

        @Test
        @DisplayName("should trigger batch processing and return result")
        void shouldTriggerBatchProcessing() {
            BatchProcessingResult expectedResult = BatchProcessingResult.builder()
                    .processedLoans(10)
                    .penalizedLoans(2)
                    .closedLoans(1)
                    .processedAt(Instant.now())
                    .message("Processed 10 loans, penalized 2, closed 1")
                    .build();

            when(batchService.processOutstandingLoans()).thenReturn(expectedResult);

            ApiResponse<BatchProcessingResult> response = batchController.processBatch();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getProcessedLoans()).isEqualTo(10);
            assertThat(response.getData().getPenalizedLoans()).isEqualTo(2);
            assertThat(response.getData().getClosedLoans()).isEqualTo(1);
            assertThat(response.getMessage()).contains("Processed 10 loans");
            verify(batchService).processOutstandingLoans();
        }

        @Test
        @DisplayName("should return correct message from result")
        void shouldReturnMessageFromResult() {
            BatchProcessingResult result = BatchProcessingResult.builder()
                    .processedLoans(0)
                    .penalizedLoans(0)
                    .closedLoans(0)
                    .processedAt(Instant.now())
                    .message("Processed 0 loans, penalized 0, closed 0")
                    .build();

            when(batchService.processOutstandingLoans()).thenReturn(result);

            ApiResponse<BatchProcessingResult> response = batchController.processBatch();

            assertThat(response.getMessage()).isEqualTo("Processed 0 loans, penalized 0, closed 0");
        }
    }

    // -------------------------------------------------------------------------
    // detectUnpaidLoans
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("detectUnpaidLoans")
    class DetectUnpaidLoans {

        @Test
        @DisplayName("should detect unpaid loans for specified month")
        void shouldDetectUnpaidLoans() {
            LocalDate month = LocalDate.of(2025, 3, 15); // will be normalized to 2025-03-01
            Map<String, Object> unpaidLoan = Map.of(
                    "loanId", UUID.randomUUID(),
                    "memberId", UUID.randomUUID(),
                    "totalAmount", new BigDecimal("10000")
            );

            when(batchService.detectUnpaidLoans(LocalDate.of(2025, 3, 1)))
                    .thenReturn(List.of(unpaidLoan));

            ApiResponse<List<Map<String, Object>>> response =
                    batchController.detectUnpaidLoans(month);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(1);
            assertThat(response.getMessage()).contains("1 unpaid loan installments");
        }

        @Test
        @DisplayName("should normalize month to first day")
        void shouldNormalizeMonthToFirstDay() {
            LocalDate month = LocalDate.of(2025, 5, 20);

            when(batchService.detectUnpaidLoans(LocalDate.of(2025, 5, 1)))
                    .thenReturn(List.of());

            batchController.detectUnpaidLoans(month);

            verify(batchService).detectUnpaidLoans(LocalDate.of(2025, 5, 1));
        }

        @Test
        @DisplayName("should handle empty result")
        void shouldHandleEmptyResult() {
            when(batchService.detectUnpaidLoans(any())).thenReturn(List.of());

            ApiResponse<List<Map<String, Object>>> response =
                    batchController.detectUnpaidLoans(LocalDate.of(2025, 3, 1));

            assertThat(response.getData()).isEmpty();
            assertThat(response.getMessage()).contains("0 unpaid loan installments");
        }
    }

    // -------------------------------------------------------------------------
    // reverseRepayment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("reverseRepayment")
    class ReverseRepayment {

        @Test
        @DisplayName("should use SecurityContextHolder for actor, not request param")
        void shouldUseSecurityContextForActor() {
            // Set up SecurityContext
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "treasurer_user", "password");
            SecurityContextHolder.getContext().setAuthentication(auth);

            UUID repaymentId = UUID.randomUUID();
            ReversalRequest request = new ReversalRequest();
            request.setReason("Duplicate payment");
            request.setActor("should_be_ignored");

            ReversalResponse expectedResponse = ReversalResponse.builder()
                    .reversalId(repaymentId)
                    .reversalType("REPAYMENT")
                    .amount(new BigDecimal("5000"))
                    .message("Repayment reversed successfully")
                    .build();

            when(reversalService.reverseRepayment(eq(repaymentId), eq("Duplicate payment"),
                    eq("treasurer_user"))).thenReturn(expectedResponse);

            ApiResponse<ReversalResponse> response =
                    batchController.reverseRepayment(repaymentId, request);

            // Verify actor came from SecurityContextHolder, not from request
            verify(reversalService).reverseRepayment(repaymentId, "Duplicate payment", "treasurer_user");
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getReversalType()).isEqualTo("REPAYMENT");
        }

        @Test
        @DisplayName("should use SYSTEM as actor when no authentication context")
        void shouldUseSystemWhenNoAuth() {
            // Clear security context
            SecurityContextHolder.clearContext();

            UUID repaymentId = UUID.randomUUID();
            ReversalRequest request = new ReversalRequest();
            request.setReason("Error correction");
            request.setActor("irrelevant");

            ReversalResponse expectedResponse = ReversalResponse.builder()
                    .reversalId(repaymentId)
                    .reversalType("REPAYMENT")
                    .amount(new BigDecimal("5000"))
                    .message("Repayment reversed successfully")
                    .build();

            when(reversalService.reverseRepayment(eq(repaymentId), eq("Error correction"),
                    eq("SYSTEM"))).thenReturn(expectedResponse);

            batchController.reverseRepayment(repaymentId, request);

            verify(reversalService).reverseRepayment(repaymentId, "Error correction", "SYSTEM");
        }

        @Test
        @DisplayName("should return response message from service")
        void shouldReturnResponseMessage() {
            Authentication auth = new UsernamePasswordAuthenticationToken("admin", "pass");
            SecurityContextHolder.getContext().setAuthentication(auth);

            UUID repaymentId = UUID.randomUUID();
            ReversalRequest request = new ReversalRequest();
            request.setReason("reason");
            request.setActor("actor");

            ReversalResponse expectedResponse = ReversalResponse.builder()
                    .reversalId(repaymentId)
                    .message("Repayment reversed successfully")
                    .build();

            when(reversalService.reverseRepayment(any(), any(), any())).thenReturn(expectedResponse);

            ApiResponse<ReversalResponse> response =
                    batchController.reverseRepayment(repaymentId, request);

            assertThat(response.getMessage()).isEqualTo("Repayment reversed successfully");
        }
    }

    // -------------------------------------------------------------------------
    // reversePenalty
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("reversePenalty")
    class ReversePenalty {

        @Test
        @DisplayName("should use SecurityContextHolder for actor")
        void shouldUseSecurityContextForActor() {
            Authentication auth = new UsernamePasswordAuthenticationToken("admin_user", "pass");
            SecurityContextHolder.getContext().setAuthentication(auth);

            UUID penaltyId = UUID.randomUUID();
            ReversalRequest request = new ReversalRequest();
            request.setReason("Wrongly applied");
            request.setActor("should_be_ignored");

            ReversalResponse expectedResponse = ReversalResponse.builder()
                    .reversalId(penaltyId)
                    .reversalType("PENALTY")
                    .message("Penalty reversed successfully")
                    .build();

            when(reversalService.reversePenalty(eq(penaltyId), eq("Wrongly applied"),
                    eq("admin_user"))).thenReturn(expectedResponse);

            ApiResponse<ReversalResponse> response =
                    batchController.reversePenalty(penaltyId, request);

            verify(reversalService).reversePenalty(penaltyId, "Wrongly applied", "admin_user");
            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should fallback to SYSTEM when no authentication")
        void shouldFallbackToSystem() {
            SecurityContextHolder.clearContext();

            UUID penaltyId = UUID.randomUUID();
            ReversalRequest request = new ReversalRequest();
            request.setReason("reason");
            request.setActor("actor");

            ReversalResponse expectedResponse = ReversalResponse.builder()
                    .reversalId(penaltyId)
                    .message("Penalty reversed")
                    .build();

            when(reversalService.reversePenalty(eq(penaltyId), eq("reason"), eq("SYSTEM")))
                    .thenReturn(expectedResponse);

            batchController.reversePenalty(penaltyId, request);

            verify(reversalService).reversePenalty(penaltyId, "reason", "SYSTEM");
        }
    }

    // -------------------------------------------------------------------------
    // Security annotation verification
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Security annotations")
    class SecurityAnnotations {

        @Test
        @DisplayName("processBatch should require ADMIN or TREASURER role")
        void processBatchShouldRequireAdminOrTreasurer() throws NoSuchMethodException {
            var annotation = LoanBatchController.class
                    .getMethod("processBatch")
                    .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains("ADMIN");
            assertThat(annotation.value()).contains("TREASURER");
        }

        @Test
        @DisplayName("detectUnpaidLoans should require ADMIN or TREASURER role")
        void detectUnpaidLoansShouldRequireAdminOrTreasurer() throws NoSuchMethodException {
            var annotation = LoanBatchController.class
                    .getMethod("detectUnpaidLoans", LocalDate.class)
                    .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains("ADMIN");
            assertThat(annotation.value()).contains("TREASURER");
        }

        @Test
        @DisplayName("reverseRepayment should require ADMIN or TREASURER role")
        void reverseRepaymentShouldRequireAdminOrTreasurer() throws NoSuchMethodException {
            var annotation = LoanBatchController.class
                    .getMethod("reverseRepayment", UUID.class, ReversalRequest.class)
                    .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains("ADMIN");
            assertThat(annotation.value()).contains("TREASURER");
        }

        @Test
        @DisplayName("reversePenalty should require ADMIN or TREASURER role")
        void reversePenaltyShouldRequireAdminOrTreasurer() throws NoSuchMethodException {
            var annotation = LoanBatchController.class
                    .getMethod("reversePenalty", UUID.class, ReversalRequest.class)
                    .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains("ADMIN");
            assertThat(annotation.value()).contains("TREASURER");
        }
    }
}
