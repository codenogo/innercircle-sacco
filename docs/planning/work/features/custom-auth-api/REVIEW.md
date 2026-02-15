# Review Report: custom-auth-api

**Date:** 2026-02-15
**Branch:** feat/custom-auth-api
**Commit:** `21ee28b` feat(custom-auth-api): replace Spring Auth Server with direct JWT REST API
**Reviewer:** Claude

## Automated Checks

| Check | Result |
|-------|--------|
| Compile | PASS |
| Tests (sacco-security -am) | PASS (122 tests, 0 failures) |
| Secret Scan | PASS |
| Dependency Audit | PASS (OAuth2 auth server removed, sacco-member added) |

## Issues Found

### Blockers (must fix)

None.

### Warnings (should fix)

| File | Line | Issue | Severity |
|------|------|-------|----------|
| `SecurityConfig.java` | 72-83 | JSON string interpolation without escaping. `authException.getMessage()` / `accessDeniedException.getMessage()` passed directly to `String.format` in JSON template. If message contains quotes or backslashes, response will be malformed JSON. Use Jackson ObjectMapper or manually escape. | Medium |
| `AuthController.java` | 111 | Refresh endpoint leaks internal error message via `e.getMessage()`. Login endpoint correctly uses static messages. Should use a generic message for consistency. | Low |

### Suggestions (optional)

| File | Line | Suggestion |
|------|------|------------|
| `JwtService.java` | 107 | `new SecureRandom()` created on every `generateRefreshToken()` call. Reuse a single instance as a class field. |
| `AuthService.java` | - | No rate limiting on login or refresh attempts. Consider brute-force protection (account lockout after N failures, or controller-level rate limiting). |
| `RefreshTokenRepository.java` | 22 | `deleteByExpiresAtBefore` method exists but is never called. Add a `@Scheduled` cleanup job for expired/revoked tokens. |
| `JwtService.java` | 76 | Access token expiry (3600s) and refresh token expiry (7d) are hardcoded. Consider making them configurable via `@Value`. |

## Manual Review Checklist

### Security

| Check | Status |
|-------|--------|
| No hardcoded credentials | PASS (admin uses BCrypt hash in migration, RSA keys from config or ephemeral) |
| Input validation present | PASS (@Valid on all request bodies, @NotBlank/@Email/@Size constraints) |
| Output encoding (XSS prevention) | WARN (String.format in SecurityConfig JSON responses - see warnings) |
| SQL injection prevention | PASS (JPA parameterized queries throughout) |
| Auth/authz correctly applied | PASS (@PreAuthorize("hasRole('ADMIN')") on admin endpoints, permitAll on auth, authenticated() on all /api/**) |
| Sensitive data not logged | PASS (passwords never logged, only usernames via SLF4J parameterized logging) |
| HTTPS/TLS for external calls | N/A (infrastructure layer) |

### Code Quality

| Check | Status |
|-------|--------|
| Functions <=50 lines | PASS |
| Clear, descriptive naming | PASS |
| No magic numbers/strings | WARN (token expiry values hardcoded - see suggestions) |
| Error handling present | PASS (proper exception hierarchy: BadCredentials, Disabled, Locked) |
| Logging appropriate | PASS (WARN for auth failures, INFO for successes, ERROR for unexpected) |
| No TODO without ticket | PASS |
| Consistent with patterns | PASS (uses ApiResponse wrapper, BaseEntity, standard Spring conventions) |

### Testing

| Check | Status |
|-------|--------|
| Unit tests for new logic | PARTIAL (AuthService, JwtService, MeController, UserAdminController lack dedicated tests) |
| Edge cases covered | PASS (for existing test classes) |
| Error cases tested | PASS (for existing test classes) |
| Integration tests (if API) | N/A (skipped ITs) |
| No flaky test patterns | PASS |

### Cross-Cutting

| Check | Status |
|-------|--------|
| API contracts preserved | PASS (new endpoints only, no breaking changes) |
| Database migrations reversible | PASS (all 3 migrations have rollback sections) |
| Backward compatible | PASS |
| Feature flag for risky changes | N/A |
| Documentation updated | PASS (summary artifacts for all 4 plans) |

### Karpathy Checklist

| Principle | Status | Notes |
|----------|--------|------|
| Think Before Coding | PASS | Clear CONTEXT.md with decisions, 4 plans with explicit boundaries |
| Simplicity First | PASS | Removed OAuth2 auth server complexity, replaced with direct JWT - simpler overall |
| Surgical Changes | PASS | Changes scoped to sacco-security module + 3 migrations, no unrelated refactors |
| Goal-Driven Execution | PASS | Each plan had explicit verify commands, all passed |

## Architectural Notes

- **RSA key management:** Ephemeral keys in dev (with WARN log), configurable PEM keys for production. Good dual-mode approach.
- **Token rotation:** Refresh token is revoked and replaced on each use. Standard security practice.
- **Stateless sessions:** CSRF disabled for `/api/**` with STATELESS session policy. Correct for JWT-based APIs.
- **Admin seed:** BCrypt-hashed password in Liquibase migration. Admin must reset password on first login (via password reset flow).
- **CORS:** Configurable allowed origins with sensible defaults (localhost:3000, localhost:8080).

## Verdict

**WARN** - Review passed with warnings.

The 2 warnings are low-to-medium severity and do not block shipping:
1. SecurityConfig JSON string interpolation could produce malformed responses in edge cases
2. Refresh endpoint leaks internal error messages

Both can be addressed in a follow-up quick fix.

**Ready for `/ship` with the above warnings noted.**

---
*Reviewed: 2026-02-15*
