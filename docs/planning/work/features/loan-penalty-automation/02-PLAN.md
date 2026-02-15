# Plan 02: Auto-Penalty Application in Batch Processing

## Goal
Wire PenaltyRule config into LoanBatchServiceImpl so overdue loans automatically receive penalties based on FLAT/PERCENTAGE rules with compounding and idempotency.

## Prerequisites
- [ ] Plan 01 complete (schema + entity changes)

## Tasks

### Task 1: Add ConfigService Penalty Lookup
**Files:** `sacco-config/src/main/java/com/innercircle/sacco/config/service/ConfigService.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/service/ConfigServiceImpl.java`
**Action:**
Add method to ConfigService interface and implementation:
- `Optional<PenaltyRule> getActivePenaltyRuleByType(PenaltyRule.PenaltyType type)` — delegates to `penaltyRuleRepository.findByPenaltyTypeAndActiveTrue()`, returns first result or empty.

This bridges sacco-config → sacco-loan so the batch job can fetch the active LOAN_DEFAULT rule.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw compile -pl sacco-config -q 2>&1 | tail -5
```

**Done when:** ConfigService compiles with new method.

### Task 2: Wire Auto-Penalty into LoanBatchServiceImpl
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`
**Action:**
Reference decisions from CONTEXT.md:
1. Inject `LoanPenaltyService`, `ConfigService`, and `LoanPenaltyRepository`
2. Replace hardcoded 30-day grace period with `SystemConfig: loan.penalty.grace_period_days`
3. Replace hardcoded 90-day default threshold with `SystemConfig: loan.penalty.default_threshold_days`
4. In the penalty detection block (currently just logs), add:
   - Fetch active `LOAN_DEFAULT` PenaltyRule via ConfigService
   - For each overdue installment past grace period:
     - **Idempotency check**: skip if `LoanPenaltyRepository.findByLoanIdAndScheduleId(loanId, scheduleId)` is non-empty
     - **Calculate amount**: if FLAT → use rule.rate directly; if PERCENTAGE → rule.rate × installment.amount / 100
     - **Compounding check**: if rule.compounding is false, skip if penalty already exists for this schedule
     - Call `LoanPenaltyService.applyPenalty(loanId, memberId, amount, reason, "SYSTEM")`
     - Set scheduleId on the penalty
     - Update `LoanApplication.totalPenalties += amount`
5. Update penalizedCount to reflect actual penalties applied (not just detected)

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw test -pl sacco-loan -q 2>&1 | tail -10
```

**Done when:** Batch processing compiles, applies penalties via PenaltyRule, existing tests pass.

### Task 3: Update LoanPenaltyService for scheduleId Support
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanPenaltyService.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanPenaltyServiceImpl.java`
**Action:**
1. Add overloaded `applyPenalty` method that accepts `scheduleId`:
   - `LoanPenalty applyPenalty(UUID loanId, UUID memberId, BigDecimal amount, String reason, String actor, UUID scheduleId)`
   - Sets `scheduleId` on the penalty entity before saving
2. Add method `getUnpaidPenalties(UUID loanId)` — delegates to `LoanPenaltyRepository.findByLoanIdAndPaidFalse()`
3. Add method `getTotalUnpaidPenalties(UUID loanId)` — delegates to `LoanPenaltyRepository.sumAmountByLoanIdAndPaidFalse()`

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw test -pl sacco-loan -q 2>&1 | tail -5
```

**Done when:** LoanPenaltyService has scheduleId support and unpaid penalty queries; all tests pass.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw test -pl sacco-loan,sacco-config -q 2>&1 | tail -10
```

## Commit Message
```
feat(loan-penalty): auto-apply penalties via PenaltyRule in batch processing

- Add ConfigService.getActivePenaltyRuleByType() for cross-module access
- Wire PenaltyRule into LoanBatchServiceImpl for auto-penalty calculation
- Support FLAT and PERCENTAGE calculation methods
- Implement compounding logic (configurable per rule)
- Use SystemConfig for grace_period_days and default_threshold_days
- Add idempotency via scheduleId to prevent double-penalizing
- Extend LoanPenaltyService with scheduleId and unpaid penalty methods
```

---
*Planned: 2026-02-15*
