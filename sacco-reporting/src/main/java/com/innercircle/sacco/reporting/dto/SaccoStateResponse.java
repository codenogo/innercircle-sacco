package com.innercircle.sacco.reporting.dto;

import java.math.BigDecimal;

public record SaccoStateResponse(
        int totalMembers,
        int activeMembers,
        BigDecimal totalShareCapital,
        BigDecimal totalOutstandingLoans,
        BigDecimal totalContributions,
        BigDecimal totalNetContributions,
        BigDecimal totalWelfareContributions,
        BigDecimal totalMeetingFines,
        BigDecimal totalWelfareBenefitsPaid,
        BigDecimal totalExitSettlements,
        BigDecimal totalExitFees,
        BigDecimal totalPayouts,
        BigDecimal loanRecoveryRate,
        BigDecimal memberGrowthRate
) {}
