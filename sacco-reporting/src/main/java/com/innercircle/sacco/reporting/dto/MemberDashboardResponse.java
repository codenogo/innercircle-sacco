package com.innercircle.sacco.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

public record MemberDashboardResponse(
        BigDecimal shareBalance,
        BigDecimal totalContributions,
        int activeLoans,
        BigDecimal outstandingLoanBalance,
        List<MemberStatementEntry> recentTransactions
) {}
