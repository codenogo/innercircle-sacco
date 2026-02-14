package com.innercircle.sacco.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private List<AccountLineItem> revenue;
    private List<AccountLineItem> expenses;
    private String totalRevenue;
    private String totalExpenses;
    private String netIncome;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountLineItem {
        private String accountCode;
        private String accountName;
        private String amount;
    }
}
