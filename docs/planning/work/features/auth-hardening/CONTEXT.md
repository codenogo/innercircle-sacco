# Auth Hardening: Migrations, RBAC, Code Fixes, Tests

## Summary

Bundle fix for post-JWT-migration gaps: register missing DB migrations, add role-based authorization across all controllers, fix review warnings, add unit tests for new auth components.

## Decisions

### 1. Register Missing Migrations (Critical)

Register `005-seed-admin-user`, `006-add-member-id-to-users`, `007-create-refresh-tokens` in `db.changelog-master.yaml`. Without these, the login/refresh system doesn't work in production.

### 2. Add @PreAuthorize Per Controller

Follow the existing pattern (LoanBatchController, DashboardController). Add class-level or method-level `@PreAuthorize` to all unprotected controllers:

| Controller | Reads | Writes |
|-----------|-------|--------|
| MemberController | ADMIN, TREASURER, MEMBER | ADMIN |
| ContributionController | ADMIN, TREASURER, MEMBER | ADMIN, TREASURER |
| ContributionCategoryController | ADMIN | ADMIN |
| PayoutController | ADMIN, TREASURER, MEMBER | ADMIN, TREASURER |
| CashDisbursementController | ADMIN, TREASURER, MEMBER | ADMIN, TREASURER |
| BankWithdrawalController | ADMIN, TREASURER, MEMBER | ADMIN, TREASURER |
| ShareWithdrawalController | ADMIN, TREASURER, MEMBER | ADMIN, TREASURER |
| LedgerController | ADMIN, TREASURER | ADMIN, TREASURER |
| AuditController | ADMIN | ADMIN |
| ConfigController | ADMIN | ADMIN |
| ExportController | ADMIN, TREASURER | ADMIN, TREASURER |

IDOR protection (restricting MEMBER to own data) is deferred to a separate feature.

### 3. Fix SecurityConfig JSON Interpolation

Escape `authException.getMessage()` in JSON response templates to prevent malformed output when messages contain quotes/backslashes.

### 4. Fix Refresh Endpoint Error Leaking

Replace `e.getMessage()` with a generic error message in the refresh token endpoint. Matches the login endpoint pattern.

### 5. Remove Legacy OAuth2 Config

Remove `oauth2.client.web-app.secret` and `oauth2.client.batch-client.secret` from `application.yml` (dead config).

### 6. Add Auth Component Tests

Unit tests for: `AuthService`, `JwtService`, `MeController`, `UserAdminController`.

## Constraints

- Must not break existing `@PreAuthorize` annotations
- Must not modify already-applied Liquibase changesets
- `@EnableMethodSecurity` already present in SecurityConfig
- Modules without spring-security dependency need it added to pom.xml
- Controller tests using `@WebMvcTest` need `@WithMockUser` or equivalent

## Open Questions

- IDOR protection for MEMBER role (viewing own data only) — deferred to follow-up
- ExportController authorization — recommend ADMIN,TREASURER (same as Dashboard)

---
*Discussed: 2026-02-16*
