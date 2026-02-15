# Quick: Apply Review Fixes

## Goal
Fix 3 issues identified during loan-penalty-automation review: GL double-counting, penalty logic duplication, and log inaccuracy.

## Files
- `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/FinancialEventListener.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanPenaltyServiceImpl.java`
- `sacco-ledger/src/test/java/com/innercircle/sacco/ledger/listener/FinancialEventListenerTest.java`

## Approach

### Fix 1: GL Penalty Income Double-Counting
In `FinancialEventListener.handleLoanRepayment()`, change the penalty portion journal line from CR Penalty Income (4003) to CR Member Account (2002). This settles the member's obligation created at penalty application time, avoiding double-counting income.

### Fix 2: Extract Penalty Processing Helper
Extract shared penalty detection/application/status-update logic from `LoanBatchServiceImpl.executeProcessing()` and `processLoan()` into a `processPenaltiesAndStatus()` helper method.

### Fix 3: Fix Log Inaccuracy
In `LoanPenaltyServiceImpl.payPenalties()`, track actual count of penalties paid and log that instead of `unpaidPenalties.size()`.

## Verify
```bash
mvn test -pl sacco-loan,sacco-ledger -am -q -DskipITs
```

---
*Planned: 2026-02-15*
