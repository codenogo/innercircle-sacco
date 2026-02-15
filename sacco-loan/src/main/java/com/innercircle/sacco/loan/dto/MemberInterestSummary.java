package com.innercircle.sacco.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class MemberInterestSummary {

    private UUID memberId;
    private UUID loanId;
    private BigDecimal totalInterestAccrued;
    private BigDecimal totalInterestPaid;
    private BigDecimal interestArrears;
    private LocalDate lastAccrualDate;
}
