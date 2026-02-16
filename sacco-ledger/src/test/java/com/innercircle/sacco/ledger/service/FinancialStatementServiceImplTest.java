package com.innercircle.sacco.ledger.service;

import com.innercircle.sacco.ledger.dto.BalanceSheetResponse;
import com.innercircle.sacco.ledger.dto.IncomeStatementResponse;
import com.innercircle.sacco.ledger.dto.TrialBalanceResponse;
import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.AccountType;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialStatementServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private FinancialStatementServiceImpl financialStatementService;

    private Account cashAccount;
    private Account loanReceivableAccount;
    private Account memberSharesAccount;
    private Account equityAccount;
    private Account interestIncomeAccount;
    private Account contributionIncomeAccount;
    private Account operatingExpenseAccount;

    @BeforeEach
    void setUp() {
        cashAccount = createAccount("1001", "Cash", AccountType.ASSET, new BigDecimal("50000.00"));
        loanReceivableAccount = createAccount("1002", "Loan Receivable", AccountType.ASSET, new BigDecimal("30000.00"));
        memberSharesAccount = createAccount("2001", "Member Shares", AccountType.LIABILITY, new BigDecimal("-60000.00"));
        equityAccount = createAccount("3001", "Retained Earnings", AccountType.EQUITY, new BigDecimal("-10000.00"));
        interestIncomeAccount = createAccount("4001", "Interest Income", AccountType.REVENUE, new BigDecimal("-8000.00"));
        contributionIncomeAccount = createAccount("4002", "Contribution Income", AccountType.REVENUE, new BigDecimal("-5000.00"));
        operatingExpenseAccount = createAccount("5001", "Operating Expenses", AccountType.EXPENSE, new BigDecimal("3000.00"));
    }

    @Nested
    @DisplayName("generateTrialBalance")
    class GenerateTrialBalanceTests {

        @Test
        @DisplayName("should generate trial balance with all active accounts")
        void shouldGenerateTrialBalanceWithAllActiveAccounts() {
            LocalDate asOfDate = LocalDate.of(2026, 2, 15);
            when(accountRepository.findByActiveTrue()).thenReturn(List.of(
                    cashAccount, loanReceivableAccount, memberSharesAccount,
                    equityAccount, interestIncomeAccount, operatingExpenseAccount
            ));

            TrialBalanceResponse result = financialStatementService.generateTrialBalance(asOfDate);

            assertNotNull(result);
            assertEquals(asOfDate, result.getAsOfDate());
            assertEquals(6, result.getAccounts().size());
            verify(accountRepository).findByActiveTrue();
        }

        @Test
        @DisplayName("should classify positive balances as debit and negative as credit")
        void shouldClassifyBalancesCorrectly() {
            when(accountRepository.findByActiveTrue()).thenReturn(List.of(cashAccount, memberSharesAccount));

            TrialBalanceResponse result = financialStatementService.generateTrialBalance(LocalDate.now());

            TrialBalanceResponse.AccountBalanceDto cashBalance = result.getAccounts().stream()
                    .filter(a -> a.getAccountCode().equals("1001"))
                    .findFirst().orElseThrow();

            // Cash has positive balance (50000) -> debit
            assertEquals("50000.00", cashBalance.getDebitBalance());
            assertEquals("0.00", cashBalance.getCreditBalance());

            TrialBalanceResponse.AccountBalanceDto sharesBalance = result.getAccounts().stream()
                    .filter(a -> a.getAccountCode().equals("2001"))
                    .findFirst().orElseThrow();

            // Member Shares has negative balance (-60000) -> credit
            assertEquals("0.00", sharesBalance.getDebitBalance());
            assertEquals("60000.00", sharesBalance.getCreditBalance());
        }

        @Test
        @DisplayName("should calculate total debits and credits correctly")
        void shouldCalculateTotalsCorrectly() {
            // Positive balances: 50000 + 30000 + 3000 = 83000 (debits)
            // Negative balances: 60000 + 10000 + 8000 + 5000 = 83000 (credits)
            when(accountRepository.findByActiveTrue()).thenReturn(List.of(
                    cashAccount, loanReceivableAccount, memberSharesAccount,
                    equityAccount, interestIncomeAccount, contributionIncomeAccount,
                    operatingExpenseAccount
            ));

            TrialBalanceResponse result = financialStatementService.generateTrialBalance(LocalDate.now());

            assertEquals("83000.00", result.getTotalDebits());
            assertEquals("83000.00", result.getTotalCredits());
            assertTrue(result.isBalanced());
        }

        @Test
        @DisplayName("should report unbalanced when debits do not equal credits")
        void shouldReportUnbalancedWhenNotEqual() {
            // Only positive balances -> debits with no credits
            Account onlyAsset = createAccount("1001", "Cash", AccountType.ASSET, new BigDecimal("5000.00"));
            when(accountRepository.findByActiveTrue()).thenReturn(List.of(onlyAsset));

            TrialBalanceResponse result = financialStatementService.generateTrialBalance(LocalDate.now());

            assertEquals("5000.00", result.getTotalDebits());
            assertEquals("0", result.getTotalCredits());
            assertFalse(result.isBalanced());
        }

        @Test
        @DisplayName("should handle empty account list")
        void shouldHandleEmptyAccountList() {
            when(accountRepository.findByActiveTrue()).thenReturn(List.of());

            TrialBalanceResponse result = financialStatementService.generateTrialBalance(LocalDate.now());

            assertNotNull(result);
            assertTrue(result.getAccounts().isEmpty());
            assertEquals("0", result.getTotalDebits());
            assertEquals("0", result.getTotalCredits());
            assertTrue(result.isBalanced());
        }

        @Test
        @DisplayName("should include account type in response")
        void shouldIncludeAccountType() {
            when(accountRepository.findByActiveTrue()).thenReturn(List.of(cashAccount));

            TrialBalanceResponse result = financialStatementService.generateTrialBalance(LocalDate.now());

            assertEquals("ASSET", result.getAccounts().get(0).getAccountType());
        }

        @Test
        @DisplayName("should handle zero balance accounts")
        void shouldHandleZeroBalanceAccounts() {
            Account zeroAccount = createAccount("1003", "Petty Cash", AccountType.ASSET, BigDecimal.ZERO);
            when(accountRepository.findByActiveTrue()).thenReturn(List.of(zeroAccount));

            TrialBalanceResponse result = financialStatementService.generateTrialBalance(LocalDate.now());

            TrialBalanceResponse.AccountBalanceDto balance = result.getAccounts().get(0);
            // Zero is >= 0, so classified as debit
            assertEquals("0", balance.getDebitBalance());
            assertEquals("0.00", balance.getCreditBalance());
        }
    }

    @Nested
    @DisplayName("generateIncomeStatement")
    class GenerateIncomeStatementTests {

        @Test
        @DisplayName("should generate income statement with revenue and expenses")
        void shouldGenerateIncomeStatement() {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 31);

            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.REVENUE))
                    .thenReturn(List.of(interestIncomeAccount, contributionIncomeAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EXPENSE))
                    .thenReturn(List.of(operatingExpenseAccount));

            IncomeStatementResponse result = financialStatementService.generateIncomeStatement(startDate, endDate);

            assertNotNull(result);
            assertEquals(startDate, result.getStartDate());
            assertEquals(endDate, result.getEndDate());
            assertEquals(2, result.getRevenue().size());
            assertEquals(1, result.getExpenses().size());
        }

        @Test
        @DisplayName("should calculate total revenue as absolute values")
        void shouldCalculateTotalRevenueAsAbsoluteValues() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.REVENUE))
                    .thenReturn(List.of(interestIncomeAccount, contributionIncomeAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EXPENSE))
                    .thenReturn(List.of());

            IncomeStatementResponse result = financialStatementService.generateIncomeStatement(
                    LocalDate.now(), LocalDate.now());

            // Revenue accounts have negative balances (-8000 and -5000), abs = 8000 + 5000 = 13000
            assertEquals("13000.00", result.getTotalRevenue());
        }

        @Test
        @DisplayName("should calculate total expenses")
        void shouldCalculateTotalExpenses() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.REVENUE))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EXPENSE))
                    .thenReturn(List.of(operatingExpenseAccount));

            IncomeStatementResponse result = financialStatementService.generateIncomeStatement(
                    LocalDate.now(), LocalDate.now());

            assertEquals("3000.00", result.getTotalExpenses());
        }

        @Test
        @DisplayName("should calculate net income as revenue minus expenses")
        void shouldCalculateNetIncome() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.REVENUE))
                    .thenReturn(List.of(interestIncomeAccount, contributionIncomeAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EXPENSE))
                    .thenReturn(List.of(operatingExpenseAccount));

            IncomeStatementResponse result = financialStatementService.generateIncomeStatement(
                    LocalDate.now(), LocalDate.now());

            // Net income = 13000 - 3000 = 10000
            assertEquals("10000.00", result.getNetIncome());
        }

        @Test
        @DisplayName("should handle empty revenue and expense lists")
        void shouldHandleEmptyLists() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.REVENUE))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EXPENSE))
                    .thenReturn(List.of());

            IncomeStatementResponse result = financialStatementService.generateIncomeStatement(
                    LocalDate.now(), LocalDate.now());

            assertTrue(result.getRevenue().isEmpty());
            assertTrue(result.getExpenses().isEmpty());
            assertEquals("0", result.getTotalRevenue());
            assertEquals("0", result.getTotalExpenses());
            assertEquals("0", result.getNetIncome());
        }

        @Test
        @DisplayName("should display revenue amounts as absolute values in line items")
        void shouldDisplayRevenueAmountsAsAbsolute() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.REVENUE))
                    .thenReturn(List.of(interestIncomeAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EXPENSE))
                    .thenReturn(List.of());

            IncomeStatementResponse result = financialStatementService.generateIncomeStatement(
                    LocalDate.now(), LocalDate.now());

            // Interest income balance is -8000, abs = 8000
            assertEquals("8000.00", result.getRevenue().get(0).getAmount());
        }

        @Test
        @DisplayName("should display expense amounts as raw values in line items")
        void shouldDisplayExpenseAmountsAsRaw() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.REVENUE))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EXPENSE))
                    .thenReturn(List.of(operatingExpenseAccount));

            IncomeStatementResponse result = financialStatementService.generateIncomeStatement(
                    LocalDate.now(), LocalDate.now());

            assertEquals("3000.00", result.getExpenses().get(0).getAmount());
        }

        @Test
        @DisplayName("should handle negative net income (loss)")
        void shouldHandleNegativeNetIncome() {
            Account highExpense = createAccount("5002", "Salary Expense", AccountType.EXPENSE, new BigDecimal("20000.00"));

            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.REVENUE))
                    .thenReturn(List.of(interestIncomeAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EXPENSE))
                    .thenReturn(List.of(highExpense));

            IncomeStatementResponse result = financialStatementService.generateIncomeStatement(
                    LocalDate.now(), LocalDate.now());

            // Net income = 8000 - 20000 = -12000
            assertEquals("-12000.00", result.getNetIncome());
        }
    }

    @Nested
    @DisplayName("generateBalanceSheet")
    class GenerateBalanceSheetTests {

        @Test
        @DisplayName("should generate balance sheet with assets, liabilities, and equity")
        void shouldGenerateBalanceSheet() {
            LocalDate asOfDate = LocalDate.of(2026, 2, 15);

            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of(cashAccount, loanReceivableAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of(memberSharesAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of(equityAccount));

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(asOfDate);

            assertNotNull(result);
            assertEquals(asOfDate, result.getAsOfDate());
            assertEquals(2, result.getAssets().size());
            assertEquals(1, result.getLiabilities().size());
            assertEquals(1, result.getEquity().size());
        }

        @Test
        @DisplayName("should calculate total assets correctly")
        void shouldCalculateTotalAssets() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of(cashAccount, loanReceivableAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of());

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            // Total assets = 50000 + 30000 = 80000
            assertEquals("80000.00", result.getTotalAssets());
        }

        @Test
        @DisplayName("should calculate total liabilities as absolute values")
        void shouldCalculateTotalLiabilitiesAsAbsolute() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of(memberSharesAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of());

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            // Member Shares balance = -60000, abs = 60000
            assertEquals("60000.00", result.getTotalLiabilities());
        }

        @Test
        @DisplayName("should calculate total equity as absolute values")
        void shouldCalculateTotalEquityAsAbsolute() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of(equityAccount));

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            // Equity balance = -10000, abs = 10000
            assertEquals("10000.00", result.getTotalEquity());
        }

        @Test
        @DisplayName("should calculate total liabilities and equity sum")
        void shouldCalculateTotalLiabilitiesAndEquity() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of(memberSharesAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of(equityAccount));

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            // Total L+E = 60000 + 10000 = 70000
            assertEquals("70000.00", result.getTotalLiabilitiesAndEquity());
        }

        @Test
        @DisplayName("should report balanced when assets equal liabilities plus equity")
        void shouldReportBalancedWhenAssetsEqualLiabilitiesAndEquity() {
            // Assets: 50000 + 30000 = 80000
            // Liabilities: abs(-60000) = 60000
            // Equity: abs(-20000) = 20000
            // L+E = 80000 = Assets -> balanced
            Account equityBalanced = createAccount("3001", "Retained Earnings", AccountType.EQUITY, new BigDecimal("-20000.00"));

            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of(cashAccount, loanReceivableAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of(memberSharesAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of(equityBalanced));

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            assertTrue(result.isBalanced());
        }

        @Test
        @DisplayName("should report unbalanced when assets do not equal liabilities plus equity")
        void shouldReportUnbalancedWhenNotEqual() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of(cashAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of(memberSharesAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of(equityAccount));

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            // Assets = 50000, L+E = 60000 + 10000 = 70000 -> not balanced
            assertFalse(result.isBalanced());
        }

        @Test
        @DisplayName("should handle empty categories")
        void shouldHandleEmptyCategories() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of());

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            assertTrue(result.getAssets().isEmpty());
            assertTrue(result.getLiabilities().isEmpty());
            assertTrue(result.getEquity().isEmpty());
            assertEquals("0", result.getTotalAssets());
            assertEquals("0", result.getTotalLiabilities());
            assertEquals("0", result.getTotalEquity());
            assertEquals("0", result.getTotalLiabilitiesAndEquity());
            assertTrue(result.isBalanced());
        }

        @Test
        @DisplayName("should display liability amounts as absolute values in line items")
        void shouldDisplayLiabilityAmountsAsAbsolute() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of(memberSharesAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of());

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            assertEquals("60000.00", result.getLiabilities().get(0).getAmount());
        }

        @Test
        @DisplayName("should display equity amounts as absolute values in line items")
        void shouldDisplayEquityAmountsAsAbsolute() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of(equityAccount));

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            assertEquals("10000.00", result.getEquity().get(0).getAmount());
        }

        @Test
        @DisplayName("should display asset amounts as raw values in line items")
        void shouldDisplayAssetAmountsAsRaw() {
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET))
                    .thenReturn(List.of(cashAccount));
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY))
                    .thenReturn(List.of());
            when(accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY))
                    .thenReturn(List.of());

            BalanceSheetResponse result = financialStatementService.generateBalanceSheet(LocalDate.now());

            assertEquals("50000.00", result.getAssets().get(0).getAmount());
        }
    }

    // --- Helper methods ---

    private Account createAccount(String code, String name, AccountType type, BigDecimal balance) {
        Account account = new Account(code, name, type, balance, name + " description", true, null, null);
        account.setId(UUID.randomUUID());
        return account;
    }
}
