# Loan Batch Processing - Implementation Context

## Problem Statement

The current loan batch processing system (`LoanBatchServiceImpl`) has significant gaps against the 8 feature requirements (7.1-7.8). While basic interest accrual and overdue detection work, critical safeguards like idempotency, sequential enforcement, and configurable processing dates are missing.

## Gap Analysis

| Req | Description | Status | Notes |
|-----|-------------|--------|-------|
| 7.1 | Process loans monthly: calculate and apply interest | MET | `accrueMonthlyInterest()` handles both REDUCING_BALANCE and FLAT_RATE |
| 7.2 | Configurable day-of-month for loan processing | PARTIAL | Cron expression in `application.yml` but not runtime-configurable |
| 7.3 | Configurable day-of-month threshold for new loans | MISSING | No threshold logic exists |
| 7.4 | Prevent processing the same month twice | MISSING | Critical - no idempotency check |
| 7.5 | Prevent skipping months (must process sequentially) | MISSING | No sequential enforcement |
| 7.6 | Skip loans created in same month as processing date | MISSING | No disbursedAt filtering |
| 7.7 | Pre-processing check: warn about unpaid loans | PARTIAL | `detectUnpaidLoans()` exists but not integrated into batch flow |
| 7.8 | Track the last processed date in system configuration | MISSING | No tracking mechanism |

## Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Config Store (7.2, 7.3) | SystemConfig DB table (key-value pairs) | Runtime-editable without redeployment; aligns with existing `system_configs` table and `ConfigService` API |
| State Tracking (7.4, 7.5, 7.8) | Dedicated `batch_processing_log` table | New entity tracks each batch run with month, status, loan count, timestamp; provides audit trail and idempotency check |
| Warning Mode (7.7) | Return warnings in `BatchProcessingResult` | Include unpaid loan warnings in the result DTO; processing proceeds regardless — warnings are informational |
| Manual Trigger Behavior | Same safeguard rules apply | Manual trigger via `POST /api/v1/loans/batch/process` respects all checks (idempotency, sequential, threshold); no bypass |
| Skip Handling (7.5) | Reject and require catch-up | If last processed = January and current = March, reject with error "Must process February first"; enforces strict sequential processing |
| Date Field (7.3, 7.6) | `disbursedAt` (Instant on LoanApplication) | Actual disbursement date is when interest liability begins; field already exists on the entity |

## Config Keys to Add

| Key | Default | Description |
|-----|---------|-------------|
| `loan.batch.processing_day_of_month` | `1` | Day of month when batch processing runs |
| `loan.batch.new_loan_threshold_day` | `15` | Loans disbursed before this day accrue interest that month |

## New Entity: BatchProcessingLog

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| processingMonth | YearMonth | The month being processed (e.g. 2026-02) |
| status | Enum | STARTED, COMPLETED, FAILED |
| loansProcessed | int | Count of loans processed |
| interestAccrued | BigDecimal | Total interest accrued |
| penalizedLoans | int | Count of penalized loans |
| closedLoans | int | Count of closed loans |
| warnings | List/JSON | Pre-processing warnings |
| startedAt | Instant | When processing started |
| completedAt | Instant | When processing completed |
| triggeredBy | String | SYSTEM (cron) or user ID |

## BatchProcessingResult DTO Changes

Add `List<String> warnings` field to hold pre-processing check results (unpaid loan warnings per 7.7).

## Processing Flow (After Implementation)

```
1. Receive trigger (cron or manual POST)
2. Determine target month
3. CHECK: Has this month already been processed? (query batch_processing_log)
   - If yes: REJECT with "Month already processed"
4. CHECK: Is this the next sequential month after last processed?
   - If no: REJECT with "Must process [missing month] first"
5. Create BatchProcessingLog entry (status=STARTED)
6. PRE-PROCESSING: Run unpaid loan detection for target month
   - Build warnings list for loans that will attract penalties
7. Get all REPAYING loans
8. FILTER: Exclude loans where disbursedAt is in the same month as target month
9. FILTER (threshold): For loans disbursed in previous month, check if disbursedAt day < threshold day
10. For each eligible loan: accrue monthly interest
11. For each loan: check overdue/default/closure status
12. Update BatchProcessingLog (status=COMPLETED, counts, warnings)
13. Update SystemConfig "loan.batch.last_processed_month"
14. Publish LoanBatchProcessedEvent
15. Return BatchProcessingResult with warnings
```

## Constraints

- Must maintain backward compatibility with existing `LoanBatchProcessedEvent` consumers
- Must work within the existing `@TransactionalEventListener` GL posting pattern
- Liquibase migrations must have rollback support
- The `batch_processing_log` table needs a unique constraint on `processing_month` for idempotency
- All new config keys must be seeded via Liquibase changeset

## Open Questions

- None — all decision points resolved during discussion

## Related Code

- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java` — Main batch service (primary modification target)
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchService.java` — Interface
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/controller/LoanBatchController.java` — REST API
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/BatchProcessingResult.java` — Result DTO
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanApplication.java` — Has disbursedAt field
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/RepaymentSchedule.java` — Repayment schedules
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanInterestHistory.java` — Interest audit trail
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/InterestCalculator.java` — Interest calculation utility
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/LoanApplicationRepository.java` — Loan queries
- `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanBatchProcessedEvent.java` — Batch event
- `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanInterestAccrualEvent.java` — Interest event
- `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/FinancialEventListener.java` — GL posting
- `sacco-config/src/main/java/com/innercircle/sacco/config/entity/SystemConfig.java` — Config entity
- `sacco-config/src/main/java/com/innercircle/sacco/config/service/ConfigService.java` — Config service interface
- `sacco-config/src/main/java/com/innercircle/sacco/config/service/ConfigServiceImpl.java` — Config service impl
- `sacco-config/src/main/resources/db/changelog/config/002-seed-default-config.yaml` — Existing config seeds

---
*Discussed: 2026-02-15*
