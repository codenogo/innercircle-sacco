# Plan 02: GL Handlers + Tests

## Goal
Add 4 new GL event handlers to FinancialEventListener for loan reversals, contribution reversals, penalty waivers, and benefits distribution — with full test coverage.

## Prerequisites
- [ ] Plan 01 complete (event records + Liquibase migration + TransactionType values)

## Tasks

### Task 1: Add GL handlers for LoanReversalEvent and ContributionReversedEvent
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/FinancialEventListener.java`
**Action:**
Add 2 new `@TransactionalEventListener(phase = BEFORE_COMMIT)` methods:

1. `handleLoanReversal(LoanReversalEvent)` — Mirror of handleLoanRepayment:
   - DR Loan Receivable (1002) — principalPortion (reinstate receivable)
   - DR Interest Receivable (1003) — interestPortion (reinstate receivable, skip if zero)
   - CR Cash (1001) — amount (reverse cash receipt)
   - If penaltyPortion > 0: DR Member Account (2002) — penaltyPortion (reinstate obligation)
   - TransactionType: `LOAN_REVERSAL`
   - Description: `"Reversal of repayment — Reversal ID: " + reversalId`

2. `handleContributionReversed(ContributionReversedEvent)` — Mirror of handleContributionReceived:
   - DR Member Shares (2001) — amount (reduce member shares)
   - CR Cash (1001) — amount (cash returned)
   - TransactionType: `CONTRIBUTION_REVERSAL`
   - Description: `"Contribution reversed — Ref: " + referenceNumber`

Add import for `ContributionReversedEvent` and `LoanReversalEvent`.

**Verify:**
```bash
mvn compile -pl sacco-ledger -am -q
```

**Done when:** Both handlers compile. GL entries match the specifications in CONTEXT.md exactly.

### Task 2: Add GL handlers for PenaltyWaivedEvent and BenefitsDistributedEvent
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/FinancialEventListener.java`
**Action:**
Add 2 more `@TransactionalEventListener(phase = BEFORE_COMMIT)` methods:

1. `handlePenaltyWaived(PenaltyWaivedEvent)`:
   - DR Bad Debt Expense (5003) — amount (write-off expense)
   - CR Member Account (2002) — amount (clear member obligation)
   - TransactionType: `PENALTY_WAIVER`
   - Description: `"Penalty waived — Penalty ID: " + penaltyId`
   - Add constant: `ACCOUNT_BAD_DEBT_EXPENSE = "5003"`

2. `handleBenefitsDistributed(BenefitsDistributedEvent)`:
   - DR Interest Income (4001) — totalInterestAmount (reduce income pool)
   - CR Member Account (2002) — totalInterestAmount (allocate to members)
   - TransactionType: `BENEFIT_DISTRIBUTION`
   - Description: `"Interest benefits distributed — Loan ID: " + loanId`

Add imports for `PenaltyWaivedEvent` and `BenefitsDistributedEvent`.

**Verify:**
```bash
mvn compile -pl sacco-ledger -am -q
```

**Done when:** Both handlers compile. All 4 new handlers present in FinancialEventListener (10 total).

### Task 3: Add tests for all 4 new GL handlers
**Files:** `sacco-ledger/src/test/java/com/innercircle/sacco/ledger/listener/FinancialEventListenerTest.java`
**Action:**
Add 4 new `@Nested` test classes following existing test patterns:

1. `HandleLoanReversalTests`:
   - Test correct journal entry creation (DR Loan Receivable + DR Interest Receivable + CR Cash)
   - Test balanced entry (debits = credits)
   - Test penalty portion handling (DR Member Account when > 0)
   - Test skips interest line when zero

2. `HandleContributionReversedTests`:
   - Test correct journal entry (DR Member Shares + CR Cash)
   - Test balanced entry
   - Test description includes reference number

3. `HandlePenaltyWaivedTests`:
   - Test correct journal entry (DR Bad Debt Expense + CR Member Account)
   - Test balanced entry
   - Add `badDebtExpenseAccount` to setUp (account code "5003")

4. `HandleBenefitsDistributedTests`:
   - Test correct journal entry (DR Interest Income + CR Member Account)
   - Test balanced entry

Follow existing test patterns: use `@Captor` for `JournalEntry`, verify `TransactionType`, verify account assignments, verify amounts.

**Verify:**
```bash
mvn test -pl sacco-ledger -am -DskipITs -q
```

**Done when:** All existing + new tests pass. New handlers have matching test coverage.

## Verification

After all tasks:
```bash
mvn test -pl sacco-ledger -am -DskipITs -q
```

## Commit Message
```
feat(event-gl-completeness): add GL handlers for reversals, waivers, and benefits

- Add handleLoanReversal GL handler (mirror of repayment)
- Add handleContributionReversed GL handler (mirror of contribution)
- Add handlePenaltyWaived GL handler (DR Bad Debt Expense / CR Member Account)
- Add handleBenefitsDistributed GL handler (DR Interest Income / CR Member Account)
- Add tests for all 4 new GL handlers
```

---
*Planned: 2026-02-15*
