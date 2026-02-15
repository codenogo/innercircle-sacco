package com.innercircle.sacco.reporting.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.reporting.dto.AdminDashboardResponse;
import com.innercircle.sacco.reporting.dto.DashboardAnalyticsResponse;
import com.innercircle.sacco.reporting.dto.MemberDashboardResponse;
import com.innercircle.sacco.reporting.dto.MonthlyDataPoint;
import com.innercircle.sacco.reporting.dto.SaccoStateResponse;
import com.innercircle.sacco.reporting.dto.TreasurerDashboardResponse;
import com.innercircle.sacco.reporting.security.ReportingAuthHelper;
import com.innercircle.sacco.reporting.service.FinancialReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final FinancialReportService financialReportService;
    private final ReportingAuthHelper authHelper;

    public DashboardController(FinancialReportService financialReportService,
                               ReportingAuthHelper authHelper) {
        this.financialReportService = financialReportService;
        this.authHelper = authHelper;
    }

    @GetMapping("/member")
    public ResponseEntity<ApiResponse<MemberDashboardResponse>> memberDashboard(
            @RequestParam UUID memberId,
            Authentication authentication) {
        authHelper.assertAccessToMember(memberId, authentication);
        MemberDashboardResponse dashboard = financialReportService.memberDashboard(memberId);
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }

    @GetMapping("/treasurer")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<TreasurerDashboardResponse>> treasurerDashboard() {
        TreasurerDashboardResponse dashboard = financialReportService.treasurerDashboard();
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> adminDashboard() {
        AdminDashboardResponse dashboard = financialReportService.adminDashboard();
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<DashboardAnalyticsResponse>> analytics(
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = financialReportService.getDashboardAnalytics(targetYear);
        return ResponseEntity.ok(ApiResponse.ok(analytics));
    }

    @GetMapping("/analytics/loans")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<List<MonthlyDataPoint>>> loansDisbursed(
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = financialReportService.getDashboardAnalytics(targetYear);
        return ResponseEntity.ok(ApiResponse.ok(analytics.loansDisbursed()));
    }

    @GetMapping("/analytics/repayments")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<List<MonthlyDataPoint>>> repayments(
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = financialReportService.getDashboardAnalytics(targetYear);
        return ResponseEntity.ok(ApiResponse.ok(analytics.amountRepaid()));
    }

    @GetMapping("/analytics/interest")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<List<MonthlyDataPoint>>> interest(
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = financialReportService.getDashboardAnalytics(targetYear);
        return ResponseEntity.ok(ApiResponse.ok(analytics.interestAccrued()));
    }

    @GetMapping("/analytics/contributions")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<List<MonthlyDataPoint>>> contributions(
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = financialReportService.getDashboardAnalytics(targetYear);
        return ResponseEntity.ok(ApiResponse.ok(analytics.contributionsReceived()));
    }

    @GetMapping("/state")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<SaccoStateResponse>> saccoState() {
        SaccoStateResponse state = financialReportService.getSaccoState();
        return ResponseEntity.ok(ApiResponse.ok(state));
    }
}
