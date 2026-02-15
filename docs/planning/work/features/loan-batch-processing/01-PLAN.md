# Plan 01: Data Foundation

## Goal
Create the `BatchProcessingLog` entity, Liquibase migrations, config seed data, and update the `BatchProcessingResult` DTO — providing the data layer that Plans 02-03 build upon.

## Prerequisites
- [ ] CONTEXT.md decisions finalized (done)

## Tasks

### Task 1: Create BatchProcessingLog entity, enum, and repository
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/BatchProcessingLog.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/BatchProcessingStatus.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/BatchProcessingLogRepository.java`

**Action:**
1. Create `BatchProcessingStatus` enum with values: `STARTED`, `COMPLETED`, `FAILED`
2. Create `BatchProcessingLog` entity extending `BaseEntity` with fields:
   - `processingMonth` (String, format "YYYY-MM", unique, not null) — stored as varchar since JPA has no native YearMonth support
   - `status` (BatchProcessingStatus enum, not null)
   - `loansProcessed` (int)
   - `interestAccrued` (BigDecimal)
   - `penalizedLoans` (int)
   - `closedLoans` (int)
   - `warningsSummary` (String, length 4000) — serialized warnings text
   - `startedAt` (Instant, not null)
   - `completedAt` (Instant, nullable)
   - `triggeredBy` (String, not null, length 100)
3. Create `BatchProcessingLogRepository` with methods:
   - `Optional<BatchProcessingLog> findByProcessingMonth(String processingMonth)`
   - `boolean existsByProcessingMonth(String processingMonth)`
   - `Optional<BatchProcessingLog> findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus status)`

**Verify:**
```bash
cd sacco-loan && mvn compile -q
```

**Done when:** Entity, enum, and repository compile without errors.

### Task 2: Create Liquibase migrations and seed config keys
**Files:**
- `sacco-loan/src/main/resources/db/changelog/loan/004-create-batch-processing-log.yaml`
- `sacco-config/src/main/resources/db/changelog/config/003-seed-batch-config.yaml`
- `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`

**Action:**
1. Create `004-create-batch-processing-log.yaml`:
   - `batch_processing_log` table with all columns matching entity
   - Unique constraint on `processing_month`
   - Index on `status`
   - Rollback: `dropTable`
2. Create `003-seed-batch-config.yaml`:
   - Seed `loan.batch.processing_day_of_month` = `"1"` (Day of month when batch processing runs)
   - Seed `loan.batch.new_loan_threshold_day` = `"15"` (Loans disbursed before this day accrue interest that month)
   - Seed `loan.batch.last_processed_month` = `""` (Last successfully processed month, initially empty)
   - Rollback: delete by config_key
3. Register both in `db.changelog-master.yaml` under their respective module sections

**Verify:**
```bash
cd sacco-app && mvn compile -q
```

**Done when:** Master changelog includes both new migrations, and project compiles.

### Task 3: Update BatchProcessingResult DTO and LoanApplicationRepository
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/BatchProcessingResult.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/LoanApplicationRepository.java`

**Action:**
1. Add `List<String> warnings` field to `BatchProcessingResult` (with `@Builder.Default` defaulting to empty list)
2. Add `String processingMonth` field to `BatchProcessingResult` (the month that was processed)
3. Add repository query to `LoanApplicationRepository`:
   - `List<LoanApplication> findByStatusAndDisbursedAtAfter(LoanStatus status, Instant after)` — to filter out same-month loans
   - `List<LoanApplication> findByStatusAndDisbursedAtBefore(LoanStatus status, Instant before)` — for threshold filtering

**Verify:**
```bash
cd sacco-loan && mvn compile -q
```

**Done when:** DTO has warnings and processingMonth fields, repository has new query methods.

## Verification

After all tasks:
```bash
mvn compile -q
```

## Commit Message
```
feat(loan-batch): add BatchProcessingLog entity, migrations, and config seeds

- Create BatchProcessingLog entity and BatchProcessingStatus enum
- Add Liquibase migration for batch_processing_log table with unique constraint
- Seed loan.batch config keys (processing_day_of_month, new_loan_threshold_day, last_processed_month)
- Add warnings and processingMonth fields to BatchProcessingResult DTO
- Add disbursedAt-based queries to LoanApplicationRepository
```

---
*Planned: 2026-02-15*
