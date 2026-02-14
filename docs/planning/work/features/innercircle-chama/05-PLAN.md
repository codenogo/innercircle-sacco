# Plan 05: Audit Trail

## Goal
Implement an immutable, append-only audit event log that captures all data mutations with actor, action, before/after JSON snapshots, queryable via REST API.

## Prerequisites
- [ ] Plan 01 complete

## Tasks

### Task 1: AuditEvent Entity + Repository
**Files:** `sacco-audit/src/main/java/com/innercircle/sacco/audit/entity/AuditEvent.java`, `sacco-audit/src/main/java/com/innercircle/sacco/audit/entity/AuditAction.java`, `sacco-audit/src/main/java/com/innercircle/sacco/audit/repository/AuditEventRepository.java`
**Action:**
1. `AuditEvent` entity: id (UUID v7), actor (userId), actorName, action (enum), entityType, entityId, beforeSnapshot (JSON text), afterSnapshot (JSON text), ipAddress, timestamp. **No updatedAt** — append-only, no updates.
2. `AuditAction` enum: CREATE, UPDATE, DELETE, APPROVE, REJECT, DISBURSE, SUSPEND, REACTIVATE, LOGIN, LOGOUT, CONFIG_CHANGE
3. `AuditEventRepository`: cursor-paginated queries, filter by entityType, entityId, actor, action, dateRange
4. Table uses `@Immutable` Hibernate annotation — no updates allowed

**Verify:**
```bash
mvn compile -pl sacco-audit -q
```

**Done when:** AuditEvent entity and repository compile.

### Task 2: Audit Event Listener + Service
**Files:** `sacco-audit/src/main/java/com/innercircle/sacco/audit/service/AuditService.java`, `sacco-audit/src/main/java/com/innercircle/sacco/audit/service/AuditServiceImpl.java`, `sacco-audit/src/main/java/com/innercircle/sacco/audit/listener/AuditEventListener.java`
**Action:**
1. `AuditService`: logEvent(actor, action, entityType, entityId, before, after), query methods
2. `AuditEventListener`: `@TransactionalEventListener` that listens to all `AuditableEvent` subtypes from sacco-common and persists AuditEvent records
3. JSON serialization of before/after snapshots using Jackson ObjectMapper
4. Extract current user from SecurityContext for actor field

**Verify:**
```bash
mvn compile -pl sacco-audit -q
```

**Done when:** Audit listener and service compile.

### Task 3: Audit REST API + Liquibase
**Files:** `sacco-audit/src/main/java/com/innercircle/sacco/audit/controller/AuditController.java`, `sacco-audit/src/main/java/com/innercircle/sacco/audit/dto/AuditEventResponse.java`, `sacco-audit/src/main/java/com/innercircle/sacco/audit/dto/AuditQueryRequest.java`, `sacco-audit/src/main/resources/db/changelog/audit/001-create-audit-events-table.yaml`
**Action:**
1. `AuditController` at `/api/v1/audit` (ADMIN, TREASURER):
   - GET `/` — list audit events with cursor pagination + filters (entityType, actor, action, dateRange)
   - GET `/{entityType}/{entityId}` — audit trail for a specific entity
   - GET `/export` — CSV export of filtered audit events
2. Liquibase: Create `audit_events` table with indexes on entity_type+entity_id, actor, action, timestamp
3. Update sacco-app's `db.changelog-master.yaml`

**Verify:**
```bash
mvn compile -pl sacco-audit -q
```

**Done when:** Audit REST API and Liquibase changelog compile.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-audit -q
```

## Commit Message
```
feat(audit): implement immutable audit trail with event listeners

- Append-only AuditEvent entity with JSON snapshots
- Spring event listener for all AuditableEvent subtypes
- Query API with cursor pagination and filters
- CSV export for audit compliance
```

---
*Planned: 2026-02-14*
