package com.innercircle.sacco.reporting.dto;

import java.util.List;

public record DashboardAnalyticsResponse(
        int year,
        List<MonthlyDataPoint> loansDisbursed,
        List<MonthlyDataPoint> amountRepaid,
        List<MonthlyDataPoint> interestAccrued,
        List<MonthlyDataPoint> contributionsReceived,
        List<MonthlyDataPoint> payoutsProcessed
) {}
