package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.AdminDashboardResponse;
import com.innercircle.sacco.reporting.dto.DashboardAnalyticsResponse;
import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberDashboardResponse;
import com.innercircle.sacco.reporting.dto.SaccoStateResponse;
import com.innercircle.sacco.reporting.dto.TreasurerDashboardResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface FinancialReportService {

    FinancialSummaryResponse overallSummary(LocalDate fromDate, LocalDate toDate);

    MemberDashboardResponse memberDashboard(UUID memberId);

    TreasurerDashboardResponse treasurerDashboard();

    AdminDashboardResponse adminDashboard();

    DashboardAnalyticsResponse getDashboardAnalytics(int year);

    SaccoStateResponse getSaccoState();
}
