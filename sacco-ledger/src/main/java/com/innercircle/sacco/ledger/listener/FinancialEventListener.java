package com.innercircle.sacco.ledger.listener;

import com.innercircle.sacco.common.event.BenefitsDistributedEvent;
import com.innercircle.sacco.common.event.ContributionReceivedEvent;
import com.innercircle.sacco.common.event.ContributionReversedEvent;
import com.innercircle.sacco.common.event.ExitFeeAppliedEvent;
import com.innercircle.sacco.common.event.InvestmentActivatedEvent;
import com.innercircle.sacco.common.event.InvestmentDisposedEvent;
import com.innercircle.sacco.common.event.InvestmentIncomeRecordedEvent;
import com.innercircle.sacco.common.event.LoanDisbursedEvent;
import com.innercircle.sacco.common.event.LoanInterestAccrualEvent;
import com.innercircle.sacco.common.event.LoanRepaymentEvent;
import com.innercircle.sacco.common.event.LoanReversalEvent;
import com.innercircle.sacco.common.event.PayoutProcessedEvent;
import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.common.event.PenaltyWaivedEvent;
import com.innercircle.sacco.common.event.PettyCashWorkflowEvent;
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
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialEventListener {

    private final LedgerService ledgerService;
    private final AccountRepository accountRepository;

    // Standard account codes
    private static final String ACCOUNT_CASH = "1001";
    private static final String ACCOUNT_MEMBER_SHARES = "2001";
    private static final String ACCOUNT_WELFARE_FUND_LIABILITY = "2003";
    private static final String ACCOUNT_LOAN_RECEIVABLE = "1002";
    private static final String ACCOUNT_INTEREST_INCOME = "4001";
    private static final String ACCOUNT_CONTRIBUTION_INCOME = "4002";
    private static final String ACCOUNT_PENALTY_INCOME = "4003";
    private static final String ACCOUNT_INTEREST_RECEIVABLE = "1003";
    private static final String ACCOUNT_MEMBER_ACCOUNT = "2002";
    private static final String ACCOUNT_BAD_DEBT_EXPENSE = "5003";
    private static final String ACCOUNT_PETTY_CASH_FLOAT = "1004";
    private static final String ACCOUNT_INVESTMENTS = "1005";
    private static final String ACCOUNT_OPERATING_EXPENSE = "5001";
    private static final String ACCOUNT_ADMIN_EXPENSE = "5002";
    private static final String ACCOUNT_INVESTMENT_INCOME = "4004";
    private static final String ACCOUNT_EXIT_FEE_INCOME = "4005";

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
        creditLine.setCreditAmount(event.contributionAmount());
        creditLine.setDescription("Member shares - " + event.referenceNumber());
        entry.addJournalLine(creditLine);

        if (event.welfareAmount().compareTo(BigDecimal.ZERO) > 0) {
            Account welfareFundLiability = getAccountByCode(ACCOUNT_WELFARE_FUND_LIABILITY);

            // CR Welfare Fund Liability
            JournalLine welfareCreditLine = new JournalLine();
            welfareCreditLine.setAccount(welfareFundLiability);
            welfareCreditLine.setDebitAmount(BigDecimal.ZERO);
            welfareCreditLine.setCreditAmount(event.welfareAmount());
            welfareCreditLine.setDescription("Welfare fund allocation - " + event.referenceNumber());
            entry.addJournalLine(welfareCreditLine);
        }

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

        Account cashAccount = getAccountByCode(ACCOUNT_CASH);
        Account debitAccount = resolvePayoutDebitAccount(event.payoutType());

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Payout processed to member - " + event.payoutType());
        entry.setTransactionType(TransactionType.PAYOUT);
        entry.setReferenceId(event.payoutId());

        // DR payout source liability
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(debitAccount);
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

    private Account resolvePayoutDebitAccount(String payoutTypeRaw) {
        String payoutType = payoutTypeRaw == null ? "" : payoutTypeRaw.trim().toUpperCase(Locale.ROOT);
        return switch (payoutType) {
            case "WELFARE_BENEFIT" -> getAccountByCode(ACCOUNT_WELFARE_FUND_LIABILITY);
            case "EXIT_SETTLEMENT", "DIVIDEND", "MERRY_GO_ROUND", "AD_HOC", "" -> getAccountByCode(ACCOUNT_MEMBER_SHARES);
            default -> getAccountByCode(ACCOUNT_MEMBER_SHARES);
        };
    }

    @EventListener
    public void handleInvestmentActivated(InvestmentActivatedEvent event) {
        log.info("Posting investment activation to ledger: {}", event.investmentId());

        Account investmentAccount = getAccountByCode(ACCOUNT_INVESTMENTS);
        Account cashAccount = getAccountByCode(ACCOUNT_CASH);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Investment activated - " + event.referenceNumber());
        entry.setTransactionType(TransactionType.INVESTMENT_PURCHASE);
        entry.setReferenceId(event.investmentId());

        // DR Investments
        JournalLine debitInvestment = new JournalLine();
        debitInvestment.setAccount(investmentAccount);
        debitInvestment.setDebitAmount(event.amount());
        debitInvestment.setCreditAmount(BigDecimal.ZERO);
        debitInvestment.setDescription("Investment asset recorded - " + event.referenceNumber());
        entry.addJournalLine(debitInvestment);

        // CR Cash
        JournalLine creditCash = new JournalLine();
        creditCash.setAccount(cashAccount);
        creditCash.setDebitAmount(BigDecimal.ZERO);
        creditCash.setCreditAmount(event.amount());
        creditCash.setDescription("Cash deployed to investment - " + event.referenceNumber());
        entry.addJournalLine(creditCash);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @EventListener
    public void handleInvestmentIncomeRecorded(InvestmentIncomeRecordedEvent event) {
        log.info("Posting investment income to ledger: {}", event.incomeId());

        Account cashAccount = getAccountByCode(ACCOUNT_CASH);
        Account investmentIncomeAccount = getAccountByCode(ACCOUNT_INVESTMENT_INCOME);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Investment income received - " + event.incomeType());
        entry.setTransactionType(TransactionType.INVESTMENT_INCOME);
        entry.setReferenceId(event.incomeId());

        // DR Cash
        JournalLine debitCash = new JournalLine();
        debitCash.setAccount(cashAccount);
        debitCash.setDebitAmount(event.amount());
        debitCash.setCreditAmount(BigDecimal.ZERO);
        debitCash.setDescription("Investment income received - " + event.referenceNumber());
        entry.addJournalLine(debitCash);

        // CR Investment Income
        JournalLine creditIncome = new JournalLine();
        creditIncome.setAccount(investmentIncomeAccount);
        creditIncome.setDebitAmount(BigDecimal.ZERO);
        creditIncome.setCreditAmount(event.amount());
        creditIncome.setDescription("Investment income recognized - " + event.incomeType());
        entry.addJournalLine(creditIncome);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    @EventListener
    public void handleInvestmentDisposed(InvestmentDisposedEvent event) {
        log.info("Posting investment disposal to ledger: {}", event.investmentId());

        Account cashAccount = getAccountByCode(ACCOUNT_CASH);
        Account investmentAccount = getAccountByCode(ACCOUNT_INVESTMENTS);
        Account expenseAccount = getAccountByCode(ACCOUNT_OPERATING_EXPENSE);

        BigDecimal proceeds = event.proceedsAmount();
        BigDecimal fees = event.fees() != null ? event.fees() : BigDecimal.ZERO;

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Investment disposed - " + event.disposalType());
        entry.setTransactionType(TransactionType.INVESTMENT_DISPOSAL);
        entry.setReferenceId(event.investmentId());

        if (proceeds.compareTo(BigDecimal.ZERO) > 0) {
            // DR Cash
            JournalLine debitCash = new JournalLine();
            debitCash.setAccount(cashAccount);
            debitCash.setDebitAmount(proceeds);
            debitCash.setCreditAmount(BigDecimal.ZERO);
            debitCash.setDescription("Investment proceeds received - " + event.referenceNumber());
            entry.addJournalLine(debitCash);

            // CR Investments
            JournalLine creditInvestment = new JournalLine();
            creditInvestment.setAccount(investmentAccount);
            creditInvestment.setDebitAmount(BigDecimal.ZERO);
            creditInvestment.setCreditAmount(proceeds);
            creditInvestment.setDescription("Investment asset derecognized - " + event.referenceNumber());
            entry.addJournalLine(creditInvestment);
        }

        if (fees.compareTo(BigDecimal.ZERO) > 0) {
            // DR Expense
            JournalLine debitExpense = new JournalLine();
            debitExpense.setAccount(expenseAccount);
            debitExpense.setDebitAmount(fees);
            debitExpense.setCreditAmount(BigDecimal.ZERO);
            debitExpense.setDescription("Investment disposal fees - " + event.referenceNumber());
            entry.addJournalLine(debitExpense);

            // CR Cash
            JournalLine creditCash = new JournalLine();
            creditCash.setAccount(cashAccount);
            creditCash.setDebitAmount(BigDecimal.ZERO);
            creditCash.setCreditAmount(fees);
            creditCash.setDescription("Cash paid for disposal fees - " + event.referenceNumber());
            entry.addJournalLine(creditCash);
        }

        if (!entry.getJournalLines().isEmpty()) {
            JournalEntry created = ledgerService.createJournalEntry(entry);
            ledgerService.postEntry(created.getId());
        }
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
        debitLine.setDebitAmount(event.contributionAmount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Member shares reversed - " + event.referenceNumber());
        entry.addJournalLine(debitLine);

        if (event.welfareAmount().compareTo(BigDecimal.ZERO) > 0) {
            Account welfareFundLiability = getAccountByCode(ACCOUNT_WELFARE_FUND_LIABILITY);

            // DR Welfare Fund Liability
            JournalLine welfareDebitLine = new JournalLine();
            welfareDebitLine.setAccount(welfareFundLiability);
            welfareDebitLine.setDebitAmount(event.welfareAmount());
            welfareDebitLine.setCreditAmount(BigDecimal.ZERO);
            welfareDebitLine.setDescription("Welfare fund reversal - " + event.referenceNumber());
            entry.addJournalLine(welfareDebitLine);
        }

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

    @EventListener
    public void handlePettyCashWorkflow(PettyCashWorkflowEvent event) {
        if ("DISBURSED".equals(event.action())) {
            log.info("Posting petty cash disbursement to ledger: {}", event.voucherId());

            Account pettyCashFloatAccount = getAccountByCode(ACCOUNT_PETTY_CASH_FLOAT);
            Account cashAccount = getAccountByCode(ACCOUNT_CASH);

            JournalEntry entry = new JournalEntry();
            entry.setTransactionDate(LocalDate.now());
            entry.setDescription("Petty cash disbursed - " + event.referenceNumber());
            entry.setTransactionType(TransactionType.PETTY_CASH_DISBURSEMENT);
            entry.setReferenceId(event.voucherId());

            // DR Petty Cash Float
            JournalLine debitLine = new JournalLine();
            debitLine.setAccount(pettyCashFloatAccount);
            debitLine.setDebitAmount(event.amount());
            debitLine.setCreditAmount(BigDecimal.ZERO);
            debitLine.setDescription("Petty cash float funded - " + event.referenceNumber());
            entry.addJournalLine(debitLine);

            // CR Cash
            JournalLine creditLine = new JournalLine();
            creditLine.setAccount(cashAccount);
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(event.amount());
            creditLine.setDescription("Cash paid out - " + event.referenceNumber());
            entry.addJournalLine(creditLine);

            JournalEntry created = ledgerService.createJournalEntry(entry);
            ledgerService.postEntry(created.getId());
            return;
        }

        if ("SETTLED".equals(event.action())) {
            log.info("Posting petty cash settlement to ledger: {}", event.voucherId());

            Account expenseAccount = getAccountByCode(resolvePettyCashExpenseAccountCode(event.expenseType()));
            Account pettyCashFloatAccount = getAccountByCode(ACCOUNT_PETTY_CASH_FLOAT);

            JournalEntry entry = new JournalEntry();
            entry.setTransactionDate(LocalDate.now());
            entry.setDescription("Petty cash settled - " + event.referenceNumber());
            entry.setTransactionType(TransactionType.PETTY_CASH_SETTLEMENT);
            entry.setReferenceId(event.voucherId());

            // DR Expense account
            JournalLine debitLine = new JournalLine();
            debitLine.setAccount(expenseAccount);
            debitLine.setDebitAmount(event.amount());
            debitLine.setCreditAmount(BigDecimal.ZERO);
            debitLine.setDescription("Petty cash expense - " + event.referenceNumber());
            entry.addJournalLine(debitLine);

            // CR Petty Cash Float
            JournalLine creditLine = new JournalLine();
            creditLine.setAccount(pettyCashFloatAccount);
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(event.amount());
            creditLine.setDescription("Petty cash float cleared - " + event.referenceNumber());
            entry.addJournalLine(creditLine);

            JournalEntry created = ledgerService.createJournalEntry(entry);
            ledgerService.postEntry(created.getId());
        }
    }

    @EventListener
    public void handleExitFeeApplied(ExitFeeAppliedEvent event) {
        log.info("Posting exit fee to ledger: {}", event.exitRequestId());

        Account memberSharesAccount = getAccountByCode(ACCOUNT_MEMBER_SHARES);
        Account exitFeeIncomeAccount = getAccountByCode(ACCOUNT_EXIT_FEE_INCOME);

        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Member exit fee applied");
        entry.setTransactionType(TransactionType.EXIT_FEE);
        entry.setReferenceId(event.exitRequestId());

        // DR Member Shares liability
        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(memberSharesAccount);
        debitLine.setDebitAmount(event.amount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Exit fee deduction from member shares");
        entry.addJournalLine(debitLine);

        // CR Exit Fee Income
        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(exitFeeIncomeAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(event.amount());
        creditLine.setDescription("Exit fee income recognized");
        entry.addJournalLine(creditLine);

        JournalEntry created = ledgerService.createJournalEntry(entry);
        ledgerService.postEntry(created.getId());
    }

    private Account getAccountByCode(String accountCode) {
        return accountRepository.findByAccountCode(accountCode)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountCode));
    }

    private String resolvePettyCashExpenseAccountCode(String expenseType) {
        if (expenseType == null) {
            return ACCOUNT_OPERATING_EXPENSE;
        }

        return switch (expenseType.toUpperCase(Locale.ROOT)) {
            case "ADMINISTRATION" -> ACCOUNT_ADMIN_EXPENSE;
            default -> ACCOUNT_OPERATING_EXPENSE;
        };
    }
}
