package com.innercircle.sacco.ledger.listener;

import com.innercircle.sacco.common.event.ContributionReceivedEvent;
import com.innercircle.sacco.common.event.LoanDisbursedEvent;
import com.innercircle.sacco.common.event.LoanRepaymentEvent;
import com.innercircle.sacco.common.event.PayoutProcessedEvent;
import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.AccountType;
import com.innercircle.sacco.ledger.entity.JournalEntry;
import com.innercircle.sacco.ledger.entity.JournalLine;
import com.innercircle.sacco.ledger.entity.TransactionType;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import com.innercircle.sacco.ledger.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialEventListenerTest {

    @Mock
    private LedgerService ledgerService;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private FinancialEventListener financialEventListener;

    @Captor
    private ArgumentCaptor<JournalEntry> journalEntryCaptor;

    private Account cashAccount;
    private Account memberSharesAccount;
    private Account loanReceivableAccount;
    private Account interestIncomeAccount;
    private Account interestReceivableAccount;
    private Account contributionIncomeAccount;
    private Account penaltyIncomeAccount;
    private Account memberAccountAccount;

    @BeforeEach
    void setUp() {
        cashAccount = createAccount("1001", "Cash", AccountType.ASSET);
        memberSharesAccount = createAccount("2001", "Member Shares", AccountType.LIABILITY);
        loanReceivableAccount = createAccount("1002", "Loan Receivable", AccountType.ASSET);
        interestIncomeAccount = createAccount("4001", "Interest Income", AccountType.REVENUE);
        interestReceivableAccount = createAccount("1003", "Interest Receivable", AccountType.ASSET);
        contributionIncomeAccount = createAccount("4002", "Contribution Income", AccountType.REVENUE);
        penaltyIncomeAccount = createAccount("4003", "Penalty Income", AccountType.REVENUE);
        memberAccountAccount = createAccount("2002", "Member Account", AccountType.LIABILITY);
    }

    @Nested
    @DisplayName("handleContributionReceived")
    class HandleContributionReceivedTests {

        @Test
        @DisplayName("should create journal entry for contribution received")
        void shouldCreateJournalEntryForContribution() {
            UUID contributionId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            BigDecimal amount = new BigDecimal("5000.00");
            ContributionReceivedEvent event = new ContributionReceivedEvent(
                    contributionId, memberId, amount, "REF-001", "admin"
            );

            setupAccountLookup("1001", cashAccount);
            setupAccountLookup("2001", memberSharesAccount);
            setupLedgerService();

            financialEventListener.handleContributionReceived(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            assertEquals(TransactionType.CONTRIBUTION, captured.getTransactionType());
            assertEquals(contributionId, captured.getReferenceId());
            assertEquals("Contribution received from member", captured.getDescription());
            assertEquals(2, captured.getJournalLines().size());

            // Verify debit to Cash
            JournalLine debitLine = captured.getJournalLines().get(0);
            assertEquals(cashAccount, debitLine.getAccount());
            assertEquals(amount, debitLine.getDebitAmount());
            assertEquals(BigDecimal.ZERO, debitLine.getCreditAmount());

            // Verify credit to Member Shares
            JournalLine creditLine = captured.getJournalLines().get(1);
            assertEquals(memberSharesAccount, creditLine.getAccount());
            assertEquals(BigDecimal.ZERO, creditLine.getDebitAmount());
            assertEquals(amount, creditLine.getCreditAmount());
        }

        @Test
        @DisplayName("should post journal entry after creation")
        void shouldPostJournalEntryAfterCreation() {
            UUID contributionId = UUID.randomUUID();
            ContributionReceivedEvent event = new ContributionReceivedEvent(
                    contributionId, UUID.randomUUID(), new BigDecimal("1000.00"), "REF-002", "admin"
            );

            setupAccountLookup("1001", cashAccount);
            setupAccountLookup("2001", memberSharesAccount);
            UUID entryId = UUID.randomUUID();
            JournalEntry createdEntry = new JournalEntry();
            createdEntry.setId(entryId);
            when(ledgerService.createJournalEntry(any(JournalEntry.class))).thenReturn(createdEntry);

            financialEventListener.handleContributionReceived(event);

            verify(ledgerService).postEntry(entryId);
        }

        @Test
        @DisplayName("should include reference number in journal line descriptions")
        void shouldIncludeReferenceInDescriptions() {
            ContributionReceivedEvent event = new ContributionReceivedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("2000.00"), "REF-003", "admin"
            );

            setupAccountLookup("1001", cashAccount);
            setupAccountLookup("2001", memberSharesAccount);
            setupLedgerService();

            financialEventListener.handleContributionReceived(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            JournalLine debitLine = captured.getJournalLines().get(0);
            assertEquals("Cash received - Contribution REF-003", debitLine.getDescription());

            JournalLine creditLine = captured.getJournalLines().get(1);
            assertEquals("Member shares - REF-003", creditLine.getDescription());
        }

        @Test
        @DisplayName("should create balanced entry (debits equal credits)")
        void shouldCreateBalancedEntry() {
            BigDecimal amount = new BigDecimal("7500.00");
            ContributionReceivedEvent event = new ContributionReceivedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), amount, "REF-004", "admin"
            );

            setupAccountLookup("1001", cashAccount);
            setupAccountLookup("2001", memberSharesAccount);
            setupLedgerService();

            financialEventListener.handleContributionReceived(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            BigDecimal totalDebits = captured.getJournalLines().stream()
                    .map(JournalLine::getDebitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredits = captured.getJournalLines().stream()
                    .map(JournalLine::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, totalDebits.compareTo(totalCredits));
        }
    }

    @Nested
    @DisplayName("handleLoanDisbursed")
    class HandleLoanDisbursedTests {

        @Test
        @DisplayName("should create journal entry for loan disbursement")
        void shouldCreateJournalEntryForLoanDisbursement() {
            UUID loanId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            BigDecimal principalAmount = new BigDecimal("50000.00");
            BigDecimal interestAmount = new BigDecimal("5000.00");
            LoanDisbursedEvent event = new LoanDisbursedEvent(
                    loanId, memberId, principalAmount, interestAmount, "admin"
            );

            setupAccountLookup("1002", loanReceivableAccount);
            setupAccountLookup("1001", cashAccount);
            setupLedgerService();

            financialEventListener.handleLoanDisbursed(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            assertEquals(TransactionType.LOAN_DISBURSEMENT, captured.getTransactionType());
            assertEquals(loanId, captured.getReferenceId());
            assertEquals("Loan disbursed to member", captured.getDescription());
            assertEquals(2, captured.getJournalLines().size());

            // Verify debit to Loan Receivable
            JournalLine debitLine = captured.getJournalLines().get(0);
            assertEquals(loanReceivableAccount, debitLine.getAccount());
            assertEquals(principalAmount, debitLine.getDebitAmount());
            assertEquals(BigDecimal.ZERO, debitLine.getCreditAmount());

            // Verify credit to Cash
            JournalLine creditLine = captured.getJournalLines().get(1);
            assertEquals(cashAccount, creditLine.getAccount());
            assertEquals(BigDecimal.ZERO, creditLine.getDebitAmount());
            assertEquals(principalAmount, creditLine.getCreditAmount());
        }

        @Test
        @DisplayName("should create balanced loan disbursement entry")
        void shouldCreateBalancedLoanDisbursementEntry() {
            BigDecimal principalAmount = new BigDecimal("100000.00");
            LoanDisbursedEvent event = new LoanDisbursedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), principalAmount, new BigDecimal("10000.00"), "admin"
            );

            setupAccountLookup("1002", loanReceivableAccount);
            setupAccountLookup("1001", cashAccount);
            setupLedgerService();

            financialEventListener.handleLoanDisbursed(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            BigDecimal totalDebits = captured.getJournalLines().stream()
                    .map(JournalLine::getDebitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredits = captured.getJournalLines().stream()
                    .map(JournalLine::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, totalDebits.compareTo(totalCredits));
        }

        @Test
        @DisplayName("should post loan disbursement entry after creation")
        void shouldPostLoanDisbursementEntry() {
            LoanDisbursedEvent event = new LoanDisbursedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("25000.00"), new BigDecimal("2500.00"), "admin"
            );

            setupAccountLookup("1002", loanReceivableAccount);
            setupAccountLookup("1001", cashAccount);
            UUID entryId = UUID.randomUUID();
            JournalEntry createdEntry = new JournalEntry();
            createdEntry.setId(entryId);
            when(ledgerService.createJournalEntry(any(JournalEntry.class))).thenReturn(createdEntry);

            financialEventListener.handleLoanDisbursed(event);

            verify(ledgerService).postEntry(entryId);
        }
    }

    @Nested
    @DisplayName("handleLoanRepayment")
    class HandleLoanRepaymentTests {

        @Test
        @DisplayName("should create journal entry with principal and interest portions")
        void shouldCreateJournalEntryWithPrincipalAndInterest() {
            UUID loanId = UUID.randomUUID();
            UUID repaymentId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            BigDecimal totalAmount = new BigDecimal("1500.00");
            BigDecimal principalPortion = new BigDecimal("1200.00");
            BigDecimal interestPortion = new BigDecimal("300.00");

            LoanRepaymentEvent event = new LoanRepaymentEvent(
                    loanId, memberId, repaymentId, totalAmount, principalPortion, interestPortion, BigDecimal.ZERO, "admin"
            );

            setupAccountLookup("1001", cashAccount);
            setupAccountLookup("1002", loanReceivableAccount);
            setupAccountLookup("1003", interestReceivableAccount);
            setupLedgerService();

            financialEventListener.handleLoanRepayment(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            assertEquals(TransactionType.LOAN_REPAYMENT, captured.getTransactionType());
            assertEquals(repaymentId, captured.getReferenceId());
            assertEquals("Loan repayment received", captured.getDescription());
            assertEquals(3, captured.getJournalLines().size());

            // Verify debit to Cash (total amount)
            JournalLine debitCash = captured.getJournalLines().get(0);
            assertEquals(cashAccount, debitCash.getAccount());
            assertEquals(totalAmount, debitCash.getDebitAmount());
            assertEquals(BigDecimal.ZERO, debitCash.getCreditAmount());

            // Verify credit to Loan Receivable (principal)
            JournalLine creditPrincipal = captured.getJournalLines().get(1);
            assertEquals(loanReceivableAccount, creditPrincipal.getAccount());
            assertEquals(BigDecimal.ZERO, creditPrincipal.getDebitAmount());
            assertEquals(principalPortion, creditPrincipal.getCreditAmount());

            // Verify credit to Interest Receivable (settles accrual)
            JournalLine creditInterest = captured.getJournalLines().get(2);
            assertEquals(interestReceivableAccount, creditInterest.getAccount());
            assertEquals(BigDecimal.ZERO, creditInterest.getDebitAmount());
            assertEquals(interestPortion, creditInterest.getCreditAmount());
        }

        @Test
        @DisplayName("should create balanced repayment entry with interest")
        void shouldCreateBalancedRepaymentEntryWithInterest() {
            BigDecimal totalAmount = new BigDecimal("2000.00");
            BigDecimal principalPortion = new BigDecimal("1500.00");
            BigDecimal interestPortion = new BigDecimal("500.00");

            LoanRepaymentEvent event = new LoanRepaymentEvent(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    totalAmount, principalPortion, interestPortion, BigDecimal.ZERO, "admin"
            );

            setupAccountLookup("1001", cashAccount);
            setupAccountLookup("1002", loanReceivableAccount);
            setupAccountLookup("1003", interestReceivableAccount);
            setupLedgerService();

            financialEventListener.handleLoanRepayment(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            BigDecimal totalDebits = captured.getJournalLines().stream()
                    .map(JournalLine::getDebitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredits = captured.getJournalLines().stream()
                    .map(JournalLine::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, totalDebits.compareTo(totalCredits));
        }

        @Test
        @DisplayName("should skip interest line when interest portion is zero")
        void shouldSkipInterestLineWhenZero() {
            BigDecimal totalAmount = new BigDecimal("1000.00");
            BigDecimal principalPortion = new BigDecimal("1000.00");
            BigDecimal interestPortion = BigDecimal.ZERO;

            LoanRepaymentEvent event = new LoanRepaymentEvent(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    totalAmount, principalPortion, interestPortion, BigDecimal.ZERO, "admin"
            );

            setupAccountLookup("1001", cashAccount);
            setupAccountLookup("1002", loanReceivableAccount);
            setupAccountLookup("1003", interestReceivableAccount);
            setupLedgerService();

            financialEventListener.handleLoanRepayment(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            // Only 2 lines: debit cash and credit loan receivable
            assertEquals(2, captured.getJournalLines().size());
        }

        @Test
        @DisplayName("should credit Member Account for penalty portion (not Penalty Income)")
        void shouldCreditMemberAccountForPenaltyPortion() {
            UUID repaymentId = UUID.randomUUID();
            BigDecimal totalAmount = new BigDecimal("2000.00");
            BigDecimal principalPortion = new BigDecimal("1200.00");
            BigDecimal interestPortion = new BigDecimal("300.00");
            BigDecimal penaltyPortion = new BigDecimal("500.00");

            LoanRepaymentEvent event = new LoanRepaymentEvent(
                    UUID.randomUUID(), UUID.randomUUID(), repaymentId,
                    totalAmount, principalPortion, interestPortion, penaltyPortion, "admin"
            );

            setupAccountLookup("1001", cashAccount);
            setupAccountLookup("1002", loanReceivableAccount);
            setupAccountLookup("1003", interestReceivableAccount);
            setupAccountLookup("2002", memberAccountAccount);
            setupLedgerService();

            financialEventListener.handleLoanRepayment(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            // 4 lines: DR Cash, CR Loan Receivable, CR Interest Receivable, CR Member Account
            assertEquals(4, captured.getJournalLines().size());

            // Verify penalty portion credits Member Account (2002), NOT Penalty Income (4003)
            JournalLine penaltyLine = captured.getJournalLines().get(3);
            assertEquals(memberAccountAccount, penaltyLine.getAccount());
            assertEquals(BigDecimal.ZERO, penaltyLine.getDebitAmount());
            assertEquals(penaltyPortion, penaltyLine.getCreditAmount());
            assertEquals("Penalty obligation settled - Repayment ID: " + repaymentId, penaltyLine.getDescription());

            // Verify balanced entry
            BigDecimal totalDebits = captured.getJournalLines().stream()
                    .map(JournalLine::getDebitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredits = captured.getJournalLines().stream()
                    .map(JournalLine::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, totalDebits.compareTo(totalCredits));
        }

        @Test
        @DisplayName("should post repayment entry after creation")
        void shouldPostRepaymentEntry() {
            LoanRepaymentEvent event = new LoanRepaymentEvent(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new BigDecimal("1000.00"), new BigDecimal("800.00"), new BigDecimal("200.00"), BigDecimal.ZERO, "admin"
            );

            setupAccountLookup("1001", cashAccount);
            setupAccountLookup("1002", loanReceivableAccount);
            setupAccountLookup("1003", interestReceivableAccount);
            UUID entryId = UUID.randomUUID();
            JournalEntry createdEntry = new JournalEntry();
            createdEntry.setId(entryId);
            when(ledgerService.createJournalEntry(any(JournalEntry.class))).thenReturn(createdEntry);

            financialEventListener.handleLoanRepayment(event);

            verify(ledgerService).postEntry(entryId);
        }
    }

    @Nested
    @DisplayName("handlePayoutProcessed")
    class HandlePayoutProcessedTests {

        @Test
        @DisplayName("should create journal entry for payout processed")
        void shouldCreateJournalEntryForPayout() {
            UUID payoutId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            BigDecimal amount = new BigDecimal("10000.00");
            PayoutProcessedEvent event = new PayoutProcessedEvent(
                    payoutId, memberId, amount, "MERRY_GO_ROUND", "admin"
            );

            setupAccountLookup("2001", memberSharesAccount);
            setupAccountLookup("1001", cashAccount);
            setupLedgerService();

            financialEventListener.handlePayoutProcessed(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            assertEquals(TransactionType.PAYOUT, captured.getTransactionType());
            assertEquals(payoutId, captured.getReferenceId());
            assertEquals("Payout processed to member - MERRY_GO_ROUND", captured.getDescription());
            assertEquals(2, captured.getJournalLines().size());

            // Verify debit to Member Shares
            JournalLine debitLine = captured.getJournalLines().get(0);
            assertEquals(memberSharesAccount, debitLine.getAccount());
            assertEquals(amount, debitLine.getDebitAmount());
            assertEquals(BigDecimal.ZERO, debitLine.getCreditAmount());

            // Verify credit to Cash
            JournalLine creditLine = captured.getJournalLines().get(1);
            assertEquals(cashAccount, creditLine.getAccount());
            assertEquals(BigDecimal.ZERO, creditLine.getDebitAmount());
            assertEquals(amount, creditLine.getCreditAmount());
        }

        @Test
        @DisplayName("should create balanced payout entry")
        void shouldCreateBalancedPayoutEntry() {
            BigDecimal amount = new BigDecimal("15000.00");
            PayoutProcessedEvent event = new PayoutProcessedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), amount, "AD_HOC", "admin"
            );

            setupAccountLookup("2001", memberSharesAccount);
            setupAccountLookup("1001", cashAccount);
            setupLedgerService();

            financialEventListener.handlePayoutProcessed(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            BigDecimal totalDebits = captured.getJournalLines().stream()
                    .map(JournalLine::getDebitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredits = captured.getJournalLines().stream()
                    .map(JournalLine::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, totalDebits.compareTo(totalCredits));
        }

        @Test
        @DisplayName("should include payout type in journal line descriptions")
        void shouldIncludePayoutTypeInDescriptions() {
            UUID payoutId = UUID.randomUUID();
            PayoutProcessedEvent event = new PayoutProcessedEvent(
                    payoutId, UUID.randomUUID(), new BigDecimal("3000.00"), "DIVIDEND", "admin"
            );

            setupAccountLookup("2001", memberSharesAccount);
            setupAccountLookup("1001", cashAccount);
            setupLedgerService();

            financialEventListener.handlePayoutProcessed(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            JournalLine debitLine = captured.getJournalLines().get(0);
            assertEquals("Payout - DIVIDEND - ID: " + payoutId, debitLine.getDescription());

            JournalLine creditLine = captured.getJournalLines().get(1);
            assertEquals("Cash paid - DIVIDEND - ID: " + payoutId, creditLine.getDescription());
        }

        @Test
        @DisplayName("should post payout entry after creation")
        void shouldPostPayoutEntry() {
            PayoutProcessedEvent event = new PayoutProcessedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("5000.00"), "MERRY_GO_ROUND", "admin"
            );

            setupAccountLookup("2001", memberSharesAccount);
            setupAccountLookup("1001", cashAccount);
            UUID entryId = UUID.randomUUID();
            JournalEntry createdEntry = new JournalEntry();
            createdEntry.setId(entryId);
            when(ledgerService.createJournalEntry(any(JournalEntry.class))).thenReturn(createdEntry);

            financialEventListener.handlePayoutProcessed(event);

            verify(ledgerService).postEntry(entryId);
        }
    }

    @Nested
    @DisplayName("handlePenaltyApplied")
    class HandlePenaltyAppliedTests {

        @Test
        @DisplayName("should create journal entry for penalty applied")
        void shouldCreateJournalEntryForPenalty() {
            UUID penaltyId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            BigDecimal amount = new BigDecimal("500.00");
            PenaltyAppliedEvent event = new PenaltyAppliedEvent(
                    penaltyId, memberId, amount, "LATE_PAYMENT", "admin"
            );

            setupAccountLookup("2002", memberAccountAccount);
            setupAccountLookup("4003", penaltyIncomeAccount);
            setupLedgerService();

            financialEventListener.handlePenaltyApplied(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            assertEquals(TransactionType.PENALTY, captured.getTransactionType());
            assertEquals(penaltyId, captured.getReferenceId());
            assertEquals("Penalty applied - LATE_PAYMENT", captured.getDescription());
            assertEquals(2, captured.getJournalLines().size());

            // Verify debit to Member Account
            JournalLine debitLine = captured.getJournalLines().get(0);
            assertEquals(memberAccountAccount, debitLine.getAccount());
            assertEquals(amount, debitLine.getDebitAmount());
            assertEquals(BigDecimal.ZERO, debitLine.getCreditAmount());

            // Verify credit to Penalty Income
            JournalLine creditLine = captured.getJournalLines().get(1);
            assertEquals(penaltyIncomeAccount, creditLine.getAccount());
            assertEquals(BigDecimal.ZERO, creditLine.getDebitAmount());
            assertEquals(amount, creditLine.getCreditAmount());
        }

        @Test
        @DisplayName("should create balanced penalty entry")
        void shouldCreateBalancedPenaltyEntry() {
            BigDecimal amount = new BigDecimal("750.00");
            PenaltyAppliedEvent event = new PenaltyAppliedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), amount, "LATE_CONTRIBUTION", "admin"
            );

            setupAccountLookup("2002", memberAccountAccount);
            setupAccountLookup("4003", penaltyIncomeAccount);
            setupLedgerService();

            financialEventListener.handlePenaltyApplied(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            BigDecimal totalDebits = captured.getJournalLines().stream()
                    .map(JournalLine::getDebitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredits = captured.getJournalLines().stream()
                    .map(JournalLine::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, totalDebits.compareTo(totalCredits));
        }

        @Test
        @DisplayName("should include penalty type in descriptions")
        void shouldIncludePenaltyTypeInDescriptions() {
            UUID penaltyId = UUID.randomUUID();
            PenaltyAppliedEvent event = new PenaltyAppliedEvent(
                    penaltyId, UUID.randomUUID(), new BigDecimal("200.00"), "MISSED_MEETING", "admin"
            );

            setupAccountLookup("2002", memberAccountAccount);
            setupAccountLookup("4003", penaltyIncomeAccount);
            setupLedgerService();

            financialEventListener.handlePenaltyApplied(event);

            verify(ledgerService).createJournalEntry(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();

            JournalLine debitLine = captured.getJournalLines().get(0);
            assertEquals("Penalty charged - MISSED_MEETING - ID: " + penaltyId, debitLine.getDescription());

            JournalLine creditLine = captured.getJournalLines().get(1);
            assertEquals("Penalty income - MISSED_MEETING - ID: " + penaltyId, creditLine.getDescription());
        }

        @Test
        @DisplayName("should post penalty entry after creation")
        void shouldPostPenaltyEntry() {
            PenaltyAppliedEvent event = new PenaltyAppliedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("300.00"), "LATE_PAYMENT", "admin"
            );

            setupAccountLookup("2002", memberAccountAccount);
            setupAccountLookup("4003", penaltyIncomeAccount);
            UUID entryId = UUID.randomUUID();
            JournalEntry createdEntry = new JournalEntry();
            createdEntry.setId(entryId);
            when(ledgerService.createJournalEntry(any(JournalEntry.class))).thenReturn(createdEntry);

            financialEventListener.handlePenaltyApplied(event);

            verify(ledgerService).postEntry(entryId);
        }
    }

    @Nested
    @DisplayName("getAccountByCode - error handling")
    class GetAccountByCodeTests {

        @Test
        @DisplayName("should throw ResourceNotFoundException when account code not found")
        void shouldThrowWhenAccountCodeNotFound() {
            when(accountRepository.findByAccountCode("1001")).thenReturn(Optional.empty());

            ContributionReceivedEvent event = new ContributionReceivedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1000.00"), "REF-001", "admin"
            );

            assertThrows(ResourceNotFoundException.class,
                    () -> financialEventListener.handleContributionReceived(event));
        }

        @Test
        @DisplayName("should throw when second account code not found")
        void shouldThrowWhenSecondAccountNotFound() {
            setupAccountLookup("1001", cashAccount);
            when(accountRepository.findByAccountCode("2001")).thenReturn(Optional.empty());

            ContributionReceivedEvent event = new ContributionReceivedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1000.00"), "REF-001", "admin"
            );

            assertThrows(ResourceNotFoundException.class,
                    () -> financialEventListener.handleContributionReceived(event));
        }
    }

    // --- Helper methods ---

    private Account createAccount(String code, String name, AccountType type) {
        Account account = new Account(code, name, type, BigDecimal.ZERO, name + " account", true);
        account.setId(UUID.randomUUID());
        return account;
    }

    private void setupAccountLookup(String accountCode, Account account) {
        when(accountRepository.findByAccountCode(accountCode)).thenReturn(Optional.of(account));
    }

    private void setupLedgerService() {
        when(ledgerService.createJournalEntry(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(UUID.randomUUID());
            return entry;
        });
    }
}
