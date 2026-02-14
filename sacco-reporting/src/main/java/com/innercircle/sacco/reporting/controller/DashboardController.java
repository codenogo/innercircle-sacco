package com.innercircle.sacco.reporting.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.reporting.dto.AdminDashboardResponse;
import com.innercircle.sacco.reporting.dto.MemberDashboardResponse;
import com.innercircle.sacco.reporting.dto.TreasurerDashboardResponse;
import com.innercircle.sacco.reporting.service.FinancialReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final FinancialReportService financialReportService;

    public DashboardController(FinancialReportService financialReportService) {
        this.financialReportService = financialReportService;
    }

    @GetMapping("/member")
    public ResponseEntity<ApiResponse<MemberDashboardResponse>> memberDashboard(
            @RequestParam UUID memberId) {
        MemberDashboardResponse dashboard = financialReportService.memberDashboard(memberId);
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }

    @GetMapping("/treasurer")
    public ResponseEntity<ApiResponse<TreasurerDashboardResponse>> treasurerDashboard() {
        TreasurerDashboardResponse dashboard = financialReportService.treasurerDashboard();
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> adminDashboard() {
        AdminDashboardResponse dashboard = financialReportService.adminDashboard();
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }
}
