package com.innercircle.sacco.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;

@Getter
@Builder
@AllArgsConstructor
public class MonthlyInterestSummary {

    private YearMonth month;
    private BigDecimal totalInterestAccrued;
    private BigDecimal totalInterestReceived;
    private BigDecimal totalInterestArrears;
    private int activeLoansCount;
    private int loansWithArrearsCount;
}
