package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.MemberStatementEntry;
import com.innercircle.sacco.reporting.dto.MemberStatementResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class MemberStatementServiceImpl implements MemberStatementService {

    private final JdbcTemplate jdbc;

    public MemberStatementServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MemberStatementResponse generateStatement(UUID memberId, LocalDate fromDate, LocalDate toDate) {
        String memberName = jdbc.queryForObject(
                "SELECT CONCAT(first_name, ' ', last_name) FROM members WHERE id = ?",
                String.class, memberId);

        List<MemberStatementEntry> entries = new ArrayList<>();
        entries.addAll(fetchContributions(memberId, fromDate, toDate));
        entries.addAll(fetchLoanDisbursements(memberId, fromDate, toDate));
        entries.addAll(fetchLoanRepayments(memberId, fromDate, toDate));
        entries.addAll(fetchPayouts(memberId, fromDate, toDate));
        entries.addAll(fetchPenalties(memberId, fromDate, toDate));

        entries.sort(Comparator.comparing(MemberStatementEntry::date));

        BigDecimal totalContributions = BigDecimal.ZERO;
        BigDecimal totalLoansReceived = BigDecimal.ZERO;
        BigDecimal totalRepayments = BigDecimal.ZERO;
        BigDecimal totalPayouts = BigDecimal.ZERO;
        BigDecimal totalPenalties = BigDecimal.ZERO;

        BigDecimal runningBalance = BigDecimal.ZERO;
        List<MemberStatementEntry> withBalance = new ArrayList<>();

        for (MemberStatementEntry entry : entries) {
            if (entry.credit() != null) {
                runningBalance = runningBalance.add(entry.credit());
            }
            if (entry.debit() != null) {
                runningBalance = runningBalance.subtract(entry.debit());
            }

            withBalance.add(new MemberStatementEntry(
                    entry.date(), entry.type(), entry.description(),
                    entry.debit(), entry.credit(), runningBalance, entry.referenceId()));

            switch (entry.type()) {
                case "CONTRIBUTION" -> totalContributions = totalContributions.add(entry.credit() != null ? entry.credit() : BigDecimal.ZERO);
                case "LOAN_DISBURSEMENT" -> totalLoansReceived = totalLoansReceived.add(entry.credit() != null ? entry.credit() : BigDecimal.ZERO);
                case "LOAN_REPAYMENT" -> totalRepayments = totalRepayments.add(entry.debit() != null ? entry.debit() : BigDecimal.ZERO);
                case "PAYOUT" -> totalPayouts = totalPayouts.add(entry.debit() != null ? entry.debit() : BigDecimal.ZERO);
                case "PENALTY" -> totalPenalties = totalPenalties.add(entry.debit() != null ? entry.debit() : BigDecimal.ZERO);
                default -> { /* no-op */ }
            }
        }

        return new MemberStatementResponse(
                memberId, memberName, fromDate, toDate,
                BigDecimal.ZERO, runningBalance,
                totalContributions, totalLoansReceived, totalRepayments, totalPayouts, totalPenalties,
                withBalance);
    }

    private List<MemberStatementEntry> fetchContributions(UUID memberId, LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT id, amount, contribution_date, contribution_type FROM contributions " +
                        "WHERE member_id = ? AND contribution_date BETWEEN ? AND ? AND status = 'CONFIRMED'",
                (rs, rowNum) -> new MemberStatementEntry(
                        rs.getTimestamp("contribution_date").toLocalDateTime(),
                        "CONTRIBUTION",
                        rs.getString("contribution_type") + " contribution",
                        null,
                        rs.getBigDecimal("amount"),
                        null,
                        rs.getObject("id", UUID.class)),
                memberId, from, to);
    }

    private List<MemberStatementEntry> fetchLoanDisbursements(UUID memberId, LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT id, amount, disbursed_at FROM loan_applications " +
                        "WHERE member_id = ? AND disbursed_at BETWEEN ? AND ? AND status = 'DISBURSED'",
                (rs, rowNum) -> new MemberStatementEntry(
                        rs.getTimestamp("disbursed_at").toLocalDateTime(),
                        "LOAN_DISBURSEMENT",
                        "Loan disbursement",
                        null,
                        rs.getBigDecimal("amount"),
                        null,
                        rs.getObject("id", UUID.class)),
                memberId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private List<MemberStatementEntry> fetchLoanRepayments(UUID memberId, LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT lr.id, lr.amount, lr.payment_date FROM loan_repayments lr " +
                        "JOIN loan_applications la ON lr.loan_id = la.id " +
                        "WHERE la.member_id = ? AND lr.payment_date BETWEEN ? AND ?",
                (rs, rowNum) -> new MemberStatementEntry(
                        rs.getTimestamp("payment_date").toLocalDateTime(),
                        "LOAN_REPAYMENT",
                        "Loan repayment",
                        rs.getBigDecimal("amount"),
                        null,
                        null,
                        rs.getObject("id", UUID.class)),
                memberId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private List<MemberStatementEntry> fetchPayouts(UUID memberId, LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT id, amount, processed_at, payout_type FROM payouts " +
                        "WHERE member_id = ? AND processed_at BETWEEN ? AND ? AND status = 'PROCESSED'",
                (rs, rowNum) -> new MemberStatementEntry(
                        rs.getTimestamp("processed_at").toLocalDateTime(),
                        "PAYOUT",
                        rs.getString("payout_type") + " payout",
                        rs.getBigDecimal("amount"),
                        null,
                        null,
                        rs.getObject("id", UUID.class)),
                memberId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private List<MemberStatementEntry> fetchPenalties(UUID memberId, LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT id, amount, applied_date FROM contribution_penalties " +
                        "WHERE member_id = ? AND applied_date BETWEEN ? AND ?",
                (rs, rowNum) -> new MemberStatementEntry(
                        rs.getTimestamp("applied_date").toLocalDateTime(),
                        "PENALTY",
                        "Late contribution penalty",
                        rs.getBigDecimal("amount"),
                        null,
                        null,
                        rs.getObject("id", UUID.class)),
                memberId, from, to);
    }
}
