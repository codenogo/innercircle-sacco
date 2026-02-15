# Plan 03: Tests

## Goal
Add comprehensive unit and integration tests for all new batch processing safeguards, covering the entity, service, and controller layers.

## Prerequisites
- [ ] Plan 01 complete (data foundation)
- [ ] Plan 02 complete (safeguard implementation)

## Tasks

### Task 1: Unit tests for BatchProcessingLog entity and repository
**Files:**
- `sacco-loan/src/test/java/com/innercircle/sacco/loan/entity/BatchProcessingLogTest.java`
- `sacco-loan/src/test/java/com/innercircle/sacco/loan/entity/BatchProcessingStatusTest.java`
- `sacco-loan/src/test/java/com/innercircle/sacco/loan/repository/BatchProcessingLogRepositoryTest.java`

**Action:**
1. Test `BatchProcessingStatus` enum values (STARTED, COMPLETED, FAILED)
2. Test `BatchProcessingLog` entity: creation, field setting, builder pattern
3. Test repository methods with Mockito:
   - `findByProcessingMonth()` returns correct log
   - `existsByProcessingMonth()` returns true/false
   - `findTopByStatusOrderByProcessingMonthDesc()` returns latest completed month

**Verify:**
```bash
cd sacco-loan && mvn test -pl . -Dtest="BatchProcessingLogTest,BatchProcessingStatusTest,BatchProcessingLogRepositoryTest" -q
```

**Done when:** All entity and repository tests pass.

### Task 2: Unit tests for LoanBatchServiceImpl safeguards
**Files:**
- `sacco-loan/src/test/java/com/innercircle/sacco/loan/service/LoanBatchServiceImplTest.java`

**Action:**
Test each safeguard in `processMonthlyLoans()`:
1. **Idempotency (7.4)**: When month already processed → throws IllegalStateException with "already processed" message
2. **Sequential enforcement (7.5)**: When last processed = Jan and target = Mar → throws IllegalStateException with "Must process February first"
3. **Sequential enforcement (7.5)**: When last processed = Jan and target = Feb → succeeds
4. **Same-month skip (7.6)**: Loans disbursed in target month are excluded from interest accrual
5. **New-loan threshold (7.3)**: Loans disbursed after threshold day in previous month are skipped
6. **Pre-processing warnings (7.7)**: Unpaid loans appear in result.warnings
7. **Batch log (7.8)**: BatchProcessingLog created with STARTED, updated to COMPLETED with counts
8. **Configurable day (7.2)**: Cron auto-trigger respects processing day of month
9. **Happy path**: Full processing with eligible loans, verify interest accrued, log updated, config updated

Mock: `BatchProcessingLogRepository`, `LoanApplicationRepository`, `ConfigService`, `RepaymentScheduleRepository`, `LoanInterestHistoryRepository`, `InterestCalculator`, `ApplicationEventPublisher`

**Verify:**
```bash
cd sacco-loan && mvn test -pl . -Dtest="LoanBatchServiceImplTest" -q
```

**Done when:** All safeguard scenarios covered and tests pass.

### Task 3: Controller tests for updated batch endpoints
**Files:**
- `sacco-loan/src/test/java/com/innercircle/sacco/loan/controller/LoanBatchControllerTest.java`

**Action:**
Test updated `processBatch()` endpoint:
1. **Success with auto-determined month**: POST `/api/v1/loans/batch/process` with no params → 200 with result
2. **Success with explicit month**: POST `/api/v1/loans/batch/process?targetMonth=2026-03` → 200
3. **Idempotency violation**: Returns 409 with "Month already processed" message
4. **Sequential violation**: Returns 409 with "Must process [month] first" message
5. **Unauthorized**: Returns 403 for non-ADMIN/TREASURER roles
6. **Warnings included**: Response body includes warnings list

Mock: `LoanBatchService`, use `@WebMvcTest` with security configuration.

**Verify:**
```bash
cd sacco-loan && mvn test -pl . -Dtest="LoanBatchControllerTest" -q
```

**Done when:** All controller test scenarios pass.

## Verification

After all tasks:
```bash
cd sacco-loan && mvn test -q
```

## Commit Message
```
test(loan-batch): add tests for batch processing safeguards

- Unit tests for BatchProcessingLog entity and repository
- Unit tests for all 7 safeguards in LoanBatchServiceImpl
- Controller tests for updated batch endpoint with error handling
```

---
*Planned: 2026-02-15*
