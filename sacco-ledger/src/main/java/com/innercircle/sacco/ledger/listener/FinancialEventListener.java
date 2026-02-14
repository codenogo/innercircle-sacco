package com.innercircle.sacco.ledger.listener;

import com.innercircle.sacco.common.event.ContributionReceivedEvent;
import com.innercircle.sacco.common.event.LoanDisbursedEvent;
import com.innercircle.sacco.common.event.LoanRepaymentEvent;
import com.innercircle.sacco.common.event.PayoutProcessedEvent;
import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.JournalEntry;
import com.innercircle.sacco.ledger.entity.JournalLine;
import com.innercircle.sacco.ledger.entity.TransactionType;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import com.innercircle.sacco.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialEventListener {

    private final LedgerService ledgerService;
    private final AccountRepository accountRepository;

    // Standard account codes
    private static final String ACCOUNT_CASH = "1001";
    private static final String ACCOUNT_MEMBER_SHARES = "2001";
    private static final String ACCOUNT_LOAN_RECEIVABLE = "1002";
    private static final String ACCOUNT_INTEREST_INCOME = "4001";
    private static final String ACCOUNT_CONTRIBUTION_INCOME = "4002";
    private static final String ACCOUNT_PENALTY_INCOME = "4003";
    private static final String ACCOUNT_MEMBER_ACCOUNT = "2002";

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleContributionReceived(ContributionReceivedEvent event) {
        log.info("Posting contribution to ledger: {}", event.contributionId());

        Account cashAccount = getAccountByCode(ACCOUNT_CASH);
        Account memberSharesAccount = getAccountByCode(ACCOUNT_MEMBER_SHARES);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Contribution received from member");
        entry.setTransactionType(TransactionType.CONTRIBUTION);
        entry.setReferenceId(event.contributionId());

        // DR Cash
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(cashAccount);
        debitLine.setDebitAmount(event.amount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Cash received - Contribution " + event.referenceNumber());
        entry.addJournalLine(debitLine);

        // CR Member Shares
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(memberSharesAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(event.amount());
        creditLine.setDescription("Member shares - " + event.referenceNumber());
        entry.addJournalLine(creditLine);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLoanDisbursed(LoanDisbursedEvent event) {
        log.info("Posting loan disbursement to ledger: {}", event.loanId());

        Account loanReceivableAccount = getAccountByCode(ACCOUNT_LOAN_RECEIVABLE);
        Account cashAccount = getAccountByCode(ACCOUNT_CASH);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Loan disbursed to member");
        entry.setTransactionType(TransactionType.LOAN_DISBURSEMENT);
        entry.setReferenceId(event.loanId());

        // DR Loans Receivable
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(loanReceivableAccount);
        debitLine.setDebitAmount(event.principalAmount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Loan disbursed - Loan ID: " + event.loanId());
        entry.addJournalLine(debitLine);

        // CR Cash
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(cashAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(event.principalAmount());
        creditLine.setDescription("Cash disbursed - Loan ID: " + event.loanId());
        entry.addJournalLine(creditLine);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLoanRepayment(LoanRepaymentEvent event) {
        log.info("Posting loan repayment to ledger: {}", event.repaymentId());

        Account cashAccount = getAccountByCode(ACCOUNT_CASH);
        Account loanReceivableAccount = getAccountByCode(ACCOUNT_LOAN_RECEIVABLE);
        Account interestIncomeAccount = getAccountByCode(ACCOUNT_INTEREST_INCOME);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Loan repayment received");
        entry.setTransactionType(TransactionType.LOAN_REPAYMENT);
        entry.setReferenceId(event.repaymentId());

        // DR Cash (total amount)
        JournalLine debitCash = new JournalLine();
        debitCash.setAccount(cashAccount);
        debitCash.setDebitAmount(event.amount());
        debitCash.setCreditAmount(BigDecimal.ZERO);
        debitCash.setDescription("Loan repayment received - Repayment ID: " + event.repaymentId());
        entry.addJournalLine(debitCash);

        // CR Loans Receivable (principal portion)
        JournalLine creditPrincipal = new JournalLine();
        creditPrincipal.setAccount(loanReceivableAccount);
        creditPrincipal.setDebitAmount(BigDecimal.ZERO);
        creditPrincipal.setCreditAmount(event.principalPortion());
        creditPrincipal.setDescription("Principal repayment - Repayment ID: " + event.repaymentId());
        entry.addJournalLine(creditPrincipal);

        // CR Interest Income (interest portion)
        if (event.interestPortion().compareTo(BigDecimal.ZERO) > 0) {
            JournalLine creditInterest = new JournalLine();
            creditInterest.setAccount(interestIncomeAccount);
            creditInterest.setDebitAmount(BigDecimal.ZERO);
            creditInterest.setCreditAmount(event.interestPortion());
            creditInterest.setDescription("Interest income - Repayment ID: " + event.repaymentId());
            entry.addJournalLine(creditInterest);
        }

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePayoutProcessed(PayoutProcessedEvent event) {
        log.info("Posting payout to ledger: {}", event.payoutId());

        Account memberSharesAccount = getAccountByCode(ACCOUNT_MEMBER_SHARES);
        Account cashAccount = getAccountByCode(ACCOUNT_CASH);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Payout processed to member - " + event.payoutType());
        entry.setTransactionType(TransactionType.PAYOUT);
        entry.setReferenceId(event.payoutId());

        // DR Member Shares
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(memberSharesAccount);
        debitLine.setDebitAmount(event.amount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Payout - " + event.payoutType() + " - ID: " + event.payoutId());
        entry.addJournalLine(debitLine);

        // CR Cash
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(cashAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(event.amount());
        creditLine.setDescription("Cash paid - " + event.payoutType() + " - ID: " + event.payoutId());
        entry.addJournalLine(creditLine);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePenaltyApplied(PenaltyAppliedEvent event) {
        log.info("Posting penalty to ledger: {}", event.penaltyId());

        Account memberAccountAccount = getAccountByCode(ACCOUNT_MEMBER_ACCOUNT);
        Account penaltyIncomeAccount = getAccountByCode(ACCOUNT_PENALTY_INCOME);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Penalty applied - " + event.penaltyType());
        entry.setTransactionType(TransactionType.PENALTY);
        entry.setReferenceId(event.penaltyId());

        // DR Member Account
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(memberAccountAccount);
        debitLine.setDebitAmount(event.amount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Penalty charged - " + event.penaltyType() + " - ID: " + event.penaltyId());
        entry.addJournalLine(debitLine);

        // CR Penalty Income
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(penaltyIncomeAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(event.amount());
        creditLine.setDescription("Penalty income - " + event.penaltyType() + " - ID: " + event.penaltyId());
        entry.addJournalLine(creditLine);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    private Account getAccountByCode(String accountCode) {
        return accountRepository.findByAccountCode(accountCode)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountCode));
    }
}
