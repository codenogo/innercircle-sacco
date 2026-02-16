# Plan 01: Add schema migration, Account entity fields, and MemberCreatedEvent record to support per-member sub-accounts

## Goal
Add schema migration, Account entity fields, and MemberCreatedEvent record to support per-member sub-accounts

## Tasks

### Task 1: Liquibase migration for account parent/member fields
**CWD:** `sacco-ledger`
**Files:** `sacco-ledger/src/main/resources/db/changelog/ledger/006-add-account-member-fields.yaml`, `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`
**Action:**
Create Liquibase migration `006-add-account-member-fields.yaml` that adds two nullable columns to the `accounts` table: `parent_account_code VARCHAR(20)` and `member_id UUID`. Add indexes: `idx_account_member_id` on `member_id`, `idx_account_parent_code` on `parent_account_code`. Register the migration in `db.changelog-master.yaml` under the Ledger module section.

**Verify:**
```bash
mvn -pl sacco-ledger compile -q
```

**Done when:** [Observable outcome]

### Task 2: Update Account entity and AccountRepository
**CWD:** `sacco-ledger`
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/entity/Account.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/repository/AccountRepository.java`
**Action:**
Add `parentAccountCode` (String, nullable, length 20) and `memberId` (UUID, nullable) fields to Account entity. Add AccountRepository methods: `List<Account> findByMemberId(UUID memberId)`, `Optional<Account> findByMemberIdAndParentAccountCode(UUID memberId, String parentAccountCode)`, `boolean existsByMemberIdAndParentAccountCode(UUID memberId, String parentAccountCode)`.

**Verify:**
```bash
mvn -pl sacco-ledger compile -q
```

**Done when:** [Observable outcome]

### Task 3: Create MemberCreatedEvent record
**CWD:** `sacco-common`
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/event/MemberCreatedEvent.java`
**Action:**
Create `MemberCreatedEvent` as a Java record in sacco-common event package. Fields: `UUID memberId`, `String memberNumber`, `String firstName`, `String lastName`, `String actor`. Implement `AuditableEvent` with `getEventType()` returning `MEMBER_CREATED`. Follow the exact pattern of `ContributionReceivedEvent`.

**Verify:**
```bash
mvn -pl sacco-common compile -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-common,sacco-ledger compile -q
```

## Commit Message
```
feat(member-account-setup): add schema migration, Account entity fields, and MemberCreatedEvent
```
