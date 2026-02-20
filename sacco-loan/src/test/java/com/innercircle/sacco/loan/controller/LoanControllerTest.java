package com.innercircle.sacco.loan.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.loan.dto.ApproveLoanRequest;
import com.innercircle.sacco.loan.dto.LoanApplicationRequest;
import com.innercircle.sacco.loan.dto.LoanResponse;
import com.innercircle.sacco.loan.dto.LoanSummaryResponse;
import com.innercircle.sacco.loan.dto.RepaymentRequest;
import com.innercircle.sacco.loan.dto.RepaymentScheduleResponse;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.entity.RepaymentStatus;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.service.InterestReportingService;
import com.innercircle.sacco.loan.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    @Mock
    private LoanService loanService;

    @Mock
    private LoanApplicationRepository loanRepository;

    @Mock
    private InterestReportingService interestReportingService;

    @Mock
    private MemberAccessHelper memberAccessHelper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LoanController loanController;

    private UUID memberId;
    private UUID loanId;
    private UUID loanProductId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        loanId = UUID.randomUUID();
        loanProductId = UUID.randomUUID();
        lenient().when(memberAccessHelper.currentActor(authentication)).thenReturn("test-user");
        // Default: non-admin user (no ROLE_ADMIN authority)
        lenient().when(authentication.getAuthorities()).thenAnswer(inv -> Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // applyForLoan
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("applyForLoan")
    class ApplyForLoan {

        @Test
        @DisplayName("should create loan and return success response")
        void shouldCreateLoan() {
            LoanApplicationRequest request = LoanApplicationRequest.builder()
                    .memberId(memberId)
                    .loanProductId(loanProductId)
                    .principalAmount(new BigDecimal("100000"))
                    .termMonths(12)
                    .purpose("Business")
                    .build();

            LoanApplication loan = createLoan(LoanStatus.PENDING);
            when(loanService.applyForLoan(memberId, loanProductId, new BigDecimal("100000"),
                    12, "Business", "test-user"))
                    .thenReturn(loan);

            ApiResponse<LoanResponse> response = loanController.applyForLoan(request, authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Loan application submitted successfully");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getMemberId()).isEqualTo(memberId);
            assertThat(response.getData().getLoanNumber()).isEqualTo("LN-ABCDEF12");
            assertThat(response.getData().getStatus()).isEqualTo(LoanStatus.PENDING);
        }

        @Test
        @DisplayName("should pass all fields to service")
        void shouldPassAllFieldsToService() {
            UUID otherProductId = UUID.randomUUID();
            LoanApplicationRequest request = LoanApplicationRequest.builder()
                    .memberId(memberId)
                    .loanProductId(otherProductId)
                    .principalAmount(new BigDecimal("50000"))
                    .termMonths(6)
                    .purpose("Education")
                    .build();

            LoanApplication loan = createLoan(LoanStatus.PENDING);
            when(loanService.applyForLoan(any(), any(), any(), any(), any(), any()))
                    .thenReturn(loan);

            loanController.applyForLoan(request, authentication);

            verify(loanService).applyForLoan(
                    memberId, otherProductId, new BigDecimal("50000"),
                    6, "Education", "test-user");
        }
    }

    // -------------------------------------------------------------------------
    // approveLoan
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("approveLoan")
    class ApproveLoan {

        @Test
        @DisplayName("should approve loan and return success response")
        void shouldApproveLoan() {
            UUID approvedBy = UUID.randomUUID();
            LoanApplication loan = createLoan(LoanStatus.APPROVED);

            when(memberAccessHelper.resolveCurrentUserId(authentication)).thenReturn(approvedBy);
            when(loanService.approveLoan(eq(loanId), eq(approvedBy), eq("test-user"),
                    isNull(), eq(false))).thenReturn(loan);

            ApiResponse<LoanResponse> response = loanController.approveLoan(loanId, null, authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Loan approved successfully");
            verify(loanService).approveLoan(eq(loanId), eq(approvedBy), eq("test-user"),
                    isNull(), eq(false));
        }
    }

    // -------------------------------------------------------------------------
    // rejectLoan
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("rejectLoan")
    class RejectLoan {

        @Test
        @DisplayName("should reject loan and return success response")
        void shouldRejectLoan() {
            UUID rejectedBy = UUID.randomUUID();
            LoanApplication loan = createLoan(LoanStatus.REJECTED);

            when(memberAccessHelper.resolveCurrentUserId(authentication)).thenReturn(rejectedBy);
            when(loanService.rejectLoan(eq(loanId), eq(rejectedBy), eq("test-user"),
                    isNull(), eq(false))).thenReturn(loan);

            ApiResponse<LoanResponse> response = loanController.rejectLoan(loanId, null, authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Loan rejected");
            verify(loanService).rejectLoan(eq(loanId), eq(rejectedBy), eq("test-user"),
                    isNull(), eq(false));
        }
    }

    // -------------------------------------------------------------------------
    // disburseLoan
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("disburseLoan")
    class DisburseLoan {

        @Test
        @DisplayName("should disburse loan and return success response")
        void shouldDisburseLoan() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setDisbursedAt(Instant.now());

            when(loanService.disburseLoan(loanId, "test-user")).thenReturn(loan);

            ApiResponse<LoanResponse> response = loanController.disburseLoan(loanId, authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Loan disbursed successfully");
            verify(loanService).disburseLoan(loanId, "test-user");
        }
    }

    // -------------------------------------------------------------------------
    // recordRepayment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("recordRepayment")
    class RecordRepayment {

        @Test
        @DisplayName("should record repayment and return success")
        void shouldRecordRepayment() {
            RepaymentRequest request = RepaymentRequest.builder()
                    .amount(new BigDecimal("10000"))
                    .referenceNumber("REF001")
                    .build();

            LoanRepayment repayment = new LoanRepayment();
            repayment.setId(UUID.randomUUID());

            when(loanService.recordRepayment(loanId, new BigDecimal("10000"), "REF001", "test-user"))
                    .thenReturn(repayment);

            ApiResponse<Void> response = loanController.recordRepayment(loanId, request, authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Repayment recorded successfully");
            assertThat(response.getData()).isNull();
            verify(loanService).recordRepayment(loanId, new BigDecimal("10000"), "REF001", "test-user");
        }
    }

    // -------------------------------------------------------------------------
    // getLoanSchedule
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getLoanSchedule")
    class GetLoanSchedule {

        @Test
        @DisplayName("should return loan schedules")
        void shouldReturnSchedules() {
            RepaymentSchedule schedule = createSchedule(1);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            when(loanService.getLoanSchedule(loanId)).thenReturn(List.of(schedule));
            when(loanService.getLoanById(loanId)).thenReturn(loan);

            ApiResponse<List<RepaymentScheduleResponse>> response = loanController.getLoanSchedule(loanId, authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no schedules exist")
        void shouldReturnEmptyList() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            when(loanService.getLoanById(loanId)).thenReturn(loan);
            when(loanService.getLoanSchedule(loanId)).thenReturn(List.of());

            ApiResponse<List<RepaymentScheduleResponse>> response = loanController.getLoanSchedule(loanId, authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // listLoans
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("listLoans")
    class ListLoans {

        @Test
        @DisplayName("should list loans with no filters")
        void shouldListLoansNoFilters() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            UUID zeroUuid = new UUID(0L, 0L);

            when(loanRepository.findByIdGreaterThanOrderById(eq(zeroUuid), any(Pageable.class)))
                    .thenReturn(List.of(loan));

            ApiResponse<CursorPage<LoanResponse>> response =
                    loanController.listLoans(null, null, null, 20);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getItems()).hasSize(1);
            assertThat(response.getData().isHasMore()).isFalse();
        }

        @Test
        @DisplayName("should filter by memberId")
        void shouldFilterByMemberId() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            UUID zeroUuid = new UUID(0L, 0L);

            when(loanRepository.findByMemberIdAndIdGreaterThanOrderById(
                    eq(memberId), eq(zeroUuid), any(Pageable.class)))
                    .thenReturn(List.of(loan));

            ApiResponse<CursorPage<LoanResponse>> response =
                    loanController.listLoans(memberId, null, null, 20);

            assertThat(response.getData().getItems()).hasSize(1);
            verify(loanRepository).findByMemberIdAndIdGreaterThanOrderById(
                    eq(memberId), eq(zeroUuid), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            UUID zeroUuid = new UUID(0L, 0L);

            when(loanRepository.findByStatusAndIdGreaterThanOrderById(
                    eq(LoanStatus.REPAYING), eq(zeroUuid), any(Pageable.class)))
                    .thenReturn(List.of(loan));

            ApiResponse<CursorPage<LoanResponse>> response =
                    loanController.listLoans(null, LoanStatus.REPAYING, null, 20);

            assertThat(response.getData().getItems()).hasSize(1);
            verify(loanRepository).findByStatusAndIdGreaterThanOrderById(
                    eq(LoanStatus.REPAYING), eq(zeroUuid), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by both memberId and status")
        void shouldFilterByMemberIdAndStatus() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            UUID zeroUuid = new UUID(0L, 0L);

            when(loanRepository.findByMemberIdAndStatusAndIdGreaterThanOrderById(
                    eq(memberId), eq(LoanStatus.REPAYING), eq(zeroUuid), any(Pageable.class)))
                    .thenReturn(List.of(loan));

            ApiResponse<CursorPage<LoanResponse>> response =
                    loanController.listLoans(memberId, LoanStatus.REPAYING, null, 20);

            assertThat(response.getData().getItems()).hasSize(1);
            verify(loanRepository).findByMemberIdAndStatusAndIdGreaterThanOrderById(
                    eq(memberId), eq(LoanStatus.REPAYING), eq(zeroUuid), any(Pageable.class));
        }

        @Test
        @DisplayName("should use cursor when provided")
        void shouldUseCursorWhenProvided() {
            UUID cursor = UUID.randomUUID();
            LoanApplication loan = createLoan(LoanStatus.REPAYING);

            when(loanRepository.findByIdGreaterThanOrderById(eq(cursor), any(Pageable.class)))
                    .thenReturn(List.of(loan));

            ApiResponse<CursorPage<LoanResponse>> response =
                    loanController.listLoans(null, null, cursor, 20);

            verify(loanRepository).findByIdGreaterThanOrderById(eq(cursor), any(Pageable.class));
        }

        @Test
        @DisplayName("should set hasMore=true when results exceed limit")
        void shouldSetHasMoreWhenExceedsLimit() {
            // Requesting limit=2, but return 3 results (limit+1)
            LoanApplication loan1 = createLoan(LoanStatus.REPAYING);
            LoanApplication loan2 = createLoan(LoanStatus.REPAYING);
            loan2.setId(UUID.randomUUID());
            LoanApplication loan3 = createLoan(LoanStatus.REPAYING);
            loan3.setId(UUID.randomUUID());

            UUID zeroUuid = new UUID(0L, 0L);
            when(loanRepository.findByIdGreaterThanOrderById(eq(zeroUuid), any(Pageable.class)))
                    .thenReturn(List.of(loan1, loan2, loan3));

            ApiResponse<CursorPage<LoanResponse>> response =
                    loanController.listLoans(null, null, null, 2);

            assertThat(response.getData().isHasMore()).isTrue();
            assertThat(response.getData().getItems()).hasSize(2);
            assertThat(response.getData().getNextCursor()).isNotNull();
        }

        @Test
        @DisplayName("should set hasMore=false when results fit within limit")
        void shouldSetHasMoreFalseWhenFitsLimit() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            UUID zeroUuid = new UUID(0L, 0L);

            when(loanRepository.findByIdGreaterThanOrderById(eq(zeroUuid), any(Pageable.class)))
                    .thenReturn(List.of(loan));

            ApiResponse<CursorPage<LoanResponse>> response =
                    loanController.listLoans(null, null, null, 20);

            assertThat(response.getData().isHasMore()).isFalse();
            assertThat(response.getData().getNextCursor()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // getLoanById
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getLoanById")
    class GetLoanById {

        @Test
        @DisplayName("should return loan by ID")
        void shouldReturnLoanById() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            when(loanService.getLoanById(loanId)).thenReturn(loan);

            ApiResponse<LoanResponse> response = loanController.getLoanById(loanId, authentication);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getId()).isEqualTo(loanId);
        }
    }

    // -------------------------------------------------------------------------
    // getMemberLoanSummary
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getMemberLoanSummary")
    class GetMemberLoanSummary {

        @Test
        @DisplayName("should calculate summary correctly")
        void shouldCalculateSummaryCorrectly() {
            LoanApplication activeLoan = createLoan(LoanStatus.REPAYING);
            activeLoan.setPrincipalAmount(new BigDecimal("100000"));
            activeLoan.setTotalRepaid(new BigDecimal("30000"));
            activeLoan.setOutstandingBalance(new BigDecimal("70000"));

            LoanApplication closedLoan = createLoan(LoanStatus.CLOSED);
            closedLoan.setId(UUID.randomUUID());
            closedLoan.setPrincipalAmount(new BigDecimal("50000"));
            closedLoan.setTotalRepaid(new BigDecimal("55000"));
            closedLoan.setOutstandingBalance(BigDecimal.ZERO);

            when(loanService.getMemberLoans(memberId)).thenReturn(List.of(activeLoan, closedLoan));

            ApiResponse<LoanSummaryResponse> response = loanController.getMemberLoanSummary(memberId, authentication);

            assertThat(response.isSuccess()).isTrue();
            LoanSummaryResponse summary = response.getData();
            assertThat(summary.getMemberId()).isEqualTo(memberId);
            assertThat(summary.getTotalLoans()).isEqualTo(2);
            assertThat(summary.getActiveLoans()).isEqualTo(1); // Only REPAYING
            assertThat(summary.getClosedLoans()).isEqualTo(1);
            assertThat(summary.getTotalBorrowed()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(summary.getTotalRepaid()).isEqualByComparingTo(new BigDecimal("85000"));
            assertThat(summary.getTotalOutstanding()).isEqualByComparingTo(new BigDecimal("70000"));
            assertThat(summary.getLoans()).hasSize(2);
        }

        @Test
        @DisplayName("should exclude PENDING and REJECTED loans from totalBorrowed")
        void shouldExcludePendingAndRejectedFromTotalBorrowed() {
            LoanApplication pendingLoan = createLoan(LoanStatus.PENDING);
            pendingLoan.setPrincipalAmount(new BigDecimal("50000"));
            pendingLoan.setTotalRepaid(BigDecimal.ZERO);
            pendingLoan.setOutstandingBalance(BigDecimal.ZERO);

            LoanApplication rejectedLoan = createLoan(LoanStatus.REJECTED);
            rejectedLoan.setId(UUID.randomUUID());
            rejectedLoan.setPrincipalAmount(new BigDecimal("75000"));
            rejectedLoan.setTotalRepaid(BigDecimal.ZERO);
            rejectedLoan.setOutstandingBalance(BigDecimal.ZERO);

            LoanApplication activeLoan = createLoan(LoanStatus.REPAYING);
            activeLoan.setId(UUID.randomUUID());
            activeLoan.setPrincipalAmount(new BigDecimal("100000"));
            activeLoan.setTotalRepaid(new BigDecimal("20000"));
            activeLoan.setOutstandingBalance(new BigDecimal("80000"));

            when(loanService.getMemberLoans(memberId))
                    .thenReturn(List.of(pendingLoan, rejectedLoan, activeLoan));

            ApiResponse<LoanSummaryResponse> response = loanController.getMemberLoanSummary(memberId, authentication);

            // totalBorrowed should only include active (non-PENDING, non-REJECTED)
            assertThat(response.getData().getTotalBorrowed()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("should count DISBURSED loans as active")
        void shouldCountDisbursedAsActive() {
            LoanApplication disbursedLoan = createLoan(LoanStatus.DISBURSED);
            disbursedLoan.setPrincipalAmount(new BigDecimal("100000"));
            disbursedLoan.setTotalRepaid(BigDecimal.ZERO);
            disbursedLoan.setOutstandingBalance(new BigDecimal("100000"));

            when(loanService.getMemberLoans(memberId)).thenReturn(List.of(disbursedLoan));

            ApiResponse<LoanSummaryResponse> response = loanController.getMemberLoanSummary(memberId, authentication);

            assertThat(response.getData().getActiveLoans()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle member with no loans")
        void shouldHandleNoLoans() {
            when(loanService.getMemberLoans(memberId)).thenReturn(List.of());

            ApiResponse<LoanSummaryResponse> response = loanController.getMemberLoanSummary(memberId, authentication);

            LoanSummaryResponse summary = response.getData();
            assertThat(summary.getTotalLoans()).isEqualTo(0);
            assertThat(summary.getActiveLoans()).isEqualTo(0);
            assertThat(summary.getClosedLoans()).isEqualTo(0);
            assertThat(summary.getTotalBorrowed()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.getTotalRepaid()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.getTotalOutstanding()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------
    private LoanApplication createLoan(LoanStatus status) {
        LoanApplication loan = new LoanApplication();
        loan.setId(loanId);
        loan.setMemberId(memberId);
        loan.setLoanNumber("LN-ABCDEF12");
        loan.setLoanProductId(loanProductId);
        loan.setPrincipalAmount(new BigDecimal("100000"));
        loan.setInterestRate(new BigDecimal("12"));
        loan.setTermMonths(12);
        loan.setInterestMethod(InterestMethod.FLAT_RATE);
        loan.setStatus(status);
        loan.setPurpose("Business");
        loan.setTotalRepaid(BigDecimal.ZERO);
        loan.setOutstandingBalance(BigDecimal.ZERO);
        loan.setTotalInterestAccrued(BigDecimal.ZERO);
        loan.setTotalInterestPaid(BigDecimal.ZERO);
        loan.setCreatedAt(Instant.now());
        loan.setUpdatedAt(Instant.now());
        return loan;
    }

    private RepaymentSchedule createSchedule(int installmentNumber) {
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setLoanId(loanId);
        schedule.setInstallmentNumber(installmentNumber);
        schedule.setDueDate(LocalDate.now().plusMonths(installmentNumber));
        schedule.setPrincipalAmount(new BigDecimal("8000"));
        schedule.setInterestAmount(new BigDecimal("2000"));
        schedule.setTotalAmount(new BigDecimal("10000"));
        schedule.setPaid(false);
        return schedule;
    }
}
