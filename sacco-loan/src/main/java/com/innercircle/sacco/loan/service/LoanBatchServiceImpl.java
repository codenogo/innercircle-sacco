package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.LoanBatchProcessedEvent;
import com.innercircle.sacco.common.event.LoanInterestAccrualEvent;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.service.ConfigService;
import com.innercircle.sacco.loan.dto.BatchProcessingResult;
import com.innercircle.sacco.loan.entity.BatchProcessingLog;
import com.innercircle.sacco.loan.entity.BatchProcessingStatus;
import com.innercircle.sacco.loan.entity.InterestEventType;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanInterestHistory;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.repository.BatchProcessingLogRepository;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.LoanInterestHistoryRepository;
import com.innercircle.sacco.loan.repository.LoanPenaltyRepository;
import com.innercircle.sacco.loan.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanBatchServiceImpl implements LoanBatchService {

    private final LoanApplicationRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanInterestHistoryRepository interestHistoryRepository;
    private final InterestCalculator interestCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final BatchProcessingLogRepository batchLogRepository;
    private final ConfigService configService;
    private final LoanPenaltyService loanPenaltyService;
    private final LoanPenaltyRepository loanPenaltyRepository;

    @Override
    @Transactional
    @Scheduled(cron = "${sacco.batch.loan-processing-cron:0 0 1 1 * *}")
    public BatchProcessingResult processOutstandingLoans() {
        log.info("Scheduled batch processing triggered");

        // 7.2: Check configurable day-of-month
        int processingDay = getConfigInt("loan.batch.processing_day_of_month", 1);
        int todayDay = LocalDate.now().getDayOfMonth();
        if (todayDay < processingDay) {
            log.info("Today ({}) is before configured processing day ({}), skipping", todayDay, processingDay);
            return BatchProcessingResult.builder()
                    .processedLoans(0)
                    .processedAt(Instant.now())
                    .message("Skipped: today is before configured processing day " + processingDay)
                    .build();
        }

        // Auto-determine target month
        YearMonth targetMonth = determineNextMonth();
        if (targetMonth == null) {
            // First run ever - process current month
            targetMonth = YearMonth.now();
        }

        return processMonthlyLoans(targetMonth, "SYSTEM");
    }

    @Override
    @Transactional
    public BatchProcessingResult processMonthlyLoans(YearMonth targetMonth, String triggeredBy) {
        log.info("Processing monthly loans for {} triggered by {}", targetMonth, triggeredBy);
        String monthKey = targetMonth.toString();

        // 7.4: Idempotency - prevent same-month double-processing
        if (batchLogRepository.existsByProcessingMonth(monthKey)) {
            throw new IllegalStateException("Month already processed: " + targetMonth);
        }

        // 7.5: Sequential enforcement - must process months in order
        String lastProcessedStr = getConfigValue("loan.batch.last_processed_month");
        if (lastProcessedStr != null && !lastProcessedStr.isBlank()) {
            YearMonth lastProcessed = YearMonth.parse(lastProcessedStr);
            YearMonth expectedNext = lastProcessed.plusMonths(1);
            if (!targetMonth.equals(expectedNext)) {
                throw new IllegalStateException(
                        "Must process " + expectedNext + " first. Cannot skip to " + targetMonth);
            }
        }

        // 7.8: Create batch log with STARTED status
        BatchProcessingLog batchLog = BatchProcessingLog.builder()
                .processingMonth(monthKey)
                .status(BatchProcessingStatus.STARTED)
                .startedAt(Instant.now())
                .triggeredBy(triggeredBy)
                .build();
        batchLogRepository.save(batchLog);

        try {
            BatchProcessingResult result = executeProcessing(targetMonth, batchLog);

            // Update batch log to COMPLETED
            batchLog.setStatus(BatchProcessingStatus.COMPLETED);
            batchLog.setLoansProcessed(result.getProcessedLoans());
            batchLog.setInterestAccrued(result.getTotalInterestAccrued());
            batchLog.setPenalizedLoans(result.getPenalizedLoans());
            batchLog.setClosedLoans(result.getClosedLoans());
            batchLog.setCompletedAt(Instant.now());
            batchLogRepository.save(batchLog);

            // 7.8: Update last processed month in config
            configService.updateSystemConfig("loan.batch.last_processed_month", monthKey);

            // Publish event
            eventPublisher.publishEvent(new LoanBatchProcessedEvent(
                    result.getProcessedLoans(),
                    result.getPenalizedLoans(),
                    result.getClosedLoans(),
                    result.getProcessedAt(),
                    UUID.randomUUID(),
                    triggeredBy
            ));

            log.info("Batch processing completed for {}: {}", targetMonth, result.getMessage());
            return result;

        } catch (Exception e) {
            // Update batch log to FAILED
            batchLog.setStatus(BatchProcessingStatus.FAILED);
            batchLog.setCompletedAt(Instant.now());
            batchLog.setWarningsSummary("Processing failed: " + e.getMessage());
            batchLogRepository.save(batchLog);
            log.error("Batch processing failed for {}: {}", targetMonth, e.getMessage(), e);
            throw e;
        }
    }

    private BatchProcessingResult executeProcessing(YearMonth targetMonth, BatchProcessingLog batchLog) {
        List<String> warnings = new ArrayList<>();

        // 7.7: Pre-processing warnings for unpaid loans
        LocalDate targetDate = targetMonth.atDay(1);
        List<Map<String, Object>> unpaidLoans = detectUnpaidLoans(targetDate);
        if (!unpaidLoans.isEmpty()) {
            String warning = String.format("%d unpaid loan installment(s) detected for %s - penalties may apply",
                    unpaidLoans.size(), targetMonth);
            warnings.add(warning);
            log.warn(warning);
        }

        // Get all REPAYING loans
        List<LoanApplication> repayingLoans = loanRepository.findByStatus(LoanStatus.REPAYING);

        // 7.6: Filter out loans disbursed in the same month as the processing month
        List<LoanApplication> eligibleLoans = new ArrayList<>();
        for (LoanApplication loan : repayingLoans) {
            if (loan.getDisbursedAt() == null) {
                eligibleLoans.add(loan);
                continue;
            }
            YearMonth disbursedMonth = YearMonth.from(loan.getDisbursedAt().atZone(ZoneId.of("UTC")));
            if (disbursedMonth.equals(targetMonth)) {
                log.info("Skipping loan {} - disbursed in target month {}", loan.getId(), targetMonth);
                warnings.add("Loan " + loan.getId() + " skipped: disbursed in same month " + targetMonth);
                continue;
            }

            // 7.3: New-loan threshold day - skip loans disbursed in the previous month after the threshold
            int thresholdDay = getConfigInt("loan.batch.new_loan_threshold_day", 15);
            YearMonth previousMonth = targetMonth.minusMonths(1);
            if (disbursedMonth.equals(previousMonth)) {
                int disbursedDay = loan.getDisbursedAt().atZone(ZoneId.of("UTC")).getDayOfMonth();
                if (disbursedDay >= thresholdDay) {
                    log.info("Skipping loan {} - disbursed on day {} of {}, after threshold day {}",
                            loan.getId(), disbursedDay, previousMonth, thresholdDay);
                    warnings.add("Loan " + loan.getId() + " skipped: disbursed after threshold day " + thresholdDay);
                    continue;
                }
            }

            eligibleLoans.add(loan);
        }

        // Accrue monthly interest on eligible loans
        int interestAccruedCount = 0;
        BigDecimal totalInterestAccrued = BigDecimal.ZERO;
        LocalDate accrualDate = targetMonth.atDay(1);

        for (LoanApplication loan : eligibleLoans) {
            try {
                BigDecimal accrued = accrueMonthlyInterest(loan, accrualDate);
                if (accrued.compareTo(BigDecimal.ZERO) > 0) {
                    interestAccruedCount++;
                    totalInterestAccrued = totalInterestAccrued.add(accrued);
                }
            } catch (Exception e) {
                log.error("Error accruing interest for loan {}: {}", loan.getId(), e.getMessage(), e);
                warnings.add("Error accruing interest for loan " + loan.getId() + ": " + e.getMessage());
            }
        }

        // Process overdue detection, penalty application, and status updates
        int processedCount = 0;
        int penalizedCount = 0;
        int closedCount = 0;
        LocalDate today = LocalDate.now();

        int gracePeriodDays = getConfigInt("loan.penalty.grace_period_days", 30);
        int defaultThresholdDays = getConfigInt("loan.penalty.default_threshold_days", 90);
        Optional<PenaltyRule> penaltyRuleOpt = configService.getActivePenaltyRuleByType(
                PenaltyRule.PenaltyType.LOAN_DEFAULT);

        for (LoanApplication loan : eligibleLoans) {
            try {
                PenaltyProcessingResult penaltyResult = processPenaltiesAndStatus(
                        loan, today, gracePeriodDays, defaultThresholdDays, penaltyRuleOpt);

                if (penaltyResult.penalized) {
                    penalizedCount++;
                }
                if (penaltyResult.closed) {
                    closedCount++;
                }

                processedCount++;
            } catch (Exception e) {
                log.error("Error processing loan {}: {}", loan.getId(), e.getMessage(), e);
                warnings.add("Error processing loan " + loan.getId() + ": " + e.getMessage());
            }
        }

        // Store warnings summary in batch log
        if (!warnings.isEmpty()) {
            batchLog.setWarningsSummary(String.join("; ", warnings));
        }

        Instant processedAt = Instant.now();

        return BatchProcessingResult.builder()
                .processedLoans(processedCount)
                .penalizedLoans(penalizedCount)
                .closedLoans(closedCount)
                .interestAccruedLoans(interestAccruedCount)
                .totalInterestAccrued(totalInterestAccrued)
                .processedAt(processedAt)
                .message(String.format("Processed %d loans, penalized %d, closed %d, interest accrued on %d loans (total: %s)",
                        processedCount, penalizedCount, closedCount, interestAccruedCount, totalInterestAccrued))
                .warnings(warnings)
                .processingMonth(targetMonth.toString())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> detectUnpaidLoans(LocalDate month) {
        log.info("Detecting unpaid loans for month: {}", month);

        List<Map<String, Object>> unpaidLoans = new ArrayList<>();

        List<LoanApplication> repayingLoans = loanRepository.findByStatus(LoanStatus.REPAYING);

        LocalDate monthStart = month.withDayOfMonth(1);
        LocalDate monthEnd = month.withDayOfMonth(month.lengthOfMonth());

        for (LoanApplication loan : repayingLoans) {
            List<RepaymentSchedule> unpaidSchedules =
                    scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId());

            for (RepaymentSchedule schedule : unpaidSchedules) {
                if (!schedule.getDueDate().isBefore(monthStart) &&
                        !schedule.getDueDate().isAfter(monthEnd)) {

                    Map<String, Object> unpaidLoanInfo = new HashMap<>();
                    unpaidLoanInfo.put("loanId", loan.getId());
                    unpaidLoanInfo.put("memberId", loan.getMemberId());
                    unpaidLoanInfo.put("installmentNumber", schedule.getInstallmentNumber());
                    unpaidLoanInfo.put("dueDate", schedule.getDueDate());
                    unpaidLoanInfo.put("totalAmount", schedule.getTotalAmount());
                    unpaidLoanInfo.put("amountPaid", schedule.getAmountPaid());
                    unpaidLoanInfo.put("outstandingAmount",
                            schedule.getTotalAmount().subtract(schedule.getAmountPaid()));
                    unpaidLoanInfo.put("principalAmount", loan.getPrincipalAmount());
                    unpaidLoanInfo.put("outstandingBalance", loan.getOutstandingBalance());

                    unpaidLoans.add(unpaidLoanInfo);
                }
            }
        }

        log.info("Found {} unpaid loan installments for month {}", unpaidLoans.size(), month);

        return unpaidLoans;
    }

    @Override
    @Transactional
    public void processLoan(UUID loanId) {
        log.info("Processing individual loan: {}", loanId);

        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with id: " + loanId));

        if (loan.getStatus() != LoanStatus.REPAYING) {
            throw new IllegalStateException("Loan is not in REPAYING status");
        }

        LocalDate today = LocalDate.now();
        int gracePeriod = getConfigInt("loan.penalty.grace_period_days", 30);
        int defaultThreshold = getConfigInt("loan.penalty.default_threshold_days", 90);
        Optional<PenaltyRule> ruleOpt = configService.getActivePenaltyRuleByType(
                PenaltyRule.PenaltyType.LOAN_DEFAULT);

        processPenaltiesAndStatus(loan, today, gracePeriod, defaultThreshold, ruleOpt);

        log.info("Individual loan processing completed for: {}", loanId);
    }

    private static class PenaltyProcessingResult {
        final boolean penalized;
        final boolean closed;

        PenaltyProcessingResult(boolean penalized, boolean closed) {
            this.penalized = penalized;
            this.closed = closed;
        }
    }

    private PenaltyProcessingResult processPenaltiesAndStatus(
            LoanApplication loan, LocalDate today, int gracePeriodDays,
            int defaultThresholdDays, Optional<PenaltyRule> penaltyRuleOpt) {

        List<RepaymentSchedule> unpaidSchedules =
                scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId());

        boolean hasOverdue = false;
        boolean loanPenalized = false;

        for (RepaymentSchedule schedule : unpaidSchedules) {
            if (schedule.getDueDate().isBefore(today)) {
                hasOverdue = true;

                if (schedule.getDueDate().plusDays(gracePeriodDays).isBefore(today) && penaltyRuleOpt.isPresent()) {
                    PenaltyRule rule = penaltyRuleOpt.get();

                    // Idempotency: check if penalty already applied for this schedule
                    List<com.innercircle.sacco.loan.entity.LoanPenalty> existing =
                            loanPenaltyRepository.findByLoanIdAndScheduleId(loan.getId(), schedule.getId());
                    if (!existing.isEmpty() && !rule.isCompounding()) {
                        continue;
                    }

                    // Calculate penalty amount
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

                        // Update loan totalPenalties
                        BigDecimal totalUnpaid = loanPenaltyRepository.sumUnpaidAmountByLoanId(loan.getId());
                        loan.setTotalPenalties(totalUnpaid);
                        loanRepository.save(loan);

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
            loanRepository.save(loan);
            log.info("Loan {} marked as DEFAULTED", loan.getId());
        }

        boolean closed = false;
        if (unpaidSchedules.isEmpty() &&
                loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loanRepository.save(loan);
            closed = true;
            log.info("Loan {} marked as CLOSED", loan.getId());
        }

        return new PenaltyProcessingResult(loanPenalized, closed);
    }

    private BigDecimal accrueMonthlyInterest(LoanApplication loan, LocalDate accrualDate) {
        BigDecimal outstandingBalance = loan.getOutstandingBalance();
        if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
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
            return BigDecimal.ZERO;
        }

        BigDecimal newTotalAccrued = loan.getTotalInterestAccrued().add(monthlyInterest);
        loan.setTotalInterestAccrued(newTotalAccrued);
        loanRepository.save(loan);

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

        eventPublisher.publishEvent(new LoanInterestAccrualEvent(
                loan.getId(),
                loan.getMemberId(),
                monthlyInterest,
                outstandingBalance,
                accrualDate,
                UUID.randomUUID(),
                "SYSTEM"
        ));

        log.info("Accrued interest {} for loan {} (balance: {}, method: {})",
                monthlyInterest, loan.getId(), outstandingBalance, loan.getInterestMethod());

        return monthlyInterest;
    }

    private YearMonth determineNextMonth() {
        String lastProcessedStr = getConfigValue("loan.batch.last_processed_month");
        if (lastProcessedStr == null || lastProcessedStr.isBlank()) {
            return null;
        }
        return YearMonth.parse(lastProcessedStr).plusMonths(1);
    }

    private String getConfigValue(String key) {
        try {
            SystemConfig config = configService.getSystemConfig(key);
            return config.getConfigValue();
        } catch (Exception e) {
            log.warn("Config key '{}' not found, returning null", key);
            return null;
        }
    }

    private int getConfigInt(String key, int defaultValue) {
        String value = getConfigValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for config key '{}': {}, using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
