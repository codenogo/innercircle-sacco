package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.LoanBatchProcessedEvent;
import com.innercircle.sacco.loan.dto.BatchProcessingResult;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.RepaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanBatchServiceImplTest {

    @Mock
    private LoanApplicationRepository loanRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LoanBatchServiceImpl batchService;

    @Captor
    private ArgumentCaptor<LoanApplication> loanCaptor;

    private UUID loanId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();
        memberId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // processOutstandingLoans
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("processOutstandingLoans")
    class ProcessOutstandingLoans {

        @Test
        @DisplayName("should process loans with no overdue schedules")
        void shouldProcessLoansWithNoOverdueSchedules() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().plusDays(30));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            BatchProcessingResult result = batchService.processOutstandingLoans();

            assertThat(result.getProcessedLoans()).isEqualTo(1);
            assertThat(result.getPenalizedLoans()).isEqualTo(0);
            assertThat(result.getClosedLoans()).isEqualTo(0);
            assertThat(result.getProcessedAt()).isNotNull();
            assertThat(result.getMessage()).contains("Processed 1 loans");
        }

        @Test
        @DisplayName("should count penalized loans when overdue by 30+ days")
        void shouldCountPenalizedLoansWhenOverdue30Days() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(31));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            BatchProcessingResult result = batchService.processOutstandingLoans();

            assertThat(result.getPenalizedLoans()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not penalize loans overdue by less than 30 days")
        void shouldNotPenalizeLoansOverdueLessThan30Days() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(15));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            BatchProcessingResult result = batchService.processOutstandingLoans();

            assertThat(result.getPenalizedLoans()).isEqualTo(0);
        }

        @Test
        @DisplayName("should mark loan as DEFAULTED when overdue by 90+ days")
        void shouldMarkAsDefaultedWhen90DaysOverdue() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(91));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            batchService.processOutstandingLoans();

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.DEFAULTED);
        }

        @Test
        @DisplayName("should not mark as defaulted when overdue by less than 90 days")
        void shouldNotMarkAsDefaultedWhenLessThan90Days() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(60));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            batchService.processOutstandingLoans();

            // Loan status should NOT be updated
            verify(loanRepository, never()).save(any(LoanApplication.class));
        }

        @Test
        @DisplayName("should close loan when all schedules paid and balance is zero")
        void shouldCloseLoanWhenAllPaidAndBalanceZero() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(BigDecimal.ZERO);

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of()); // No unpaid schedules

            BatchProcessingResult result = batchService.processOutstandingLoans();

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.CLOSED);
            assertThat(result.getClosedLoans()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not close loan when unpaid schedules exist")
        void shouldNotCloseLoanWhenUnpaidSchedulesExist() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(BigDecimal.ZERO);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().plusDays(30));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            BatchProcessingResult result = batchService.processOutstandingLoans();

            assertThat(result.getClosedLoans()).isEqualTo(0);
        }

        @Test
        @DisplayName("should not close loan when outstanding balance is positive")
        void shouldNotCloseLoanWhenBalancePositive() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("1000"));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of());

            BatchProcessingResult result = batchService.processOutstandingLoans();

            assertThat(result.getClosedLoans()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle empty loan list")
        void shouldHandleEmptyLoanList() {
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of());

            BatchProcessingResult result = batchService.processOutstandingLoans();

            assertThat(result.getProcessedLoans()).isEqualTo(0);
            assertThat(result.getPenalizedLoans()).isEqualTo(0);
            assertThat(result.getClosedLoans()).isEqualTo(0);
        }

        @Test
        @DisplayName("should continue processing when individual loan throws exception")
        void shouldContinueOnException() {
            LoanApplication loan1 = createLoan(LoanStatus.REPAYING);
            LoanApplication loan2 = createLoan(LoanStatus.REPAYING);
            loan2.setId(UUID.randomUUID());
            loan2.setOutstandingBalance(BigDecimal.ZERO);

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan1, loan2));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan1.getId()))
                    .thenThrow(new RuntimeException("DB error"));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan2.getId()))
                    .thenReturn(List.of());

            BatchProcessingResult result = batchService.processOutstandingLoans();

            // One failed, one succeeded (the second processed + closed)
            assertThat(result.getProcessedLoans()).isEqualTo(1);
        }

        @Test
        @DisplayName("should publish LoanBatchProcessedEvent")
        void shouldPublishBatchProcessedEvent() {
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of());

            batchService.processOutstandingLoans();

            ArgumentCaptor<LoanBatchProcessedEvent> eventCaptor =
                    ArgumentCaptor.forClass(LoanBatchProcessedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            LoanBatchProcessedEvent event = eventCaptor.getValue();
            assertThat(event.processedLoans()).isEqualTo(0);
            assertThat(event.actor()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("should process multiple loans with mixed statuses")
        void shouldProcessMultipleLoansWithMixedStatuses() {
            LoanApplication loan1 = createLoan(LoanStatus.REPAYING);
            LoanApplication loan2 = createLoan(LoanStatus.REPAYING);
            loan2.setId(UUID.randomUUID());

            // loan1: overdue 91 days -> defaulted
            RepaymentSchedule schedule1 = createSchedule(1, LocalDate.now().minusDays(91));
            // loan2: all paid, zero balance -> closed
            loan2.setOutstandingBalance(BigDecimal.ZERO);

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan1, loan2));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan1.getId()))
                    .thenReturn(List.of(schedule1));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan2.getId()))
                    .thenReturn(List.of());

            BatchProcessingResult result = batchService.processOutstandingLoans();

            assertThat(result.getProcessedLoans()).isEqualTo(2);
            assertThat(result.getClosedLoans()).isEqualTo(1);
            assertThat(result.getPenalizedLoans()).isEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // detectUnpaidLoans
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("detectUnpaidLoans")
    class DetectUnpaidLoans {

        @Test
        @DisplayName("should detect unpaid loans due in the specified month")
        void shouldDetectUnpaidLoansInMonth() {
            LocalDate month = LocalDate.of(2025, 3, 1);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("90000"));

            RepaymentSchedule schedule = createSchedule(3, LocalDate.of(2025, 3, 15));
            schedule.setTotalAmount(new BigDecimal("10000"));
            schedule.setAmountPaid(BigDecimal.ZERO);

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            List<Map<String, Object>> result = batchService.detectUnpaidLoans(month);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("loanId")).isEqualTo(loan.getId());
            assertThat(result.get(0).get("memberId")).isEqualTo(memberId);
            assertThat(result.get(0).get("installmentNumber")).isEqualTo(3);
            assertThat(result.get(0).get("dueDate")).isEqualTo(LocalDate.of(2025, 3, 15));
            assertThat(result.get(0).get("totalAmount")).isEqualTo(new BigDecimal("10000"));
            assertThat(result.get(0).get("amountPaid")).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should not include schedules due outside the specified month")
        void shouldNotIncludeSchedulesOutsideMonth() {
            LocalDate month = LocalDate.of(2025, 3, 1);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);

            // Due in April, not March
            RepaymentSchedule schedule = createSchedule(4, LocalDate.of(2025, 4, 15));
            schedule.setTotalAmount(new BigDecimal("10000"));
            schedule.setAmountPaid(BigDecimal.ZERO);

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            List<Map<String, Object>> result = batchService.detectUnpaidLoans(month);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no repaying loans exist")
        void shouldReturnEmptyWhenNoRepayingLoans() {
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of());

            List<Map<String, Object>> result = batchService.detectUnpaidLoans(LocalDate.of(2025, 3, 1));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should include schedules due on first day of month")
        void shouldIncludeSchedulesDueOnFirstDayOfMonth() {
            LocalDate month = LocalDate.of(2025, 3, 1);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("50000"));

            RepaymentSchedule schedule = createSchedule(3, LocalDate.of(2025, 3, 1));
            schedule.setTotalAmount(new BigDecimal("5000"));
            schedule.setAmountPaid(BigDecimal.ZERO);

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            List<Map<String, Object>> result = batchService.detectUnpaidLoans(month);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should include schedules due on last day of month")
        void shouldIncludeSchedulesDueOnLastDayOfMonth() {
            LocalDate month = LocalDate.of(2025, 3, 1);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("50000"));

            RepaymentSchedule schedule = createSchedule(3, LocalDate.of(2025, 3, 31));
            schedule.setTotalAmount(new BigDecimal("5000"));
            schedule.setAmountPaid(BigDecimal.ZERO);

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            List<Map<String, Object>> result = batchService.detectUnpaidLoans(month);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should calculate outstanding amount correctly")
        void shouldCalculateOutstandingAmountCorrectly() {
            LocalDate month = LocalDate.of(2025, 3, 1);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("50000"));

            RepaymentSchedule schedule = createSchedule(3, LocalDate.of(2025, 3, 15));
            schedule.setTotalAmount(new BigDecimal("10000"));
            schedule.setAmountPaid(new BigDecimal("3000"));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            List<Map<String, Object>> result = batchService.detectUnpaidLoans(month);

            assertThat(result).hasSize(1);
            assertThat((BigDecimal) result.get(0).get("outstandingAmount"))
                    .isEqualByComparingTo(new BigDecimal("7000"));
        }
    }

    // -------------------------------------------------------------------------
    // processLoan (single loan)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("processLoan")
    class ProcessLoan {

        @Test
        @DisplayName("should throw for non-existent loan")
        void shouldThrowForNonExistentLoan() {
            when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> batchService.processLoan(loanId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found");
        }

        @Test
        @DisplayName("should throw for non-repaying loan")
        void shouldThrowForNonRepayingLoan() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> batchService.processLoan(loanId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Loan is not in REPAYING status");
        }

        @Test
        @DisplayName("should process loan with no overdue installments")
        void shouldProcessLoanWithNoOverdue() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().plusDays(30));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            batchService.processLoan(loanId);

            verify(loanRepository, never()).save(any(LoanApplication.class));
        }

        @Test
        @DisplayName("should mark loan as DEFAULTED when 90+ days overdue")
        void shouldMarkAsDefaultedWhen90DaysOverdue() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(91));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            batchService.processLoan(loanId);

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.DEFAULTED);
        }

        @Test
        @DisplayName("should close loan when all paid and balance zero")
        void shouldCloseLoanWhenAllPaidAndBalanceZero() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(BigDecimal.ZERO);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of());

            batchService.processLoan(loanId);

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.CLOSED);
        }

        @Test
        @DisplayName("should not close loan when unpaid schedules remain")
        void shouldNotCloseLoanWhenUnpaidSchedulesRemain() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(BigDecimal.ZERO);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().plusDays(30));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            batchService.processLoan(loanId);

            verify(loanRepository, never()).save(any(LoanApplication.class));
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------
    private LoanApplication createLoan(LoanStatus status) {
        LoanApplication loan = new LoanApplication();
        loan.setId(loanId);
        loan.setMemberId(memberId);
        loan.setPrincipalAmount(new BigDecimal("100000"));
        loan.setInterestRate(new BigDecimal("12"));
        loan.setTermMonths(12);
        loan.setInterestMethod("FLAT_RATE");
        loan.setStatus(status);
        loan.setTotalRepaid(BigDecimal.ZERO);
        loan.setOutstandingBalance(new BigDecimal("100000"));
        return loan;
    }

    private RepaymentSchedule createSchedule(int installmentNumber, LocalDate dueDate) {
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setLoanId(loanId);
        schedule.setInstallmentNumber(installmentNumber);
        schedule.setDueDate(dueDate);
        schedule.setTotalAmount(new BigDecimal("10000"));
        schedule.setPrincipalAmount(new BigDecimal("8000"));
        schedule.setInterestAmount(new BigDecimal("2000"));
        schedule.setAmountPaid(BigDecimal.ZERO);
        schedule.setPaid(false);
        return schedule;
    }
}
