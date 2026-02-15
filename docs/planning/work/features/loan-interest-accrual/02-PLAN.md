# Plan 02: LoanInterestHistory, Monthly Accrual Batch & GL Integration

## Goal
Create the LoanInterestHistory entity for audit trail, extend the monthly batch job to accrue interest on all active loans, and integrate with the general ledger for accrual-basis interest recognition.

## Prerequisites
- [x] CONTEXT.md decisions finalized
- [ ] Plan 01 complete (config-driven loan application, enum migration, interest tracking fields)

## Tasks

### Task 1: Create LoanInterestHistory entity, repository, and LoanInterestAccrualEvent
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanInterestHistory.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/InterestEventType.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/LoanInterestHistoryRepository.java`
- `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanInterestAccrualEvent.java`

**Action:**
1. Create `InterestEventType` enum: `MONTHLY_ACCRUAL`, `REPAYMENT_APPLIED`, `ADJUSTMENT`
2. Create `LoanInterestHistory` entity extending `BaseEntity`:
   - `loanId` (UUID, not null)
   - `memberId` (UUID, not null)
   - `accrualDate` (LocalDate, not null — the month-end date)
   - `interestAmount` (BigDecimal, precision 19 scale 2 — interest for this period)
   - `outstandingBalanceSnapshot` (BigDecimal, precision 19 scale 2 — balance at time of accrual)
   - `cumulativeInterestAccrued` (BigDecimal, precision 19 scale 2 — running total)
   - `interestRate` (BigDecimal, precision 5 scale 2 — rate snapshot)
   - `eventType` (InterestEventType enum)
   - `description` (String, length 500)
   - Table: `loan_interest_history`
3. Create `LoanInterestHistoryRepository` with query methods:
   - `findByLoanIdOrderByAccrualDateDesc(UUID loanId)`
   - `findByLoanIdAndAccrualDateBetween(UUID loanId, LocalDate start, LocalDate end)`
   - `findByAccrualDateBetween(LocalDate start, LocalDate end)` — portfolio-level query
   - `findByMemberIdOrderByAccrualDateDesc(UUID memberId)`
4. Create `LoanInterestAccrualEvent` record in sacco-common following existing event pattern:
   - `loanId`, `memberId`, `interestAmount`, `outstandingBalance`, `accrualDate`, `actor`
   - Implements `AuditableEvent` with eventType `"INTEREST_ACCRUAL"`

**Verify:**
```bash
cd sacco-loan && mvn compile -q && cd ../sacco-common && mvn compile -q
```

**Done when:** LoanInterestHistory entity, repository, and LoanInterestAccrualEvent compile cleanly.

### Task 2: Add INTEREST_ACCRUAL to TransactionType, seed Interest Receivable account, add accrual GL handler
**Files:**
- `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/entity/TransactionType.java`
- `sacco-ledger/src/main/resources/db/changelog/ledger/003-seed-interest-receivable.yaml`
- `sacco-ledger/src/main/resources/db/changelog/db.changelog-master.yaml` (if exists, add include)
- `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/FinancialEventListener.java`

**Action:**
1. Add `INTEREST_ACCRUAL` to `TransactionType` enum
2. Create Liquibase changeset `003-seed-interest-receivable.yaml` to seed account:
   - Code: `1003`, Name: `Interest Receivable`, Type: `ASSET`, Balance: `0.00`, Description: `Accrued interest receivable from loans`
3. Add `ACCOUNT_INTEREST_RECEIVABLE = "1003"` constant in `FinancialEventListener`
4. Add `handleInterestAccrual(LoanInterestAccrualEvent event)` method to `FinancialEventListener`:
   - DR Interest Receivable (1003) — interestAmount
   - CR Interest Income (4001) — interestAmount
   - TransactionType: `INTEREST_ACCRUAL`
   - ReferenceId: loanId
5. Update Liquibase master changelog to include new changeset

**Verify:**
```bash
cd sacco-ledger && mvn compile -q
```

**Done when:** TransactionType includes INTEREST_ACCRUAL, Interest Receivable account seeded, FinancialEventListener handles accrual event with correct journal entry.

### Task 3: Extend LoanBatchServiceImpl with monthly interest accrual logic
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchService.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/BatchProcessingResult.java`

**Action:**
1. Inject `InterestCalculator`, `LoanInterestHistoryRepository` into `LoanBatchServiceImpl`
2. Add `accrueMonthlyInterest()` method (called from `processOutstandingLoans`):
   - For each REPAYING loan:
     a. Get current `outstandingBalance` (live balance after repayments — per decision #2)
     b. Calculate monthly interest using `InterestCalculator`:
        - REDUCING_BALANCE: `outstandingBalance * monthlyRate` (interest on current balance)
        - FLAT_RATE: `principalAmount * monthlyRate` (interest on original principal)
     c. Create `LoanInterestHistory` record with: accrualDate (1st of month), interestAmount, outstandingBalanceSnapshot, cumulativeInterestAccrued, eventType=MONTHLY_ACCRUAL
     d. Update `totalInterestAccrued` on `LoanApplication` (add interestAmount)
     e. Publish `LoanInterestAccrualEvent` for GL posting
3. Track accrual counts in `BatchProcessingResult` — add `interestAccruedLoans` and `totalInterestAccrued` fields
4. Call `accrueMonthlyInterest()` at the start of `processOutstandingLoans()` before penalty/overdue checks
5. Use BigDecimal scale 6 for intermediate calculations, scale 2 for final amounts, HALF_UP rounding

**Verify:**
```bash
mvn test -pl sacco-loan -q
```

**Done when:** Monthly batch job accrues interest for all REPAYING loans, creates audit history records, publishes GL events, and updates cumulative tracking fields.

## Verification

After all tasks:
```bash
mvn test -pl sacco-loan -q
mvn test -pl sacco-ledger -q
mvn test -pl sacco-common -q
```

## Commit Message
```
feat(loan): add monthly interest accrual batch with GL integration

- Create LoanInterestHistory entity for full interest audit trail
- Create LoanInterestAccrualEvent for GL posting
- Add INTEREST_ACCRUAL to TransactionType enum
- Seed Interest Receivable account (1003) in chart of accounts
- Extend LoanBatchServiceImpl with monthly accrual on outstanding balance
- Post monthly accrual journal entries: DR Interest Receivable / CR Interest Income
```

---
*Planned: 2026-02-15*
