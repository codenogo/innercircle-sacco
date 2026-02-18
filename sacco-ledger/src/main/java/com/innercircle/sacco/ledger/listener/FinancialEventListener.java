package com.innercircle.sacco.ledger.listener;

import com.innercircle.sacco.common.event.BenefitsDistributedEvent;
import com.innercircle.sacco.common.event.ContributionReceivedEvent;
import com.innercircle.sacco.common.event.ContributionReversedEvent;
import com.innercircle.sacco.common.event.LoanDisbursedEvent;
import com.innercircle.sacco.common.event.LoanInterestAccrualEvent;
import com.innercircle.sacco.common.event.LoanRepaymentEvent;
import com.innercircle.sacco.common.event.LoanReversalEvent;
import com.innercircle.sacco.common.event.PayoutProcessedEvent;
import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.common.event.PenaltyWaivedEvent;
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
import org.springframework.context.event.EventListener;

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
    private static final String ACCOUNT_INTEREST_RECEIVABLE = "1003";
    private static final String ACCOUNT_MEMBER_ACCOUNT = "2002";
    private static final String ACCOUNT_BAD_DEBT_EXPENSE = "5003";

    @EventListener
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

    @EventListener
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

    @EventListener
    public void handleLoanRepayment(LoanRepaymentEvent event) {
        log.info("Posting loan repayment to ledger: {}", event.repaymentId());

        Account cashAccount = getAccountByCode(ACCOUNT_CASH);
        Account loanReceivableAccount = getAccountByCode(ACCOUNT_LOAN_RECEIVABLE);
        Account interestReceivableAccount = getAccountByCode(ACCOUNT_INTEREST_RECEIVABLE);

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

        // CR Interest Receivable (interest portion) - settles the accrual from monthly batch
        if (event.interestPortion().compareTo(BigDecimal.ZERO) > 0) {
            JournalLine creditInterest = new JournalLine();
            creditInterest.setAccount(interestReceivableAccount);
            creditInterest.setDebitAmount(BigDecimal.ZERO);
            creditInterest.setCreditAmount(event.interestPortion());
            creditInterest.setDescription("Interest receivable settled - Repayment ID: " + event.repaymentId());
            entry.addJournalLine(creditInterest);
        }

        // CR Member Account (penalty portion) - settles the obligation created at penalty application
        if (event.penaltyPortion() != null && event.penaltyPortion().compareTo(BigDecimal.ZERO) > 0) {
            Account memberAccount = getAccountByCode(ACCOUNT_MEMBER_ACCOUNT);
            JournalLine creditPenalty = new JournalLine();
            creditPenalty.setAccount(memberAccount);
            creditPenalty.setDebitAmount(BigDecimal.ZERO);
            creditPenalty.setCreditAmount(event.penaltyPortion());
            creditPenalty.setDescription("Penalty obligation settled - Repayment ID: " + event.repaymentId());
            entry.addJournalLine(creditPenalty);
        }

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @EventListener
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

    @EventListener
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

    @EventListener
    public void handleInterestAccrual(LoanInterestAccrualEvent event) {
        log.info("Posting interest accrual to ledger: loan {}", event.loanId());

        Account interestReceivableAccount = getAccountByCode(ACCOUNT_INTEREST_RECEIVABLE);
        Account interestIncomeAccount = getAccountByCode(ACCOUNT_INTEREST_INCOME);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(event.accrualDate());
        entry.setDescription("Monthly interest accrual - Loan ID: " + event.loanId());
        entry.setTransactionType(TransactionType.INTEREST_ACCRUAL);
        entry.setReferenceId(event.loanId());

        // DR Interest Receivable
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(interestReceivableAccount);
        debitLine.setDebitAmount(event.interestAmount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Interest accrual - Loan ID: " + event.loanId());
        entry.addJournalLine(debitLine);

        // CR Interest Income
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(interestIncomeAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(event.interestAmount());
        creditLine.setDescription("Interest income accrual - Loan ID: " + event.loanId());
        entry.addJournalLine(creditLine);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @EventListener
    public void handleLoanReversal(LoanReversalEvent event) {
        log.info("Posting loan reversal to ledger: {}", event.reversalId());

        Account loanReceivableAccount = getAccountByCode(ACCOUNT_LOAN_RECEIVABLE);
        Account cashAccount = getAccountByCode(ACCOUNT_CASH);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Loan repayment reversal - " + event.reversalType());
        entry.setTransactionType(TransactionType.LOAN_REVERSAL);
        entry.setReferenceId(event.reversalId());

        // DR Loan Receivable (principal portion)
        JournalLine debitPrincipal = new JournalLine();
        debitPrincipal.setAccount(loanReceivableAccount);
        debitPrincipal.setDebitAmount(event.principalPortion());
        debitPrincipal.setCreditAmount(BigDecimal.ZERO);
        debitPrincipal.setDescription("Loan receivable restored - Reversal ID: " + event.reversalId());
        entry.addJournalLine(debitPrincipal);

        // DR Interest Receivable (interest portion, skip if zero)
        if (event.interestPortion().compareTo(BigDecimal.ZERO) > 0) {
            Account interestReceivableAccount = getAccountByCode(ACCOUNT_INTEREST_RECEIVABLE);
            JournalLine debitInterest = new JournalLine();
            debitInterest.setAccount(interestReceivableAccount);
            debitInterest.setDebitAmount(event.interestPortion());
            debitInterest.setCreditAmount(BigDecimal.ZERO);
            debitInterest.setDescription("Interest receivable restored - Reversal ID: " + event.reversalId());
            entry.addJournalLine(debitInterest);
        }

        // DR Member Account (penalty portion, skip if zero)
        if (event.penaltyPortion() != null && event.penaltyPortion().compareTo(BigDecimal.ZERO) > 0) {
            Account memberAccount = getAccountByCode(ACCOUNT_MEMBER_ACCOUNT);
            JournalLine debitPenalty = new JournalLine();
            debitPenalty.setAccount(memberAccount);
            debitPenalty.setDebitAmount(event.penaltyPortion());
            debitPenalty.setCreditAmount(BigDecimal.ZERO);
            debitPenalty.setDescription("Penalty obligation restored - Reversal ID: " + event.reversalId());
            entry.addJournalLine(debitPenalty);
        }

        // CR Cash (total amount)
        JournalLine creditCash = new JournalLine();
        creditCash.setAccount(cashAccount);
        creditCash.setDebitAmount(BigDecimal.ZERO);
        creditCash.setCreditAmount(event.amount());
        creditCash.setDescription("Cash reversed - Reversal ID: " + event.reversalId());
        entry.addJournalLine(creditCash);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @EventListener
    public void handleContributionReversed(ContributionReversedEvent event) {
        log.info("Posting contribution reversal to ledger: {}", event.contributionId());

        Account memberSharesAccount = getAccountByCode(ACCOUNT_MEMBER_SHARES);
        Account cashAccount = getAccountByCode(ACCOUNT_CASH);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Contribution reversal - " + event.referenceNumber());
        entry.setTransactionType(TransactionType.CONTRIBUTION_REVERSAL);
        entry.setReferenceId(event.contributionId());

        // DR Member Shares
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(memberSharesAccount);
        debitLine.setDebitAmount(event.amount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Member shares reversed - " + event.referenceNumber());
        entry.addJournalLine(debitLine);

        // CR Cash
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(cashAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(event.amount());
        creditLine.setDescription("Cash reversed - " + event.referenceNumber());
        entry.addJournalLine(creditLine);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @EventListener
    public void handlePenaltyWaived(PenaltyWaivedEvent event) {
        log.info("Posting penalty waiver to ledger: {}", event.penaltyId());

        Account badDebtExpenseAccount = getAccountByCode(ACCOUNT_BAD_DEBT_EXPENSE);
        Account memberAccount = getAccountByCode(ACCOUNT_MEMBER_ACCOUNT);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Penalty waived - " + event.reason());
        entry.setTransactionType(TransactionType.PENALTY_WAIVER);
        entry.setReferenceId(event.penaltyId());

        // DR Bad Debt Expense
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(badDebtExpenseAccount);
        debitLine.setDebitAmount(event.amount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Bad debt expense - Penalty waived ID: " + event.penaltyId());
        entry.addJournalLine(debitLine);

        // CR Member Account
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(memberAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(event.amount());
        creditLine.setDescription("Member obligation relieved - Penalty waived ID: " + event.penaltyId());
        entry.addJournalLine(creditLine);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @EventListener
    public void handleBenefitsDistributed(BenefitsDistributedEvent event) {
        log.info("Posting benefits distribution to ledger: loan {}", event.loanId());

        Account interestIncomeAccount = getAccountByCode(ACCOUNT_INTEREST_INCOME);
        Account memberAccount = getAccountByCode(ACCOUNT_MEMBER_ACCOUNT);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Benefits distributed - " + event.beneficiaryCount() + " beneficiaries");
        entry.setTransactionType(TransactionType.BENEFIT_DISTRIBUTION);
        entry.setReferenceId(event.loanId());

        // DR Interest Income
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(interestIncomeAccount);
        debitLine.setDebitAmount(event.totalInterestAmount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Interest income distributed - Loan ID: " + event.loanId());
        entry.addJournalLine(debitLine);

        // CR Member Account
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(memberAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(event.totalInterestAmount());
        creditLine.setDescription("Benefits credited - Loan ID: " + event.loanId());
        entry.addJournalLine(creditLine);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    private Account getAccountByCode(String accountCode) {
        return accountRepository.findByAccountCode(accountCode)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountCode));
    }
}
