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
public class TrialBalanceResponse {

    private LocalDate asOfDate;
    private List<AccountBalanceDto> accounts;
    private String totalDebits;
    private String totalCredits;
    private boolean balanced;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountBalanceDto {
        private String accountCode;
        private String accountName;
        private String accountType;
        private String debitBalance;
        private String creditBalance;
    }
}
