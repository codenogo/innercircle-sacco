# Plan 02: JWT Service + Security Config Overhaul

## Goal
Replace Spring Authorization Server with direct JWT issuance — create JwtService, update SecurityConfig, remove AuthorizationServerConfig and OAuth2 auth server dependency.

## Prerequisites
- [ ] Plan 01 complete (entities exist, but not strictly required — can run in parallel)

## Tasks

### Task 1: Create JwtService
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/service/JwtService.java`

**Action:**
- Create `JwtService` class annotated `@Service`
- Inject RSA key config using `@Value("${oauth2.jwt.rsa.public-key:}")` and `@Value("${oauth2.jwt.rsa.private-key:}")`
- Port RSA key loading logic from `AuthorizationServerConfig.loadRsaKeyFromConfig()` and `generateRsaKey()` (dev fallback)
- Initialize `NimbusJwtEncoder` and `NimbusJwtDecoder` in `@PostConstruct`
- Public methods:
  - `String generateAccessToken(UserAccount user)` — Creates JWT with claims: `sub` (username), `userId`, `email`, `roles`, `authorities`. Expiry: 1 hour. Issuer: `innercircle-sacco`
  - `String generateRefreshToken()` — Generates a secure random 32-byte hex string (not a JWT)
  - `Jwt decodeToken(String token)` — Decodes and validates a JWT using the public key
- Expose `JwtDecoder jwtDecoder()` and `JwtEncoder jwtEncoder()` as getters for SecurityConfig to use

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q
```

**Done when:** JwtService compiles with RSA key loading and JWT generation methods.

### Task 2: Update SecurityConfig + Add JSON Error Handling
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/config/SecurityConfig.java`

**Action:**
- Add `PasswordEncoder` bean (BCryptPasswordEncoder) — moving from AuthorizationServerConfig
- Add `JwtDecoder` bean that delegates to `JwtService.jwtDecoder()`
- Remove `formLogin(Customizer.withDefaults())`
- Remove `@Order(2)` annotation (no longer a second filter chain)
- Update `authorizeHttpRequests`:
  - Add `/api/auth/login` to permitAll
  - Add `/api/auth/refresh` to permitAll
  - Keep existing forgot/reset password permitAll
  - Keep `/api/**` authenticated
- Add custom `AuthenticationEntryPoint` returning JSON `{"error": "Unauthorized", "message": "...", "status": 401}` instead of HTML redirect
- Add custom `AccessDeniedHandler` returning JSON `{"error": "Forbidden", "message": "...", "status": 403}`

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q
```

**Done when:** SecurityConfig has own PasswordEncoder, JwtDecoder, JSON error handlers, no form login.

### Task 3: Remove OAuth2 Authorization Server
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/config/AuthorizationServerConfig.java` (DELETE)
- `sacco-security/src/main/java/com/innercircle/sacco/security/config/JwtTokenCustomizer.java` (DELETE)
- `sacco-security/pom.xml` (EDIT — remove spring-security-oauth2-authorization-server dependency)

**Action:**
- Delete `AuthorizationServerConfig.java` entirely
- Delete `JwtTokenCustomizer.java` entirely
- Remove `spring-security-oauth2-authorization-server` dependency from `sacco-security/pom.xml`
- Keep `spring-boot-starter-oauth2-resource-server` (still needed for JWT resource server)
- Verify no remaining imports reference deleted classes

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q && mvn -pl sacco-security test -q
```

**Done when:** OAuth2 Authorization Server completely removed, project compiles and tests pass with JwtService as the sole JWT provider.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q && mvn -pl sacco-security test -q
```

## Commit Message
```
feat(custom-auth-api): replace Spring Authorization Server with direct JWT via JwtService

- Create JwtService with RSA key loading and JWT generation
- Update SecurityConfig with PasswordEncoder, JwtDecoder, JSON error handlers
- Remove form login from SecurityConfig
- Delete AuthorizationServerConfig and JwtTokenCustomizer
- Remove spring-security-oauth2-authorization-server dependency
```

---
*Planned: 2026-02-15*
