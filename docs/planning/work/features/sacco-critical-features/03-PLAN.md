# Plan 03: Email Integration & Password Reset

## Goal
Add Spring Mail integration for notifications and implement a secure password reset flow with time-limited tokens.

## Prerequisites
- [x] Security module with UserAccount entity
- [x] Application configuration in application.yml

## Tasks

### Task 1: Add Spring Mail Integration & EmailService
**Files:**
- `sacco-common/src/main/java/com/innercircle/sacco/common/service/EmailService.java`
- `sacco-common/src/main/java/com/innercircle/sacco/common/service/EmailServiceImpl.java`
- `sacco-app/src/main/resources/application.yml` (add mail config)
- `pom.xml` (parent — add spring-boot-starter-mail dependency to sacco-common)
- `sacco-common/pom.xml` (add spring-boot-starter-mail dependency)

**Action:**
Add `spring-boot-starter-mail` dependency to sacco-common module's pom.xml. Create `EmailService` interface with methods:
- `sendSimpleEmail(String to, String subject, String body)`
- `sendPasswordResetEmail(String to, String resetToken, String resetUrl)`

Create `EmailServiceImpl` using `JavaMailSender`. Add mail configuration to application.yml with externalized properties:
```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
    from: ${MAIL_FROM:noreply@innercircle-sacco.co.ke}
```

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-common -q -DskipTests
```

**Done when:** EmailService compiles, mail config in application.yml, dependency added.

### Task 2: Create Password Reset Token Entity & Migration
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/entity/PasswordResetToken.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/repository/PasswordResetTokenRepository.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/ForgotPasswordRequest.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/ResetPasswordRequest.java`
- `sacco-security/src/main/resources/db/changelog/security/004-create-password-reset-tokens.yaml`
- `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml` (add include)

**Action:**
Create `PasswordResetToken` entity extending `BaseEntity` with fields: userId (UUID), token (String, unique), expiresAt (Instant), used (boolean). Create Liquibase migration for `password_reset_tokens` table with unique index on token and index on userId. Create DTOs: `ForgotPasswordRequest` (email), `ResetPasswordRequest` (token, newPassword). Add changelog include to master. Token expiry is 24 hours.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-security -q -DskipTests
```

**Done when:** Entity compiles, migration YAML valid, DTOs created.

### Task 3: Create Password Reset Service & Controller
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/service/PasswordResetService.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/service/PasswordResetServiceImpl.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/controller/AuthController.java`

**Action:**
Create `PasswordResetService` with methods:
- `requestPasswordReset(String email)` — finds user by email, generates UUID token, saves PasswordResetToken with 24h expiry, sends email via EmailService with reset link
- `resetPassword(String token, String newPassword)` — validates token (exists, not expired, not used), updates user password (BCrypt encoded), marks token as used
- `validateToken(String token)` — checks if token is valid (for frontend to verify before showing form)

Create `AuthController` at `/api/auth` with PUBLIC endpoints (add to SecurityConfig's permitAll):
- `POST /api/auth/forgot-password` — body: ForgotPasswordRequest. Always returns 200 (don't leak whether email exists).
- `POST /api/auth/reset-password` — body: ResetPasswordRequest. Returns success or error.
- `GET /api/auth/validate-token?token={token}` — Returns whether token is valid.

Update `SecurityConfig.java` to permit these new endpoints (add them to the existing `.requestMatchers("/api/auth/register").permitAll()` line).

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-security -q -DskipTests
```

**Done when:** Service compiles, controller endpoints defined, security config updated.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn clean compile -q -DskipTests
```

## Commit Message
```
feat(security): add email integration and password reset flow

- Spring Mail integration with externalized SMTP config
- Password reset tokens with 24h expiry
- Forgot password and reset password public endpoints
- Security-safe: doesn't leak email existence
```

---
*Planned: 2026-02-15*
