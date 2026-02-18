# Plan 03: Build OutboxProcessor and DeadLetterRetryJob scheduled processors in sacco-app

## Goal
Build OutboxProcessor and DeadLetterRetryJob scheduled processors in sacco-app

## Tasks

### Task 1: Create OutboxProcessor scheduled job
**Files:** `sacco-app/src/main/java/com/innercircle/sacco/outbox/OutboxProcessor.java`
**Action:**
Create @Component OutboxProcessor in sacco-app with @Scheduled(fixedDelay = 5000) method. The processor: (1) SELECT pending outbox entries ordered by created_at (batch of 50), (2) for each entry: set status to PROCESSING, deserialize the event payload via EventSerializer, dispatch via ApplicationEventPublisher.publishEvent(), (3) on success: set status to PROCESSED and processed_at, (4) on failure: create EventDeadLetter entry with error message, set outbox status to FAILED. Use @Transactional for each individual event processing (not the whole batch). Use optimistic locking (version field) to prevent double-processing in clustered environments.

**Verify:**
```bash
mvn -pl sacco-app compile -q
```

**Done when:** [Observable outcome]

### Task 2: Create DeadLetterRetryJob scheduled job
**Files:** `sacco-app/src/main/java/com/innercircle/sacco/outbox/DeadLetterRetryJob.java`
**Action:**
Create @Component DeadLetterRetryJob in sacco-app with @Scheduled(fixedDelay = 300000) method (every 5 minutes). The job: (1) SELECT dead letter entries with status PENDING_RETRY, next_retry_at <= now, retries < max_retries, (2) for each: set status to RETRYING, deserialize event via EventSerializer, dispatch via ApplicationEventPublisher, (3) on success: set status to RESOLVED, (4) on failure: increment retries, calculate next_retry_at with exponential backoff (5min * 3^retries, capped at 4h), set status back to PENDING_RETRY, (5) if retries >= max_retries: set status to FAILED and log.warn with event details for manual intervention.

**Verify:**
```bash
mvn -pl sacco-app compile -q
```

**Done when:** [Observable outcome]

### Task 3: Add unit tests for OutboxProcessor and DeadLetterRetryJob
**Files:** `sacco-app/src/test/java/com/innercircle/sacco/outbox/OutboxProcessorTest.java`, `sacco-app/src/test/java/com/innercircle/sacco/outbox/DeadLetterRetryJobTest.java`
**Action:**
Write Mockito unit tests. OutboxProcessorTest: (1) processes pending entries and marks PROCESSED, (2) moves failed entries to dead letter, (3) handles empty queue gracefully, (4) handles deserialization errors. DeadLetterRetryJobTest: (1) retries eligible entries and marks RESOLVED on success, (2) increments retries and sets backoff on failure, (3) marks FAILED when max retries exceeded, (4) skips entries with next_retry_at in the future.

**Verify:**
```bash
mvn -pl sacco-app test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-app compile -q
mvn -pl sacco-app test -q
```

## Commit Message
```
feat(event-hardening): add outbox processor and dead letter retry job
```
