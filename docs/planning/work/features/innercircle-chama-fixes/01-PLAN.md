# Plan 01: Financial Calculation Fixes

## Goal
Fix incorrect ledger debit/credit balance logic, final installment interest miscalculation, disbursement race condition, and non-atomic journal entry number generation.

## Prerequisites
- [ ] None (first plan)

## Tasks

### Task 1: Fix ledger debit/credit balance logic (B1)
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/entity/AccountType.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/service/LedgerServiceImpl.java`
**Action:**
1. Add `isNormalDebit()` method to `AccountType` enum:
   - Returns `true` for `ASSET`, `EXPENSE` (increase on debit)
   - Returns `false` for `LIABILITY`, `EQUITY`, `REVENUE` (increase on credit)
2. In `LedgerServiceImpl.postEntry()` (line 67-68), replace the flat `debitAmount.subtract(creditAmount)` with account-type-aware logic:
   ```java
   BigDecimal balanceChange;
   if (account.getAccountType().isNormalDebit()) {
       balanceChange = debitAmount.subtract(creditAmount);
   } else {
       balanceChange = creditAmount.subtract(debitAmount);
   }
   ```

**Verify:**
```bash
mvn compile -pl sacco-ledger -q
```

**Done when:** AccountType has `isNormalDebit()` method, and balance updates are branched by account type.

### Task 2: Fix final installment interest + disbursement race (B3, W8)
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/RepaymentScheduleGenerator.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`
**Action:**
1. In `RepaymentScheduleGenerator.generateReducingBalanceSchedule()` (lines 66-80): The last installment currently uses the `interestAmount` computed earlier in the loop iteration, but that interest was calculated on the `remainingPrincipal` *before* the principal adjustment on line 83. Fix: for `i == termMonths`, compute interest fresh on the *current* `remainingPrincipal`:
   ```java
   if (i == termMonths) {
       principalAmount = remainingPrincipal;
       interestAmount = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
       BigDecimal totalAmount = principalAmount.add(interestAmount);
       // ... create schedule
   }
   ```
   Note: the interest recalculation line is already correct at lines 59-60. The issue is that `remainingPrincipal` was already decremented on line 83 for non-final installments, but for the final installment we break before line 83. So interest is computed correctly on the true remaining balance. **Verify**: trace through the logic to confirm the interest is calculated on the actual remaining principal, not a stale value from a previous iteration's subtraction.

2. In `LoanServiceImpl.disburseLoan()` (lines 111, 139-143): Remove the double save. Instead of saving as DISBURSED then updating to REPAYING, set the status directly to REPAYING before saving:
   ```java
   loan.setStatus(LoanStatus.REPAYING);
   loan.setDisbursedAt(now);
   // ... generate schedule, save schedule ...
   LoanApplication savedLoan = loanRepository.save(loan);
   ```
   Remove lines 142-143 (`savedLoan.setStatus(LoanStatus.REPAYING); loanRepository.save(savedLoan);`).

**Verify:**
```bash
mvn compile -pl sacco-loan -q
```

**Done when:** Final installment interest is recalculated on actual remaining principal. Disbursement uses a single save with REPAYING status.

### Task 3: Fix journal entry number generation (W9)
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/repository/JournalEntryRepository.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/service/LedgerServiceImpl.java`, `sacco-ledger/src/main/resources/db/changelog/ledger/003-create-journal-entry-seq.yaml` (new), `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`
**Action:**
1. Create new Liquibase changeset `003-create-journal-entry-seq.yaml` in the ledger module's changelog dir:
   ```yaml
   databaseChangeLog:
     - changeSet:
         id: 003-create-journal-entry-seq
         author: claude
         changes:
           - createSequence:
               sequenceName: journal_entry_number_seq
               startValue: 1
               incrementBy: 1
         rollback:
           - dropSequence:
               sequenceName: journal_entry_number_seq
   ```
2. Add include in `db.changelog-master.yaml` under the Ledger module section.
3. Add native query to `JournalEntryRepository`:
   ```java
   @Query(value = "SELECT nextval('journal_entry_number_seq')", nativeQuery = true)
   Long getNextEntryNumber();
   ```
4. Update `LedgerServiceImpl.generateEntryNumber()` to use the new method:
   ```java
   public String generateEntryNumber() {
       Long nextVal = journalEntryRepository.getNextEntryNumber();
       return String.format("JE%06d", nextVal);
   }
   ```

**Verify:**
```bash
mvn compile -pl sacco-ledger,sacco-app -q
```

**Done when:** Entry number uses atomic DB sequence instead of read-then-increment.

## Verification

After all tasks:
```bash
mvn compile -q
```

## Commit Message
```
fix(ledger,loan): fix balance logic, interest calc, disbursement race, entry number atomicity

- B1: Branch ledger balance update by account type (debit-normal vs credit-normal)
- B3: Recalculate interest for final installment on actual remaining principal
- W8: Remove double save in loan disbursement, set REPAYING directly
- W9: Use DB sequence for atomic journal entry number generation
```

---
*Planned: 2026-02-15*
