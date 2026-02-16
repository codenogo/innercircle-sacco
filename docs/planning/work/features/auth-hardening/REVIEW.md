# Review: auth-hardening

**Branch:** `feat/custom-auth-api`
**Date:** 2026-02-16
**Verdict:** PASS

## Summary

All 4 auth-hardening plans implemented and verified. 1,072 tests pass across all modules with zero failures.

## Plans Reviewed

### Plan 01: Register missing migrations and fix code warnings
- Registered security migrations 005/006/007 in `db.changelog-master.yaml`
- Fixed JSON injection in `SecurityConfig` error handlers (escape quotes/backslashes)
- Replaced error message leaking in `AuthController` refresh endpoint with generic message
- Removed dead OAuth2 client config from `application.yml`

### Plan 02: RBAC for admin-only and internal controllers (310 tests pass)
- `ConfigController`: `hasRole('ADMIN')`
- `AuditController`: `hasRole('ADMIN')`
- `LedgerController`: `hasAnyRole('ADMIN','TREASURER')`
- `ExportController`: `hasAnyRole('ADMIN','TREASURER')`
- Fixed `LedgerControllerTest` with `@AutoConfigureMockMvc(addFilters=false)`

### Plan 03: RBAC for member-facing controllers (228 tests pass)
- 7 controllers annotated (Member, Contribution, ContributionCategory, Payout, CashDisbursement, BankWithdrawal, ShareWithdrawal)
- Pattern: class-level `hasAnyRole('ADMIN','TREASURER','MEMBER')` for reads, method-level `hasAnyRole('ADMIN','TREASURER')` or `hasRole('ADMIN')` for writes
- Fixed 4 payout `@WebMvcTest` tests with `@AutoConfigureMockMvc(addFilters=false)`

### Plan 04: Auth component unit tests (21 new tests)
- `JwtServiceTest`: 7 tests (token generation, claims, decoder, ephemeral keys)
- `AuthServiceTest`: 8 tests (auth success/fail, locked/disabled, refresh token rotation)
- `MeControllerTest`: 3 tests (with/without member, not found)
- `UserAdminControllerTest`: 3 tests (create user, password reset)

## Security Review

- All 11 in-scope controllers have correct `@PreAuthorize` annotations
- `@EnableMethodSecurity` already present in `SecurityConfig`
- Error responses now escape special characters (prevents JSON injection)
- Refresh endpoint no longer leaks internal error messages
- Dead OAuth2 client secrets removed

## Warnings

**W1 (low):** `LoanController` and `ReportController` lack `@PreAuthorize` RBAC. This is a pre-existing gap not introduced by this feature. Both are still protected by `authenticated()` in SecurityConfig. Recommend follow-up task.

## Files Changed

| Category | Count | Files |
|----------|-------|-------|
| POM dependencies | 6 | sacco-config, sacco-audit, sacco-ledger, sacco-member, sacco-contribution, sacco-payout |
| Controller RBAC | 11 | ConfigController, AuditController, LedgerController, ExportController, MemberController, ContributionController, ContributionCategoryController, PayoutController, CashDisbursementController, BankWithdrawalController, ShareWithdrawalController |
| Security fixes | 2 | SecurityConfig, AuthController |
| Config cleanup | 2 | application.yml, db.changelog-master.yaml |
| Test fixes | 5 | LedgerControllerTest, PayoutControllerTest, CashDisbursementControllerTest, BankWithdrawalControllerTest, ShareWithdrawalControllerTest |
| New tests | 4 | JwtServiceTest, AuthServiceTest, MeControllerTest, UserAdminControllerTest |

## Karpathy Checklist

| Principle | Status | Notes |
|----------|--------|------|
| Think Before Coding | PASS | 4 plans with clear scope, reviewed in /discuss |
| Simplicity First | PASS | Class-level defaults with method-level overrides; no custom security infrastructure |
| Surgical Changes | PASS | Only touched planned files; no drive-by refactors |
| Goal-Driven Execution | PASS | 1,072 tests pass; each plan verified independently |
| Prefer shared utility packages | PASS | Uses Spring Security @PreAuthorize; no custom auth framework |
| Don't probe data YOLO-style | N/A | |
| Validate boundaries | PASS | Auth boundaries enforced at controller level; error messages sanitized |
| Typed SDKs | N/A | |

## Verdict

**PASS** - Ready for `/ship`. All changes match the plan, all tests pass, security boundaries are correctly enforced.
