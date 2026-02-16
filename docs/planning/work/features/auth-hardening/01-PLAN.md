# Plan 01: Register missing security migrations and fix code warnings from review

## Goal
Register missing security migrations and fix code warnings from review

## Tasks

### Task 1: Register 3 missing security migrations in master changelog
**Files:** `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`
**Action:**
Add includes for security/005-seed-admin-user.yaml, security/006-add-member-id-to-users.yaml, security/007-create-refresh-tokens.yaml after the existing security/004 entry. Order matters: 005 seeds admin user (depends on user_accounts), 006 adds member_id FK (depends on members table), 007 creates refresh_tokens (depends on user_accounts).

**Verify:**
```bash
grep '005-seed-admin-user' sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml
grep '006-add-member-id-to-users' sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml
grep '007-create-refresh-tokens' sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml
```

**Done when:** [Observable outcome]

### Task 2: Fix SecurityConfig JSON interpolation and refresh endpoint error leaking
**Files:** `sacco-security/src/main/java/com/innercircle/sacco/security/config/SecurityConfig.java`, `sacco-security/src/main/java/com/innercircle/sacco/security/controller/AuthController.java`
**Action:**
In SecurityConfig: replace String.format JSON templates with Jackson ObjectMapper or manually escape authException.getMessage() and accessDeniedException.getMessage() (replace quotes with \\" before interpolation). In AuthController refresh endpoint: replace e.getMessage() with a generic 'Invalid or expired refresh token' message, matching the static-message pattern used in the login endpoint.

**Verify:**
```bash
mvn -pl sacco-security compile -q
```

**Done when:** [Observable outcome]

### Task 3: Remove unused OAuth2 config from application.yml
**Files:** `sacco-app/src/main/resources/application.yml`
**Action:**
Remove the oauth2.client.web-app.secret and oauth2.client.batch-client.secret properties. Keep oauth2.jwt.rsa.* properties (still used by JwtService).

**Verify:**
```bash
! grep 'web-app' sacco-app/src/main/resources/application.yml
! grep 'batch-client' sacco-app/src/main/resources/application.yml
grep 'rsa' sacco-app/src/main/resources/application.yml
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-security test -q
```

## Commit Message
```
fix(auth-hardening): register missing migrations and fix security code warnings
```
