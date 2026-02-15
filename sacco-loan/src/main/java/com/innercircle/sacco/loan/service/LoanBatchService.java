package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.dto.BatchProcessingResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LoanBatchService {

    /**
     * Process all outstanding loans - checks for overdue installments,
     * applies penalties, and updates loan statuses.
     *
     * @return BatchProcessingResult with processing statistics
     */
    BatchProcessingResult processOutstandingLoans();

    /**
     * Process monthly loans with full safeguards (7.2-7.8).
     * Includes idempotency check, sequential enforcement, loan filtering,
     * and batch log tracking.
     *
     * @param targetMonth the month to process
     * @param triggeredBy who triggered the processing (username or "SYSTEM")
     * @return BatchProcessingResult with processing statistics and warnings
     */
    BatchProcessingResult processMonthlyLoans(YearMonth targetMonth, String triggeredBy);

    /**
     * Detect loans with unpaid installments for a given month.
     *
     * @param month the month to check (YYYY-MM format)
     * @return list of overdue loan details
     */
    List<Map<String, Object>> detectUnpaidLoans(LocalDate month);

    /**
     * Process a single loan - check schedule, apply penalties if overdue.
     *
     * @param loanId the loan ID to process
     */
    void processLoan(UUID loanId);
}
