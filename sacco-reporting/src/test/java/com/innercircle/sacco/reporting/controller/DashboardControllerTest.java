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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private FinancialReportService financialReportService;

    @Mock
    private ReportingAuthHelper authHelper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DashboardController dashboardController;

    // ===================== memberDashboard tests =====================

    @Test
    void memberDashboard_shouldReturnDashboard() {
        UUID memberId = UUID.randomUUID();
        MemberDashboardResponse dashboard = new MemberDashboardResponse(
                new BigDecimal("50000.00"),
                new BigDecimal("120000.00"),
                2,
                new BigDecimal("30000.00"),
                Collections.emptyList());
        when(financialReportService.memberDashboard(memberId)).thenReturn(dashboard);

        ResponseEntity<ApiResponse<MemberDashboardResponse>> response =
                dashboardController.memberDashboard(memberId, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().shareBalance())
                .isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(response.getBody().getData().activeLoans()).isEqualTo(2);
        verify(authHelper).assertAccessToMember(eq(memberId), eq(authentication));
    }

    @Test
    void memberDashboard_shouldCallAuthHelperBeforeService() {
        UUID memberId = UUID.randomUUID();
        MemberDashboardResponse dashboard = new MemberDashboardResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, Collections.emptyList());
        when(financialReportService.memberDashboard(memberId)).thenReturn(dashboard);

        dashboardController.memberDashboard(memberId, authentication);

        verify(authHelper).assertAccessToMember(memberId, authentication);
        verify(financialReportService).memberDashboard(memberId);
    }

    // ===================== treasurerDashboard tests =====================

    @Test
    void treasurerDashboard_shouldReturnDashboard() {
        TreasurerDashboardResponse dashboard = new TreasurerDashboardResponse(
                new BigDecimal("250000.00"),
                new BigDecimal("100000.00"),
                5, 3,
                new BigDecimal("150000.00"),
                42L,
                new BigDecimal("500000.00"));
        when(financialReportService.treasurerDashboard()).thenReturn(dashboard);

        ResponseEntity<ApiResponse<TreasurerDashboardResponse>> response =
                dashboardController.treasurerDashboard();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().pendingApprovals()).isEqualTo(5);
        assertThat(response.getBody().getData().overdueLoans()).isEqualTo(3);
        assertThat(response.getBody().getData().activeMemberCount()).isEqualTo(42L);
    }

    // ===================== adminDashboard tests =====================

    @Test
    void adminDashboard_shouldReturnDashboard() {
        AdminDashboardResponse dashboard = new AdminDashboardResponse(
                100L, 85L,
                new BigDecimal("1000000.00"),
                new BigDecimal("500000.00"),
                5, 30,
                new BigDecimal("750000.00"),
                20);
        when(financialReportService.adminDashboard()).thenReturn(dashboard);

        ResponseEntity<ApiResponse<AdminDashboardResponse>> response =
                dashboardController.adminDashboard();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().totalMembers()).isEqualTo(100L);
        assertThat(response.getBody().getData().activeMembers()).isEqualTo(85L);
        assertThat(response.getBody().getData().totalLoanProducts()).isEqualTo(5);
    }

    // ===================== analytics tests =====================

    @Test
    void analytics_withSpecifiedYear_shouldReturnAnalytics() {
        DashboardAnalyticsResponse analytics = createAnalytics(2025);
        when(financialReportService.getDashboardAnalytics(2025)).thenReturn(analytics);

        ResponseEntity<ApiResponse<DashboardAnalyticsResponse>> response =
                dashboardController.analytics(2025);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().year()).isEqualTo(2025);
    }

    @Test
    void analytics_withNullYear_shouldDefaultToCurrentYear() {
        int currentYear = java.time.LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = createAnalytics(currentYear);
        when(financialReportService.getDashboardAnalytics(currentYear)).thenReturn(analytics);

        ResponseEntity<ApiResponse<DashboardAnalyticsResponse>> response =
                dashboardController.analytics(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(financialReportService).getDashboardAnalytics(currentYear);
    }

    // ===================== analytics sub-endpoints tests =====================

    @Test
    void loansDisbursed_shouldReturnLoanData() {
        List<MonthlyDataPoint> loanData = List.of(
                new MonthlyDataPoint(1, "January", new BigDecimal("50000")),
                new MonthlyDataPoint(2, "February", new BigDecimal("75000")));
        DashboardAnalyticsResponse analytics = new DashboardAnalyticsResponse(
                2025, loanData, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());
        when(financialReportService.getDashboardAnalytics(2025)).thenReturn(analytics);

        ResponseEntity<ApiResponse<List<MonthlyDataPoint>>> response =
                dashboardController.loansDisbursed(2025);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(2);
        assertThat(response.getBody().getData().get(0).month()).isEqualTo(1);
        assertThat(response.getBody().getData().get(0).amount())
                .isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void loansDisbursed_withNullYear_shouldDefaultToCurrentYear() {
        int currentYear = java.time.LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = createAnalytics(currentYear);
        when(financialReportService.getDashboardAnalytics(currentYear)).thenReturn(analytics);

        dashboardController.loansDisbursed(null);

        verify(financialReportService).getDashboardAnalytics(currentYear);
    }

    @Test
    void repayments_shouldReturnRepaymentData() {
        List<MonthlyDataPoint> repaymentData = List.of(
                new MonthlyDataPoint(1, "January", new BigDecimal("30000")));
        DashboardAnalyticsResponse analytics = new DashboardAnalyticsResponse(
                2025, Collections.emptyList(), repaymentData, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());
        when(financialReportService.getDashboardAnalytics(2025)).thenReturn(analytics);

        ResponseEntity<ApiResponse<List<MonthlyDataPoint>>> response =
                dashboardController.repayments(2025);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).amount())
                .isEqualByComparingTo(new BigDecimal("30000"));
    }

    @Test
    void repayments_withNullYear_shouldDefaultToCurrentYear() {
        int currentYear = java.time.LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = createAnalytics(currentYear);
        when(financialReportService.getDashboardAnalytics(currentYear)).thenReturn(analytics);

        dashboardController.repayments(null);

        verify(financialReportService).getDashboardAnalytics(currentYear);
    }

    @Test
    void interest_shouldReturnInterestData() {
        List<MonthlyDataPoint> interestData = List.of(
                new MonthlyDataPoint(3, "March", new BigDecimal("12000")));
        DashboardAnalyticsResponse analytics = new DashboardAnalyticsResponse(
                2025, Collections.emptyList(), Collections.emptyList(), interestData,
                Collections.emptyList(), Collections.emptyList());
        when(financialReportService.getDashboardAnalytics(2025)).thenReturn(analytics);

        ResponseEntity<ApiResponse<List<MonthlyDataPoint>>> response =
                dashboardController.interest(2025);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).monthName()).isEqualTo("March");
    }

    @Test
    void interest_withNullYear_shouldDefaultToCurrentYear() {
        int currentYear = java.time.LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = createAnalytics(currentYear);
        when(financialReportService.getDashboardAnalytics(currentYear)).thenReturn(analytics);

        dashboardController.interest(null);

        verify(financialReportService).getDashboardAnalytics(currentYear);
    }

    @Test
    void contributions_shouldReturnContributionData() {
        List<MonthlyDataPoint> contributionData = List.of(
                new MonthlyDataPoint(1, "January", new BigDecimal("100000")),
                new MonthlyDataPoint(2, "February", new BigDecimal("110000")),
                new MonthlyDataPoint(3, "March", new BigDecimal("105000")));
        DashboardAnalyticsResponse analytics = new DashboardAnalyticsResponse(
                2025, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), contributionData, Collections.emptyList());
        when(financialReportService.getDashboardAnalytics(2025)).thenReturn(analytics);

        ResponseEntity<ApiResponse<List<MonthlyDataPoint>>> response =
                dashboardController.contributions(2025);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(3);
    }

    @Test
    void contributions_withNullYear_shouldDefaultToCurrentYear() {
        int currentYear = java.time.LocalDate.now().getYear();
        DashboardAnalyticsResponse analytics = createAnalytics(currentYear);
        when(financialReportService.getDashboardAnalytics(currentYear)).thenReturn(analytics);

        dashboardController.contributions(null);

        verify(financialReportService).getDashboardAnalytics(currentYear);
    }

    // ===================== saccoState tests =====================

    @Test
    void saccoState_shouldReturnState() {
        SaccoStateResponse state = new SaccoStateResponse(
                100, 85,
                new BigDecimal("500000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("600000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("85.50"),
                new BigDecimal("12.30"));
        when(financialReportService.getSaccoState()).thenReturn(state);

        ResponseEntity<ApiResponse<SaccoStateResponse>> response =
                dashboardController.saccoState();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().totalMembers()).isEqualTo(100);
        assertThat(response.getBody().getData().activeMembers()).isEqualTo(85);
        assertThat(response.getBody().getData().loanRecoveryRate())
                .isEqualByComparingTo(new BigDecimal("85.50"));
    }

    @Test
    void saccoState_withZeroMembers_shouldReturnState() {
        SaccoStateResponse state = new SaccoStateResponse(
                0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        when(financialReportService.getSaccoState()).thenReturn(state);

        ResponseEntity<ApiResponse<SaccoStateResponse>> response =
                dashboardController.saccoState();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().totalMembers()).isEqualTo(0);
    }

    // ===================== helper methods =====================

    private DashboardAnalyticsResponse createAnalytics(int year) {
        return new DashboardAnalyticsResponse(
                year,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }
}
