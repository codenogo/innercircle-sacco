# Custom Auth API - Implementation Context

## Summary

Replace Spring Authorization Server's form-based OAuth2 login with direct JWT REST endpoints. Remove the built-in Authorization Server, add `/api/auth/login`, `/api/auth/refresh`, `/api/v1/me`, and admin user-creation endpoints. Seed an initial admin user via Liquibase. Link UserAccount to Member for self-service.

## Current State

| Component | Current | Target |
|-----------|---------|--------|
| Login | Spring form login at `/login` (OAuth2 Authorization Code + PKCE) | REST `POST /api/auth/login` returning JWT |
| Token issuance | Spring Authorization Server (`AuthorizationServerConfig`) | Direct JWT creation via `JwtEncoder` |
| Registration | No endpoint (DTO exists, route declared `permitAll` but unimplemented) | Admin-only `POST /api/v1/admin/users` |
| Token refresh | OAuth2 refresh_token grant | `POST /api/auth/refresh` with refresh token |
| Current user | None | `GET /api/v1/me` returning profile + roles + member info |
| Seed admin | None (no users in DB after migration) | Liquibase migration seeding default admin |
| User-Member link | None | `member_id` FK on `user_accounts` table |
| OAuth2 clients | In-memory `web-app` + `batch-client` | Remove (no longer needed) |

## Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| IdP strategy | Direct JWT (no external IdP) | Single-app SACCO; Keycloak/external IdP is overkill. Keep it simple with self-contained JWT issuance. |
| Registration | Admin-only user creation | SACCO context: members are known entities. Admin creates accounts and sends password-reset email. No public self-registration. |
| Token strategy | Short-lived access (1h) + refresh token (7d) | Industry standard. Short access token limits blast radius of token theft. Refresh token enables seamless UX. |
| Current user endpoint | `GET /api/v1/me` | Frontend needs user profile, roles, and linked member data after login to hydrate the UI. Borrowed from CommHub's `/api/v1/me` pattern. |
| Seed admin | Liquibase migration with BCrypt-hashed default password | System must be usable on first startup. Admin must change password on first login. |
| Onboarding flow | Password reset link via email | Admin creates user, system emails reset link. More secure than sending temp passwords in transit. |
| User-Member link | `member_id` FK on `user_accounts` | Enables `/api/v1/me` to return linked member data. Required for member self-service features. |
| Batch client auth | Keep as API key or client-credentials | Batch jobs (interest accrual, etc.) still need service-to-service auth. Simplify from OAuth2 client-credentials to a simpler mechanism. |

## Architecture

### New Auth Flow

```
Frontend                     Backend
   │                            │
   │  POST /api/auth/login      │
   │  {username, password}      │
   │───────────────────────────▶│
   │                            │── Validate credentials
   │                            │── Generate access + refresh JWT
   │  {accessToken, refreshToken, │
   │   expiresIn, tokenType}    │
   │◀───────────────────────────│
   │                            │
   │  GET /api/v1/me            │
   │  Authorization: Bearer ... │
   │───────────────────────────▶│
   │                            │── Decode JWT, load user + member
   │  {id, username, email,     │
   │   roles, member: {...}}    │
   │◀───────────────────────────│
   │                            │
   │  POST /api/auth/refresh    │
   │  {refreshToken}            │
   │───────────────────────────▶│
   │                            │── Validate refresh token
   │                            │── Issue new access token
   │  {accessToken, expiresIn}  │
   │◀───────────────────────────│
```

### Admin User Creation Flow

```
Admin                        Backend                    Email
  │                            │                          │
  │  POST /api/v1/admin/users  │                          │
  │  {username, email, roles,  │                          │
  │   memberId}                │                          │
  │───────────────────────────▶│                          │
  │                            │── Create UserAccount     │
  │                            │── Link to Member         │
  │                            │── Generate reset token   │
  │                            │── Send reset email ─────▶│
  │  {user details}            │                          │
  │◀───────────────────────────│                          │
```

### New Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/auth/login` | Public | Authenticate, return access + refresh tokens |
| POST | `/api/auth/refresh` | Public | Exchange refresh token for new access token |
| POST | `/api/auth/forgot-password` | Public | Request password reset email (existing) |
| POST | `/api/auth/reset-password` | Public | Reset password with token (existing) |
| GET | `/api/v1/me` | Bearer | Current user profile + roles + member info |
| POST | `/api/v1/admin/users` | ADMIN | Create user account (admin-only) |
| GET | `/api/v1/users` | ADMIN | List users (existing) |
| GET | `/api/v1/users/{id}` | ADMIN | Get user (existing) |
| PATCH | `/api/v1/users/{id}/*` | ADMIN | Activate/deactivate/lock/unlock (existing) |
| PUT | `/api/v1/users/{id}/roles` | ADMIN | Update roles (existing) |
| DELETE | `/api/v1/users/{id}` | ADMIN | Soft delete (existing) |
| POST | `/api/v1/admin/users/{id}/password-reset` | ADMIN | Trigger password reset for user |

### DTOs

**LoginRequest:**
```java
{
  "username": "string",   // required
  "password": "string"    // required
}
```

**LoginResponse:**
```java
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**RefreshTokenRequest:**
```java
{
  "refreshToken": "string"  // required
}
```

**MeResponse:**
```java
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "enabled": true,
  "roles": ["ADMIN", "MEMBER"],
  "member": {                    // null if not linked
    "id": "uuid",
    "firstName": "string",
    "lastName": "string",
    "memberNumber": "string"
  },
  "createdAt": "instant",
  "updatedAt": "instant"
}
```

**CreateUserRequest (admin):**
```java
{
  "username": "string",         // required
  "email": "string",            // required, valid email
  "roleNames": ["MEMBER"],      // required, at least one
  "memberId": "uuid",           // optional, link to member
  "sendPasswordResetEmail": true // default true
}
```

## What to Remove

| File/Component | Action |
|----------------|--------|
| `AuthorizationServerConfig.java` | Remove entirely (Spring Authorization Server) |
| `JwtTokenCustomizer.java` (OAuth2TokenCustomizer) | Remove — replaced by direct JWT building in AuthService |
| `spring-boot-starter-oauth2-authorization-server` dependency | Remove from pom.xml |
| OAuth2 tables migration (`003-create-oauth2-tables.yaml`) | Keep but mark deprecated (data exists in DB) |
| `RegisterRequest.java` | Repurpose as `CreateUserRequest` or remove |
| `web-app` / `batch-client` registered clients | Remove (InMemoryRegisteredClientRepository) |
| Form login config (`formLogin(Customizer.withDefaults())`) | Remove |

## What to Add

| Component | Details |
|-----------|---------|
| `AuthService` | Login validation, JWT generation, refresh token management |
| `AuthController` (extend existing) | `/api/auth/login`, `/api/auth/refresh` |
| `MeController` | `GET /api/v1/me` |
| `UserAdminController` | `POST /api/v1/admin/users`, trigger password reset |
| `RefreshToken` entity + repository | Persistent refresh tokens (DB-backed) |
| Liquibase: `005-seed-admin-user.yaml` | Insert default admin with BCrypt password |
| Liquibase: `006-add-member-id-to-users.yaml` | Add `member_id` FK column to `user_accounts` |
| Liquibase: `007-create-refresh-tokens.yaml` | Create `refresh_tokens` table |
| JWT utility / `JwtService` | Encode/decode access tokens using RSA keys |

## Constraints

- Must preserve existing `/api/v1/users` admin endpoints (no breaking changes)
- Must preserve existing password reset flow (forgot + reset endpoints)
- RSA key configuration (`oauth2.jwt.rsa.*`) should continue to work for JWT signing
- BCrypt password encoding must remain consistent
- Existing `SaccoUserDetailsService` can be reused for credential validation
- Batch client auth needs a replacement mechanism (API key or service token)
- Default admin password must meet existing complexity rules (8+ chars, upper, lower, digit, special)

## Open Questions

- Should the default admin password be configurable via environment variable instead of hardcoded in Liquibase? (Recommendation: use env var `SACCO_ADMIN_DEFAULT_PASSWORD` with fallback)
- Should refresh tokens be single-use (rotate on each refresh) or reusable? (Recommendation: rotate for security)
- How should batch client authenticate after removing OAuth2 client-credentials? (Recommendation: defer to a separate task)

## Related Code

- `sacco-security/.../config/AuthorizationServerConfig.java` — To be removed
- `sacco-security/.../config/SecurityConfig.java` — To be modified (remove form login, keep resource server)
- `sacco-security/.../config/JwtTokenCustomizer.java` — To be removed
- `sacco-security/.../service/SaccoUserDetailsService.java` — Reused for credential validation
- `sacco-security/.../service/PasswordResetServiceImpl.java` — Keep, extend for admin-triggered resets
- `sacco-security/.../controller/UserManagementController.java` — Keep existing endpoints
- `sacco-security/.../controller/AuthController.java` — Extend with login/refresh
- `sacco-security/.../entity/UserAccount.java` — Add `memberId` field
- `sacco-security/.../repository/UserAccountRepository.java` — Keep
- `sacco-member/.../entity/Member.java` — Referenced via FK

## Reference: CommHub Patterns Borrowed

| Pattern | CommHub Source | Sacco Adaptation |
|---------|---------------|------------------|
| `/api/v1/me` endpoint | `MeController` + `MeResponse` | Simplified: no multi-tenant, no external identity |
| Admin user creation | `UserAdminController` + `CreateUserRequest` | Simplified: no IdP integration, direct DB |
| Password reset on creation | `UserAdminService.createUser()` → `sendPasswordResetEmail` | Reuse existing `PasswordResetService` |
| Stateless JWT resource server | `OidcSecurityConfig` | Similar: pure resource server after removing AuthorizationServer |
| JSON error responses (not HTML) | `OidcSecurityConfig` exception handlers | Add custom `AuthenticationEntryPoint` returning JSON |

---
*Discussed: 2026-02-15*
