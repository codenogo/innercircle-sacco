# Plan 02: Add correlationId to AuditableEvent interface and update all 18 event records

## Goal
Add correlationId to AuditableEvent interface and update all 18 event records

## Tasks

### Task 1: Extend AuditableEvent interface with correlationId and update first 9 event records
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/event/AuditableEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/MemberCreatedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/ContributionReceivedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/ContributionCreatedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/ContributionReversedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanApplicationEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanDisbursedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanRepaymentEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanStatusChangeEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanReversalEvent.java`
**Action:**
Add UUID getCorrelationId() method to AuditableEvent interface. Update the first 9 event records to include a UUID correlationId parameter in the record constructor and implement getCorrelationId(). For each record: add correlationId as the last parameter before actor.

**Verify:**
```bash
mvn -pl sacco-common compile -q
```

**Done when:** [Observable outcome]

### Task 2: Update remaining 8 event records with correlationId
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanInterestAccrualEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanBatchProcessedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/PayoutProcessedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/PayoutStatusChangeEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/PenaltyAppliedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/PenaltyPaidEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/PenaltyWaivedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/BenefitsDistributedEvent.java`
**Action:**
Update the remaining 8 event records to include UUID correlationId parameter in the record constructor and implement getCorrelationId(). Same pattern as task 1.

**Verify:**
```bash
mvn -pl sacco-common compile -q
```

**Done when:** [Observable outcome]

### Task 3: Fix all event construction call sites across modules to pass correlationId
**Files:** `sacco-member/src/main/java/com/innercircle/sacco/member/service/MemberServiceImpl.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanReversalServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBenefitServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanPenaltyServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PayoutServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/BankWithdrawalServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/ShareWithdrawalServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/CashDisbursementServiceImpl.java`
**Action:**
Find every call site that constructs any of the 17 event records (excluding AuditableEvent interface) and add UUID.randomUUID() as the correlationId argument. Use a single correlationId per business operation (e.g., one UUID per service method call shared across all events published in that method). Fix all compilation errors in test files that construct events directly.

**Verify:**
```bash
mvn -pl sacco-common,sacco-member,sacco-contribution,sacco-loan,sacco-payout compile -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-common,sacco-member,sacco-contribution,sacco-loan,sacco-payout,sacco-ledger,sacco-audit compile -q
mvn -pl sacco-common,sacco-member,sacco-contribution,sacco-loan,sacco-payout,sacco-ledger,sacco-audit test -q
```

## Commit Message
```
feat(event-hardening): add correlationId to AuditableEvent and all 17 event records
```
