# Plan 03: Security Hardening

## Goal
Fix hardcoded secrets, permit-all security bypass, RSA key regeneration, actuator/swagger exposure, and missing interest rate validation.

## Prerequisites
- [ ] Plan 01 complete (ledger module changes must not conflict)

## Tasks

### Task 1: Fix SecurityConfig (B5, W2)
**Files:** `sacco-security/src/main/java/com/innercircle/sacco/security/config/SecurityConfig.java`
**Action:**
1. Change `.anyRequest().permitAll()` (line 37) to `.anyRequest().authenticated()` — secure by default.
2. Restrict actuator to ADMIN role: `.requestMatchers("/actuator/**").hasRole("ADMIN")` (line 33).
3. Restrict Swagger to authenticated users in production: `.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()` (line 34). Keep permitAll for dev via a Spring profile condition — for now, just change to `.authenticated()`.

**Verify:**
```bash
mvn compile -pl sacco-security -q
```

**Done when:** No more `.anyRequest().permitAll()`. Actuator requires ADMIN. Swagger requires authentication.

### Task 2: Fix AuthorizationServerConfig (B4, W1)
**Files:** `sacco-security/src/main/java/com/innercircle/sacco/security/config/AuthorizationServerConfig.java`, `sacco-app/src/main/resources/application.yml`
**Action:**
1. Add `@Value` properties for client secrets:
   - `@Value("${oauth2.client.web-app.secret:changeme}")` for web-app client (line 60)
   - `@Value("${oauth2.client.batch-client.secret:changeme}")` for batch client (line 82)
2. Replace hardcoded `"web-app-secret"` and `"batch-client-secret"` with injected values.
3. For RSA keys (lines 96-119): Replace `generateRsaKey()` with loading from `application.yml` properties (`oauth2.jwt.rsa.public-key` / `oauth2.jwt.rsa.private-key`). Keep the key generation as a fallback when properties are not set (dev mode).
4. Add the `oauth2.*` properties to `application.yml` with env var placeholders:
   ```yaml
   oauth2:
     client:
       web-app:
         secret: ${OAUTH2_WEB_SECRET:changeme}
       batch-client:
         secret: ${OAUTH2_BATCH_SECRET:changeme}
   ```

**Verify:**
```bash
mvn compile -pl sacco-security,sacco-app -q
```

**Done when:** No hardcoded secrets in source. RSA keys loaded from config with dev fallback.

### Task 3: Add interest rate validation (W10)
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/LoanApplicationRequest.java`
**Action:**
Add `@DecimalMax(value = "100.0", message = "Interest rate must not exceed 100%")` to the `interestRate` field (line 29-30). Import `jakarta.validation.constraints.DecimalMax`.

**Verify:**
```bash
mvn compile -pl sacco-loan -q
```

**Done when:** Interest rate has both `@DecimalMin("0.0")` and `@DecimalMax("100.0")` validation.

## Verification

After all tasks:
```bash
mvn compile -q
```

## Commit Message
```
fix(security,loan): harden security config, externalize secrets, add rate validation

- B4: Externalize OAuth2 client secrets to application.yml with env var placeholders
- B5: Change .anyRequest().permitAll() to .authenticated()
- W1: Load RSA keys from config with dev fallback
- W2: Restrict actuator to ADMIN, Swagger to authenticated
- W10: Add @DecimalMax(100.0) to interest rate
```

---
*Planned: 2026-02-15*
