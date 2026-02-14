# Plan 07: Ledger / Accounting

## Goal
Implement double-entry general ledger with chart of accounts, journal entries, event-driven posting, and financial statements (trial balance, P&L, balance sheet).

## Prerequisites
- [ ] Plan 01 complete

## Tasks

### Task 1: Ledger Entities + Chart of Accounts
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/entity/Account.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/entity/AccountType.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/entity/JournalEntry.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/entity/JournalLine.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/entity/TransactionType.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/repository/AccountRepository.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/repository/JournalEntryRepository.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/repository/JournalLineRepository.java`
**Action:**
1. `Account` entity: accountCode (UNIQUE), name, accountType (ASSET, LIABILITY, EQUITY, INCOME, EXPENSE), parentAccountId (nullable, for hierarchy), balance (BigDecimal), active
2. `JournalEntry` entity: entryNumber (auto-generated), transactionDate, description, transactionType, referenceId (UUID of source entity), posted (boolean), postedAt
3. `JournalLine` entity: journalEntryId, accountId, debitAmount (BigDecimal), creditAmount (BigDecimal), description
4. **Constraint**: Every JournalEntry's lines must sum debits == credits (balance to zero)
5. Repositories with cursor pagination

**Verify:**
```bash
mvn compile -pl sacco-ledger -q
```

**Done when:** All ledger entities and repositories compile.

### Task 2: Ledger Service + Event Listeners
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/service/LedgerService.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/service/LedgerServiceImpl.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/FinancialEventListener.java`
**Action:**
1. `LedgerService`: createJournalEntry (with balance validation — debits must equal credits), postEntry, getAccountBalance, getAccountTransactions
2. `FinancialEventListener` with `@TransactionalEventListener`:
   - `ContributionReceivedEvent` → DR Bank/Cash, CR Member Savings
   - `LoanDisbursedEvent` → DR Loans Receivable, CR Bank/Cash
   - `LoanRepaymentEvent` → DR Bank/Cash, CR Loans Receivable + CR Interest Income
   - `PayoutProcessedEvent` → DR Member Savings, CR Bank/Cash
   - `PenaltyAppliedEvent` → DR Member Account, CR Penalty Income
3. Account lookup by code for automatic posting

**Verify:**
```bash
mvn compile -pl sacco-ledger -q
```

**Done when:** Ledger service and event listeners compile.

### Task 3: Financial Statements + REST API + Liquibase
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/service/FinancialStatementService.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/service/FinancialStatementServiceImpl.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/controller/LedgerController.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/dto/JournalEntryResponse.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/dto/TrialBalanceResponse.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/dto/BalanceSheetResponse.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/dto/IncomeStatementResponse.java`, `sacco-ledger/src/main/resources/db/changelog/ledger/001-create-ledger-tables.yaml`, `sacco-ledger/src/main/resources/db/changelog/ledger/002-seed-chart-of-accounts.yaml`
**Action:**
1. `FinancialStatementService`: trialBalance(dateRange), incomeStatement(dateRange), balanceSheet(asOfDate)
2. `LedgerController` at `/api/v1/ledger` (TREASURER, ADMIN):
   - GET `/accounts` — chart of accounts
   - GET `/journal-entries` — journal entries (cursor-paginated)
   - GET `/trial-balance` — trial balance for date range
   - GET `/income-statement` — P&L for date range
   - GET `/balance-sheet` — balance sheet as of date
3. Liquibase 001: accounts, journal_entries, journal_lines tables with indexes
4. Liquibase 002: Seed predefined chart of accounts (Bank, Cash, Member Savings, Loans Receivable, Interest Income, Penalty Income, Share Capital, etc.)
5. Update sacco-app changelog-master

**Verify:**
```bash
mvn compile -pl sacco-ledger -q
```

**Done when:** Financial statements, REST API, and changelogs complete.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-ledger -q
```

## Commit Message
```
feat(ledger): implement double-entry accounting with event-driven posting

- Chart of accounts with predefined chama account structure
- Journal entries with debit/credit balance validation
- Event listeners for automatic ledger posting
- Trial balance, income statement, balance sheet endpoints
```

---
*Planned: 2026-02-14*
