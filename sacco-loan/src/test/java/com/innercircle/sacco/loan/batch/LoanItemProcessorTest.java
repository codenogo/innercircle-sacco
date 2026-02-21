package com.innercircle.sacco.loan.batch;

import com.innercircle.sacco.common.event.LoanInterestAccrualEvent;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.service.ConfigService;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanPenalty;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.repository.LoanInterestHistoryRepository;
import com.innercircle.sacco.loan.repository.LoanPenaltyRepository;
import com.innercircle.sacco.loan.repository.RepaymentScheduleRepository;
import com.innercircle.sacco.loan.service.LoanPenaltyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoanItemProcessorTest {

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private LoanInterestHistoryRepository interestHistoryRepository;

    @Mock
    private EventOutboxWriter outboxWriter;

    @Mock
    private ConfigService configService;

    @Mock
    private LoanPenaltyService loanPenaltyService;

    @Mock
    private LoanPenaltyRepository loanPenaltyRepository;

    private LoanItemProcessor processor;
    private StepExecution stepExecution;

    private UUID loanId;
    private UUID memberId;

    @BeforeEach
    void setUp() throws Exception {
        processor = new LoanItemProcessor(
                scheduleRepository, interestHistoryRepository, outboxWriter,
                configService, loanPenaltyService, loanPenaltyRepository);

        loanId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        // Initialize counters and targetDateStr via beforeStep (reads from job parameters)
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", "2026-03-01")
                .toJobParameters();
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "test"), jobParameters);
        stepExecution = new StepExecution("testStep", jobExecution);
        processor.beforeStep(stepExecution);

        // Default config mocks
        setupConfigMock("loan.penalty.grace_period_days", "30");
        setupConfigMock("loan.penalty.default_threshold_days", "90");
    }

    private void setupConfigMock(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigValue(value);
        when(configService.getSystemConfig(key)).thenReturn(config);
    }

    private LoanApplication createLoan(Instant disbursedAt) {
        LoanApplication loan = new LoanApplication();
        loan.setId(loanId);
        loan.setMemberId(memberId);
        loan.setPrincipalAmount(new BigDecimal("100000"));
        loan.setInterestRate(new BigDecimal("12"));
        loan.setTermMonths(12);
        loan.setInterestMethod(InterestMethod.REDUCING_BALANCE);
        loan.setStatus(LoanStatus.REPAYING);
        loan.setTotalRepaid(BigDecimal.ZERO);
        loan.setOutstandingBalance(new BigDecimal("100000"));
        loan.setTotalInterestAccrued(BigDecimal.ZERO);
        loan.setDisbursedAt(disbursedAt);
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

    // ─── Disbursement Filtering ───────────────────────────────────────────

    @Nested
    @DisplayName("Disbursement date filtering")
    class DisbursementFiltering {

        @Test
        @DisplayName("should skip loan disbursed after target date")
        void shouldSkipLoanDisbursedAfterTargetDate() throws Exception {
            // Disbursed March 10, target=March 1 → skip
            Instant disbursedAt = LocalDate.of(2026, 3, 10)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            LoanApplication result = processor.process(loan);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should skip loan disbursed on target date")
        void shouldSkipLoanDisbursedOnTargetDate() throws Exception {
            // Disbursed on March 1 = target date → skip
            Instant disbursedAt = LocalDate.of(2026, 3, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            LoanApplication result = processor.process(loan);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should include loan disbursed before target date")
        void shouldIncludeLoanDisbursedBeforeTargetDate() throws Exception {
            // Disbursed Feb 28, target=March 1 → include
            Instant disbursedAt = LocalDate.of(2026, 2, 28)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of());
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            LoanApplication result = processor.process(loan);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should include loan disbursed well before target date")
        void shouldIncludeLoanDisbursedWellBeforeTargetDate() throws Exception {
            // Disbursed in Jan 2026, target=March 1, 2026 → include
            Instant disbursedAt = LocalDate.of(2026, 1, 5)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of());
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            LoanApplication result = processor.process(loan);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should skip loan with null disbursedAt")
        void shouldSkipLoanWithNullDisbursedAt() throws Exception {
            LoanApplication loan = createLoan(null);

            LoanApplication result = processor.process(loan);

            assertThat(result).isNull();
        }
    }

    // ─── Interest Accrual ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Interest accrual")
    class InterestAccrual {

        @Test
        @DisplayName("should accrue interest using reducing balance method")
        void shouldAccrueInterestReducingBalance() throws Exception {
            Instant disbursedAt = LocalDate.of(2026, 1, 5)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);
            loan.setInterestMethod(InterestMethod.REDUCING_BALANCE);
            loan.setOutstandingBalance(new BigDecimal("100000"));

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of());
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            LoanApplication result = processor.process(loan);

            assertThat(result).isNotNull();
            // 12% annual / 365 days on 100000 = 32.88
            assertThat(result.getTotalInterestAccrued()).isEqualByComparingTo(new BigDecimal("32.88"));
            verify(interestHistoryRepository).save(any());
            verify(outboxWriter).write(any(LoanInterestAccrualEvent.class), eq("LoanApplication"), eq(loanId));
        }

        @Test
        @DisplayName("should accrue interest using flat rate method")
        void shouldAccrueInterestFlatRate() throws Exception {
            Instant disbursedAt = LocalDate.of(2026, 1, 5)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);
            loan.setInterestMethod(InterestMethod.FLAT_RATE);
            loan.setPrincipalAmount(new BigDecimal("100000"));
            loan.setOutstandingBalance(new BigDecimal("50000")); // reduced balance

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of());
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            LoanApplication result = processor.process(loan);

            assertThat(result).isNotNull();
            // Flat rate: 12% annual / 365 days on principal 100000 = 32.88
            assertThat(result.getTotalInterestAccrued()).isEqualByComparingTo(new BigDecimal("32.88"));
        }

        @Test
        @DisplayName("should skip interest accrual when balance is zero")
        void shouldSkipInterestAccrualWhenBalanceZero() throws Exception {
            Instant disbursedAt = LocalDate.of(2026, 1, 5)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);
            loan.setOutstandingBalance(BigDecimal.ZERO);

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of());
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            LoanApplication result = processor.process(loan);

            assertThat(result).isNotNull();
            assertThat(result.getTotalInterestAccrued()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(interestHistoryRepository, never()).save(any());
            verify(outboxWriter, never()).write(any(LoanInterestAccrualEvent.class), any(), any());
        }

        @Test
        @DisplayName("should increment interest accrued counter")
        void shouldIncrementInterestAccruedCounter() throws Exception {
            Instant disbursedAt = LocalDate.of(2026, 1, 5)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of());
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            processor.process(loan);

            assertThat(stepExecution.getExecutionContext().getInt("interestAccruedCount")).isEqualTo(1);
            String totalStr = (String) stepExecution.getExecutionContext().get("totalInterestAccrued");
            assertThat(new BigDecimal(totalStr)).isGreaterThan(BigDecimal.ZERO);
        }
    }

    // ─── Penalties ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Penalty processing")
    class PenaltyProcessing {

        @Test
        @DisplayName("should apply flat penalty for overdue schedule past grace period")
        void shouldApplyFlatPenalty() throws Exception {
            Instant disbursedAt = LocalDate.of(2025, 12, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            // Schedule overdue by 45 days (> 30 grace period)
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(45));

            PenaltyRule rule = new PenaltyRule();
            rule.setId(UUID.randomUUID());
            rule.setPenaltyType(PenaltyRule.PenaltyType.LOAN_DEFAULT);
            rule.setCalculationMethod(PenaltyRule.CalculationMethod.FLAT);
            rule.setRate(new BigDecimal("500"));
            rule.setActive(true);
            rule.setCompounding(false);

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of(schedule));
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.of(rule));
            when(loanPenaltyRepository.findByLoanIdAndScheduleId(loanId, schedule.getId()))
                    .thenReturn(List.of());
            when(loanPenaltyRepository.sumUnpaidAmountByLoanId(loanId))
                    .thenReturn(new BigDecimal("500"));

            processor.process(loan);

            verify(loanPenaltyService).applyPenalty(
                    eq(loanId), eq(memberId), eq(new BigDecimal("500")),
                    any(String.class), eq("SYSTEM"), eq(schedule.getId()));
            assertThat(stepExecution.getExecutionContext().getInt("penalizedCount")).isEqualTo(1);
        }

        @Test
        @DisplayName("should apply percentage penalty for overdue schedule")
        void shouldApplyPercentagePenalty() throws Exception {
            Instant disbursedAt = LocalDate.of(2025, 12, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(45));

            PenaltyRule rule = new PenaltyRule();
            rule.setId(UUID.randomUUID());
            rule.setPenaltyType(PenaltyRule.PenaltyType.LOAN_DEFAULT);
            rule.setCalculationMethod(PenaltyRule.CalculationMethod.PERCENTAGE);
            rule.setRate(new BigDecimal("5"));
            rule.setActive(true);
            rule.setCompounding(false);

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of(schedule));
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.of(rule));
            when(loanPenaltyRepository.findByLoanIdAndScheduleId(loanId, schedule.getId()))
                    .thenReturn(List.of());
            when(loanPenaltyRepository.sumUnpaidAmountByLoanId(loanId))
                    .thenReturn(new BigDecimal("500"));

            processor.process(loan);

            // 5% of 10000 (totalAmount - amountPaid) = 500
            ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(loanPenaltyService).applyPenalty(
                    eq(loanId), eq(memberId), amountCaptor.capture(),
                    any(String.class), eq("SYSTEM"), eq(schedule.getId()));
            assertThat(amountCaptor.getValue()).isEqualByComparingTo(new BigDecimal("500"));
        }

        @Test
        @DisplayName("should skip penalty when already applied and not compounding")
        void shouldSkipPenaltyWhenAlreadyAppliedAndNotCompounding() throws Exception {
            Instant disbursedAt = LocalDate.of(2025, 12, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(45));

            PenaltyRule rule = new PenaltyRule();
            rule.setId(UUID.randomUUID());
            rule.setPenaltyType(PenaltyRule.PenaltyType.LOAN_DEFAULT);
            rule.setCalculationMethod(PenaltyRule.CalculationMethod.FLAT);
            rule.setRate(new BigDecimal("500"));
            rule.setActive(true);
            rule.setCompounding(false);

            LoanPenalty existingPenalty = new LoanPenalty();
            existingPenalty.setId(UUID.randomUUID());

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of(schedule));
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.of(rule));
            when(loanPenaltyRepository.findByLoanIdAndScheduleId(loanId, schedule.getId()))
                    .thenReturn(List.of(existingPenalty));

            processor.process(loan);

            verify(loanPenaltyService, never()).applyPenalty(any(), any(), any(), any(), any(), any());
            assertThat(stepExecution.getExecutionContext().getInt("penalizedCount")).isEqualTo(0);
        }

        @Test
        @DisplayName("should not penalize when schedule overdue less than grace period")
        void shouldNotPenalizeWhenWithinGracePeriod() throws Exception {
            Instant disbursedAt = LocalDate.of(2025, 12, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            // Schedule overdue by 15 days (< 30 grace period)
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(15));

            PenaltyRule rule = new PenaltyRule();
            rule.setId(UUID.randomUUID());
            rule.setPenaltyType(PenaltyRule.PenaltyType.LOAN_DEFAULT);
            rule.setCalculationMethod(PenaltyRule.CalculationMethod.FLAT);
            rule.setRate(new BigDecimal("500"));
            rule.setActive(true);
            rule.setCompounding(false);

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of(schedule));
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.of(rule));

            processor.process(loan);

            verify(loanPenaltyService, never()).applyPenalty(any(), any(), any(), any(), any(), any());
        }
    }

    // ─── Loan Status Changes ──────────────────────────────────────────────

    @Nested
    @DisplayName("Loan status changes")
    class LoanStatusChanges {

        @Test
        @DisplayName("should mark loan as DEFAULTED when 90+ days overdue")
        void shouldMarkAsDefaultedWhen90DaysOverdue() throws Exception {
            Instant disbursedAt = LocalDate.of(2025, 10, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(91));

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of(schedule));
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            LoanApplication result = processor.process(loan);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(LoanStatus.DEFAULTED);
        }

        @Test
        @DisplayName("should not mark as DEFAULTED when less than 90 days overdue")
        void shouldNotDefaultWhenLessThan90Days() throws Exception {
            Instant disbursedAt = LocalDate.of(2025, 12, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);

            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(60));

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of(schedule));
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            LoanApplication result = processor.process(loan);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(LoanStatus.REPAYING);
        }

        @Test
        @DisplayName("should mark loan as CLOSED when all paid and balance zero")
        void shouldMarkAsClosedWhenAllPaidAndBalanceZero() throws Exception {
            Instant disbursedAt = LocalDate.of(2025, 12, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);
            loan.setOutstandingBalance(BigDecimal.ZERO);

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of()); // all paid
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            LoanApplication result = processor.process(loan);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(LoanStatus.CLOSED);
            assertThat(stepExecution.getExecutionContext().getInt("closedCount")).isEqualTo(1);
        }

        @Test
        @DisplayName("should not close loan when unpaid schedules remain")
        void shouldNotCloseWhenUnpaidSchedulesRemain() throws Exception {
            Instant disbursedAt = LocalDate.of(2025, 12, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);
            loan.setOutstandingBalance(BigDecimal.ZERO);

            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().plusDays(30));

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of(schedule));
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            LoanApplication result = processor.process(loan);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isNotEqualTo(LoanStatus.CLOSED);
            assertThat(stepExecution.getExecutionContext().getInt("closedCount")).isEqualTo(0);
        }
    }

    // ─── Counter Tracking ─────────────────────────────────────────────────

    @Nested
    @DisplayName("StepExecution counter tracking")
    class CounterTracking {

        @Test
        @DisplayName("beforeStep should initialize all counters to zero")
        void beforeStepShouldInitializeCounters() {
            assertThat(stepExecution.getExecutionContext().getInt("penalizedCount")).isEqualTo(0);
            assertThat(stepExecution.getExecutionContext().getInt("closedCount")).isEqualTo(0);
            assertThat(stepExecution.getExecutionContext().getInt("interestAccruedCount")).isEqualTo(0);
            String totalStr = (String) stepExecution.getExecutionContext().get("totalInterestAccrued");
            assertThat(totalStr).isEqualTo("0");
        }

        @Test
        @DisplayName("totalInterestAccrued should use BigDecimal-safe string storage")
        void totalInterestAccruedShouldUseBigDecimalSafeStorage() throws Exception {
            Instant disbursedAt = LocalDate.of(2026, 1, 5)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            LoanApplication loan = createLoan(disbursedAt);
            loan.setOutstandingBalance(new BigDecimal("100000"));

            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(List.of());
            when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                    .thenReturn(Optional.empty());

            processor.process(loan);

            // Verify stored as string, not double
            Object rawValue = stepExecution.getExecutionContext().get("totalInterestAccrued");
            assertThat(rawValue).isInstanceOf(String.class);
            BigDecimal storedValue = new BigDecimal((String) rawValue);
            assertThat(storedValue).isEqualByComparingTo(new BigDecimal("32.88"));
        }
    }
}
