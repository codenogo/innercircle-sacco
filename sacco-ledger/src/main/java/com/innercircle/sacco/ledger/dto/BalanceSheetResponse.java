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
public class BalanceSheetResponse {

    private LocalDate asOfDate;
    private List<AccountLineItem> assets;
    private List<AccountLineItem> liabilities;
    private List<AccountLineItem> equity;
    private String totalAssets;
    private String totalLiabilities;
    private String totalEquity;
    private String totalLiabilitiesAndEquity;
    private boolean balanced;

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
