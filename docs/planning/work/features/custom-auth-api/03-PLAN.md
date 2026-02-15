# Plan 03: Auth Service + Login/Refresh Endpoints

## Goal
Implement the login and token refresh flow — AuthService for credential validation and token management, DTOs, and REST endpoints on AuthController.

## Prerequisites
- [ ] Plan 01 complete (RefreshToken entity + repository)
- [ ] Plan 02 complete (JwtService + SecurityConfig updated)

## Tasks

### Task 1: Create Auth DTOs
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/LoginRequest.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/LoginResponse.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/RefreshTokenRequest.java`

**Action:**

**LoginRequest.java:**
```java
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
```

**LoginResponse.java:**
```java
public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,    // "Bearer"
    long expiresIn       // 3600
) {}
```

**RefreshTokenRequest.java:**
```java
public record RefreshTokenRequest(
    @NotBlank String refreshToken
) {}
```

Use Java records for immutability. Use Jakarta validation annotations.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q
```

**Done when:** All 3 DTO records compile.

### Task 2: Create AuthService
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/service/AuthService.java`

**Action:**
- `@Service` with `@Transactional` on write methods
- Inject: `SaccoUserDetailsService`, `PasswordEncoder`, `JwtService`, `UserAccountRepository`, `RefreshTokenRepository`
- Methods:
  - `LoginResponse authenticate(LoginRequest request)`:
    1. Load user via `SaccoUserDetailsService.loadUserByUsername()`
    2. Verify password with `PasswordEncoder.matches()`
    3. Load `UserAccount` from repository for JWT claims
    4. Generate access token via `JwtService.generateAccessToken()`
    5. Generate refresh token string via `JwtService.generateRefreshToken()`
    6. Persist `RefreshToken` entity (userId, token, expiresAt = now + 7 days)
    7. Return `LoginResponse`
    8. Throw `BadCredentialsException` on invalid credentials (generic message to prevent enumeration)
  - `LoginResponse refreshAccessToken(RefreshTokenRequest request)`:
    1. Find refresh token in DB
    2. Validate: not revoked, not expired
    3. Revoke old refresh token (rotation)
    4. Load UserAccount by userId
    5. Generate new access token + new refresh token
    6. Persist new RefreshToken, return new LoginResponse
    7. Throw `BadCredentialsException` if token invalid/expired/revoked

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q
```

**Done when:** AuthService compiles with authenticate and refreshAccessToken methods.

### Task 3: Extend AuthController with Login and Refresh Endpoints
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/controller/AuthController.java`

**Action:**
- Inject `AuthService`
- Add endpoint `POST /api/auth/login`:
  - Accepts `@Valid @RequestBody LoginRequest`
  - Returns `ResponseEntity<ApiResponse<LoginResponse>>`
  - Catches `BadCredentialsException` → 401 with generic error message
  - Catches `DisabledException`, `LockedException` → 403 with specific message
- Add endpoint `POST /api/auth/refresh`:
  - Accepts `@Valid @RequestBody RefreshTokenRequest`
  - Returns `ResponseEntity<ApiResponse<LoginResponse>>`
  - Catches `BadCredentialsException` → 401
- Use existing `ApiResponse` wrapper pattern from the project

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q && mvn -pl sacco-security test -q
```

**Done when:** POST /api/auth/login and POST /api/auth/refresh endpoints compile and existing tests pass.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q && mvn -pl sacco-security test -q
```

## Commit Message
```
feat(custom-auth-api): add AuthService with login and token refresh endpoints

- Create LoginRequest, LoginResponse, RefreshTokenRequest DTOs
- Create AuthService with authenticate and refreshAccessToken
- Extend AuthController with POST /api/auth/login and POST /api/auth/refresh
- Implement refresh token rotation for security
```

---
*Planned: 2026-02-15*
