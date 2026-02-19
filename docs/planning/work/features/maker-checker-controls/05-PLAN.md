# Plan 05: Add status workflow to Cash Disbursements (PENDING->APPROVED->RECORDED) with maker-checker enforcement

## Goal
Add status workflow to Cash Disbursements (PENDING->APPROVED->RECORDED) with maker-checker enforcement

## Tasks

### Task 1: Add status enum, entity fields, and Liquibase migration for Cash Disbursements
**CWD:** `sacco-payout`
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/CashDisbursement.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/CashDisbursementStatus.java`, `sacco-payout/src/main/resources/db/changelog/payout/004-cash-disbursement-add-status-workflow.yaml`, `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`
**Action:**
Create CashDisbursementStatus enum: PENDING, APPROVED, RECORDED. Add status field to CashDisbursement entity (default PENDING), add approvedBy (String, nullable) and approvedAt (Instant, nullable) fields. Create Liquibase migration to add status (varchar, not null, default 'PENDING'), approved_by (varchar 255, nullable), approved_at (timestamp, nullable) columns. Register in master changelog. Update recordDisbursement to create in PENDING status instead of directly recording.

**Verify:**
```bash
mvn -pl sacco-payout compile -q
```

**Done when:** [Observable outcome]

### Task 2: Add approve and record service methods, controller endpoints, wire guard
**CWD:** `sacco-payout`
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/CashDisbursementServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/CashDisbursementService.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/CashDisbursementController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/dto/ApproveCashDisbursementRequest.java`
**Action:**
Refactor: (1) Rename existing recordDisbursement to createDisbursement — creates in PENDING status. (2) Add approveDisbursement(id, actor, overrideReason, isAdmin) — validates PENDING, calls MakerCheckerGuard, transitions to APPROVED. (3) Add recordDisbursement(id, actor) — validates APPROVED, transitions to RECORDED, sets receipt details. Create ApproveCashDisbursementRequest DTO. Add PUT /api/v1/cash-disbursements/{id}/approve endpoint. Update existing POST to create in PENDING. Existing signoff can remain on RECORDED status.

**Verify:**
```bash
mvn -pl sacco-payout compile -q
```

**Done when:** [Observable outcome]

### Task 3: Unit tests for Cash Disbursement workflow and maker-checker
**CWD:** `sacco-payout`
**Files:** `sacco-payout/src/test/java/com/innercircle/sacco/payout/service/CashDisbursementServiceImplTest.java`
**Action:**
Test: (1) createDisbursement sets PENDING status, (2) approveDisbursement by different user succeeds (PENDING->APPROVED), (3) approve by creator throws, (4) approve with ADMIN override succeeds, (5) recordDisbursement requires APPROVED status, (6) signoff still works on RECORDED, (7) full flow: create -> approve -> record -> signoff.

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
feat(maker-checker-controls): add status workflow and maker-checker to cash disbursements
```
