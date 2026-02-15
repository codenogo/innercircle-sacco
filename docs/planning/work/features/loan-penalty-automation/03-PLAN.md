# Plan 03: Repayment Allocation with Penalties

## Goal
Update repayment allocation to Interest -> Penalties -> Principal order, with penalty marking and balance tracking.

## Prerequisites
- [ ] Plan 01 complete (schema + entity changes)
- [ ] Plan 02 complete (auto-penalty + LoanPenaltyService extensions)

## Tasks

### Task 1: Update Repayment Allocation in LoanServiceImpl
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`
**Action:**
Reference decision from CONTEXT.md: Interest -> Penalties -> Principal allocation order.

Modify `recordRepayment()` method to insert a penalty allocation step between interest and principal:

1. **Step 1 (existing)**: Allocate to accrued but unpaid interest first
2. **Step 2 (NEW)**: After interest is settled, allocate remaining to unpaid penalties:
   - Fetch unpaid penalties via `LoanPenaltyService.getUnpaidPenalties(loanId)`
   - For each unpaid penalty (ordered by appliedAt):
     - If remaining >= penalty.amount: mark penalty as paid (set paid=true, paidAt=now), subtract from remaining, decrease loan.totalPenalties
     - If remaining < penalty.amount: partial payment not supported — skip to principal (penalties are atomic)
   - Save updated penalties
3. **Step 3 (existing)**: Allocate remaining to principal via schedule installments

Also update the `LoanRepaymentEvent` to include penalty allocation amount for GL entries.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw test -pl sacco-loan -q 2>&1 | tail -10
```

**Done when:** Repayment allocates in Interest -> Penalties -> Principal order; existing tests pass.

### Task 2: Add Penalty Payment Methods to LoanPenaltyService
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanPenaltyService.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanPenaltyServiceImpl.java`
**Action:**
Add methods to support penalty payment from repayment flow:
1. `void markPenaltyPaid(UUID penaltyId, String actor)` — sets paid=true, paidAt=now, saves, publishes event
2. `BigDecimal payPenalties(UUID loanId, BigDecimal availableAmount, String actor)` — iterates unpaid penalties oldest-first, marks each paid if amount covers it, returns total paid amount

This encapsulates penalty payment logic so LoanServiceImpl stays clean.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw compile -pl sacco-loan -q 2>&1 | tail -5
```

**Done when:** LoanPenaltyService has payment methods; module compiles.

### Task 3: Update LoanResponse DTO and Tests
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/LoanResponse.java`, `sacco-loan/src/test/java/com/innercircle/sacco/loan/service/LoanServiceImplTest.java`
**Action:**
1. `LoanResponse.java` — add `totalPenalties` field to the DTO and `from()` mapping
2. Update existing repayment tests in `LoanServiceImplTest.java`:
   - Ensure existing interest-first tests still pass (no penalty = same behavior)
   - Add test: repayment with outstanding penalties allocates Interest -> Penalties -> Principal
   - Add test: repayment insufficient for penalties skips to principal after interest

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw test -pl sacco-loan -q 2>&1 | tail -10
```

**Done when:** LoanResponse includes totalPenalties; new and existing repayment tests pass.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw test -pl sacco-loan,sacco-config,sacco-ledger -q 2>&1 | tail -10
```

## Commit Message
```
feat(loan-penalty): repayment allocation with Interest -> Penalties -> Principal

- Insert penalty allocation step in recordRepayment() between interest and principal
- Add markPenaltyPaid() and payPenalties() to LoanPenaltyService
- Update LoanResponse DTO to include totalPenalties
- Add repayment tests covering penalty allocation scenarios
```

---
*Planned: 2026-02-15*
