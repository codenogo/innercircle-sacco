package com.innercircle.sacco.loan.dto;

import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LoanResponse {

    private UUID id;
    private UUID memberId;
    private UUID loanProductId;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private InterestMethod interestMethod;
    private LoanStatus status;
    private String purpose;
    private UUID approvedBy;
    private Instant approvedAt;
    private Instant disbursedAt;
    private BigDecimal totalRepaid;
    private BigDecimal outstandingBalance;
    private BigDecimal totalInterestAccrued;
    private BigDecimal totalInterestPaid;
    private BigDecimal totalPenalties;
    private Instant createdAt;
    private Instant updatedAt;

    public static LoanResponse from(LoanApplication loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .memberId(loan.getMemberId())
                .loanProductId(loan.getLoanProductId())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .termMonths(loan.getTermMonths())
                .interestMethod(loan.getInterestMethod())
                .status(loan.getStatus())
                .purpose(loan.getPurpose())
                .approvedBy(loan.getApprovedBy())
                .approvedAt(loan.getApprovedAt())
                .disbursedAt(loan.getDisbursedAt())
                .totalRepaid(loan.getTotalRepaid())
                .outstandingBalance(loan.getOutstandingBalance())
                .totalInterestAccrued(loan.getTotalInterestAccrued())
                .totalInterestPaid(loan.getTotalInterestPaid())
                .totalPenalties(loan.getTotalPenalties())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }
}
