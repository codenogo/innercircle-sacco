# Plan 03: Publisher Wiring + Lifecycle Events

## Goal
Wire event publishing for contribution reversals, penalty waivers, and all lifecycle events across sacco-contribution, sacco-loan, and sacco-payout modules.

## Prerequisites
- [ ] Plan 01 complete (event records exist)
- [ ] Plan 02 complete (GL handlers exist to consume events)

## Tasks

### Task 1: Wire ContributionReversedEvent + PenaltyWaivedEvent publishing
**Files:** `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionServiceImpl.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionPenaltyServiceImpl.java`
**Action:**
1. In `ContributionServiceImpl.reverseContribution()`: Replace the `// TODO: Publish ContributionReversedEvent when defined` with:
   ```java
   eventPublisher.publishEvent(new ContributionReversedEvent(
       contribution.getId(), contribution.getMemberId(),
       contribution.getAmount(), contribution.getReferenceNumber(), actor));
   ```
   Ensure `ApplicationEventPublisher` is injected (check if already present).

2. In `ContributionPenaltyServiceImpl.waivePenalty()`: Replace the `// TODO: Publish PenaltyWaivedEvent when defined` with:
   ```java
   eventPublisher.publishEvent(new PenaltyWaivedEvent(
       penalty.getId(), penalty.getMemberId(),
       penalty.getAmount(), "Waived by " + actor, actor));
   ```
   Ensure `ApplicationEventPublisher` is injected.

**Verify:**
```bash
mvn compile -pl sacco-contribution -am -q
mvn test -pl sacco-contribution -am -DskipITs -q
```

**Done when:** Both TODO comments replaced with event publishing. Existing tests still pass. ContributionReversedEvent and PenaltyWaivedEvent are now published.

### Task 2: Wire lifecycle events in sacco-loan
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`
**Action:**
1. In `LoanServiceImpl`: Publish `LoanApplicationEvent` in:
   - `applyForLoan()` — action "APPLIED"
   - `approveLoan()` — action "APPROVED"
   - `rejectLoan()` — action "REJECTED"

2. In `LoanServiceImpl` or relevant method: Publish `LoanStatusChangeEvent` in:
   - `closeLoan()` — previousStatus from entity, newStatus "CLOSED"
   - Any `defaultLoan()` or equivalent — previousStatus from entity, newStatus "DEFAULTED"

Use existing `ApplicationEventPublisher` (already injected for other events).

**Verify:**
```bash
mvn compile -pl sacco-loan -am -q
mvn test -pl sacco-loan -am -DskipITs -q
```

**Done when:** Loan lifecycle events published at all relevant state transitions. Existing tests pass.

### Task 3: Wire lifecycle events in sacco-contribution + sacco-payout
**Files:** `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PayoutServiceImpl.java`
**Action:**
1. In `ContributionServiceImpl.recordContribution()` and `bulkRecordContributions()`: Publish `ContributionCreatedEvent` after saving each contribution.

2. In `PayoutServiceImpl`: Publish `PayoutStatusChangeEvent` in:
   - `createPayout()` — action "CREATED"
   - `approvePayout()` — action "APPROVED"

Use existing `ApplicationEventPublisher` in each service.

**Verify:**
```bash
mvn compile -pl sacco-contribution,sacco-payout -am -q
mvn test -pl sacco-contribution,sacco-payout -am -DskipITs -q
```

**Done when:** ContributionCreatedEvent and PayoutStatusChangeEvent published at correct lifecycle points. All existing tests pass.

## Verification

After all tasks:
```bash
mvn test -pl sacco-contribution,sacco-loan,sacco-payout -am -DskipITs -q
```

## Commit Message
```
feat(event-gl-completeness): wire event publishing for reversals, waivers, and lifecycle

- Publish ContributionReversedEvent in reverseContribution()
- Publish PenaltyWaivedEvent in waivePenalty()
- Publish LoanApplicationEvent for apply/approve/reject
- Publish LoanStatusChangeEvent for close/default
- Publish ContributionCreatedEvent for record/bulk
- Publish PayoutStatusChangeEvent for create/approve
```

---
*Planned: 2026-02-15*
