package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.LoanBatchProcessedEvent;
import com.innercircle.sacco.loan.dto.BatchProcessingResult;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanBatchServiceImpl implements LoanBatchService {

    private final LoanApplicationRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @Scheduled(cron = "${sacco.batch.loan-processing-cron:0 0 1 1 * *}")
    public BatchProcessingResult processOutstandingLoans() {
        log.info("Starting batch processing of outstanding loans");

        List<LoanApplication> repayingLoans = loanRepository.findByStatus(LoanStatus.REPAYING);

        int processedCount = 0;
        int penalizedCount = 0;
        int closedCount = 0;

        LocalDate today = LocalDate.now();

        for (LoanApplication loan : repayingLoans) {
            try {
                // Get all unpaid schedules for this loan
                List<RepaymentSchedule> unpaidSchedules =
                    scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId());

                boolean hasOverdue = false;
                boolean shouldPenalize = false;

                for (RepaymentSchedule schedule : unpaidSchedules) {
                    if (schedule.getDueDate().isBefore(today)) {
                        hasOverdue = true;
                        // Check if significantly overdue (e.g., more than 30 days)
                        if (schedule.getDueDate().plusDays(30).isBefore(today)) {
                            shouldPenalize = true;
                        }
                    }
                }

                if (shouldPenalize) {
                    // Apply penalty logic here (would integrate with penalty service if available)
                    // For now, we just count it
                    penalizedCount++;
                    log.info("Loan {} is significantly overdue and needs penalty", loan.getId());
                }

                // Check if loan should be marked as defaulted
                if (hasOverdue && unpaidSchedules.stream()
                        .anyMatch(s -> s.getDueDate().plusDays(90).isBefore(today))) {
                    loan.setStatus(LoanStatus.DEFAULTED);
                    loanRepository.save(loan);
                    log.info("Loan {} marked as DEFAULTED", loan.getId());
                }

                // Check if loan should be closed (all paid)
                if (unpaidSchedules.isEmpty() &&
                    loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
                    loan.setStatus(LoanStatus.CLOSED);
                    loanRepository.save(loan);
                    closedCount++;
                    log.info("Loan {} marked as CLOSED", loan.getId());
                }

                processedCount++;
            } catch (Exception e) {
                log.error("Error processing loan {}: {}", loan.getId(), e.getMessage(), e);
            }
        }

        Instant processedAt = Instant.now();

        BatchProcessingResult result = BatchProcessingResult.builder()
                .processedLoans(processedCount)
                .penalizedLoans(penalizedCount)
                .closedLoans(closedCount)
                .processedAt(processedAt)
                .message(String.format("Processed %d loans, penalized %d, closed %d",
                    processedCount, penalizedCount, closedCount))
                .build();

        // Publish event
        eventPublisher.publishEvent(new LoanBatchProcessedEvent(
                processedCount,
                penalizedCount,
                closedCount,
                processedAt,
                "SYSTEM"
        ));

        log.info("Batch processing completed: {}", result.getMessage());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> detectUnpaidLoans(LocalDate month) {
        log.info("Detecting unpaid loans for month: {}", month);

        List<Map<String, Object>> unpaidLoans = new ArrayList<>();

        // Get all REPAYING loans
        List<LoanApplication> repayingLoans = loanRepository.findByStatus(LoanStatus.REPAYING);

        LocalDate monthStart = month.withDayOfMonth(1);
        LocalDate monthEnd = month.withDayOfMonth(month.lengthOfMonth());

        for (LoanApplication loan : repayingLoans) {
            // Find schedules due in this month that are unpaid
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

        // Get all unpaid schedules
        List<RepaymentSchedule> unpaidSchedules =
            scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loan.getId());

        boolean hasOverdue = false;

        for (RepaymentSchedule schedule : unpaidSchedules) {
            if (schedule.getDueDate().isBefore(today)) {
                hasOverdue = true;
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                    schedule.getDueDate(), today);

                log.info("Loan {} has overdue installment #{} - {} days overdue",
                    loanId, schedule.getInstallmentNumber(), daysOverdue);

                // If significantly overdue (more than 30 days), apply penalty
                if (daysOverdue > 30) {
                    log.info("Loan {} installment #{} requires penalty application",
                        loanId, schedule.getInstallmentNumber());
                    // Penalty application would be handled here
                }
            }
        }

        // Mark as defaulted if any installment is 90+ days overdue
        if (hasOverdue && unpaidSchedules.stream()
                .anyMatch(s -> s.getDueDate().plusDays(90).isBefore(today))) {
            loan.setStatus(LoanStatus.DEFAULTED);
            loanRepository.save(loan);
            log.info("Loan {} marked as DEFAULTED", loanId);
        }

        // Check if should be closed
        if (unpaidSchedules.isEmpty() &&
            loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loanRepository.save(loan);
            log.info("Loan {} marked as CLOSED", loanId);
        }

        log.info("Individual loan processing completed for: {}", loanId);
    }
}
