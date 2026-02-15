package com.innercircle.sacco.reporting.controller;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberStatementResponse;
import com.innercircle.sacco.reporting.security.ReportingAuthHelper;
import com.innercircle.sacco.reporting.service.ExportService;
import com.innercircle.sacco.reporting.service.FinancialReportService;
import com.innercircle.sacco.reporting.service.MemberStatementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/export")
public class ExportController {

    private final MemberStatementService memberStatementService;
    private final FinancialReportService financialReportService;
    private final ExportService exportService;
    private final ReportingAuthHelper authHelper;

    public ExportController(MemberStatementService memberStatementService,
                            FinancialReportService financialReportService,
                            ExportService exportService,
                            ReportingAuthHelper authHelper) {
        this.memberStatementService = memberStatementService;
        this.financialReportService = financialReportService;
        this.exportService = exportService;
        this.authHelper = authHelper;
    }

    @GetMapping("/member-statement/{memberId}/pdf")
    public ResponseEntity<byte[]> memberStatementPdf(
            @PathVariable UUID memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        authHelper.assertAccessToMember(memberId, authentication);
        validateDateRange(fromDate, toDate);
        MemberStatementResponse statement = memberStatementService.generateStatement(memberId, fromDate, toDate);
        byte[] pdf = exportService.memberStatementToPdf(statement);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement-" + memberId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/member-statement/{memberId}/csv")
    public ResponseEntity<byte[]> memberStatementCsv(
            @PathVariable UUID memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        authHelper.assertAccessToMember(memberId, authentication);
        validateDateRange(fromDate, toDate);
        MemberStatementResponse statement = memberStatementService.generateStatement(memberId, fromDate, toDate);
        byte[] csv = exportService.memberStatementToCsv(statement);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement-" + memberId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/financial-summary/csv")
    public ResponseEntity<byte[]> financialSummaryCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        FinancialSummaryResponse summary = financialReportService.overallSummary(fromDate, toDate);
        byte[] csv = exportService.financialSummaryToCsv(summary);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financial-summary.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate must be before toDate");
        }
    }
}
