# Plan 06: Data Integrity & Common Fixes

## Goal
Fix UUID generation, exception logging, optimistic locking, payout reference numbers, FK constraints, unique constraints, and add rollback definitions.

## Prerequisites
- [ ] Plans 01-05 complete (all module-specific fixes must be in place before cross-cutting changes)

## Tasks

### Task 1: Fix common module (W5, W6, W7)
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/util/UuidGenerator.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/exception/GlobalExceptionHandler.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/model/BaseEntity.java`, `sacco-common/pom.xml`
**Action:**
1. **W5 — UUID v7**: Replace custom bit manipulation in `UuidGenerator` with the `com.fasterxml.uuid:java-uuid-generator` library.
   - Add dependency to `sacco-common/pom.xml`: `com.fasterxml.uuid:java-uuid-generator:5.1.0`
   - Rewrite `generateV7()` to use `Generators.timeBasedEpochGenerator().generate()` (returns UUID v7).
2. **W6 — Exception logging**: In `GlobalExceptionHandler.handleGeneral()` (line 43-49), add `log.error("Unhandled exception", ex)` before returning the response. Add `@Slf4j` annotation (or declare a Logger field).
3. **W7 — Optimistic locking**: Add `@Version` field to `BaseEntity`:
   ```java
   @Version
   private Long version;
   ```
   Import `jakarta.persistence.Version`.

**Verify:**
```bash
mvn compile -pl sacco-common -q
```

**Done when:** UUID v7 uses battle-tested library. Generic exceptions are logged. BaseEntity has `@Version` for optimistic locking.

### Task 2: Fix payout reference number + Liquibase for version column (W11, W7)
**Files:** `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PayoutServiceImpl.java`, `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`, `sacco-common/src/main/resources/db/changelog/common/001-add-version-column.yaml` (new)
**Action:**
1. **W11**: Fix `generateReferenceNumber()` (line 141-142) in `PayoutServiceImpl` to use UUID v7 substring:
   ```java
   private String generateReferenceNumber() {
       return "PAY-" + UuidGenerator.generateV7().toString().substring(0, 8).toUpperCase();
   }
   ```
2. **W7**: Create Liquibase changeset to add `version BIGINT DEFAULT 0` to all entity tables: `user_accounts`, `members`, `contributions`, `loan_applications`, `repayment_schedules`, `payouts`, `journal_entries`, `ledger_entries`, `accounts`, `audit_events`, `chama_configs`.
3. Include the new changeset in `db.changelog-master.yaml`.

**Verify:**
```bash
mvn compile -pl sacco-payout,sacco-app -q
```

**Done when:** Payout reference uses UUID v7 substring. Version column added to all tables.

### Task 3: Liquibase — FK constraints, unique constraints, rollbacks (B8, W18, W19)
**Files:** `sacco-app/src/main/resources/db/changelog/common/002-add-fk-and-unique-constraints.yaml` (new), `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`
**Action:**
1. **B8**: Create a single changeset with `ON DELETE RESTRICT` foreign key constraints for all entity relationships:
   - `contributions.member_id → members.id`
   - `loan_applications.member_id → members.id`
   - `repayment_schedules.loan_id → loan_applications.id`
   - `payouts.member_id → members.id`
   - `journal_entries` / `ledger_entries` relationships
   - `user_accounts` → roles relationships
   Include rollback: `dropForeignKeyConstraint`.
2. **W19**: Add UNIQUE constraints on `reference_number` columns in `contributions`, `payouts`, and `journal_entries` tables. Include rollback: `dropUniqueConstraint`.
3. **W18**: Add rollback definitions (already included as part of each constraint addition above).
4. Include in `db.changelog-master.yaml`.

**Verify:**
```bash
mvn compile -pl sacco-app -q
```

**Done when:** FK constraints with ON DELETE RESTRICT on all relationships. UNIQUE on reference numbers. All changesets have rollback definitions.

## Verification

After all tasks:
```bash
mvn compile -q
```

## Commit Message
```
fix(common,payout,app): data integrity fixes and cross-cutting improvements

- W5: Replace custom UUID v7 with java-uuid-generator library
- W6: Add log.error to generic exception handler
- W7: Add @Version optimistic locking to BaseEntity + migration
- W11: Use UUID v7 substring for payout reference numbers
- B8: Add ON DELETE RESTRICT FK constraints for all entity relationships
- W18: Add rollback definitions to all new changesets
- W19: Add UNIQUE constraints on reference_number columns
```

---
*Planned: 2026-02-15*
