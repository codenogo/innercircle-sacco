# Plan 01: Create outbox and dead letter database tables, JPA entities, repositories, and EventOutboxWriter in sacco-common

## Goal
Create outbox and dead letter database tables, JPA entities, repositories, and EventOutboxWriter in sacco-common

## Tasks

### Task 1: Create Liquibase migrations for event_outbox and event_dead_letter tables
**Files:** `sacco-common/src/main/resources/db/changelog/common/003-create-event-outbox-table.yaml`, `sacco-common/src/main/resources/db/changelog/common/004-create-event-dead-letter-table.yaml`, `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`
**Action:**
Create Liquibase YAML migrations for event_outbox (id UUID PK, event_type VARCHAR(100), aggregate_type VARCHAR(100), aggregate_id UUID, correlation_id UUID, idempotency_key VARCHAR(255) UNIQUE, payload TEXT, status VARCHAR(20) DEFAULT 'PENDING', created_at TIMESTAMP, processed_at TIMESTAMP, version BIGINT) and event_dead_letter (id UUID PK, outbox_id UUID UNIQUE FK, event_type VARCHAR(100), correlation_id UUID, payload TEXT, error_message TEXT, retries INT DEFAULT 0, max_retries INT DEFAULT 5, next_retry_at TIMESTAMP, status VARCHAR(20) DEFAULT 'PENDING_RETRY', created_at TIMESTAMP, last_retry_at TIMESTAMP, version BIGINT). Register both in db.changelog-master.yaml under Common cross-cutting migrations section.

**Verify:**
```bash
mvn -pl sacco-app liquibase:validate -DskipTests 2>&1 || echo 'Liquibase plugin not configured, verify via compile'
mvn -pl sacco-common compile -q
```

**Done when:** [Observable outcome]

### Task 2: Create EventOutbox and EventDeadLetter JPA entities with enums
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/outbox/EventOutbox.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/outbox/EventOutboxStatus.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/outbox/EventDeadLetter.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/outbox/EventDeadLetterStatus.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/outbox/EventOutboxRepository.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/outbox/EventDeadLetterRepository.java`
**Action:**
Create EventOutbox entity extending BaseEntity with fields matching the event_outbox table. EventOutboxStatus enum: PENDING, PROCESSING, PROCESSED, FAILED. EventDeadLetter entity extending BaseEntity with fields matching event_dead_letter table. EventDeadLetterStatus enum: PENDING_RETRY, RETRYING, RESOLVED, FAILED. Create Spring Data JPA repositories: EventOutboxRepository (findByStatusOrderByCreatedAtAsc) and EventDeadLetterRepository (findByStatusAndNextRetryAtBeforeAndRetriesLessThan).

**Verify:**
```bash
mvn -pl sacco-common compile -q
```

**Done when:** [Observable outcome]

### Task 3: Create EventOutboxWriter service for writing events to outbox
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/outbox/EventOutboxWriter.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/outbox/EventSerializer.java`
**Action:**
Create EventSerializer utility class that serializes AuditableEvent records to JSON using Jackson ObjectMapper (handle UUID, BigDecimal, Instant, LocalDate, enums) and deserializes JSON back to event objects. Create EventOutboxWriter @Service that accepts an AuditableEvent, serializes it via EventSerializer, generates an idempotency_key (eventType + aggregateId + timestamp hash), and persists an EventOutbox entity with status PENDING. The writer participates in the caller's transaction (no new @Transactional).

**Verify:**
```bash
mvn -pl sacco-common compile -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-common compile -q
mvn -pl sacco-common test -q
```

## Commit Message
```
feat(event-hardening): add outbox and dead letter infrastructure (entities, migrations, writer)
```
