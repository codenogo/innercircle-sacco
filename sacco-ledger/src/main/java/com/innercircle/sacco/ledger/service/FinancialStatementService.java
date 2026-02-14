package com.innercircle.sacco.ledger.service;

import com.innercircle.sacco.ledger.dto.BalanceSheetResponse;
import com.innercircle.sacco.ledger.dto.IncomeStatementResponse;
import com.innercircle.sacco.ledger.dto.TrialBalanceResponse;

import java.time.LocalDate;

public interface FinancialStatementService {

    /**
     * Generate a trial balance as of a specific date.
     *
     * @param asOfDate the date for the trial balance
     * @return the trial balance
     */
    TrialBalanceResponse generateTrialBalance(LocalDate asOfDate);

    /**
     * Generate an income statement for a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return the income statement
     */
    IncomeStatementResponse generateIncomeStatement(LocalDate startDate, LocalDate endDate);

    /**
     * Generate a balance sheet as of a specific date.
     *
     * @param asOfDate the date for the balance sheet
     * @return the balance sheet
     */
    BalanceSheetResponse generateBalanceSheet(LocalDate asOfDate);
}
