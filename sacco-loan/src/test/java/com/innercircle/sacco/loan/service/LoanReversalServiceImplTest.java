package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.LoanReversalEvent;
import com.innercircle.sacco.loan.dto.ReversalResponse;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.entity.RepaymentStatus;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.LoanRepaymentRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanReversalServiceImplTest {

    @Mock
    private LoanRepaymentRepository repaymentRepository;

    @Mock
    private LoanApplicationRepository loanRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LoanReversalServiceImpl reversalService;

    @Captor
    private ArgumentCaptor<LoanApplication> loanCaptor;

    @Captor
    private ArgumentCaptor<RepaymentSchedule> scheduleCaptor;

    private UUID loanId;
    private UUID memberId;
    private UUID repaymentId;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        repaymentId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // reverseRepayment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("reverseRepayment")
    class ReverseRepayment {

        @Test
        @DisplayName("should reverse a confirmed repayment successfully")
        void shouldReverseConfirmedRepayment() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("10000"), new BigDecimal("8000"), new BigDecimal("2000"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("10000"), new BigDecimal("90000"));

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId)).thenReturn(new ArrayList<>());

            ReversalResponse result = reversalService.reverseRepayment(repaymentId, "Test reason", "admin");

            assertThat(result.getReversalType()).isEqualTo("REPAYMENT");
            assertThat(result.getOriginalTransactionId()).isEqualTo(repaymentId);
            assertThat(result.getLoanId()).isEqualTo(loanId);
            assertThat(result.getMemberId()).isEqualTo(memberId);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(result.getReason()).isEqualTo("Test reason");
            assertThat(result.getActor()).isEqualTo("admin");
            assertThat(result.getReversedAt()).isNotNull();
            assertThat(result.getMessage()).isEqualTo("Repayment reversed successfully");
        }

        @Test
        @DisplayName("should mark repayment as REVERSED")
        void shouldMarkRepaymentAsReversed() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("5000"), new BigDecimal("95000"));

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId)).thenReturn(new ArrayList<>());

            reversalService.reverseRepayment(repaymentId, "reason", "admin");

            assertThat(repayment.getStatus()).isEqualTo(RepaymentStatus.REVERSED);
            verify(repaymentRepository).save(repayment);
        }

        @Test
        @DisplayName("should restore loan outstanding balance")
        void shouldRestoreLoanBalance() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("10000"), new BigDecimal("8000"), new BigDecimal("2000"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("10000"), new BigDecimal("90000"));

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId)).thenReturn(new ArrayList<>());

            reversalService.reverseRepayment(repaymentId, "reason", "admin");

            verify(loanRepository).save(loanCaptor.capture());
            LoanApplication savedLoan = loanCaptor.getValue();
            assertThat(savedLoan.getTotalRepaid()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(savedLoan.getOutstandingBalance()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("should reopen CLOSED loan to REPAYING")
        void shouldReopenClosedLoan() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000"));
            LoanApplication loan = createLoan(LoanStatus.CLOSED,
                    new BigDecimal("100000"), BigDecimal.ZERO);

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId)).thenReturn(new ArrayList<>());

            reversalService.reverseRepayment(repaymentId, "reason", "admin");

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.REPAYING);
        }

        @Test
        @DisplayName("should not change status of REPAYING loan")
        void shouldNotChangeStatusOfRepayingLoan() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("50000"), new BigDecimal("50000"));

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId)).thenReturn(new ArrayList<>());

            reversalService.reverseRepayment(repaymentId, "reason", "admin");

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.REPAYING);
        }

        @Test
        @DisplayName("should throw for already reversed repayment")
        void shouldThrowForAlreadyReversed() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.REVERSED,
                    new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000"));

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));

            assertThatThrownBy(() ->
                    reversalService.reverseRepayment(repaymentId, "reason", "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Repayment is already reversed");
        }

        @Test
        @DisplayName("should throw for non-existent repayment")
        void shouldThrowForNonExistentRepayment() {
            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    reversalService.reverseRepayment(repaymentId, "reason", "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Repayment not found");
        }

        @Test
        @DisplayName("should throw for non-existent loan associated with repayment")
        void shouldThrowForNonExistentLoan() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000"));

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    reversalService.reverseRepayment(repaymentId, "reason", "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found");
        }

        @Test
        @DisplayName("should throw when reversal would result in negative total repaid")
        void shouldThrowWhenNegativeBalance() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("50000"), new BigDecimal("40000"), new BigDecimal("10000"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("10000"), new BigDecimal("90000"));

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() ->
                    reversalService.reverseRepayment(repaymentId, "reason", "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Reversal would result in negative total repaid");
        }

        @Test
        @DisplayName("should reverse schedule installments in LIFO order")
        void shouldReverseScheduleInLifoOrder() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("10000"), new BigDecimal("8000"), new BigDecimal("2000"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("10000"), new BigDecimal("90000"));

            RepaymentSchedule schedule1 = createSchedule(1, new BigDecimal("5000"), new BigDecimal("5000"), true);
            RepaymentSchedule schedule2 = createSchedule(2, new BigDecimal("5000"), new BigDecimal("5000"), true);

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule1, schedule2)));

            reversalService.reverseRepayment(repaymentId, "reason", "admin");

            // Schedule 2 should be reversed first (LIFO), then schedule 1
            verify(scheduleRepository, times(2)).save(scheduleCaptor.capture());
            List<RepaymentSchedule> savedSchedules = scheduleCaptor.getAllValues();

            // First saved is schedule2 (reversed), second is schedule1 (reversed)
            assertThat(savedSchedules.get(0).getInstallmentNumber()).isEqualTo(2);
            assertThat(savedSchedules.get(0).getPaid()).isFalse();
            assertThat(savedSchedules.get(1).getInstallmentNumber()).isEqualTo(1);
            assertThat(savedSchedules.get(1).getPaid()).isFalse();
        }

        @Test
        @DisplayName("should handle partial schedule reversal")
        void shouldHandlePartialScheduleReversal() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("3000"), new BigDecimal("2400"), new BigDecimal("600"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("3000"), new BigDecimal("97000"));

            // Only schedule2 has been paid (5000)
            RepaymentSchedule schedule1 = createSchedule(1, new BigDecimal("5000"), BigDecimal.ZERO, false);
            RepaymentSchedule schedule2 = createSchedule(2, new BigDecimal("5000"), new BigDecimal("5000"), true);

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule1, schedule2)));

            reversalService.reverseRepayment(repaymentId, "reason", "admin");

            verify(scheduleRepository, times(1)).save(scheduleCaptor.capture());
            RepaymentSchedule savedSchedule = scheduleCaptor.getValue();
            assertThat(savedSchedule.getInstallmentNumber()).isEqualTo(2);
            assertThat(savedSchedule.getAmountPaid()).isEqualByComparingTo(new BigDecimal("2000"));
            assertThat(savedSchedule.getPaid()).isFalse();
        }

        @Test
        @DisplayName("should skip schedules with zero amount paid")
        void shouldSkipSchedulesWithZeroAmountPaid() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("5000"), new BigDecimal("95000"));

            // Schedule1 has no payment, schedule2 has full payment
            RepaymentSchedule schedule1 = createSchedule(1, new BigDecimal("5000"), BigDecimal.ZERO, false);
            RepaymentSchedule schedule2 = createSchedule(2, new BigDecimal("5000"), new BigDecimal("5000"), true);

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule1, schedule2)));

            reversalService.reverseRepayment(repaymentId, "reason", "admin");

            // Only schedule2 should be saved (reversed)
            verify(scheduleRepository, times(1)).save(any(RepaymentSchedule.class));
        }

        @Test
        @DisplayName("should publish LoanReversalEvent")
        void shouldPublishReversalEvent() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("10000"), new BigDecimal("8000"), new BigDecimal("2000"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("10000"), new BigDecimal("90000"));

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId)).thenReturn(new ArrayList<>());

            reversalService.reverseRepayment(repaymentId, "Duplicate payment", "admin");

            ArgumentCaptor<LoanReversalEvent> eventCaptor = ArgumentCaptor.forClass(LoanReversalEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            LoanReversalEvent event = eventCaptor.getValue();
            assertThat(event.reversalType()).isEqualTo("REPAYMENT");
            assertThat(event.originalTransactionId()).isEqualTo(repaymentId);
            assertThat(event.loanId()).isEqualTo(loanId);
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(event.principalPortion()).isEqualByComparingTo(new BigDecimal("8000"));
            assertThat(event.interestPortion()).isEqualByComparingTo(new BigDecimal("2000"));
            assertThat(event.reason()).isEqualTo("Duplicate payment");
            assertThat(event.actor()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should handle schedules with null amountPaid")
        void shouldHandleNullAmountPaid() {
            LoanRepayment repayment = createRepayment(RepaymentStatus.CONFIRMED,
                    new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000"));
            LoanApplication loan = createLoan(LoanStatus.REPAYING,
                    new BigDecimal("5000"), new BigDecimal("95000"));

            RepaymentSchedule schedule = createSchedule(1, new BigDecimal("5000"), null, false);

            when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));

            // Should not throw - null amountPaid should be skipped
            reversalService.reverseRepayment(repaymentId, "reason", "admin");

            // Schedule with null amountPaid should be skipped
            verify(scheduleRepository).findByLoanIdOrderByInstallmentNumber(loanId);
        }
    }

    // -------------------------------------------------------------------------
    // reversePenalty
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("reversePenalty")
    class ReversePenalty {

        @Test
        @DisplayName("should throw UnsupportedOperationException")
        void shouldThrowUnsupportedOperationException() {
            UUID penaltyId = UUID.randomUUID();

            assertThatThrownBy(() ->
                    reversalService.reversePenalty(penaltyId, "reason", "admin"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Penalty reversal not yet implemented");
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------
    private LoanRepayment createRepayment(RepaymentStatus status, BigDecimal amount,
                                           BigDecimal principalPortion, BigDecimal interestPortion) {
        LoanRepayment repayment = new LoanRepayment();
        repayment.setId(repaymentId);
        repayment.setLoanId(loanId);
        repayment.setMemberId(memberId);
        repayment.setAmount(amount);
        repayment.setPrincipalPortion(principalPortion);
        repayment.setInterestPortion(interestPortion);
        repayment.setRepaymentDate(LocalDate.now());
        repayment.setReferenceNumber("REF001");
        repayment.setStatus(status);
        return repayment;
    }

    private LoanApplication createLoan(LoanStatus status, BigDecimal totalRepaid,
                                        BigDecimal outstandingBalance) {
        LoanApplication loan = new LoanApplication();
        loan.setId(loanId);
        loan.setMemberId(memberId);
        loan.setPrincipalAmount(new BigDecimal("100000"));
        loan.setInterestRate(new BigDecimal("12"));
        loan.setTermMonths(12);
        loan.setInterestMethod("FLAT_RATE");
        loan.setStatus(status);
        loan.setTotalRepaid(totalRepaid);
        loan.setOutstandingBalance(outstandingBalance);
        return loan;
    }

    private RepaymentSchedule createSchedule(int installmentNumber, BigDecimal totalAmount,
                                              BigDecimal amountPaid, boolean paid) {
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setLoanId(loanId);
        schedule.setInstallmentNumber(installmentNumber);
        schedule.setDueDate(LocalDate.now().minusMonths(1));
        schedule.setTotalAmount(totalAmount);
        schedule.setPrincipalAmount(totalAmount.multiply(new BigDecimal("0.8")));
        schedule.setInterestAmount(totalAmount.multiply(new BigDecimal("0.2")));
        schedule.setAmountPaid(amountPaid);
        schedule.setPaid(paid);
        return schedule;
    }
}
