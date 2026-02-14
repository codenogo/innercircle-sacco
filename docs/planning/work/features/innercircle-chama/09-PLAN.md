# Plan 09: Payout Management

## Goal
Implement payout processing (merry-go-round, ad-hoc), bank withdrawals, cash disbursements with approval workflow and domain events.

## Prerequisites
- [ ] Plan 01 complete

## Tasks

### Task 1: Payout Entities + Repository
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/Payout.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/PayoutType.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/PayoutStatus.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/BankWithdrawal.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/WithdrawalStatus.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/CashDisbursement.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/entity/ShareWithdrawal.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/repository/PayoutRepository.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/repository/BankWithdrawalRepository.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/repository/CashDisbursementRepository.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/repository/ShareWithdrawalRepository.java`
**Action:**
1. `Payout` entity: memberId, amount, type (MERRY_GO_ROUND, AD_HOC, DIVIDEND), status (PENDING, APPROVED, PROCESSED, FAILED), approvedBy, processedAt, referenceNumber
2. `BankWithdrawal` entity: memberId, amount, bankName, accountNumber, referenceNumber, status, transactionDate, reconciled
3. `CashDisbursement` entity: memberId, amount, receivedBy, disbursedBy, signoffBy, receiptNumber, disbursementDate
4. `ShareWithdrawal` entity: memberId, amount, type (PARTIAL, FULL), status (PENDING, APPROVED, PROCESSED), currentShareBalance, newShareBalance, approvedBy
5. Repositories with cursor pagination and member/status/date filters

**Verify:**
```bash
mvn compile -pl sacco-payout -q
```

**Done when:** All payout entities and repositories compile.

### Task 2: Payout Service + Events
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PayoutService.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PayoutServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/BankWithdrawalService.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/BankWithdrawalServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/CashDisbursementService.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/CashDisbursementServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/ShareWithdrawalService.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/ShareWithdrawalServiceImpl.java`
**Action:**
1. `PayoutService`: createPayout, approvePayout (TREASURER/ADMIN), processPayout (publishes `PayoutProcessedEvent`), getPayoutHistory
2. `BankWithdrawalService`: initiateWithdrawal, confirmWithdrawal, markReconciled, getUnreconciled
3. `CashDisbursementService`: recordDisbursement (with receipt number), signoff (by treasurer), getDisbursementHistory
4. `ShareWithdrawalService`: requestWithdrawal, approveWithdrawal, processWithdrawal (validates balance, updates member share balance)
5. All mutations publish AuditableEvent subtypes

**Verify:**
```bash
mvn compile -pl sacco-payout -q
```

**Done when:** All payout services compile with event publishing.

### Task 3: Payout REST API + Liquibase
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/PayoutController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/BankWithdrawalController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/CashDisbursementController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/ShareWithdrawalController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/dto/PayoutRequest.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/dto/PayoutResponse.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/dto/BankWithdrawalRequest.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/dto/CashDisbursementRequest.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/dto/ShareWithdrawalRequest.java`, `sacco-payout/src/main/resources/db/changelog/payout/001-create-payout-tables.yaml`
**Action:**
1. Controllers at `/api/v1/payouts`, `/api/v1/bank-withdrawals`, `/api/v1/cash-disbursements`, `/api/v1/share-withdrawals`
2. Full CRUD + approval + processing endpoints with role-based access
3. Liquibase: payouts, bank_withdrawals, cash_disbursements, share_withdrawals tables with indexes
4. Update sacco-app changelog-master

**Verify:**
```bash
mvn compile -pl sacco-payout -q
```

**Done when:** REST APIs and changelogs complete.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-payout -q
```

## Commit Message
```
feat(payout): implement payout, bank withdrawal, cash disbursement, share withdrawal

- Merry-go-round and ad-hoc payout workflows
- Bank withdrawal with reconciliation tracking
- Cash disbursement with receipt and signoff
- Share withdrawal with balance validation
```

---
*Planned: 2026-02-14*
