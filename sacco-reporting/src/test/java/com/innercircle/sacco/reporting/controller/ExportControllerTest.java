package com.innercircle.sacco.reporting.controller;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberStatementResponse;
import com.innercircle.sacco.reporting.security.ReportingAuthHelper;
import com.innercircle.sacco.reporting.service.ExportService;
import com.innercircle.sacco.reporting.service.FinancialReportService;
import com.innercircle.sacco.reporting.service.MemberStatementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private MemberStatementService memberStatementService;

    @Mock
    private FinancialReportService financialReportService;

    @Mock
    private ExportService exportService;

    @Mock
    private ReportingAuthHelper authHelper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExportController exportController;

    // ===================== memberStatementPdf tests =====================

    @Test
    void memberStatementPdf_withValidInput_shouldReturnPdf() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        MemberStatementResponse statement = createStatement(memberId, from, to);
        byte[] pdfBytes = "%PDF-fake-content".getBytes(StandardCharsets.UTF_8);

        when(memberStatementService.generateStatement(memberId, from, to)).thenReturn(statement);
        when(exportService.memberStatementToPdf(statement)).thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = exportController.memberStatementPdf(
                memberId, from, to, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pdfBytes);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=statement-" + memberId + ".pdf");
    }

    @Test
    void memberStatementPdf_shouldCallAuthHelper() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 6, 30);

        MemberStatementResponse statement = createStatement(memberId, from, to);
        when(memberStatementService.generateStatement(memberId, from, to)).thenReturn(statement);
        when(exportService.memberStatementToPdf(any())).thenReturn(new byte[0]);

        exportController.memberStatementPdf(memberId, from, to, authentication);

        verify(authHelper).assertAccessToMember(eq(memberId), eq(authentication));
    }

    @Test
    void memberStatementPdf_withFromAfterTo_shouldThrowBusinessException() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 12, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() -> exportController.memberStatementPdf(
                memberId, from, to, authentication))
                .isInstanceOf(BusinessException.class)
                .hasMessage("fromDate must be before toDate");
    }

    @Test
    void memberStatementPdf_shouldCallGenerateStatementThenExport() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to = LocalDate.of(2025, 3, 31);

        MemberStatementResponse statement = createStatement(memberId, from, to);
        when(memberStatementService.generateStatement(memberId, from, to)).thenReturn(statement);
        when(exportService.memberStatementToPdf(statement)).thenReturn(new byte[]{1, 2, 3});

        exportController.memberStatementPdf(memberId, from, to, authentication);

        verify(memberStatementService).generateStatement(memberId, from, to);
        verify(exportService).memberStatementToPdf(statement);
    }

    // ===================== memberStatementCsv tests =====================

    @Test
    void memberStatementCsv_withValidInput_shouldReturnCsv() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        MemberStatementResponse statement = createStatement(memberId, from, to);
        byte[] csvBytes = "Date,Type,Description\n2025-01-15,CONTRIBUTION,Monthly"
                .getBytes(StandardCharsets.UTF_8);

        when(memberStatementService.generateStatement(memberId, from, to)).thenReturn(statement);
        when(exportService.memberStatementToCsv(statement)).thenReturn(csvBytes);

        ResponseEntity<byte[]> response = exportController.memberStatementCsv(
                memberId, from, to, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(csvBytes);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv"));
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=statement-" + memberId + ".csv");
    }

    @Test
    void memberStatementCsv_shouldCallAuthHelper() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 6, 30);

        MemberStatementResponse statement = createStatement(memberId, from, to);
        when(memberStatementService.generateStatement(memberId, from, to)).thenReturn(statement);
        when(exportService.memberStatementToCsv(any())).thenReturn(new byte[0]);

        exportController.memberStatementCsv(memberId, from, to, authentication);

        verify(authHelper).assertAccessToMember(eq(memberId), eq(authentication));
    }

    @Test
    void memberStatementCsv_withFromAfterTo_shouldThrowBusinessException() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 12, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() -> exportController.memberStatementCsv(
                memberId, from, to, authentication))
                .isInstanceOf(BusinessException.class)
                .hasMessage("fromDate must be before toDate");
    }

    @Test
    void memberStatementCsv_shouldCallGenerateStatementThenExport() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 4, 1);
        LocalDate to = LocalDate.of(2025, 4, 30);

        MemberStatementResponse statement = createStatement(memberId, from, to);
        when(memberStatementService.generateStatement(memberId, from, to)).thenReturn(statement);
        when(exportService.memberStatementToCsv(statement)).thenReturn(new byte[]{1});

        exportController.memberStatementCsv(memberId, from, to, authentication);

        verify(memberStatementService).generateStatement(memberId, from, to);
        verify(exportService).memberStatementToCsv(statement);
    }

    // ===================== financialSummaryCsv tests =====================

    @Test
    void financialSummaryCsv_withValidDateRange_shouldReturnCsv() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        FinancialSummaryResponse summary = createSummary(from, to);
        byte[] csvBytes = "Metric,Amount\nTotal Contributions,500000.00"
                .getBytes(StandardCharsets.UTF_8);

        when(financialReportService.overallSummary(from, to)).thenReturn(summary);
        when(exportService.financialSummaryToCsv(summary)).thenReturn(csvBytes);

        ResponseEntity<byte[]> response = exportController.financialSummaryCsv(from, to);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(csvBytes);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv"));
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=financial-summary.csv");
    }

    @Test
    void financialSummaryCsv_withFromAfterTo_shouldThrowBusinessException() {
        LocalDate from = LocalDate.of(2025, 12, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() -> exportController.financialSummaryCsv(from, to))
                .isInstanceOf(BusinessException.class)
                .hasMessage("fromDate must be before toDate");
    }

    @Test
    void financialSummaryCsv_shouldCallOverallSummaryThenExport() {
        LocalDate from = LocalDate.of(2025, 6, 1);
        LocalDate to = LocalDate.of(2025, 6, 30);

        FinancialSummaryResponse summary = createSummary(from, to);
        when(financialReportService.overallSummary(from, to)).thenReturn(summary);
        when(exportService.financialSummaryToCsv(summary)).thenReturn(new byte[]{1});

        exportController.financialSummaryCsv(from, to);

        verify(financialReportService).overallSummary(from, to);
        verify(exportService).financialSummaryToCsv(summary);
    }

    @Test
    void financialSummaryCsv_withSameDates_shouldNotThrow() {
        LocalDate sameDate = LocalDate.of(2025, 7, 15);

        FinancialSummaryResponse summary = createSummary(sameDate, sameDate);
        when(financialReportService.overallSummary(sameDate, sameDate)).thenReturn(summary);
        when(exportService.financialSummaryToCsv(summary)).thenReturn(new byte[0]);

        ResponseEntity<byte[]> response = exportController.financialSummaryCsv(sameDate, sameDate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===================== helper methods =====================

    private MemberStatementResponse createStatement(UUID memberId, LocalDate from, LocalDate to) {
        return new MemberStatementResponse(
                memberId, "Test User", from, to,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyList());
    }

    private FinancialSummaryResponse createSummary(LocalDate from, LocalDate to) {
        return new FinancialSummaryResponse(
                from, to,
                new BigDecimal("500000.00"), new BigDecimal("300000.00"),
                new BigDecimal("200000.00"), new BigDecimal("100000.00"),
                new BigDecimal("10000.00"), new BigDecimal("310000.00"),
                50L, 25L, new BigDecimal("150000.00"));
    }
}
