# Plan 02: Loan Repayment Tracking

## Goal
Add partial payment tracking to RepaymentSchedule entity and fix payment allocation to correctly handle partial payments.

## Prerequisites
- [ ] Plan 01 complete (LoanServiceImpl changes must not conflict)

## Tasks

### Task 1: Add amountPaid field to RepaymentSchedule entity
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/RepaymentSchedule.java`
**Action:**
Add a new field to track how much has been paid towards each schedule item:
```java
@Column(nullable = false, precision = 19, scale = 2)
private BigDecimal amountPaid = BigDecimal.ZERO;
```
This allows partial payments to accumulate. The `paid` boolean should be set to `true` only when `amountPaid >= totalAmount`.

**Verify:**
```bash
mvn compile -pl sacco-loan -q
```

**Done when:** RepaymentSchedule entity has `amountPaid` field with default `BigDecimal.ZERO`.

### Task 2: Add Liquibase migration for amountPaid column
**Files:** `sacco-loan/src/main/resources/db/changelog/loan/002-add-amount-paid-column.yaml` (new), `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`
**Action:**
1. Create Liquibase changeset to add `amount_paid DECIMAL(19,2) NOT NULL DEFAULT 0.00` to `repayment_schedules` table.
2. Add include in `db.changelog-master.yaml` under the Loan module section.
3. Include rollback: `dropColumn`.

**Verify:**
```bash
mvn compile -pl sacco-loan,sacco-app -q
```

**Done when:** Migration file exists and compiles.

### Task 3: Fix LoanServiceImpl payment allocation
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`
**Action:**
In `recordRepayment()` (lines 182-207), fix the payment allocation loop:
1. Calculate each schedule's outstanding as `schedule.getTotalAmount().subtract(schedule.getAmountPaid())` instead of the full `schedule.getTotalAmount()`.
2. When paying, update `schedule.setAmountPaid(schedule.getAmountPaid().add(paymentForSchedule))`.
3. Mark as paid only when `amountPaid.compareTo(totalAmount) >= 0`.
4. Always save the schedule (even partial payments) to persist the updated `amountPaid`.

```java
for (RepaymentSchedule schedule : unpaidSchedules) {
    if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;

    BigDecimal scheduleOutstanding = schedule.getTotalAmount().subtract(schedule.getAmountPaid());
    BigDecimal paymentForSchedule = remainingAmount.min(scheduleOutstanding);

    // Proportional interest/principal allocation
    BigDecimal interestPortion = schedule.getInterestAmount()
            .multiply(paymentForSchedule)
            .divide(schedule.getTotalAmount(), 2, RoundingMode.HALF_UP);
    BigDecimal principalPortion = paymentForSchedule.subtract(interestPortion);

    totalInterestPaid = totalInterestPaid.add(interestPortion);
    totalPrincipalPaid = totalPrincipalPaid.add(principalPortion);

    schedule.setAmountPaid(schedule.getAmountPaid().add(paymentForSchedule));
    if (schedule.getAmountPaid().compareTo(schedule.getTotalAmount()) >= 0) {
        schedule.setPaid(true);
    }
    scheduleRepository.save(schedule);

    remainingAmount = remainingAmount.subtract(paymentForSchedule);
}
```

**Verify:**
```bash
mvn compile -pl sacco-loan -q
```

**Done when:** Partial payments accumulate in `amountPaid`. Schedule marked paid only when fully paid.

## Verification

After all tasks:
```bash
mvn compile -q
```

## Commit Message
```
fix(loan): add partial payment tracking to repayment schedule

- B2: Add amountPaid field to RepaymentSchedule entity
- B2: Liquibase migration for amount_paid column
- B2: Fix payment allocation to track partial payments correctly
```

---
*Planned: 2026-02-15*
