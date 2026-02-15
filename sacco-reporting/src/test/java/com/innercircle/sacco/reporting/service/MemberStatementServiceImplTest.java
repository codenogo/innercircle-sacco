package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.MemberStatementEntry;
import com.innercircle.sacco.reporting.dto.MemberStatementResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberStatementServiceImplTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private MemberStatementServiceImpl memberStatementService;

    @Test
    void generateStatement_withNoTransactions_shouldReturnEmptyStatement() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(jdbc.queryForObject(anyString(), eq(String.class), eq(memberId)))
                .thenReturn("John Doe");

        // All queries return empty lists
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        MemberStatementResponse result = memberStatementService.generateStatement(memberId, from, to);

        assertThat(result).isNotNull();
        assertThat(result.memberId()).isEqualTo(memberId);
        assertThat(result.memberName()).isEqualTo("John Doe");
        assertThat(result.fromDate()).isEqualTo(from);
        assertThat(result.toDate()).isEqualTo(to);
        assertThat(result.openingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.closingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.entries()).isEmpty();
        assertThat(result.totalContributions()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalLoansReceived()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalRepayments()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalPayouts()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalPenalties()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void generateStatement_withContributions_shouldCalculateRunningBalance() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        UUID refId1 = UUID.randomUUID();
        UUID refId2 = UUID.randomUUID();

        when(jdbc.queryForObject(anyString(), eq(String.class), eq(memberId)))
                .thenReturn("Jane Smith");

        MemberStatementEntry contribution1 = new MemberStatementEntry(
                LocalDateTime.of(2025, 1, 15, 10, 0),
                "CONTRIBUTION", "SHARE contribution",
                null, new BigDecimal("5000.00"), null, refId1);

        MemberStatementEntry contribution2 = new MemberStatementEntry(
                LocalDateTime.of(2025, 2, 15, 10, 0),
                "CONTRIBUTION", "SHARE contribution",
                null, new BigDecimal("5000.00"), null, refId2);

        // First query returns contributions, rest return empty
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(contribution1, contribution2))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        MemberStatementResponse result = memberStatementService.generateStatement(memberId, from, to);

        assertThat(result.entries()).hasSize(2);
        assertThat(result.totalContributions()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(result.closingBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));

        // Check running balances
        assertThat(result.entries().get(0).runningBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.entries().get(1).runningBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void generateStatement_withMixedTransactions_shouldCalculateCorrectly() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 6, 30);

        when(jdbc.queryForObject(anyString(), eq(String.class), eq(memberId)))
                .thenReturn("Alice Johnson");

        MemberStatementEntry contribution = new MemberStatementEntry(
                LocalDateTime.of(2025, 1, 10, 9, 0),
                "CONTRIBUTION", "Monthly contribution",
                null, new BigDecimal("10000.00"), null, UUID.randomUUID());

        MemberStatementEntry repayment = new MemberStatementEntry(
                LocalDateTime.of(2025, 2, 10, 9, 0),
                "LOAN_REPAYMENT", "Loan repayment",
                new BigDecimal("3000.00"), null, null, UUID.randomUUID());

        MemberStatementEntry payout = new MemberStatementEntry(
                LocalDateTime.of(2025, 3, 10, 9, 0),
                "PAYOUT", "DIVIDEND payout",
                new BigDecimal("2000.00"), null, null, UUID.randomUUID());

        MemberStatementEntry penalty = new MemberStatementEntry(
                LocalDateTime.of(2025, 4, 10, 9, 0),
                "PENALTY", "Late contribution penalty",
                new BigDecimal("500.00"), null, null, UUID.randomUUID());

        MemberStatementEntry loanDisbursement = new MemberStatementEntry(
                LocalDateTime.of(2025, 5, 10, 9, 0),
                "LOAN_DISBURSEMENT", "Loan disbursement",
                null, new BigDecimal("50000.00"), null, UUID.randomUUID());

        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(contribution))       // contributions
                .thenReturn(List.of(loanDisbursement))   // loan disbursements
                .thenReturn(List.of(repayment))          // loan repayments
                .thenReturn(List.of(payout))             // payouts
                .thenReturn(List.of(penalty));           // penalties

        MemberStatementResponse result = memberStatementService.generateStatement(memberId, from, to);

        assertThat(result.entries()).hasSize(5);
        assertThat(result.totalContributions()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(result.totalLoansReceived()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(result.totalRepayments()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(result.totalPayouts()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.totalPenalties()).isEqualByComparingTo(new BigDecimal("500.00"));

        // closing = 10000 + 50000 - 3000 - 2000 - 500 = 54500
        assertThat(result.closingBalance()).isEqualByComparingTo(new BigDecimal("54500.00"));
    }

    @Test
    void generateStatement_entriesShouldBeSortedByDate() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(jdbc.queryForObject(anyString(), eq(String.class), eq(memberId)))
                .thenReturn("Bob Brown");

        MemberStatementEntry laterEntry = new MemberStatementEntry(
                LocalDateTime.of(2025, 6, 15, 10, 0),
                "CONTRIBUTION", "Contribution",
                null, new BigDecimal("5000.00"), null, UUID.randomUUID());

        MemberStatementEntry earlierEntry = new MemberStatementEntry(
                LocalDateTime.of(2025, 1, 15, 10, 0),
                "LOAN_REPAYMENT", "Repayment",
                new BigDecimal("2000.00"), null, null, UUID.randomUUID());

        // contributions returns the later entry; repayments returns the earlier
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(laterEntry))      // contributions
                .thenReturn(Collections.emptyList())   // loan disbursements
                .thenReturn(List.of(earlierEntry))    // loan repayments
                .thenReturn(Collections.emptyList())   // payouts
                .thenReturn(Collections.emptyList());  // penalties

        MemberStatementResponse result = memberStatementService.generateStatement(memberId, from, to);

        assertThat(result.entries()).hasSize(2);
        // Should be sorted by date: earlier first
        assertThat(result.entries().get(0).type()).isEqualTo("LOAN_REPAYMENT");
        assertThat(result.entries().get(1).type()).isEqualTo("CONTRIBUTION");
    }

    @Test
    void generateStatement_openingBalanceShouldBeZero() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(jdbc.queryForObject(anyString(), eq(String.class), eq(memberId)))
                .thenReturn("Test User");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        MemberStatementResponse result = memberStatementService.generateStatement(memberId, from, to);

        assertThat(result.openingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void generateStatement_withOnlyDebits_shouldHaveNegativeBalance() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(jdbc.queryForObject(anyString(), eq(String.class), eq(memberId)))
                .thenReturn("Debtor User");

        MemberStatementEntry repayment = new MemberStatementEntry(
                LocalDateTime.of(2025, 3, 1, 10, 0),
                "LOAN_REPAYMENT", "Loan repayment",
                new BigDecimal("5000.00"), null, null, UUID.randomUUID());

        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(Collections.emptyList())   // contributions
                .thenReturn(Collections.emptyList())   // loan disbursements
                .thenReturn(List.of(repayment))        // loan repayments
                .thenReturn(Collections.emptyList())   // payouts
                .thenReturn(Collections.emptyList());  // penalties

        MemberStatementResponse result = memberStatementService.generateStatement(memberId, from, to);

        assertThat(result.closingBalance()).isEqualByComparingTo(new BigDecimal("-5000.00"));
        assertThat(result.entries().get(0).runningBalance()).isEqualByComparingTo(new BigDecimal("-5000.00"));
    }

    @Test
    void generateStatement_withNullCreditAndDebit_shouldNotAffectBalance() {
        UUID memberId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(jdbc.queryForObject(anyString(), eq(String.class), eq(memberId)))
                .thenReturn("Test User");

        // An entry with null credit and null debit (edge case)
        MemberStatementEntry noOpEntry = new MemberStatementEntry(
                LocalDateTime.of(2025, 2, 1, 10, 0),
                "UNKNOWN_TYPE", "Some entry",
                null, null, null, UUID.randomUUID());

        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(noOpEntry))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        MemberStatementResponse result = memberStatementService.generateStatement(memberId, from, to);

        assertThat(result.closingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
