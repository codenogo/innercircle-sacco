# Plan 02: Batch Loan Processing & Unpaid Detection

## Goal
Implement automated monthly batch loan processing (interest accrual, penalty application, status updates) and unpaid loan detection.

## Prerequisites
- [x] Loan module with repayment schedule
- [x] Penalty service implemented
- [x] Ledger integration via events

## Tasks

### Task 1: Create LoanBatchService for Monthly Processing
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchService.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/BatchProcessingResult.java`
- `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanBatchProcessedEvent.java`

**Action:**
Create batch processing service with methods:
- `processOutstandingLoans()` — iterates all REPAYING loans, checks for overdue installments, applies penalties for late payments, updates loan statuses. Returns `BatchProcessingResult` with counts of processed, penalized, and closed loans.
- `detectUnpaidLoans(LocalDate month)` — finds all loans with unpaid installments for a given month, returns list of overdue loan details.
- `processLoan(UUID loanId)` — process a single loan (check schedule, apply penalties if overdue).

Use `@Scheduled(cron = "${sacco.batch.loan-processing-cron:0 0 1 1 * *}")` for monthly auto-processing (1st of each month at 1 AM). Query overdue schedules using `RepaymentScheduleRepository`. Publish `LoanBatchProcessedEvent` after batch completes.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-common,sacco-loan -q -DskipTests
```

**Done when:** Batch service compiles, scheduled annotation configured, penalty application logic complete.

### Task 2: Create Loan Reversal System
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanReversalService.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanReversalServiceImpl.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/ReversalRequest.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/ReversalResponse.java`
- `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanReversalEvent.java`

**Action:**
Create reversal service with methods:
- `reverseRepayment(UUID repaymentId, String reason, String actor)` — reverses a loan repayment: marks original repayment as REVERSED, restores loan outstanding balance, un-marks repayment schedule installments, publishes `LoanReversalEvent` for compensating ledger entries.
- `reversePenalty(UUID penaltyId, String reason, String actor)` — reverses a penalty application.

Both operations require ADMIN or TREASURER role. Both create audit-friendly records and publish events for ledger compensation.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-common,sacco-loan -q -DskipTests
```

**Done when:** Reversal service compiles, compensating logic is correct, events published.

### Task 3: Create Batch & Reversal Controller Endpoints
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/controller/LoanBatchController.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/RepaymentStatus.java` (add REVERSED status)

**Action:**
Create REST controller at `/api/v1/loans/batch` with endpoints:
- `POST /api/v1/loans/batch/process` — Trigger batch processing (ADMIN/TREASURER). Returns BatchProcessingResult.
- `GET /api/v1/loans/batch/unpaid?month={YYYY-MM}` — Detect unpaid loans for a month (ADMIN/TREASURER).
- `POST /api/v1/loans/reversals/repayment/{repaymentId}` — Reverse a repayment (ADMIN/TREASURER). Body: ReversalRequest with reason.
- `POST /api/v1/loans/reversals/penalty/{penaltyId}` — Reverse a penalty (ADMIN/TREASURER). Body: ReversalRequest with reason.

Add REVERSED to RepaymentStatus enum. All endpoints require `@PreAuthorize("hasAnyRole('ADMIN','TREASURER')")`.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-loan -q -DskipTests
```

**Done when:** Controller compiles, all endpoints secured, enum updated.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn clean compile -q -DskipTests
```

## Commit Message
```
feat(loan): add batch processing, unpaid detection, and reversal system

- Monthly batch processing with scheduled cron job
- Unpaid loan detection for a given month
- Repayment and penalty reversal with compensating events
- ADMIN/TREASURER authorization for batch operations
```

---
*Planned: 2026-02-15*
