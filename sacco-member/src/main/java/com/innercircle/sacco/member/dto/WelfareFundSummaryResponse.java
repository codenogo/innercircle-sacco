package com.innercircle.sacco.member.dto;

import java.math.BigDecimal;

public record WelfareFundSummaryResponse(
        BigDecimal totalWelfareContributions,
        BigDecimal totalWelfarePayouts,
        BigDecimal availableBalance,
        long pendingClaims
) {}
