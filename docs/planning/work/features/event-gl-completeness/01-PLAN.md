# Plan 01: Foundation — Event Records + Liquibase + TransactionType

## Goal
Establish all prerequisite event definitions, database migration, and enum values needed by GL handlers and publishers.

## Prerequisites
- [ ] CONTEXT.md decisions finalized

## Tasks

### Task 1: Create PenaltyWaivedEvent + enrich LoanReversalEvent
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/event/PenaltyWaivedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanReversalEvent.java`
**Action:**
1. Create `PenaltyWaivedEvent` record with fields: `penaltyId` (UUID), `memberId` (UUID), `amount` (BigDecimal), `reason` (String), `actor` (String). Implement `AuditableEvent` with `getEventType()` returning `"PENALTY_WAIVED"`.
2. Add `penaltyPortion` (BigDecimal) field to `LoanReversalEvent` record — insert after `interestPortion`. This enables the GL handler to reverse penalty portions correctly.

**Verify:**
```bash
mvn compile -pl sacco-common -q
```

**Done when:** Both event records compile. PenaltyWaivedEvent exists with correct fields. LoanReversalEvent has penaltyPortion field.

### Task 2: Liquibase migration for Bad Debt Expense + TransactionType enum
**Files:** `sacco-ledger/src/main/resources/db/changelog/ledger/005-seed-bad-debt-expense.yaml`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/entity/TransactionType.java`
**Action:**
1. Create NEW changeset `005-seed-bad-debt-expense.yaml` (do NOT modify existing 002-seed). Insert account 5003 "Bad Debt Expense" type EXPENSE into `accounts` table. Follow the exact column pattern from 002-seed-chart-of-accounts.yaml.
2. Add to `TransactionType` enum: `LOAN_REVERSAL`, `CONTRIBUTION_REVERSAL`, `PENALTY_WAIVER`, `BENEFIT_DISTRIBUTION`.
3. Register the new changeset in the ledger changelog master include list.

**Verify:**
```bash
mvn compile -pl sacco-ledger -am -q
```

**Done when:** New migration file exists with correct changeset. TransactionType has 4 new values. Compiles cleanly.

### Task 3: Create lifecycle event records
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanApplicationEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanStatusChangeEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/ContributionCreatedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/PayoutStatusChangeEvent.java`
**Action:**
Create 4 minimal audit-only event records (per CONTEXT.md "lifecycle events" decision):
1. `LoanApplicationEvent(UUID loanId, UUID memberId, String action, String actor)` — action: "APPLIED"/"APPROVED"/"REJECTED"
2. `LoanStatusChangeEvent(UUID loanId, String previousStatus, String newStatus, String actor)` — for close/default
3. `ContributionCreatedEvent(UUID contributionId, UUID memberId, BigDecimal amount, String referenceNumber, String actor)` — for record/bulk
4. `PayoutStatusChangeEvent(UUID payoutId, UUID memberId, String action, String actor)` — action: "CREATED"/"APPROVED"

All implement `AuditableEvent`. Keep payloads minimal for audit efficiency (per constraint).

**Verify:**
```bash
mvn compile -pl sacco-common -q
```

**Done when:** All 4 lifecycle event records compile and implement AuditableEvent.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-common,sacco-ledger -am -q
mvn test -pl sacco-common,sacco-ledger -am -DskipITs -q
```

## Commit Message
```
feat(event-gl-completeness): add event records, Liquibase migration, and TransactionType values

- Create PenaltyWaivedEvent record for penalty waiver GL
- Add penaltyPortion field to LoanReversalEvent
- Add Liquibase migration for account 5003 Bad Debt Expense
- Add LOAN_REVERSAL, CONTRIBUTION_REVERSAL, PENALTY_WAIVER, BENEFIT_DISTRIBUTION to TransactionType
- Create lifecycle event records: LoanApplicationEvent, LoanStatusChangeEvent, ContributionCreatedEvent, PayoutStatusChangeEvent
```

---
*Planned: 2026-02-15*
