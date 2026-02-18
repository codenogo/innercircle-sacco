# Plan 06: Refactor all services to use TransitionGuard.validate() instead of ad-hoc status checks

## Goal
Refactor all services to use TransitionGuard.validate() instead of ad-hoc status checks

## Tasks

### Task 1: Refactor loan module to use TransitionGuard for all status transitions
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanReversalServiceImpl.java`
**Action:**
Replace all ad-hoc 'if (status != X) throw IllegalStateException(...)' checks with TransitionGuard.LOAN.validate(currentStatus, targetStatus) calls. In LoanServiceImpl: approveLoan, rejectLoan, disburseLoan, recordRepayment (REPAYING transition), closeLoan. In LoanBatchServiceImpl: batch status transitions (DEFAULTED, CLOSED). In LoanReversalServiceImpl: reversal status checks. Inject or reference TransitionGuards.LOAN. Update test expectations: InvalidStateTransitionException instead of IllegalStateException.

**Verify:**
```bash
mvn -pl sacco-loan compile -q
mvn -pl sacco-loan test -q
```

**Done when:** [Observable outcome]

### Task 2: Refactor payout and contribution services to use TransitionGuard
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PayoutServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/BankWithdrawalServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/ShareWithdrawalServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/CashDisbursementServiceImpl.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionServiceImpl.java`
**Action:**
Replace ad-hoc status checks with TransitionGuards.PAYOUT.validate() and TransitionGuards.CONTRIBUTION.validate() respectively. In PayoutServiceImpl/BankWithdrawal/ShareWithdrawal/CashDisbursement: approve and process transitions. In ContributionServiceImpl: confirm and reverse transitions. Update test expectations.

**Verify:**
```bash
mvn -pl sacco-payout,sacco-contribution compile -q
mvn -pl sacco-payout,sacco-contribution test -q
```

**Done when:** [Observable outcome]

### Task 3: Refactor member service to use TransitionGuard and add GlobalExceptionHandler mapping
**Files:** `sacco-member/src/main/java/com/innercircle/sacco/member/service/MemberServiceImpl.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/exception/GlobalExceptionHandler.java`
**Action:**
Replace ad-hoc member status checks with TransitionGuards.MEMBER.validate() in MemberServiceImpl (suspend, reactivate, deactivate methods). Add InvalidStateTransitionException handler to GlobalExceptionHandler that returns 409 Conflict with message containing current state, target state, and entity type. Update test expectations.

**Verify:**
```bash
mvn -pl sacco-member,sacco-common compile -q
mvn -pl sacco-member,sacco-common test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-common,sacco-member,sacco-contribution,sacco-loan,sacco-payout compile -q
mvn -pl sacco-common,sacco-member,sacco-contribution,sacco-loan,sacco-payout test -q
```

## Commit Message
```
feat(event-hardening): replace ad-hoc status checks with TransitionGuard across all modules
```
