# Plan 02: Batch Processing Safeguards

## Goal
Rewrite `LoanBatchServiceImpl` with all 7 safeguards (7.2-7.8) and update the controller to support the new processing flow.

## Prerequisites
- [ ] Plan 01 complete (BatchProcessingLog entity, migrations, DTO updates)

## Tasks

### Task 1: Implement idempotency, sequential enforcement, and batch log tracking
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchService.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`

**Action:**
1. Add new method to `LoanBatchService` interface:
   - `BatchProcessingResult processMonthlyLoans(YearMonth targetMonth, String triggeredBy)`
2. In `LoanBatchServiceImpl`:
   - Inject `BatchProcessingLogRepository` and `ConfigService`
   - Implement `processMonthlyLoans(YearMonth targetMonth, String triggeredBy)`:
     a. **Idempotency (7.4)**: Check `batchLogRepo.existsByProcessingMonth(targetMonth.toString())` — if exists, throw `IllegalStateException("Month already processed: " + targetMonth)`
     b. **Sequential enforcement (7.5)**: Query `loan.batch.last_processed_month` from `ConfigService`. If not empty, parse to YearMonth and verify `targetMonth == lastProcessed.plusMonths(1)`. If not sequential, throw `IllegalStateException("Must process [missing month] first")`
     c. **Batch log (7.8)**: Create `BatchProcessingLog` with status=STARTED, save it
     d. Delegate to the actual loan processing logic (existing interest accrual + overdue detection)
     e. On success: update log to COMPLETED with counts, update `loan.batch.last_processed_month` config key
     f. On failure: update log to FAILED
   - Update existing `processOutstandingLoans()` (the `@Scheduled` cron method) to auto-determine target month and delegate to `processMonthlyLoans()`

**Verify:**
```bash
cd sacco-loan && mvn compile -q
```

**Done when:** Idempotency check prevents double-processing, sequential enforcement rejects skipped months, batch log tracks each run.

### Task 2: Implement loan filtering, configurable day, and pre-processing warnings
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`

**Action:**
1. Within `processMonthlyLoans()`, after safeguard checks:
   a. **Configurable day (7.2)**: Read `loan.batch.processing_day_of_month` from `ConfigService`. In the `@Scheduled` auto-trigger, check if today's day >= configured day before proceeding.
   b. **Same-month skip (7.6)**: Filter out loans where `disbursedAt` falls within the target processing month. Use `YearMonth.from(loan.getDisbursedAt().atZone(ZoneId.of("UTC")))` to compare.
   c. **New-loan threshold (7.3)**: Read `loan.batch.new_loan_threshold_day` from config. For loans disbursed in the month BEFORE the target month, check if the disbursement day-of-month < threshold day. If not (disbursed after threshold), skip interest accrual for this run.
   d. **Pre-processing warnings (7.7)**: Before processing, call `detectUnpaidLoans()` for the target month. Convert results to warning strings and include in `BatchProcessingResult.warnings`. Also store summary in `BatchProcessingLog.warningsSummary`.
2. Ensure filtered loan list is used for the interest accrual loop (only eligible loans get interest applied).

**Verify:**
```bash
cd sacco-loan && mvn compile -q
```

**Done when:** Loans are correctly filtered by disbursedAt, threshold day is respected, warnings are populated.

### Task 3: Update LoanBatchController
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/controller/LoanBatchController.java`

**Action:**
1. Update `processBatch()` endpoint to accept optional `@RequestParam YearMonth targetMonth`:
   - If provided: use that month (for manual catch-up processing)
   - If not provided: auto-determine next month to process (last processed + 1)
2. Pass `triggeredBy` from `getCurrentUsername()` to `processMonthlyLoans()`
3. Handle new exceptions from safeguards:
   - `IllegalStateException` for "Month already processed" — return `ApiResponse.error(409, message)`
   - `IllegalStateException` for "Must process [month] first" — return `ApiResponse.error(409, message)`
4. Add `@ExceptionHandler` or use existing `GlobalExceptionHandler` for these cases

**Verify:**
```bash
cd sacco-loan && mvn compile -q
```

**Done when:** Controller accepts optional targetMonth, passes triggeredBy, and returns clear error responses for safeguard violations.

## Verification

After all tasks:
```bash
mvn compile -q
```

## Commit Message
```
feat(loan-batch): implement monthly processing safeguards (7.2-7.8)

- Add idempotency check to prevent same-month double-processing
- Enforce sequential month processing (reject skipped months)
- Filter loans by disbursedAt to skip same-month disbursements
- Add configurable new-loan threshold day for interest accrual
- Integrate pre-processing warnings for unpaid loans
- Track each batch run in batch_processing_log table
- Update controller to accept optional targetMonth parameter
```

---
*Planned: 2026-02-15*
