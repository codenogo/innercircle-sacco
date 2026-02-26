package com.innercircle.sacco.reporting.dto;

import java.math.BigDecimal;

public record TreasurerDashboardResponse(
        BigDecimal totalCollectionsThisMonth,
        BigDecimal totalNetContributionsThisMonth,
        BigDecimal totalWelfareThisMonth,
        BigDecimal totalMeetingFinesThisMonth,
        BigDecimal totalWelfareClaimsThisMonth,
        BigDecimal totalExitSettlementsThisMonth,
        BigDecimal totalDisbursementsThisMonth,
        int pendingApprovals,
        int overdueLoans,
        BigDecimal cashPosition,
        long activeMemberCount,
        BigDecimal totalShareCapital
) {}
