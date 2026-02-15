package com.innercircle.sacco.reporting.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.reporting.dto.AdminDashboardResponse;
import com.innercircle.sacco.reporting.dto.MemberDashboardResponse;
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
}
