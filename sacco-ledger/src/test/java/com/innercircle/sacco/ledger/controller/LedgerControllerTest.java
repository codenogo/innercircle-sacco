package com.innercircle.sacco.ledger.controller;

import com.innercircle.sacco.ledger.dto.BalanceSheetResponse;
import com.innercircle.sacco.ledger.dto.IncomeStatementResponse;
import com.innercircle.sacco.ledger.dto.TrialBalanceResponse;
import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.AccountType;
import com.innercircle.sacco.ledger.entity.JournalEntry;
import com.innercircle.sacco.ledger.entity.JournalLine;
import com.innercircle.sacco.ledger.entity.TransactionType;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import com.innercircle.sacco.ledger.repository.JournalEntryRepository;
import com.innercircle.sacco.ledger.service.FinancialStatementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LedgerController.class)
@AutoConfigureMockMvc(addFilters = false)
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private JournalEntryRepository journalEntryRepository;

    @MockitoBean
    private FinancialStatementService financialStatementService;

    private Account cashAccount;
    private Account memberSharesAccount;
    private JournalEntry testJournalEntry;

    @BeforeEach
    void setUp() {
        cashAccount = new Account("1001", "Cash", AccountType.ASSET, new BigDecimal("50000.00"), "Cash on hand", true, null, null);
        cashAccount.setId(UUID.randomUUID());
        cashAccount.setCreatedAt(Instant.now());
        cashAccount.setUpdatedAt(Instant.now());

        memberSharesAccount = new Account("2001", "Member Shares", AccountType.LIABILITY, new BigDecimal("40000.00"), "Member shares", true, null, null);
        memberSharesAccount.setId(UUID.randomUUID());
        memberSharesAccount.setCreatedAt(Instant.now());
        memberSharesAccount.setUpdatedAt(Instant.now());

        testJournalEntry = new JournalEntry();
        testJournalEntry.setId(UUID.randomUUID());
        testJournalEntry.setEntryNumber("JE000001");
        testJournalEntry.setTransactionDate(LocalDate.of(2026, 2, 15));
        testJournalEntry.setDescription("Test contribution");
        testJournalEntry.setTransactionType(TransactionType.CONTRIBUTION);
        testJournalEntry.setReferenceId(UUID.randomUUID());
        testJournalEntry.setPosted(true);
        testJournalEntry.setPostedAt(Instant.now());
        testJournalEntry.setCreatedAt(Instant.now());
        testJournalEntry.setUpdatedAt(Instant.now());

        JournalLine debitLine = new JournalLine();
        debitLine.setId(UUID.randomUUID());
        debitLine.setAccount(cashAccount);
        debitLine.setDebitAmount(new BigDecimal("5000.00"));
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Cash received");
        testJournalEntry.addJournalLine(debitLine);

        JournalLine creditLine = new JournalLine();
        creditLine.setId(UUID.randomUUID());
        creditLine.setAccount(memberSharesAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(new BigDecimal("5000.00"));
        creditLine.setDescription("Member shares credited");
        testJournalEntry.addJournalLine(creditLine);
    }

    @Nested
    @DisplayName("GET /api/v1/ledger/accounts")
    class GetChartOfAccountsTests {

        @Test
        @DisplayName("should return chart of accounts")
        void shouldReturnChartOfAccounts() throws Exception {
            when(accountRepository.findByActiveTrue()).thenReturn(List.of(cashAccount, memberSharesAccount));

            mockMvc.perform(get("/api/v1/ledger/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].accountCode").value("1001"))
                    .andExpect(jsonPath("$.data[0].accountName").value("Cash"))
                    .andExpect(jsonPath("$.data[0].accountType").value("ASSET"))
                    .andExpect(jsonPath("$.data[1].accountCode").value("2001"));
        }

        @Test
        @DisplayName("should return empty list when no active accounts")
        void shouldReturnEmptyListWhenNoAccounts() throws Exception {
            when(accountRepository.findByActiveTrue()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/ledger/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ledger/journal-entries")
    class GetJournalEntriesTests {

        @Test
        @DisplayName("should return journal entries with default pagination")
        void shouldReturnJournalEntriesWithDefaultPagination() throws Exception {
            Page<JournalEntry> page = new PageImpl<>(
                    List.of(testJournalEntry),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "transactionDate")),
                    1
            );
            when(journalEntryRepository.findByPostedTrue(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/ledger/journal-entries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.number").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(true))
                    .andExpect(jsonPath("$.data.content[0].entryNumber").value("JE000001"))
                    .andExpect(jsonPath("$.data.content[0].transactionType").value("CONTRIBUTION"))
                    .andExpect(jsonPath("$.data.content[0].posted").value(true))
                    .andExpect(jsonPath("$.data.content[0].journalLines").isArray())
                    .andExpect(jsonPath("$.data.content[0].journalLines.length()").value(2));
        }

        @Test
        @DisplayName("should return journal entries with custom pagination")
        void shouldReturnJournalEntriesWithCustomPagination() throws Exception {
            Page<JournalEntry> page = new PageImpl<>(
                    List.of(testJournalEntry),
                    PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "transactionDate")),
                    11
            );
            when(journalEntryRepository.findByPostedTrue(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/ledger/journal-entries")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.number").value(1))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(11))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.first").value(false))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("should return journal entries with ascending sort")
        void shouldReturnJournalEntriesWithAscSort() throws Exception {
            Page<JournalEntry> page = new PageImpl<>(
                    List.of(testJournalEntry),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "entryNumber")),
                    1
            );
            when(journalEntryRepository.findByPostedTrue(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/ledger/journal-entries")
                            .param("sort", "entryNumber,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should map journal lines correctly in response")
        void shouldMapJournalLinesCorrectly() throws Exception {
            Page<JournalEntry> page = new PageImpl<>(List.of(testJournalEntry));
            when(journalEntryRepository.findByPostedTrue(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/ledger/journal-entries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].journalLines[0].accountCode").value("1001"))
                    .andExpect(jsonPath("$.data.content[0].journalLines[0].accountName").value("Cash"))
                    .andExpect(jsonPath("$.data.content[0].journalLines[0].debitAmount").value("5000.00"))
                    .andExpect(jsonPath("$.data.content[0].journalLines[0].creditAmount").value("0"))
                    .andExpect(jsonPath("$.data.content[0].journalLines[1].accountCode").value("2001"))
                    .andExpect(jsonPath("$.data.content[0].journalLines[1].debitAmount").value("0"))
                    .andExpect(jsonPath("$.data.content[0].journalLines[1].creditAmount").value("5000.00"));
        }

        @Test
        @DisplayName("should return empty page when no entries")
        void shouldReturnEmptyPage() throws Exception {
            Page<JournalEntry> emptyPage = new PageImpl<>(List.of());
            when(journalEntryRepository.findByPostedTrue(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/ledger/journal-entries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.number").value(0))
                    .andExpect(jsonPath("$.data.size").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ledger/trial-balance")
    class GetTrialBalanceTests {

        @Test
        @DisplayName("should return trial balance with specific date")
        void shouldReturnTrialBalanceWithDate() throws Exception {
            LocalDate asOfDate = LocalDate.of(2026, 2, 15);
            TrialBalanceResponse trialBalance = TrialBalanceResponse.builder()
                    .asOfDate(asOfDate)
                    .accounts(List.of(
                            TrialBalanceResponse.AccountBalanceDto.builder()
                                    .accountCode("1001")
                                    .accountName("Cash")
                                    .accountType("ASSET")
                                    .debitBalance("50000.00")
                                    .creditBalance("0.00")
                                    .build()
                    ))
                    .totalDebits("50000.00")
                    .totalCredits("50000.00")
                    .balanced(true)
                    .build();

            when(financialStatementService.generateTrialBalance(asOfDate)).thenReturn(trialBalance);

            mockMvc.perform(get("/api/v1/ledger/trial-balance")
                            .param("asOfDate", "2026-02-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.asOfDate").value("2026-02-15"))
                    .andExpect(jsonPath("$.data.accounts").isArray())
                    .andExpect(jsonPath("$.data.accounts[0].accountCode").value("1001"))
                    .andExpect(jsonPath("$.data.totalDebits").value("50000.00"))
                    .andExpect(jsonPath("$.data.totalCredits").value("50000.00"))
                    .andExpect(jsonPath("$.data.balanced").value(true));
        }

        @Test
        @DisplayName("should use current date when asOfDate not provided")
        void shouldUseCurrentDateWhenNotProvided() throws Exception {
            TrialBalanceResponse trialBalance = TrialBalanceResponse.builder()
                    .asOfDate(LocalDate.now())
                    .accounts(List.of())
                    .totalDebits("0")
                    .totalCredits("0")
                    .balanced(true)
                    .build();

            when(financialStatementService.generateTrialBalance(LocalDate.now())).thenReturn(trialBalance);

            mockMvc.perform(get("/api/v1/ledger/trial-balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.balanced").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ledger/income-statement")
    class GetIncomeStatementTests {

        @Test
        @DisplayName("should return income statement for date range")
        void shouldReturnIncomeStatement() throws Exception {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 31);

            IncomeStatementResponse incomeStatement = IncomeStatementResponse.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .revenue(List.of(
                            IncomeStatementResponse.AccountLineItem.builder()
                                    .accountCode("4001")
                                    .accountName("Interest Income")
                                    .amount("8000.00")
                                    .build()
                    ))
                    .expenses(List.of(
                            IncomeStatementResponse.AccountLineItem.builder()
                                    .accountCode("5001")
                                    .accountName("Operating Expenses")
                                    .amount("3000.00")
                                    .build()
                    ))
                    .totalRevenue("8000.00")
                    .totalExpenses("3000.00")
                    .netIncome("5000.00")
                    .build();

            when(financialStatementService.generateIncomeStatement(startDate, endDate)).thenReturn(incomeStatement);

            mockMvc.perform(get("/api/v1/ledger/income-statement")
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.startDate").value("2026-01-01"))
                    .andExpect(jsonPath("$.data.endDate").value("2026-01-31"))
                    .andExpect(jsonPath("$.data.revenue").isArray())
                    .andExpect(jsonPath("$.data.revenue[0].accountCode").value("4001"))
                    .andExpect(jsonPath("$.data.revenue[0].amount").value("8000.00"))
                    .andExpect(jsonPath("$.data.expenses").isArray())
                    .andExpect(jsonPath("$.data.expenses[0].accountCode").value("5001"))
                    .andExpect(jsonPath("$.data.totalRevenue").value("8000.00"))
                    .andExpect(jsonPath("$.data.totalExpenses").value("3000.00"))
                    .andExpect(jsonPath("$.data.netIncome").value("5000.00"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ledger/balance-sheet")
    class GetBalanceSheetTests {

        @Test
        @DisplayName("should return balance sheet with specific date")
        void shouldReturnBalanceSheetWithDate() throws Exception {
            LocalDate asOfDate = LocalDate.of(2026, 2, 15);

            BalanceSheetResponse balanceSheet = BalanceSheetResponse.builder()
                    .asOfDate(asOfDate)
                    .assets(List.of(
                            BalanceSheetResponse.AccountLineItem.builder()
                                    .accountCode("1001")
                                    .accountName("Cash")
                                    .amount("50000.00")
                                    .build()
                    ))
                    .liabilities(List.of(
                            BalanceSheetResponse.AccountLineItem.builder()
                                    .accountCode("2001")
                                    .accountName("Member Shares")
                                    .amount("40000.00")
                                    .build()
                    ))
                    .equity(List.of(
                            BalanceSheetResponse.AccountLineItem.builder()
                                    .accountCode("3001")
                                    .accountName("Retained Earnings")
                                    .amount("10000.00")
                                    .build()
                    ))
                    .totalAssets("50000.00")
                    .totalLiabilities("40000.00")
                    .totalEquity("10000.00")
                    .totalLiabilitiesAndEquity("50000.00")
                    .balanced(true)
                    .build();

            when(financialStatementService.generateBalanceSheet(asOfDate)).thenReturn(balanceSheet);

            mockMvc.perform(get("/api/v1/ledger/balance-sheet")
                            .param("asOfDate", "2026-02-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.asOfDate").value("2026-02-15"))
                    .andExpect(jsonPath("$.data.assets").isArray())
                    .andExpect(jsonPath("$.data.assets[0].accountCode").value("1001"))
                    .andExpect(jsonPath("$.data.assets[0].amount").value("50000.00"))
                    .andExpect(jsonPath("$.data.liabilities").isArray())
                    .andExpect(jsonPath("$.data.liabilities[0].accountCode").value("2001"))
                    .andExpect(jsonPath("$.data.equity").isArray())
                    .andExpect(jsonPath("$.data.equity[0].accountCode").value("3001"))
                    .andExpect(jsonPath("$.data.totalAssets").value("50000.00"))
                    .andExpect(jsonPath("$.data.totalLiabilities").value("40000.00"))
                    .andExpect(jsonPath("$.data.totalEquity").value("10000.00"))
                    .andExpect(jsonPath("$.data.totalLiabilitiesAndEquity").value("50000.00"))
                    .andExpect(jsonPath("$.data.balanced").value(true));
        }

        @Test
        @DisplayName("should use current date when asOfDate not provided")
        void shouldUseCurrentDateWhenNotProvided() throws Exception {
            BalanceSheetResponse balanceSheet = BalanceSheetResponse.builder()
                    .asOfDate(LocalDate.now())
                    .assets(List.of())
                    .liabilities(List.of())
                    .equity(List.of())
                    .totalAssets("0")
                    .totalLiabilities("0")
                    .totalEquity("0")
                    .totalLiabilitiesAndEquity("0")
                    .balanced(true)
                    .build();

            when(financialStatementService.generateBalanceSheet(LocalDate.now())).thenReturn(balanceSheet);

            mockMvc.perform(get("/api/v1/ledger/balance-sheet"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.balanced").value(true));
        }

        @Test
        @DisplayName("should return unbalanced balance sheet")
        void shouldReturnUnbalancedBalanceSheet() throws Exception {
            LocalDate asOfDate = LocalDate.of(2026, 2, 15);

            BalanceSheetResponse balanceSheet = BalanceSheetResponse.builder()
                    .asOfDate(asOfDate)
                    .assets(List.of())
                    .liabilities(List.of())
                    .equity(List.of())
                    .totalAssets("50000.00")
                    .totalLiabilities("30000.00")
                    .totalEquity("10000.00")
                    .totalLiabilitiesAndEquity("40000.00")
                    .balanced(false)
                    .build();

            when(financialStatementService.generateBalanceSheet(asOfDate)).thenReturn(balanceSheet);

            mockMvc.perform(get("/api/v1/ledger/balance-sheet")
                            .param("asOfDate", "2026-02-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balanced").value(false));
        }
    }
}
