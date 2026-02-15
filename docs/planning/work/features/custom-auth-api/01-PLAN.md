# Plan 01: Database Migrations + Entity Changes

## Goal
Set up the data layer — Liquibase migrations for seed admin, member-id FK, and refresh tokens table; update entities accordingly.

## Prerequisites
- [ ] CONTEXT.md decisions finalized

## Tasks

### Task 1: Create Liquibase Migrations
**Files:**
- `sacco-security/src/main/resources/db/changelog/security/005-seed-admin-user.yaml`
- `sacco-security/src/main/resources/db/changelog/security/006-add-member-id-to-users.yaml`
- `sacco-security/src/main/resources/db/changelog/security/007-create-refresh-tokens.yaml`

**Action:**

**005-seed-admin-user.yaml:**
- Insert a default admin user into `user_accounts` with:
  - Fixed UUIDv7: `01940000-0000-7000-8000-000000000100`
  - username: `admin`
  - email: `admin@innercircle.co.ke`
  - password: BCrypt hash of `Admin@1234` (pre-computed: `$2a$10$...` — generate with `BCryptPasswordEncoder().encode("Admin@1234")`)
  - enabled: true, account_non_locked: true
- Insert into `user_account_roles` linking admin user to ADMIN role (`01940000-0000-7000-8000-000000000001`)
- Rollback: DELETE from user_account_roles, DELETE from user_accounts WHERE username = 'admin'

**006-add-member-id-to-users.yaml:**
- Add nullable `member_id` (uuid) column to `user_accounts`
- Add FK constraint: `fk_user_accounts_member` referencing `members(id)` ON DELETE SET NULL
- Add index: `idx_user_accounts_member_id` on `member_id`
- Rollback: drop FK, drop index, drop column

**007-create-refresh-tokens.yaml:**
- Create `refresh_tokens` table with columns:
  - `id` (uuid, PK)
  - `user_id` (uuid, NOT NULL, FK to user_accounts ON DELETE CASCADE)
  - `token` (varchar(512), NOT NULL, UNIQUE)
  - `expires_at` (timestamp with time zone, NOT NULL)
  - `revoked` (boolean, default false, NOT NULL)
  - `created_at`, `updated_at` (timestamp with time zone, NOT NULL)
  - `created_by` (varchar(255))
  - `version` (bigint)
- Indexes: `idx_refresh_tokens_token`, `idx_refresh_tokens_user_id`
- Rollback: drop table

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q
```

**Done when:** All 3 migration files exist and module compiles.

### Task 2: Create RefreshToken Entity + Repository
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/entity/RefreshToken.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/repository/RefreshTokenRepository.java`

**Action:**

**RefreshToken.java:**
- Entity extending `BaseEntity`, table `refresh_tokens`
- Fields: `userId` (UUID, NOT NULL), `token` (String, NOT NULL, unique), `expiresAt` (Instant, NOT NULL), `revoked` (Boolean, default false)
- Use `@Column` annotations matching the migration schema
- Lombok: `@Entity @Table @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`

**RefreshTokenRepository.java:**
- `JpaRepository<RefreshToken, UUID>`
- Methods:
  - `Optional<RefreshToken> findByToken(String token)`
  - `List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId)`
  - `void deleteByExpiresAtBefore(Instant cutoff)`
  - `@Modifying @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId") void revokeAllByUserId(@Param("userId") UUID userId)`

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q
```

**Done when:** Entity and repository compile without errors.

### Task 3: Update UserAccount Entity with memberId
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/entity/UserAccount.java`

**Action:**
- Add `memberId` field (UUID, nullable) with `@Column(name = "member_id")`:
  ```java
  @Column(name = "member_id")
  private UUID memberId;
  ```
- Do NOT add a JPA `@ManyToOne` relationship to Member (cross-module boundary — use UUID FK only)
- Keep all existing fields and annotations unchanged

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q && mvn -pl sacco-security test -q
```

**Done when:** UserAccount has `memberId` field, module compiles and tests pass.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q && mvn -pl sacco-security test -q
```

## Commit Message
```
feat(custom-auth-api): add migrations for seed admin, member-id FK, refresh tokens + entities

- Add 005-seed-admin-user.yaml with default admin and ADMIN role
- Add 006-add-member-id-to-users.yaml with member_id FK column
- Add 007-create-refresh-tokens.yaml table
- Create RefreshToken entity and repository
- Add memberId field to UserAccount
```

---
*Planned: 2026-02-15*
