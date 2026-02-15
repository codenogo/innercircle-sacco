package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.AdminDashboardResponse;
import com.innercircle.sacco.reporting.dto.DashboardAnalyticsResponse;
import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberDashboardResponse;
import com.innercircle.sacco.reporting.dto.SaccoStateResponse;
import com.innercircle.sacco.reporting.dto.TreasurerDashboardResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinancialReportServiceImplTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private FinancialReportServiceImpl financialReportService;

    // ===================== overallSummary tests =====================

    @Test
    void overallSummary_shouldReturnFinancialSummary() {
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2025, 12, 31);

        // All querySum and queryCount go through the 3-arg overload (varargs becomes Object[])
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("100000.00"));
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(25L);

        FinancialSummaryResponse result = financialReportService.overallSummary(fromDate, toDate);

        assertThat(result).isNotNull();
        assertThat(result.fromDate()).isEqualTo(fromDate);
        assertThat(result.toDate()).isEqualTo(toDate);
        assertThat(result.totalContributions()).isNotNull();
        assertThat(result.activeMemberCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void overallSummary_withNullReturns_shouldDefaultToZero() {
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2025, 6, 30);

        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(BigDecimal.ZERO);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);

        FinancialSummaryResponse result = financialReportService.overallSummary(fromDate, toDate);

        assertThat(result.totalContributions()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalLoansDisbursed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalRepayments()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalPayouts()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalPenaltiesCollected()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.activeMemberCount()).isEqualTo(0);
    }

    @Test
    void overallSummary_netPositionShouldBeCalculatedCorrectly() {
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2025, 12, 31);

        // contributions=100, repayments=50, penalties=10, loansDisbursed=80, payouts=20
        // net = 100+50+10 - 80 - 20 = 60
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("100"))   // totalContributions
                .thenReturn(new BigDecimal("80"))    // totalLoansDisbursed
                .thenReturn(new BigDecimal("50"))    // totalRepayments
                .thenReturn(new BigDecimal("20"))    // totalPayouts
                .thenReturn(new BigDecimal("10"))    // totalPenalties
                .thenReturn(new BigDecimal("30"));   // outstandingLoanBalance
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(10L);  // activeMemberCount and activeLoansCount

        FinancialSummaryResponse result = financialReportService.overallSummary(fromDate, toDate);

        assertThat(result.netPosition()).isEqualByComparingTo(new BigDecimal("60"));
    }

    // ===================== memberDashboard tests =====================

    @Test
    void memberDashboard_shouldReturnMemberData() {
        UUID memberId = UUID.randomUUID();

        // querySum calls: jdbc.queryForObject(sql, BigDecimal.class, args) where args is Object[]
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("50000.00"));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any()))
                .thenReturn(2);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(java.util.Collections.emptyList());

        MemberDashboardResponse result = financialReportService.memberDashboard(memberId);

        assertThat(result).isNotNull();
        assertThat(result.shareBalance()).isNotNull();
        assertThat(result.activeLoans()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void memberDashboard_withZeroBalance_shouldReturnZeros() {
        UUID memberId = UUID.randomUUID();

        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(BigDecimal.ZERO);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any()))
                .thenReturn(0);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(java.util.Collections.emptyList());

        MemberDashboardResponse result = financialReportService.memberDashboard(memberId);

        assertThat(result.shareBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.activeLoans()).isEqualTo(0);
        assertThat(result.outstandingLoanBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.recentTransactions()).isEmpty();
    }

    // ===================== treasurerDashboard tests =====================

    @Test
    void treasurerDashboard_shouldReturnDashboardData() {
        // querySum calls go through 3-arg overload
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("200000.00"));
        // Direct Integer calls are 2-arg overload
        when(jdbc.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(5);
        // queryCount calls go through 3-arg overload
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(50L);

        TreasurerDashboardResponse result = financialReportService.treasurerDashboard();

        assertThat(result).isNotNull();
        assertThat(result.totalCollectionsThisMonth()).isNotNull();
        assertThat(result.pendingApprovals()).isGreaterThanOrEqualTo(0);
        assertThat(result.activeMemberCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void treasurerDashboard_withZeroValues_shouldReturnZeros() {
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(BigDecimal.ZERO);
        when(jdbc.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(0);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);

        TreasurerDashboardResponse result = financialReportService.treasurerDashboard();

        assertThat(result.totalCollectionsThisMonth()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.pendingApprovals()).isEqualTo(0);
        assertThat(result.overdueLoans()).isEqualTo(0);
    }

    // ===================== adminDashboard tests =====================

    @Test
    void adminDashboard_shouldReturnDashboardData() {
        // queryCount calls go through 3-arg overload (varargs)
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(100L);
        // querySum calls go through 3-arg overload (varargs)
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("1000000.00"));
        // Direct Integer calls are 2-arg overload
        when(jdbc.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(10);

        AdminDashboardResponse result = financialReportService.adminDashboard();

        assertThat(result).isNotNull();
        assertThat(result.totalMembers()).isEqualTo(100);
        assertThat(result.activeMembers()).isEqualTo(100);
        assertThat(result.totalAssets()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(result.totalLoanProducts()).isEqualTo(10);
    }

    @Test
    void adminDashboard_withZeroValues_shouldReturnZeros() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(BigDecimal.ZERO);
        when(jdbc.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(0);

        AdminDashboardResponse result = financialReportService.adminDashboard();

        assertThat(result.totalMembers()).isEqualTo(0);
        assertThat(result.activeMembers()).isEqualTo(0);
        assertThat(result.totalLoanProducts()).isEqualTo(0);
        assertThat(result.totalActiveLoans()).isEqualTo(0);
        assertThat(result.recentAuditEventsCount()).isEqualTo(0);
    }

    // ===================== getDashboardAnalytics tests =====================

    @Test
    void getDashboardAnalytics_shouldReturnAnalyticsForYear() {
        // Mock all monthly queries (they use RowCallbackHandler internally)
        doNothing().when(jdbc).query(anyString(), any(RowCallbackHandler.class), any(Object.class));

        DashboardAnalyticsResponse result = financialReportService.getDashboardAnalytics(2025);

        assertThat(result).isNotNull();
        assertThat(result.year()).isEqualTo(2025);
        assertThat(result.loansDisbursed()).hasSize(12);
        assertThat(result.amountRepaid()).hasSize(12);
        assertThat(result.interestAccrued()).hasSize(12);
        assertThat(result.contributionsReceived()).hasSize(12);
        assertThat(result.payoutsProcessed()).hasSize(12);
    }

    @Test
    void getDashboardAnalytics_monthlyDataShouldCoverAllMonths() {
        doNothing().when(jdbc).query(anyString(), any(RowCallbackHandler.class), any(Object.class));

        DashboardAnalyticsResponse result = financialReportService.getDashboardAnalytics(2024);

        // Every monthly series should have 12 entries
        assertThat(result.loansDisbursed()).hasSize(12);
        assertThat(result.loansDisbursed().get(0).month()).isEqualTo(1);
        assertThat(result.loansDisbursed().get(0).monthName()).isEqualTo("JANUARY");
        assertThat(result.loansDisbursed().get(11).month()).isEqualTo(12);
        assertThat(result.loansDisbursed().get(11).monthName()).isEqualTo("DECEMBER");
    }

    @Test
    void getDashboardAnalytics_emptyData_shouldReturnZerosForAllMonths() {
        doNothing().when(jdbc).query(anyString(), any(RowCallbackHandler.class), any(Object.class));

        DashboardAnalyticsResponse result = financialReportService.getDashboardAnalytics(2023);

        for (int i = 0; i < 12; i++) {
            assertThat(result.loansDisbursed().get(i).amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.amountRepaid().get(i).amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.interestAccrued().get(i).amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.contributionsReceived().get(i).amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.payoutsProcessed().get(i).amount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ===================== getSaccoState tests =====================

    @Test
    void getSaccoState_shouldReturnState() {
        // queryCount calls jdbc.queryForObject(sql, Long.class, new Object[]{}) -- empty varargs
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(100L)   // totalMembers
                .thenReturn(80L)    // activeMembers
                .thenReturn(60L);   // membersOneYearAgo

        // querySum calls jdbc.queryForObject(sql, BigDecimal.class, new Object[]{}) -- empty varargs
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("500000"))   // totalShareCapital
                .thenReturn(new BigDecimal("200000"))   // totalOutstandingLoans
                .thenReturn(new BigDecimal("1000000"))  // totalContributions
                .thenReturn(new BigDecimal("300000"))   // totalPayouts
                .thenReturn(new BigDecimal("150000"))   // totalRepaid
                .thenReturn(new BigDecimal("200000"));  // totalDisbursed

        SaccoStateResponse result = financialReportService.getSaccoState();

        assertThat(result).isNotNull();
        assertThat(result.totalMembers()).isEqualTo(100);
        assertThat(result.activeMembers()).isEqualTo(80);
        assertThat(result.totalShareCapital()).isNotNull();
        assertThat(result.totalOutstandingLoans()).isNotNull();
    }

    @Test
    void getSaccoState_withZeroDisbursed_loanRecoveryRateShouldBeZero() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(10L)    // totalMembers
                .thenReturn(8L)     // activeMembers
                .thenReturn(5L);    // membersOneYearAgo

        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(BigDecimal.ZERO)    // totalShareCapital
                .thenReturn(BigDecimal.ZERO)    // totalOutstandingLoans
                .thenReturn(BigDecimal.ZERO)    // totalContributions
                .thenReturn(BigDecimal.ZERO)    // totalPayouts
                .thenReturn(BigDecimal.ZERO)    // totalRepaid
                .thenReturn(BigDecimal.ZERO);   // totalDisbursed (zero => recovery rate = 0)

        SaccoStateResponse result = financialReportService.getSaccoState();

        assertThat(result.loanRecoveryRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSaccoState_withZeroMembersOneYearAgo_growthRateShouldBeZero() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(10L)    // totalMembers
                .thenReturn(10L)    // activeMembers
                .thenReturn(0L);    // membersOneYearAgo (zero => growth rate = 0)

        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(new BigDecimal("100"))
                .thenReturn(new BigDecimal("200"));

        SaccoStateResponse result = financialReportService.getSaccoState();

        assertThat(result.memberGrowthRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSaccoState_loanRecoveryRateShouldBePercentage() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(50L)
                .thenReturn(40L)
                .thenReturn(30L);

        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("100000"))   // totalShareCapital
                .thenReturn(new BigDecimal("50000"))    // totalOutstandingLoans
                .thenReturn(new BigDecimal("200000"))   // totalContributions
                .thenReturn(new BigDecimal("50000"))    // totalPayouts
                .thenReturn(new BigDecimal("75000"))    // totalRepaid
                .thenReturn(new BigDecimal("100000"));  // totalDisbursed
        // recovery = 75000/100000 * 100 = 75.00%

        SaccoStateResponse result = financialReportService.getSaccoState();

        assertThat(result.loanRecoveryRate()).isEqualByComparingTo(new BigDecimal("75.0000"));
    }

    @Test
    void getSaccoState_nullQueryResults_shouldDefaultToZero() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(null);

        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(null);

        SaccoStateResponse result = financialReportService.getSaccoState();

        assertThat(result.totalMembers()).isEqualTo(0);
        assertThat(result.activeMembers()).isEqualTo(0);
        assertThat(result.totalShareCapital()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
