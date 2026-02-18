package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.LoanBatchProcessedEvent;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.service.ConfigService;
import com.innercircle.sacco.loan.dto.BatchProcessingResult;
import com.innercircle.sacco.loan.entity.BatchProcessingLog;
import com.innercircle.sacco.loan.entity.BatchProcessingStatus;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanPenalty;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.repository.BatchProcessingLogRepository;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.LoanInterestHistoryRepository;
import com.innercircle.sacco.loan.repository.LoanPenaltyRepository;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoanBatchServiceImplTest {

    @Mock
    private LoanApplicationRepository loanRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private LoanInterestHistoryRepository interestHistoryRepository;

    @Mock
    private InterestCalculator interestCalculator;

    @Mock
    private EventOutboxWriter outboxWriter;

    @Mock
    private BatchProcessingLogRepository batchLogRepository;

    @Mock
    private ConfigService configService;

    @Mock
    private LoanPenaltyService loanPenaltyService;

    @Mock
    private LoanPenaltyRepository loanPenaltyRepository;

    @InjectMocks
    private LoanBatchServiceImpl batchService;

    @Captor
    private ArgumentCaptor<LoanApplication> loanCaptor;

    @Captor
    private ArgumentCaptor<BatchProcessingLog> batchLogCaptor;

    private UUID loanId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();
        memberId = UUID.randomUUID();
    }

    private void setupDefaultBatchMocks() {
        SystemConfig processingDayConfig = new SystemConfig();
        processingDayConfig.setConfigValue("1");
        when(configService.getSystemConfig("loan.batch.processing_day_of_month")).thenReturn(processingDayConfig);
        when(configService.getSystemConfig("loan.batch.last_processed_month")).thenThrow(new RuntimeException("Not found"));
        when(batchLogRepository.existsByProcessingMonth(any())).thenReturn(false);
        when(batchLogRepository.save(any())).thenAnswer(inv -> {
            BatchProcessingLog log = inv.getArgument(0);
            if (log.getId() == null) {
                log.setId(UUID.randomUUID());
            }
            return log;
        });
        when(configService.updateSystemConfig(any(), any())).thenReturn(new SystemConfig());
        SystemConfig thresholdConfig = new SystemConfig();
        thresholdConfig.setConfigValue("15");
        when(configService.getSystemConfig("loan.batch.new_loan_threshold_day")).thenReturn(thresholdConfig);
    }

    private void setupPenaltyRuleMock() {
        PenaltyRule rule = new PenaltyRule();
        rule.setId(UUID.randomUUID());
        rule.setName("Late Loan Repayment");
        rule.setPenaltyType(PenaltyRule.PenaltyType.LOAN_DEFAULT);
        rule.setCalculationMethod(PenaltyRule.CalculationMethod.FLAT);
        rule.setRate(new BigDecimal("500"));
        rule.setActive(true);
        rule.setCompounding(false);
        when(configService.getActivePenaltyRuleByType(PenaltyRule.PenaltyType.LOAN_DEFAULT))
                .thenReturn(Optional.of(rule));
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
            setupDefaultBatchMocks();
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
            setupDefaultBatchMocks();
            setupPenaltyRuleMock();
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(31));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));
            when(loanPenaltyRepository.findByLoanIdAndScheduleId(any(), any()))
                    .thenReturn(List.of());
            when(loanPenaltyRepository.sumUnpaidAmountByLoanId(any()))
                    .thenReturn(new BigDecimal("500"));

            BatchProcessingResult result = batchService.processOutstandingLoans();

            assertThat(result.getPenalizedLoans()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not penalize loans overdue by less than 30 days")
        void shouldNotPenalizeLoansOverdueLessThan30Days() {
            setupDefaultBatchMocks();
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
            setupDefaultBatchMocks();
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(91));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            batchService.processOutstandingLoans();

            // Called twice: once for interest accrual, once for DEFAULTED status
            verify(loanRepository, times(2)).save(loanCaptor.capture());
            List<LoanApplication> savedLoans = loanCaptor.getAllValues();
            assertThat(savedLoans.get(savedLoans.size() - 1).getStatus()).isEqualTo(LoanStatus.DEFAULTED);
        }

        @Test
        @DisplayName("should not mark as defaulted when overdue by less than 90 days")
        void shouldNotMarkAsDefaultedWhenLessThan90Days() {
            setupDefaultBatchMocks();
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            RepaymentSchedule schedule = createSchedule(1, LocalDate.now().minusDays(60));

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId()))
                    .thenReturn(List.of(schedule));

            batchService.processOutstandingLoans();

            // Save called once for interest accrual only; status should remain REPAYING
            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.REPAYING);
        }

        @Test
        @DisplayName("should close loan when all schedules paid and balance is zero")
        void shouldCloseLoanWhenAllPaidAndBalanceZero() {
            setupDefaultBatchMocks();
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
            setupDefaultBatchMocks();
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
            setupDefaultBatchMocks();
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
            setupDefaultBatchMocks();
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of());

            BatchProcessingResult result = batchService.processOutstandingLoans();

            assertThat(result.getProcessedLoans()).isEqualTo(0);
            assertThat(result.getPenalizedLoans()).isEqualTo(0);
            assertThat(result.getClosedLoans()).isEqualTo(0);
        }

        @Test
        @DisplayName("should continue processing when individual loan throws exception")
        void shouldContinueOnException() {
            setupDefaultBatchMocks();
            LoanApplication loan1 = createLoan(LoanStatus.REPAYING);
            LoanApplication loan2 = createLoan(LoanStatus.REPAYING);
            loan2.setId(UUID.randomUUID());
            loan2.setOutstandingBalance(BigDecimal.ZERO);

            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan1, loan2));
            // First call (detectUnpaidLoans) returns empty, second call (processing loop) throws
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan1.getId()))
                    .thenReturn(List.of())
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
            setupDefaultBatchMocks();
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of());

            batchService.processOutstandingLoans();

            ArgumentCaptor<LoanBatchProcessedEvent> eventCaptor =
                    ArgumentCaptor.forClass(LoanBatchProcessedEvent.class);
            verify(outboxWriter).write(eventCaptor.capture(), eq("LoanApplication"), any(UUID.class));

            LoanBatchProcessedEvent event = eventCaptor.getValue();
            assertThat(event.processedLoans()).isEqualTo(0);
            assertThat(event.actor()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("should process multiple loans with mixed statuses")
        void shouldProcessMultipleLoansWithMixedStatuses() {
            setupDefaultBatchMocks();
            setupPenaltyRuleMock();
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
            when(loanPenaltyRepository.findByLoanIdAndScheduleId(any(), any()))
                    .thenReturn(List.of());
            when(loanPenaltyRepository.sumUnpaidAmountByLoanId(any()))
                    .thenReturn(new BigDecimal("500"));

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
    // processMonthlyLoans
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("processMonthlyLoans")
    class ProcessMonthlyLoans {

        @Test
        @DisplayName("should throw when month already processed (idempotency)")
        void shouldThrowWhenMonthAlreadyProcessed() {
            YearMonth targetMonth = YearMonth.of(2026, 2);
            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(true);

            assertThatThrownBy(() -> batchService.processMonthlyLoans(targetMonth, "SYSTEM"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Month already processed");
        }

        @Test
        @DisplayName("should succeed when processing sequential month after last processed")
        void shouldSucceedWhenProcessingSequentialMonth() {
            YearMonth targetMonth = YearMonth.of(2026, 2);
            SystemConfig lastProcessedConfig = new SystemConfig();
            lastProcessedConfig.setConfigValue("2026-01");
            SystemConfig thresholdConfig = new SystemConfig();
            thresholdConfig.setConfigValue("15");

            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(false);
            when(configService.getSystemConfig("loan.batch.last_processed_month")).thenReturn(lastProcessedConfig);
            when(configService.getSystemConfig("loan.batch.new_loan_threshold_day")).thenReturn(thresholdConfig);
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of());
            when(batchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(configService.updateSystemConfig(any(), any())).thenReturn(new SystemConfig());

            BatchProcessingResult result = batchService.processMonthlyLoans(targetMonth, "SYSTEM");

            assertThat(result).isNotNull();
            verify(configService).updateSystemConfig("loan.batch.last_processed_month", "2026-02");
        }

        @Test
        @DisplayName("should throw when trying to skip a month (sequential enforcement)")
        void shouldThrowWhenSkippingMonth() {
            YearMonth targetMonth = YearMonth.of(2026, 3);
            SystemConfig lastProcessedConfig = new SystemConfig();
            lastProcessedConfig.setConfigValue("2026-01");

            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(false);
            when(configService.getSystemConfig("loan.batch.last_processed_month")).thenReturn(lastProcessedConfig);

            assertThatThrownBy(() -> batchService.processMonthlyLoans(targetMonth, "SYSTEM"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Must process 2026-02 first");
        }

        @Test
        @DisplayName("should skip loans disbursed in target month")
        void shouldSkipLoansDisbursedInTargetMonth() {
            YearMonth targetMonth = YearMonth.of(2026, 2);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setDisbursedAt(Instant.from(YearMonth.of(2026, 2).atDay(10).atStartOfDay(ZoneId.of("UTC"))));

            SystemConfig thresholdConfig = new SystemConfig();
            thresholdConfig.setConfigValue("15");

            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(false);
            when(configService.getSystemConfig("loan.batch.last_processed_month")).thenThrow(new RuntimeException("Not found"));
            when(configService.getSystemConfig("loan.batch.new_loan_threshold_day")).thenReturn(thresholdConfig);
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(batchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(configService.updateSystemConfig(any(), any())).thenReturn(new SystemConfig());

            BatchProcessingResult result = batchService.processMonthlyLoans(targetMonth, "SYSTEM");

            assertThat(result.getProcessedLoans()).isEqualTo(0);
        }

        @Test
        @DisplayName("should skip loans disbursed after threshold day of previous month")
        void shouldSkipLoansDisbursedAfterThresholdDay() {
            YearMonth targetMonth = YearMonth.of(2026, 2);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setDisbursedAt(Instant.from(YearMonth.of(2026, 1).atDay(20).atStartOfDay(ZoneId.of("UTC"))));

            SystemConfig thresholdConfig = new SystemConfig();
            thresholdConfig.setConfigValue("15");

            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(false);
            when(configService.getSystemConfig("loan.batch.last_processed_month")).thenThrow(new RuntimeException("Not found"));
            when(configService.getSystemConfig("loan.batch.new_loan_threshold_day")).thenReturn(thresholdConfig);
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(batchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(configService.updateSystemConfig(any(), any())).thenReturn(new SystemConfig());

            BatchProcessingResult result = batchService.processMonthlyLoans(targetMonth, "SYSTEM");

            assertThat(result.getProcessedLoans()).isEqualTo(0);
        }

        @Test
        @DisplayName("should include loans disbursed on or before threshold day of previous month")
        void shouldIncludeLoansDisbursedBeforeThresholdDay() {
            YearMonth targetMonth = YearMonth.of(2026, 2);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setDisbursedAt(Instant.from(YearMonth.of(2026, 1).atDay(10).atStartOfDay(ZoneId.of("UTC"))));

            SystemConfig thresholdConfig = new SystemConfig();
            thresholdConfig.setConfigValue("15");

            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(false);
            when(configService.getSystemConfig("loan.batch.last_processed_month")).thenThrow(new RuntimeException("Not found"));
            when(configService.getSystemConfig("loan.batch.new_loan_threshold_day")).thenReturn(thresholdConfig);
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId())).thenReturn(List.of());
            when(batchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(configService.updateSystemConfig(any(), any())).thenReturn(new SystemConfig());

            BatchProcessingResult result = batchService.processMonthlyLoans(targetMonth, "SYSTEM");

            assertThat(result.getProcessedLoans()).isEqualTo(1);
        }

        @Test
        @DisplayName("should include warnings for unpaid loans in pre-processing phase")
        void shouldIncludeWarningsForUnpaidLoans() {
            YearMonth targetMonth = YearMonth.of(2026, 2);
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setDisbursedAt(Instant.from(YearMonth.of(2026, 1).atDay(10).atStartOfDay(ZoneId.of("UTC"))));

            // Unpaid schedule must be due in the target month (February) for warning detection
            RepaymentSchedule unpaidSchedule = createSchedule(2, LocalDate.of(2026, 2, 15));
            unpaidSchedule.setTotalAmount(new BigDecimal("10000"));
            unpaidSchedule.setAmountPaid(new BigDecimal("5000"));

            SystemConfig thresholdConfig = new SystemConfig();
            thresholdConfig.setConfigValue("15");

            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(false);
            when(configService.getSystemConfig("loan.batch.last_processed_month")).thenThrow(new RuntimeException("Not found"));
            when(configService.getSystemConfig("loan.batch.new_loan_threshold_day")).thenReturn(thresholdConfig);
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId())).thenReturn(List.of(unpaidSchedule));
            when(batchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(configService.updateSystemConfig(any(), any())).thenReturn(new SystemConfig());

            BatchProcessingResult result = batchService.processMonthlyLoans(targetMonth, "SYSTEM");

            assertThat(result.getWarnings()).isNotEmpty();
        }

        @Test
        @DisplayName("should save batch log with STARTED then COMPLETED status")
        void shouldSaveBatchLogLifecycle() {
            YearMonth targetMonth = YearMonth.of(2026, 2);
            SystemConfig thresholdConfig = new SystemConfig();
            thresholdConfig.setConfigValue("15");

            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(false);
            when(configService.getSystemConfig("loan.batch.last_processed_month")).thenThrow(new RuntimeException("Not found"));
            when(configService.getSystemConfig("loan.batch.new_loan_threshold_day")).thenReturn(thresholdConfig);
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of());
            when(batchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(configService.updateSystemConfig(any(), any())).thenReturn(new SystemConfig());

            batchService.processMonthlyLoans(targetMonth, "SYSTEM");

            // Verify batch log was saved at least twice (STARTED + COMPLETED)
            verify(batchLogRepository, atLeast(2)).save(batchLogCaptor.capture());
            List<BatchProcessingLog> savedLogs = batchLogCaptor.getAllValues();
            assertThat(savedLogs).hasSizeGreaterThanOrEqualTo(2);

            // Since Mockito captures references (not snapshots), the same mutated object
            // shows the final COMPLETED status for all captures. Verify the final state.
            BatchProcessingLog finalLog = savedLogs.get(savedLogs.size() - 1);
            assertThat(finalLog.getStatus()).isEqualTo(BatchProcessingStatus.COMPLETED);
            assertThat(finalLog.getProcessingMonth()).isEqualTo("2026-02");
            assertThat(finalLog.getTriggeredBy()).isEqualTo("SYSTEM");
            assertThat(finalLog.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should update batch log to FAILED when processing throws exception")
        void shouldUpdateBatchLogToFailedOnException() {
            YearMonth targetMonth = YearMonth.of(2026, 2);

            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(false);
            when(configService.getSystemConfig("loan.batch.last_processed_month")).thenThrow(new RuntimeException("Not found"));
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenThrow(new RuntimeException("Database error"));
            when(batchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> batchService.processMonthlyLoans(targetMonth, "SYSTEM"))
                    .isInstanceOf(RuntimeException.class);

            // Verify batch log was saved at least twice (STARTED + FAILED)
            verify(batchLogRepository, atLeast(2)).save(batchLogCaptor.capture());
            List<BatchProcessingLog> savedLogs = batchLogCaptor.getAllValues();

            // Same object reference, final state should be FAILED
            BatchProcessingLog finalLog = savedLogs.get(savedLogs.size() - 1);
            assertThat(finalLog.getStatus()).isEqualTo(BatchProcessingStatus.FAILED);
            assertThat(finalLog.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should update config with processed month on success")
        void shouldUpdateConfigWithProcessedMonthOnSuccess() {
            YearMonth targetMonth = YearMonth.of(2026, 2);
            SystemConfig thresholdConfig = new SystemConfig();
            thresholdConfig.setConfigValue("15");

            when(batchLogRepository.existsByProcessingMonth(targetMonth.toString())).thenReturn(false);
            when(configService.getSystemConfig("loan.batch.last_processed_month")).thenThrow(new RuntimeException("Not found"));
            when(configService.getSystemConfig("loan.batch.new_loan_threshold_day")).thenReturn(thresholdConfig);
            when(loanRepository.findByStatus(LoanStatus.REPAYING)).thenReturn(List.of());
            when(batchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(configService.updateSystemConfig(any(), any())).thenReturn(new SystemConfig());

            batchService.processMonthlyLoans(targetMonth, "SYSTEM");

            verify(configService).updateSystemConfig("loan.batch.last_processed_month", "2026-02");
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
        loan.setInterestMethod(InterestMethod.FLAT_RATE);
        loan.setStatus(status);
        loan.setTotalRepaid(BigDecimal.ZERO);
        loan.setOutstandingBalance(new BigDecimal("100000"));
        loan.setTotalInterestAccrued(BigDecimal.ZERO);
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
