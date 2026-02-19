# Plan 03: Enforce maker-checker on Payout, Share Withdrawal, and Petty Cash approval flows

## Goal
Enforce maker-checker on Payout, Share Withdrawal, and Petty Cash approval flows

## Tasks

### Task 1: Wire guard into PayoutServiceImpl and ShareWithdrawalServiceImpl
**CWD:** `sacco-payout`
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PayoutServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PayoutService.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/ShareWithdrawalServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/ShareWithdrawalService.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/PayoutController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/ShareWithdrawalController.java`
**Action:**
Add overrideReason + isAdmin params to approvePayout and approveWithdrawal service methods. Call MakerCheckerGuard.assertOrOverride(entity.getCreatedBy(), actor, overrideReason, isAdmin) before status transition. Create ApprovePayoutRequest and ApproveShareWithdrawalRequest DTOs with optional overrideReason. Update controllers to accept DTOs and extract isAdmin from Authentication. If override, publish event with 'OVERRIDE_APPROVED' type.

**Verify:**
```bash
mvn -pl sacco-payout compile -q
```

**Done when:** [Observable outcome]

### Task 2: Wire guard into PettyCashServiceImpl
**CWD:** `sacco-payout`
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PettyCashServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PettyCashService.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/PettyCashController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/dto/ApprovePettyCashRequest.java`
**Action:**
Add overrideReason + isAdmin params to PettyCashService.approveVoucher(). Call MakerCheckerGuard.assertOrOverride(voucher.getCreatedBy(), actor, overrideReason, isAdmin) before SUBMITTED->APPROVED transition. Create ApprovePettyCashRequest DTO with optional overrideReason. Update PettyCashController to accept DTO and extract isAdmin from Authentication.

**Verify:**
```bash
mvn -pl sacco-payout compile -q
```

**Done when:** [Observable outcome]

### Task 3: Unit tests for payout, share withdrawal, and petty cash maker-checker
**CWD:** `sacco-payout`
**Files:** `sacco-payout/src/test/java/com/innercircle/sacco/payout/service/PayoutServiceImplTest.java`, `sacco-payout/src/test/java/com/innercircle/sacco/payout/service/ShareWithdrawalServiceImplTest.java`, `sacco-payout/src/test/java/com/innercircle/sacco/payout/service/PettyCashServiceImplTest.java`
**Action:**
For each service, add tests: (1) approve by different user succeeds, (2) approve by creator throws MakerCheckerViolationException, (3) approve by creator with ADMIN override + reason succeeds, (4) approve by creator with override but no reason throws. Follow existing test patterns (Mockito, @ExtendWith(MockitoExtension.class)).

**Verify:**
```bash
mvn -pl sacco-payout test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-payout test -q
```

## Commit Message
```
feat(maker-checker-controls): enforce maker-checker on payout, share withdrawal, and petty cash approvals
```
