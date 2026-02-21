package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.AdminDashboardResponse;
import com.innercircle.sacco.reporting.dto.DashboardAnalyticsResponse;
import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberDashboardResponse;
import com.innercircle.sacco.reporting.dto.MemberStatementEntry;
import com.innercircle.sacco.reporting.dto.MonthlyDataPoint;
import com.innercircle.sacco.reporting.dto.SaccoStateResponse;
import com.innercircle.sacco.reporting.dto.TreasurerDashboardResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FinancialReportServiceImpl implements FinancialReportService {

    private static final String ACCOUNT_CASH_CODE = "1001";
    private static final String ACCOUNT_MEMBER_SHARES_CODE = "2001";
    private static final String SUM_MEMBER_SHARE_CAPITAL_SQL =
            "SELECT COALESCE(SUM(balance), 0) FROM accounts WHERE account_code = '" + ACCOUNT_MEMBER_SHARES_CODE + "'";
    private static final String SUM_AVAILABLE_CASH_SQL =
            "SELECT COALESCE(SUM(balance), 0) FROM accounts WHERE account_code = '" + ACCOUNT_CASH_CODE + "'";

    private final JdbcTemplate jdbc;

    public FinancialReportServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public FinancialSummaryResponse overallSummary(LocalDate fromDate, LocalDate toDate) {
        BigDecimal totalContributions = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM contributions WHERE status = 'CONFIRMED' AND contribution_date BETWEEN ? AND ?",
                fromDate, toDate);

        BigDecimal totalLoansDisbursed = querySum(
                "SELECT COALESCE(SUM(principal_amount), 0) FROM loan_applications WHERE status IN ('DISBURSED','REPAYING','CLOSED') AND disbursed_at BETWEEN ? AND ?",
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        BigDecimal totalRepayments = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM loan_repayments WHERE repayment_date BETWEEN ? AND ?",
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        BigDecimal totalPayouts = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE status = 'PROCESSED' AND processed_at BETWEEN ? AND ?",
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        BigDecimal totalPenalties = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM contribution_penalties WHERE created_at BETWEEN ? AND ?",
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        BigDecimal netPosition = totalContributions.add(totalRepayments).add(totalPenalties)
                .subtract(totalLoansDisbursed).subtract(totalPayouts);

        long activeMemberCount = queryCount("SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'");

        long activeLoansCount = queryCount(
                "SELECT COUNT(*) FROM loan_applications WHERE status IN ('DISBURSED','REPAYING')");

        BigDecimal outstandingLoanBalance = querySum(
                "SELECT COALESCE(SUM(principal_amount - COALESCE((SELECT SUM(lr.amount) FROM loan_repayments lr WHERE lr.loan_id = la.id), 0)), 0) " +
                        "FROM loan_applications la WHERE la.status IN ('DISBURSED','REPAYING')");

        return new FinancialSummaryResponse(
                fromDate, toDate,
                totalContributions, totalLoansDisbursed, totalRepayments,
                totalPayouts, totalPenalties, netPosition,
                activeMemberCount, activeLoansCount, outstandingLoanBalance);
    }

    @Override
    public MemberDashboardResponse memberDashboard(UUID memberId) {
        BigDecimal shareBalance = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM contributions WHERE member_id = ? AND status = 'CONFIRMED'",
                memberId);

        BigDecimal totalContributions = shareBalance;

        int activeLoans = jdbc.queryForObject(
                "SELECT COUNT(*) FROM loan_applications WHERE member_id = ? AND status IN ('DISBURSED','REPAYING')",
                Integer.class, memberId);

        BigDecimal outstandingLoanBalance = querySum(
                "SELECT COALESCE(SUM(la.principal_amount - COALESCE((SELECT SUM(lr.amount) FROM loan_repayments lr WHERE lr.loan_id = la.id), 0)), 0) " +
                        "FROM loan_applications la WHERE la.member_id = ? AND la.status IN ('DISBURSED','REPAYING')",
                memberId);

        List<MemberStatementEntry> recentTransactions = jdbc.query(
                "SELECT id, amount, contribution_date as txn_date, 'CONTRIBUTION' as txn_type FROM contributions " +
                        "WHERE member_id = ? AND status = 'CONFIRMED' " +
                        "UNION ALL " +
                        "SELECT lr.id, lr.amount, lr.repayment_date as txn_date, 'REPAYMENT' as txn_type FROM loan_repayments lr " +
                        "JOIN loan_applications la ON lr.loan_id = la.id WHERE la.member_id = ? " +
                        "ORDER BY txn_date DESC LIMIT 10",
                (rs, rowNum) -> {
                    String type = rs.getString("txn_type");
                    BigDecimal amount = rs.getBigDecimal("amount");
                    return new MemberStatementEntry(
                            rs.getTimestamp("txn_date").toLocalDateTime(),
                            type, type.equals("CONTRIBUTION") ? "Contribution" : "Loan repayment",
                            type.equals("REPAYMENT") ? amount : null,
                            type.equals("CONTRIBUTION") ? amount : null,
                            null,
                            rs.getObject("id", UUID.class));
                },
                memberId, memberId);

        return new MemberDashboardResponse(shareBalance, totalContributions, activeLoans, outstandingLoanBalance, recentTransactions);
    }

    @Override
    public TreasurerDashboardResponse treasurerDashboard() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        BigDecimal totalCollections = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM contributions WHERE status = 'CONFIRMED' AND contribution_date BETWEEN ? AND ?",
                startOfMonth, endOfMonth);

        BigDecimal payoutDisbursements = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE status = 'PROCESSED' AND processed_at BETWEEN ? AND ?",
                startOfMonth.atStartOfDay(), endOfMonth.plusDays(1).atStartOfDay());
        BigDecimal pettyCashDisbursements = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM petty_cash_vouchers " +
                        "WHERE status IN ('DISBURSED', 'SETTLED') AND disbursed_at BETWEEN ? AND ?",
                startOfMonth.atStartOfDay(), endOfMonth.plusDays(1).atStartOfDay());
        BigDecimal totalDisbursements = payoutDisbursements.add(pettyCashDisbursements);

        int pendingApprovals = jdbc.queryForObject(
                "SELECT COUNT(*) FROM loan_applications WHERE status = 'PENDING'",
                Integer.class);

        int overdueLoans = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT la.id) FROM loan_applications la " +
                        "JOIN repayment_schedules rs ON rs.loan_id = la.id " +
                        "WHERE la.status IN ('DISBURSED','REPAYING') AND rs.paid = false AND rs.due_date < CURRENT_DATE",
                Integer.class);

        BigDecimal cashPosition = querySum(SUM_AVAILABLE_CASH_SQL);

        long activeMemberCount = queryCount("SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'");

        BigDecimal totalShareCapital = querySum(SUM_MEMBER_SHARE_CAPITAL_SQL);

        return new TreasurerDashboardResponse(
                totalCollections, totalDisbursements, pendingApprovals,
                overdueLoans, cashPosition, activeMemberCount, totalShareCapital);
    }

    @Override
    public AdminDashboardResponse adminDashboard() {
        long totalMembers = queryCount("SELECT COUNT(*) FROM members");
        long activeMembers = queryCount("SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'");

        BigDecimal totalAssets = querySum(
                "SELECT COALESCE(SUM(balance), 0) FROM accounts WHERE account_type = 'ASSET'");

        BigDecimal totalLiabilities = querySum(
                "SELECT COALESCE(SUM(balance), 0) FROM accounts WHERE account_type = 'LIABILITY'");

        int totalLoanProducts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM loan_product_configs WHERE active = true",
                Integer.class);

        int totalActiveLoans = jdbc.queryForObject(
                "SELECT COUNT(*) FROM loan_applications WHERE status IN ('DISBURSED','REPAYING')",
                Integer.class);

        BigDecimal totalOutstandingLoans = querySum(
                "SELECT COALESCE(SUM(principal_amount), 0) FROM loan_applications WHERE status IN ('DISBURSED','REPAYING')");

        int recentAuditEvents = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE timestamp >= CURRENT_TIMESTAMP - INTERVAL '24 hours'",
                Integer.class);

        return new AdminDashboardResponse(
                totalMembers, activeMembers, totalAssets, totalLiabilities,
                totalLoanProducts, totalActiveLoans, totalOutstandingLoans, recentAuditEvents);
    }

    @Override
    public DashboardAnalyticsResponse getDashboardAnalytics(int year) {
        List<MonthlyDataPoint> loansDisbursed = getMonthlyLoansDisbursed(year);
        List<MonthlyDataPoint> amountRepaid = getMonthlyRepayments(year);
        List<MonthlyDataPoint> interestAccrued = getMonthlyInterest(year);
        List<MonthlyDataPoint> contributionsReceived = getMonthlyContributions(year);
        List<MonthlyDataPoint> payoutsProcessed = getMonthlyPayouts(year);

        return new DashboardAnalyticsResponse(
                year, loansDisbursed, amountRepaid, interestAccrued,
                contributionsReceived, payoutsProcessed
        );
    }

    @Override
    public SaccoStateResponse getSaccoState() {
        int totalMembers = (int) queryCount("SELECT COUNT(*) FROM members");
        int activeMembers = (int) queryCount("SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'");

        BigDecimal totalShareCapital = querySum(SUM_MEMBER_SHARE_CAPITAL_SQL);

        BigDecimal totalOutstandingLoans = querySum(
                "SELECT COALESCE(SUM(la.principal_amount - COALESCE((SELECT SUM(lr.amount) FROM loan_repayments lr WHERE lr.loan_id = la.id), 0)), 0) " +
                        "FROM loan_applications la WHERE la.status IN ('REPAYING', 'DISBURSED')");

        BigDecimal totalContributions = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM contributions WHERE status = 'CONFIRMED'");

        BigDecimal payoutDisbursements = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE status = 'PROCESSED'");
        BigDecimal pettyCashDisbursements = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM petty_cash_vouchers " +
                        "WHERE status IN ('DISBURSED', 'SETTLED') AND disbursed_at IS NOT NULL");
        BigDecimal totalPayouts = payoutDisbursements.add(pettyCashDisbursements);

        // Loan recovery rate: total repayments / total loans disbursed (all time)
        BigDecimal totalRepaid = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM loan_repayments");
        BigDecimal totalDisbursed = querySum(
                "SELECT COALESCE(SUM(principal_amount), 0) FROM loan_applications WHERE status IN ('REPAYING', 'CLOSED', 'DISBURSED')");

        BigDecimal loanRecoveryRate = totalDisbursed.compareTo(BigDecimal.ZERO) > 0
                ? totalRepaid.divide(totalDisbursed, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // Member growth rate: (active members - members 1 year ago) / members 1 year ago * 100
        int membersOneYearAgo = (int) queryCount(
                "SELECT COUNT(*) FROM members WHERE created_at <= CURRENT_DATE - INTERVAL '1 year'");

        BigDecimal memberGrowthRate = membersOneYearAgo > 0
                ? new BigDecimal(activeMembers - membersOneYearAgo)
                .divide(new BigDecimal(membersOneYearAgo), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        return new SaccoStateResponse(
                totalMembers, activeMembers, totalShareCapital,
                totalOutstandingLoans, totalContributions, totalPayouts,
                loanRecoveryRate, memberGrowthRate
        );
    }

    private List<MonthlyDataPoint> getMonthlyLoansDisbursed(int year) {
        String sql = "SELECT EXTRACT(MONTH FROM disbursed_at) AS month, SUM(principal_amount) AS amount " +
                "FROM loan_applications " +
                "WHERE status IN ('REPAYING', 'CLOSED') AND EXTRACT(YEAR FROM disbursed_at) = ? " +
                "GROUP BY EXTRACT(MONTH FROM disbursed_at)";

        Map<Integer, BigDecimal> monthlyData = new HashMap<>();
        jdbc.query(sql, rs -> {
            int month = rs.getInt("month");
            BigDecimal amount = rs.getBigDecimal("amount");
            monthlyData.put(month, amount != null ? amount : BigDecimal.ZERO);
        }, year);

        return fillAllMonths(monthlyData);
    }

    private List<MonthlyDataPoint> getMonthlyRepayments(int year) {
        String sql = "SELECT EXTRACT(MONTH FROM repayment_date) AS month, SUM(amount) AS amount " +
                "FROM loan_repayments " +
                "WHERE EXTRACT(YEAR FROM repayment_date) = ? " +
                "GROUP BY EXTRACT(MONTH FROM repayment_date)";

        Map<Integer, BigDecimal> monthlyData = new HashMap<>();
        jdbc.query(sql, rs -> {
            int month = rs.getInt("month");
            BigDecimal amount = rs.getBigDecimal("amount");
            monthlyData.put(month, amount != null ? amount : BigDecimal.ZERO);
        }, year);

        return fillAllMonths(monthlyData);
    }

    private List<MonthlyDataPoint> getMonthlyInterest(int year) {
        String sql = "SELECT EXTRACT(MONTH FROM repayment_date) AS month, SUM(interest_portion) AS amount " +
                "FROM loan_repayments " +
                "WHERE EXTRACT(YEAR FROM repayment_date) = ? " +
                "GROUP BY EXTRACT(MONTH FROM repayment_date)";

        Map<Integer, BigDecimal> monthlyData = new HashMap<>();
        jdbc.query(sql, rs -> {
            int month = rs.getInt("month");
            BigDecimal amount = rs.getBigDecimal("amount");
            monthlyData.put(month, amount != null ? amount : BigDecimal.ZERO);
        }, year);

        return fillAllMonths(monthlyData);
    }

    private List<MonthlyDataPoint> getMonthlyContributions(int year) {
        String sql = "SELECT EXTRACT(MONTH FROM contribution_date) AS month, SUM(amount) AS amount " +
                "FROM contributions " +
                "WHERE status = 'CONFIRMED' AND EXTRACT(YEAR FROM contribution_date) = ? " +
                "GROUP BY EXTRACT(MONTH FROM contribution_date)";

        Map<Integer, BigDecimal> monthlyData = new HashMap<>();
        jdbc.query(sql, rs -> {
            int month = rs.getInt("month");
            BigDecimal amount = rs.getBigDecimal("amount");
            monthlyData.put(month, amount != null ? amount : BigDecimal.ZERO);
        }, year);

        return fillAllMonths(monthlyData);
    }

    private List<MonthlyDataPoint> getMonthlyPayouts(int year) {
        String sql = "SELECT EXTRACT(MONTH FROM processed_at) AS month, SUM(amount) AS amount " +
                "FROM payouts " +
                "WHERE status = 'PROCESSED' AND EXTRACT(YEAR FROM processed_at) = ? " +
                "GROUP BY EXTRACT(MONTH FROM processed_at)";

        Map<Integer, BigDecimal> monthlyData = new HashMap<>();
        jdbc.query(sql, rs -> {
            int month = rs.getInt("month");
            BigDecimal amount = rs.getBigDecimal("amount");
            monthlyData.put(month, amount != null ? amount : BigDecimal.ZERO);
        }, year);

        return fillAllMonths(monthlyData);
    }

    private List<MonthlyDataPoint> fillAllMonths(Map<Integer, BigDecimal> monthlyData) {
        List<MonthlyDataPoint> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            BigDecimal amount = monthlyData.getOrDefault(month, BigDecimal.ZERO);
            String monthName = Month.of(month).name();
            result.add(new MonthlyDataPoint(month, monthName, amount));
        }
        return result;
    }

    private BigDecimal querySum(String sql, Object... args) {
        BigDecimal result = jdbc.queryForObject(sql, BigDecimal.class, args);
        return result != null ? result : BigDecimal.ZERO;
    }

    private long queryCount(String sql, Object... args) {
        Long result = jdbc.queryForObject(sql, Long.class, args);
        return result != null ? result : 0L;
    }
}
