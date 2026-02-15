package com.innercircle.sacco.reporting.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberStatementResponse;
import com.innercircle.sacco.reporting.security.ReportingAuthHelper;
import com.innercircle.sacco.reporting.service.FinancialReportService;
import com.innercircle.sacco.reporting.service.MemberStatementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private MemberStatementService memberStatementService;

    @Mock
    private FinancialReportService financialReportService;

    @Mock
    private ReportingAuthHelper authHelper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReportController reportController;

    // ===================== memberStatement tests =====================

    @Test
    void memberStatement_withValidDateRange_shouldReturnStatement() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        MemberStatementResponse statement = new MemberStatementResponse(
                memberId, "John Doe", from, to,
                BigDecimal.ZERO, new BigDecimal("50000.00"),
                new BigDecimal("50000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyList());
        when(memberStatementService.generateStatement(memberId, from, to)).thenReturn(statement);

        ResponseEntity<ApiResponse<MemberStatementResponse>> response =
                reportController.memberStatement(memberId, from, to, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().memberId()).isEqualTo(memberId);
        assertThat(response.getBody().getData().memberName()).isEqualTo("John Doe");
        assertThat(response.getBody().getData().closingBalance())
                .isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    void memberStatement_shouldCallAuthHelperAndValidateDate() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 6, 30);

        MemberStatementResponse statement = new MemberStatementResponse(
                memberId, "Test User", from, to,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyList());
        when(memberStatementService.generateStatement(memberId, from, to)).thenReturn(statement);

        reportController.memberStatement(memberId, from, to, authentication);

        verify(authHelper).assertAccessToMember(eq(memberId), eq(authentication));
        verify(memberStatementService).generateStatement(memberId, from, to);
    }

    @Test
    void memberStatement_withFromAfterTo_shouldThrowBusinessException() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 12, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() -> reportController.memberStatement(memberId, from, to, authentication))
                .isInstanceOf(BusinessException.class)
                .hasMessage("fromDate must be before toDate");
    }

    @Test
    void memberStatement_withSameDates_shouldNotThrow() {
        UUID memberId = UUID.randomUUID();
        LocalDate sameDate = LocalDate.of(2025, 6, 15);

        MemberStatementResponse statement = new MemberStatementResponse(
                memberId, "Same Date User", sameDate, sameDate,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyList());
        when(memberStatementService.generateStatement(memberId, sameDate, sameDate)).thenReturn(statement);

        ResponseEntity<ApiResponse<MemberStatementResponse>> response =
                reportController.memberStatement(memberId, sameDate, sameDate, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===================== financialSummary tests =====================

    @Test
    void financialSummary_withValidDateRange_shouldReturnSummary() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        FinancialSummaryResponse summary = new FinancialSummaryResponse(
                from, to,
                new BigDecimal("500000.00"), new BigDecimal("300000.00"),
                new BigDecimal("200000.00"), new BigDecimal("100000.00"),
                new BigDecimal("10000.00"), new BigDecimal("310000.00"),
                50L, 25L, new BigDecimal("150000.00"));
        when(financialReportService.overallSummary(from, to)).thenReturn(summary);

        ResponseEntity<ApiResponse<FinancialSummaryResponse>> response =
                reportController.financialSummary(from, to);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().totalContributions())
                .isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(response.getBody().getData().activeMemberCount()).isEqualTo(50L);
    }

    @Test
    void financialSummary_withFromAfterTo_shouldThrowBusinessException() {
        LocalDate from = LocalDate.of(2025, 12, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() -> reportController.financialSummary(from, to))
                .isInstanceOf(BusinessException.class)
                .hasMessage("fromDate must be before toDate");
    }

    @Test
    void financialSummary_withSameDates_shouldNotThrow() {
        LocalDate sameDate = LocalDate.of(2025, 6, 15);

        FinancialSummaryResponse summary = new FinancialSummaryResponse(
                sameDate, sameDate,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, BigDecimal.ZERO);
        when(financialReportService.overallSummary(sameDate, sameDate)).thenReturn(summary);

        ResponseEntity<ApiResponse<FinancialSummaryResponse>> response =
                reportController.financialSummary(sameDate, sameDate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void financialSummary_withZeroValues_shouldReturnSummary() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        FinancialSummaryResponse summary = new FinancialSummaryResponse(
                from, to,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, BigDecimal.ZERO);
        when(financialReportService.overallSummary(from, to)).thenReturn(summary);

        ResponseEntity<ApiResponse<FinancialSummaryResponse>> response =
                reportController.financialSummary(from, to);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().netPosition())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
