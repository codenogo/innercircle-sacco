package com.innercircle.sacco.reporting.dto;

import java.math.BigDecimal;

public record AdminDashboardResponse(
        long totalMembers,
        long activeMembers,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        int totalLoanProducts,
        int totalActiveLoans,
        BigDecimal totalOutstandingLoans,
        int recentAuditEventsCount
) {}
