package com.innercircle.sacco.reporting.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberStatementResponse;
import com.innercircle.sacco.reporting.security.ReportingAuthHelper;
import com.innercircle.sacco.reporting.service.FinancialReportService;
import com.innercircle.sacco.reporting.service.MemberStatementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final MemberStatementService memberStatementService;
    private final FinancialReportService financialReportService;
    private final ReportingAuthHelper authHelper;

    public ReportController(MemberStatementService memberStatementService,
                            FinancialReportService financialReportService,
                            ReportingAuthHelper authHelper) {
        this.memberStatementService = memberStatementService;
        this.financialReportService = financialReportService;
        this.authHelper = authHelper;
    }

    @GetMapping("/member-statement/{memberId}")
    public ResponseEntity<ApiResponse<MemberStatementResponse>> memberStatement(
            @PathVariable UUID memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        authHelper.assertAccessToMember(memberId, authentication);
        validateDateRange(fromDate, toDate);
        MemberStatementResponse statement = memberStatementService.generateStatement(memberId, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.ok(statement));
    }

    @GetMapping("/financial-summary")
    public ResponseEntity<ApiResponse<FinancialSummaryResponse>> financialSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        FinancialSummaryResponse summary = financialReportService.overallSummary(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate must be before toDate");
        }
    }
}
