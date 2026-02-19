# Plan 02: Enforce maker-checker on loan approval: fix createdBy bug, add overrideReason to approve flow, wire MakerCheckerGuard

## Goal
Enforce maker-checker on loan approval: fix createdBy bug, add overrideReason to approve flow, wire MakerCheckerGuard

## Tasks

### Task 1: Fix LoanApplication createdBy and create ApproveLoanRequest DTO
**CWD:** `sacco-loan`
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/ApproveLoanRequest.java`
**Action:**
In LoanServiceImpl.applyForLoan(), add loan.setCreatedBy(actor) using the actor parameter (derive from MemberAccessHelper pattern used by the controller — may need to add actor param if not present). Create ApproveLoanRequest DTO with optional overrideReason field (@Size(max=500)).

**Verify:**
```bash
mvn -pl sacco-loan compile -q
```

**Done when:** [Observable outcome]

### Task 2: Wire MakerCheckerGuard into approveLoan and update controller
**CWD:** `sacco-loan`
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanService.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/controller/LoanController.java`
**Action:**
Update LoanService.approveLoan() signature to accept overrideReason and isAdmin flag (or Authentication). In LoanServiceImpl.approveLoan(), call MakerCheckerGuard.assertOrOverride(loan.getCreatedBy(), actor, overrideReason, isAdmin) before the status transition. If override is used, publish event with type 'OVERRIDE_APPROVED' instead of 'APPROVED'. Update LoanController to accept @RequestBody ApproveLoanRequest, extract isAdmin from Authentication, and pass to service. Apply same pattern to rejectLoan (reject should also be maker-checker protected).

**Verify:**
```bash
mvn -pl sacco-loan compile -q
```

**Done when:** [Observable outcome]

### Task 3: Unit tests for loan maker-checker enforcement
**CWD:** `sacco-loan`
**Files:** `sacco-loan/src/test/java/com/innercircle/sacco/loan/service/LoanServiceImplTest.java`
**Action:**
Add tests: (1) approveLoan by different user succeeds, (2) approveLoan by same user (creator) throws MakerCheckerViolationException, (3) approveLoan by same user with ADMIN override + reason succeeds, (4) approveLoan by same user with ADMIN override but no reason throws, (5) verify createdBy is now set in applyForLoan, (6) rejectLoan by same user throws.

**Verify:**
```bash
mvn -pl sacco-loan test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-loan test -q
```

## Commit Message
```
feat(maker-checker-controls): enforce maker-checker on loan approval with ADMIN override
```
