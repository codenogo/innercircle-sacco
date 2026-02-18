# Event Hardening - Implementation Context

## Problem Statement

The event-driven architecture works but lacks resilience and formality:
1. **No retry/dead letter** - If a `@TransactionalEventListener` handler fails (GL, audit, account setup), the entire business transaction rolls back with a generic 500 error. No recovery mechanism.
2. **No state transition guards** - Loan lifecycle (7 states), payout (4 states), contribution (3 states) transitions are enforced by scattered `if (status != X) throw` checks across 6+ service classes. Easy to miss invalid transitions.
3. **No event traceability** - No correlation ID linking a business operation to its downstream events. No idempotency keys to prevent double-processing.

## Scope

Two workstreams:

### Workstream 1: Transactional Outbox + Dead Letter

Replace `ApplicationEventPublisher.publishEvent()` with outbox-based event publishing across the entire system.

**Current flow (synchronous, coupled):**
```
Service.doWork()
  └── save entity
  └── eventPublisher.publishEvent(event)  ← synchronous in same tx
        └── @TransactionalEventListener BEFORE_COMMIT
              └── FinancialEventListener.handleX()  ← GL entry created
              └── MemberAccountListener.handleX()   ← account setup
        └── @TransactionalEventListener AFTER_COMMIT
              └── AuditEventListener.handleX()       ← audit log
```

**Target flow (outbox, decoupled):**
```
Service.doWork()
  └── save entity
  └── outboxWriter.write(event)  ← writes to event_outbox table (same tx)
  └── COMMIT

OutboxProcessor (@Scheduled, every 5s)
  └── SELECT * FROM event_outbox WHERE status = 'PENDING' ORDER BY created_at
        └── dispatch event via ApplicationEventPublisher
        └── ON SUCCESS: mark status = 'PROCESSED'
        └── ON FAILURE: move to event_dead_letter, increment retry count

DeadLetterRetryJob (@Scheduled, every 5min)
  └── SELECT * FROM event_dead_letter WHERE retries < max AND next_retry_at <= now
        └── re-dispatch event
        └── ON SUCCESS: mark RESOLVED
        └── ON FAILURE: increment retries, set next_retry_at with exponential backoff
        └── IF retries >= max: mark FAILED (manual intervention required)
```

### Workstream 2: State Transition Guards

Create a `TransitionGuard<S>` utility in `sacco-common` that defines allowed transitions for each status enum. Services call `TransitionGuard.validate()` instead of ad-hoc `if/throw` checks.

## Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| GL coupling | Transactional outbox (decouple from BEFORE_COMMIT) | Decouples GL posting from business transaction. GL failures no longer roll back business operations. Eventually consistent (1-5s delay) is acceptable for a small SACCO. |
| Consistency model | Accept eventual consistency | Small delay (1-5s) between business commit and GL posting is acceptable. Outbox processor runs frequently. No "GL pending" indicator needed initially. |
| Retry scope | Full outbox for all events | All 18 event types go through the outbox. Unified reliability guarantee for GL, audit, account setup, and any future listeners. |
| Dead letter strategy | Exponential backoff with max retries | Failed events move to dead letter. Retry with exponential backoff (5min, 15min, 1h, 4h). Max 5 retries before marking FAILED for manual review. |
| Outbox module location | `sacco-common` for entities/writer, `sacco-app` for processor | All modules already depend on sacco-common. Outbox entity, repository, and writer live there. The @Scheduled processor lives in sacco-app (the boot application). |
| Guard location | Separate `TransitionGuard<S>` utility in sacco-common | Single file to review all transition rules for an entity type. More flexible than enum methods. Discoverable. |
| Guard API | `validate()` throws `InvalidStateTransitionException` | Fail-fast behavior. Consistent enforcement. No risk of caller forgetting to check a boolean return. |
| Correlation IDs | Explicit `correlationId()` on `AuditableEvent` interface | All 18 event records get a `correlationId` field. Enables tracing a business operation across all downstream events. More visible than MDC-only. |
| Idempotency | `idempotencyKey` on outbox entries | Each outbox entry has a unique key (derived from event type + entity ID + timestamp). Handlers check for duplicate processing. |
| Event serialization | JSON via Jackson in outbox table | Event payloads serialized as JSON in the `payload` column. Enables inspection, debugging, and manual replay. |

## Schema Changes

### New Table: `event_outbox`

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| event_type | VARCHAR(100) | Fully qualified event class name |
| aggregate_type | VARCHAR(100) | Entity type (e.g., "LoanApplication") |
| aggregate_id | UUID | Entity ID that produced the event |
| correlation_id | UUID | Traces related events back to originating operation |
| idempotency_key | VARCHAR(255) | Unique key for de-duplication |
| payload | TEXT (JSON) | Serialized event data |
| status | VARCHAR(20) | PENDING, PROCESSING, PROCESSED, FAILED |
| created_at | TIMESTAMP | When event was written |
| processed_at | TIMESTAMP | When event was dispatched |
| version | BIGINT | Optimistic locking |

### New Table: `event_dead_letter`

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| outbox_id | UUID | FK to original outbox entry |
| event_type | VARCHAR(100) | Event class name |
| correlation_id | UUID | From original event |
| payload | TEXT (JSON) | Serialized event data |
| error_message | TEXT | Last failure reason |
| retries | INT | Number of retry attempts |
| max_retries | INT | Maximum retries allowed (default 5) |
| next_retry_at | TIMESTAMP | Next scheduled retry (exponential backoff) |
| status | VARCHAR(20) | PENDING_RETRY, RETRYING, RESOLVED, FAILED |
| created_at | TIMESTAMP | When moved to dead letter |
| last_retry_at | TIMESTAMP | Last retry attempt |
| version | BIGINT | Optimistic locking |

### AuditableEvent Interface Change

```java
public interface AuditableEvent {
    String getEventType();
    String getActor();
    UUID getCorrelationId();    // NEW
}
```

All 18 event records updated with `correlationId` field.

### TransitionGuard Registrations

| Entity | States | Transitions |
|--------|--------|-------------|
| LoanApplication (LoanStatus) | PENDING, APPROVED, REJECTED, DISBURSED, REPAYING, CLOSED, DEFAULTED | PENDING→APPROVED, PENDING→REJECTED, APPROVED→DISBURSED, DISBURSED→REPAYING, REPAYING→CLOSED, REPAYING→DEFAULTED |
| Payout (PayoutStatus) | PENDING, APPROVED, PROCESSED, FAILED | PENDING→APPROVED, APPROVED→PROCESSED, APPROVED→FAILED, PENDING→FAILED |
| Contribution (ContributionStatus) | PENDING, CONFIRMED, REVERSED | PENDING→CONFIRMED, CONFIRMED→REVERSED |
| Member (MemberStatus) | ACTIVE, SUSPENDED, DEACTIVATED | ACTIVE→SUSPENDED, SUSPENDED→ACTIVE, ACTIVE→DEACTIVATED, SUSPENDED→DEACTIVATED |

## Constraints

- Must not break existing tests (605+ tests). Existing `@TransactionalEventListener` handlers remain but are now dispatched by the outbox processor instead of directly by Spring.
- Outbox processor must be idempotent - safe to restart without double-processing.
- JSON serialization of event records must handle all existing field types (UUID, BigDecimal, Instant, LocalDate, String, enums).
- Dead letter table needs a unique constraint on `outbox_id` to prevent duplicate dead letter entries.
- Liquibase migrations for `event_outbox` and `event_dead_letter` tables.
- Existing services that call `eventPublisher.publishEvent()` must be refactored to use `EventOutboxWriter.write()` instead.
- `TransitionGuard` must be registered for all 4 status enums before removing ad-hoc checks from services.

## Migration Strategy

1. Add outbox infrastructure (tables, writer, processor, dead letter) as new code - no existing changes yet.
2. Add `correlationId` to `AuditableEvent` and update all 18 event records.
3. Add `TransitionGuard` utility and register all 4 state machines.
4. Refactor services one-by-one: replace `eventPublisher.publishEvent()` with `outboxWriter.write()` and ad-hoc status checks with `transitionGuard.validate()`.
5. Change `@TransactionalEventListener` handlers to `@EventListener` (no longer need transaction phase since events come from outbox processor, not the original transaction).
6. Add dead letter monitoring (log warnings for FAILED entries).

## Open Questions

1. Should the outbox processor use polling (simple `@Scheduled`) or database notification (PostgreSQL LISTEN/NOTIFY) for lower latency?
   - **Recommendation:** Start with polling (5s interval). LISTEN/NOTIFY is an optimization for later if latency matters.
2. Should we add a simple admin endpoint to view/retry dead letter entries?
   - **Recommendation:** Yes, but defer to a follow-up task. Log warnings are sufficient for MVP.
3. Should the `TransitionGuard` support registering callbacks/actions on transitions (e.g., "on REPAYING→CLOSED, publish LoanStatusChangeEvent")?
   - **Recommendation:** No. Keep guards pure (validate only). Side effects stay in the service layer. Avoids coupling.

## Related Code

| File | Relevance |
|------|-----------|
| `sacco-common/.../event/AuditableEvent.java` | Interface to extend with correlationId |
| `sacco-common/.../event/*.java` (18 files) | All event records to update |
| `sacco-ledger/.../listener/FinancialEventListener.java` | 10 GL handlers to change from @TransactionalEventListener to @EventListener |
| `sacco-ledger/.../listener/MemberAccountListener.java` | Account setup handler to change |
| `sacco-audit/.../listener/AuditEventListener.java` | Audit handler to change |
| `sacco-loan/.../service/LoanServiceImpl.java` | 7 setStatus() calls to refactor to TransitionGuard |
| `sacco-loan/.../service/LoanBatchServiceImpl.java` | 3 setStatus() calls + event publishing |
| `sacco-payout/.../service/PayoutServiceImpl.java` | 2 setStatus() calls |
| `sacco-payout/.../service/ShareWithdrawalServiceImpl.java` | 2 setStatus() calls |
| `sacco-payout/.../service/BankWithdrawalServiceImpl.java` | 1 setStatus() call |
| `sacco-contribution/.../service/ContributionServiceImpl.java` | 2 setStatus() calls |
| `sacco-member/.../service/MemberServiceImpl.java` | 2 setStatus() calls |
| `sacco-loan/.../service/LoanReversalServiceImpl.java` | 2 setStatus() calls |
| `sacco-loan/.../service/LoanBenefitServiceImpl.java` | Uses @EventListener (already post-commit) |
| `sacco-common/.../model/BaseEntity.java` | Base entity for new outbox/dead letter entities |
| `sacco-app/.../db.changelog-master.yaml` | Register new migrations |

---
*Discussed: 2026-02-16*
