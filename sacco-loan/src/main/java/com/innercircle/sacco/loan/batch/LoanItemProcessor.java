package com.innercircle.sacco.loan.batch;

import com.innercircle.sacco.common.event.LoanInterestAccrualEvent;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.service.ConfigService;
import com.innercircle.sacco.loan.entity.InterestEventType;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanInterestHistory;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.repository.LoanInterestHistoryRepository;
import com.innercircle.sacco.loan.repository.LoanPenaltyRepository;
import com.innercircle.sacco.loan.repository.RepaymentScheduleRepository;
import com.innercircle.sacco.loan.service.LoanPenaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class LoanItemProcessor implements ItemProcessor<LoanApplication, LoanApplication> {

    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanInterestHistoryRepository interestHistoryRepository;
    private final EventOutboxWriter outboxWriter;
    private final ConfigService configService;
    private final LoanPenaltyService loanPenaltyService;
    private final LoanPenaltyRepository loanPenaltyRepository;

    private String targetMonthStr;

    private org.springframework.batch.core.StepExecution stepExecution;

    @org.springframework.batch.core.annotation.BeforeStep
    public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        this.targetMonthStr = stepExecution.getJobExecution().getJobParameters().getString("targetMonth");
        stepExecution.getExecutionContext().putInt("penalizedCount", 0);
        stepExecution.getExecutionContext().putInt("closedCount", 0);
        stepExecution.getExecutionContext().putInt("interestAccruedCount", 0);
        stepExecution.getExecutionContext().putString("totalInterestAccrued", "0");
    }

    private void incrementCounter(String key) {
        if (stepExecution != null) {
            int current = stepExecution.getExecutionContext().getInt(key, 0);
            stepExecution.getExecutionContext().putInt(key, current + 1);
        }
    }

    private void addAmount(String key, BigDecimal amount) {
        if (stepExecution != null) {
            String current = (String) stepExecution.getExecutionContext().get(key);
            BigDecimal running = current != null ? new BigDecimal(current) : BigDecimal.ZERO;
            stepExecution.getExecutionContext().putString(key, running.add(amount).toPlainString());
        }
    }

    @Override
    public LoanApplication process(LoanApplication loan) {
        YearMonth targetMonth = YearMonth.parse(targetMonthStr);

        // Filter out loans disbursed in the same month as the processing month
        if (loan.getDisbursedAt() != null) {
            YearMonth disbursedMonth = YearMonth.from(loan.getDisbursedAt().atZone(ZoneId.of("UTC")));
            if (disbursedMonth.equals(targetMonth)) {
                log.info("Skipping loan {} - disbursed in target month {}", loan.getId(), targetMonth);
                return null; // Skip
            }

            int thresholdDay = getConfigInt("loan.batch.new_loan_threshold_day", 15);
            YearMonth previousMonth = targetMonth.minusMonths(1);
            if (disbursedMonth.equals(previousMonth)) {
                int disbursedDay = loan.getDisbursedAt().atZone(ZoneId.of("UTC")).getDayOfMonth();
                if (disbursedDay >= thresholdDay) {
                    log.info("Skipping loan {} - disbursed on day {} of {}, after threshold day {}",
                            loan.getId(), disbursedDay, previousMonth, thresholdDay);
                    return null; // Skip
                }
            }
        } else {
            // Un-disbursed loans stuck in REPAYING? Shouldn't happen but defensive
            return null;
        }

        LocalDate accrualDate = targetMonth.atDay(1);
        accrueMonthlyInterest(loan, accrualDate);

        LocalDate today = LocalDate.now();
        int gracePeriodDays = getConfigInt("loan.penalty.grace_period_days", 30);
        int defaultThresholdDays = getConfigInt("loan.penalty.default_threshold_days", 90);
        Optional<PenaltyRule> penaltyRuleOpt = configService.getActivePenaltyRuleByType(
                PenaltyRule.PenaltyType.LOAN_DEFAULT);

        processPenaltiesAndStatus(loan, today, gracePeriodDays, defaultThresholdDays, penaltyRuleOpt);

        return loan;
    }

    private void processPenaltiesAndStatus(
            LoanApplication loan, LocalDate today, int gracePeriodDays,
            int defaultThresholdDays, Optional<PenaltyRule> penaltyRuleOpt) {

        List<RepaymentSchedule> unpaidSchedules =
                scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId());

        boolean hasOverdue = false;
        boolean loanPenalized = false;
        boolean loanClosed = false;

        for (RepaymentSchedule schedule : unpaidSchedules) {
            if (schedule.getDueDate().isBefore(today)) {
                hasOverdue = true;

                if (schedule.getDueDate().plusDays(gracePeriodDays).isBefore(today) && penaltyRuleOpt.isPresent()) {
                    PenaltyRule rule = penaltyRuleOpt.get();

                    List<com.innercircle.sacco.loan.entity.LoanPenalty> existing =
                            loanPenaltyRepository.findByLoanIdAndScheduleId(loan.getId(), schedule.getId());
                    if (!existing.isEmpty() && !rule.isCompounding()) {
                        continue;
                    }

                    BigDecimal overdueAmount = schedule.getTotalAmount().subtract(schedule.getAmountPaid());
                    BigDecimal penaltyAmount;
                    if (rule.getCalculationMethod() == PenaltyRule.CalculationMethod.FLAT) {
                        penaltyAmount = rule.getRate();
                    } else {
                        penaltyAmount = overdueAmount.multiply(rule.getRate())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    }

                    if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
                        String reason = String.format("Late repayment penalty: installment #%d overdue since %s (%s %s)",
                                schedule.getInstallmentNumber(), schedule.getDueDate(),
                                rule.getCalculationMethod(), rule.getRate());

                        loanPenaltyService.applyPenalty(
                                loan.getId(), loan.getMemberId(), penaltyAmount,
                                reason, "SYSTEM", schedule.getId());

                        BigDecimal totalUnpaid = loanPenaltyRepository.sumUnpaidAmountByLoanId(loan.getId());
                        loan.setTotalPenalties(totalUnpaid);

                        loanPenalized = true;

                        log.info("Applied {} penalty of {} for loan {} installment #{}",
                                rule.getCalculationMethod(), penaltyAmount, loan.getId(),
                                schedule.getInstallmentNumber());
                    }
                }
            }
        }

        if (hasOverdue && unpaidSchedules.stream()
                .anyMatch(s -> s.getDueDate().plusDays(defaultThresholdDays).isBefore(today))) {
            loan.setStatus(LoanStatus.DEFAULTED);
            log.info("Loan {} marked as DEFAULTED", loan.getId());
        }

        if (unpaidSchedules.isEmpty() && loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loanClosed = true;
            log.info("Loan {} marked as CLOSED", loan.getId());
        }

        if (loanPenalized) {
            incrementCounter("penalizedCount");
        }
        if (loanClosed) {
            incrementCounter("closedCount");
        }
    }

    private void accrueMonthlyInterest(LoanApplication loan, LocalDate accrualDate) {
        BigDecimal outstandingBalance = loan.getOutstandingBalance();
        if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal annualRate = loan.getInterestRate();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        BigDecimal monthlyInterest;
        if (loan.getInterestMethod() == InterestMethod.REDUCING_BALANCE) {
            monthlyInterest = outstandingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        } else {
            monthlyInterest = loan.getPrincipalAmount().multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        }

        if (monthlyInterest.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal newTotalAccrued = loan.getTotalInterestAccrued().add(monthlyInterest);
        loan.setTotalInterestAccrued(newTotalAccrued);

        LoanInterestHistory history = new LoanInterestHistory();
        history.setLoanId(loan.getId());
        history.setMemberId(loan.getMemberId());
        history.setAccrualDate(accrualDate);
        history.setInterestAmount(monthlyInterest);
        history.setOutstandingBalanceSnapshot(outstandingBalance);
        history.setCumulativeInterestAccrued(newTotalAccrued);
        history.setInterestRate(annualRate);
        history.setEventType(InterestEventType.MONTHLY_ACCRUAL);
        history.setDescription(String.format("Monthly interest accrual: %s on balance %s at %s%% p.a.",
                monthlyInterest, outstandingBalance, annualRate));
        interestHistoryRepository.save(history);

        outboxWriter.write(new LoanInterestAccrualEvent(
                loan.getId(),
                loan.getMemberId(),
                monthlyInterest,
                outstandingBalance,
                accrualDate,
                UUID.randomUUID(),
                "SYSTEM"
        ), "LoanApplication", loan.getId());

        incrementCounter("interestAccruedCount");
        addAmount("totalInterestAccrued", monthlyInterest);

        log.info("Accrued interest {} for loan {} (balance: {}, method: {})",
                monthlyInterest, loan.getId(), outstandingBalance, loan.getInterestMethod());
    }

    private int getConfigInt(String key, int defaultValue) {
        try {
            SystemConfig config = configService.getSystemConfig(key);
            String value = config.getConfigValue();
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.warn("Invalid integer for config key '{}', using default {}", key, defaultValue);
            return defaultValue;
        }
    }
}
