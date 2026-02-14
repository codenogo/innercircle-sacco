package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.AdminDashboardResponse;
import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberDashboardResponse;
import com.innercircle.sacco.reporting.dto.MemberStatementEntry;
import com.innercircle.sacco.reporting.dto.TreasurerDashboardResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class FinancialReportServiceImpl implements FinancialReportService {

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
                "SELECT COALESCE(SUM(amount), 0) FROM loan_applications WHERE status = 'DISBURSED' AND disbursed_at BETWEEN ? AND ?",
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        BigDecimal totalRepayments = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM loan_repayments WHERE payment_date BETWEEN ? AND ?",
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        BigDecimal totalPayouts = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE status = 'PROCESSED' AND processed_at BETWEEN ? AND ?",
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        BigDecimal totalPenalties = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM contribution_penalties WHERE applied_date BETWEEN ? AND ?",
                fromDate, toDate);

        BigDecimal netPosition = totalContributions.add(totalRepayments).add(totalPenalties)
                .subtract(totalLoansDisbursed).subtract(totalPayouts);

        long activeMemberCount = queryCount("SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'");

        long activeLoansCount = queryCount(
                "SELECT COUNT(*) FROM loan_applications WHERE status = 'DISBURSED'");

        BigDecimal outstandingLoanBalance = querySum(
                "SELECT COALESCE(SUM(amount - COALESCE((SELECT SUM(lr.amount) FROM loan_repayments lr WHERE lr.loan_id = la.id), 0)), 0) " +
                        "FROM loan_applications la WHERE la.status = 'DISBURSED'");

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
                "SELECT COUNT(*) FROM loan_applications WHERE member_id = ? AND status = 'DISBURSED'",
                Integer.class, memberId);

        BigDecimal outstandingLoanBalance = querySum(
                "SELECT COALESCE(SUM(la.amount - COALESCE((SELECT SUM(lr.amount) FROM loan_repayments lr WHERE lr.loan_id = la.id), 0)), 0) " +
                        "FROM loan_applications la WHERE la.member_id = ? AND la.status = 'DISBURSED'",
                memberId);

        List<MemberStatementEntry> recentTransactions = jdbc.query(
                "SELECT id, amount, contribution_date as txn_date, 'CONTRIBUTION' as txn_type FROM contributions " +
                        "WHERE member_id = ? AND status = 'CONFIRMED' " +
                        "UNION ALL " +
                        "SELECT lr.id, lr.amount, lr.payment_date as txn_date, 'REPAYMENT' as txn_type FROM loan_repayments lr " +
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

        BigDecimal totalDisbursements = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE status = 'PROCESSED' AND processed_at BETWEEN ? AND ?",
                startOfMonth.atStartOfDay(), endOfMonth.plusDays(1).atStartOfDay());

        int pendingApprovals = jdbc.queryForObject(
                "SELECT COUNT(*) FROM loan_applications WHERE status = 'PENDING'",
                Integer.class);

        int overdueLoans = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT la.id) FROM loan_applications la " +
                        "JOIN repayment_schedules rs ON rs.loan_id = la.id " +
                        "WHERE la.status = 'DISBURSED' AND rs.status = 'PENDING' AND rs.due_date < CURRENT_DATE",
                Integer.class);

        BigDecimal cashPosition = querySum(
                "SELECT COALESCE(SUM(CASE WHEN account_type = 'ASSET' THEN balance ELSE -balance END), 0) FROM accounts");

        long activeMemberCount = queryCount("SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'");

        BigDecimal totalShareCapital = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM contributions WHERE status = 'CONFIRMED' AND contribution_type = 'SHARE'");

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
                "SELECT COUNT(*) FROM loan_applications WHERE status = 'DISBURSED'",
                Integer.class);

        BigDecimal totalOutstandingLoans = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM loan_applications WHERE status = 'DISBURSED'");

        int recentAuditEvents = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE timestamp >= CURRENT_TIMESTAMP - INTERVAL '24 hours'",
                Integer.class);

        return new AdminDashboardResponse(
                totalMembers, activeMembers, totalAssets, totalLiabilities,
                totalLoanProducts, totalActiveLoans, totalOutstandingLoans, recentAuditEvents);
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
