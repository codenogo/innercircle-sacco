# Plan 01: Schema & Entity Changes for Penalty Tracking

## Goal
Add database columns and update JPA entities to support penalty tracking, payment marking, and configurable compounding.

## Prerequisites
- [ ] Plan 03 of loan-interest-accrual complete (interest-first repayment in place)
- [ ] CONTEXT.md decisions finalized

## Tasks

### Task 1: Liquibase Migrations
**Files:** `src/main/resources/db/changelog/loan/007-add-penalty-tracking-columns.yaml`, `src/main/resources/db/changelog/config/004-add-penalty-compounding.yaml`, `src/main/resources/db/changelog/config/005-seed-penalty-system-config.yaml`
**Action:**
1. Create `007-add-penalty-tracking-columns.yaml`:
   - Add `paid` (boolean, default false) to `loan_penalties`
   - Add `paid_at` (timestamp) to `loan_penalties`
   - Add `schedule_id` (uuid) to `loan_penalties`
   - Add `total_penalties` (decimal 19,2, default 0) to `loan_applications`
2. Create `004-add-penalty-compounding.yaml`:
   - Add `compounding` (boolean, default false) to `penalty_rules`
3. Create `005-seed-penalty-system-config.yaml`:
   - Insert `loan.penalty.grace_period_days` = `30` into `system_configs`
   - Insert `loan.penalty.default_threshold_days` = `90` into `system_configs`

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw test -pl sacco-loan -q -Dtest=LoanApplicationTests 2>&1 | tail -5
```

**Done when:** Migrations parse without errors; existing tests still pass.

### Task 2: Update JPA Entities
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanPenalty.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanApplication.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/entity/PenaltyRule.java`
**Action:**
1. `LoanPenalty.java` — add fields:
   - `private Boolean paid = false;`
   - `private Instant paidAt;`
   - `private UUID scheduleId;`
2. `LoanApplication.java` — add field:
   - `private BigDecimal totalPenalties = BigDecimal.ZERO;`
3. `PenaltyRule.java` — add field:
   - `private boolean compounding = false;`

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw compile -pl sacco-loan,sacco-config -q 2>&1 | tail -5
```

**Done when:** Both modules compile cleanly with new fields.

### Task 3: Add LoanPenaltyRepository Query Methods
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/LoanPenaltyRepository.java`
**Action:**
Add Spring Data JPA query methods:
- `List<LoanPenalty> findByLoanIdAndPaidFalse(UUID loanId);` — get unpaid penalties for a loan
- `List<LoanPenalty> findByLoanIdAndScheduleId(UUID loanId, UUID scheduleId);` — idempotency check for batch
- `BigDecimal sumAmountByLoanIdAndPaidFalse(UUID loanId);` — total unpaid penalty balance (use `@Query`)

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw test -pl sacco-loan -q 2>&1 | tail -5
```

**Done when:** sacco-loan compiles and all existing tests pass.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && ./mvnw test -pl sacco-loan,sacco-config -q 2>&1 | tail -10
```

## Commit Message
```
feat(loan-penalty): schema and entity changes for penalty tracking

- Add paid/paidAt/scheduleId columns to loan_penalties table
- Add totalPenalties column to loan_applications table
- Add compounding column to penalty_rules table
- Seed SystemConfig entries for grace_period_days and default_threshold_days
- Update JPA entities with new fields
- Add LoanPenaltyRepository query methods for unpaid penalties
```

---
*Planned: 2026-02-15*
