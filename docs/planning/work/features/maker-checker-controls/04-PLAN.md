# Plan 04: Add approval workflow to Bank Withdrawals (new APPROVED status, migration, endpoint) with maker-checker enforcement

## Goal
Add approval workflow to Bank Withdrawals (new APPROVED status, migration, endpoint) with maker-checker enforcement

## Tasks

### Task 1: Add APPROVED status to BankWithdrawal entity and Liquibase migration
**CWD:** `sacco-payout`
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/BankWithdrawal.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/WithdrawalStatus.java`, `sacco-payout/src/main/resources/db/changelog/payout/003-bank-withdrawal-add-approval.yaml`, `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`
**Action:**
Add APPROVED to WithdrawalStatus enum (between PENDING and COMPLETED if possible, or at end). Add approvedBy (String, nullable) and approvedAt (Instant, nullable) fields to BankWithdrawal entity. Create Liquibase migration to add approved_by (varchar 255, nullable) and approved_at (timestamp, nullable) columns to bank_withdrawals table. Register migration in master changelog. Update confirmWithdrawal to require APPROVED status (not PENDING).

**Verify:**
```bash
mvn -pl sacco-payout compile -q
```

**Done when:** [Observable outcome]

### Task 2: Add approveWithdrawal service method, controller endpoint, and wire guard
**CWD:** `sacco-payout`
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/BankWithdrawalServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/BankWithdrawalService.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/BankWithdrawalController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/dto/ApproveBankWithdrawalRequest.java`
**Action:**
Add approveWithdrawal(UUID withdrawalId, String actor, String overrideReason, boolean isAdmin) to BankWithdrawalService interface and impl. Method validates PENDING status, calls MakerCheckerGuard.assertOrOverride(), transitions to APPROVED, sets approvedBy and approvedAt. Create ApproveBankWithdrawalRequest DTO with optional overrideReason. Add PUT /api/v1/bank-withdrawals/{id}/approve endpoint to controller. Update flow: PENDING → APPROVED → COMPLETED.

**Verify:**
```bash
mvn -pl sacco-payout compile -q
```

**Done when:** [Observable outcome]

### Task 3: Unit tests for Bank Withdrawal approval and maker-checker
**CWD:** `sacco-payout`
**Files:** `sacco-payout/src/test/java/com/innercircle/sacco/payout/service/BankWithdrawalServiceImplTest.java`
**Action:**
Test: (1) approveWithdrawal by different user succeeds, (2) approve by creator throws MakerCheckerViolationException, (3) approve with ADMIN override succeeds, (4) confirmWithdrawal now requires APPROVED status (not PENDING), (5) confirmWithdrawal on PENDING throws, (6) full flow: initiate -> approve -> confirm works.

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
feat(maker-checker-controls): add approval workflow to bank withdrawals with maker-checker
```
