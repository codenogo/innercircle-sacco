# Plan 04: User Account Management API

## Goal
Add admin user account management endpoints for activate/deactivate, search, role management, and account status control.

## Prerequisites
- [x] Security module with UserAccount and Role entities
- [x] UserAccountRepository exists

## Tasks

### Task 1: Create UserManagementService
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/service/UserManagementService.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/service/UserManagementServiceImpl.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/UserSearchRequest.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/UpdateUserRequest.java`

**Action:**
Create service interface and implementation with methods:
- `activateUser(UUID userId)` — sets enabled=true
- `deactivateUser(UUID userId)` — sets enabled=false
- `lockUser(UUID userId)` — sets accountNonLocked=false
- `unlockUser(UUID userId)` — sets accountNonLocked=true
- `searchUsers(String query, String cursor, int limit)` — search by username, email, or name (using LIKE queries via JdbcTemplate)
- `getUserById(UUID userId)` — returns UserResponse
- `listUsers(String cursor, int limit)` — paginated user list
- `updateUserRoles(UUID userId, Set<String> roleNames)` — updates user's role assignments
- `deleteUser(UUID userId)` — soft delete (deactivate, not remove)

Create DTOs: `UserSearchRequest` (query, cursor, limit), `UpdateUserRequest` (enabled, accountNonLocked, roles).

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-security -q -DskipTests
```

**Done when:** Service compiles with all methods, DTOs created.

### Task 2: Create UserManagementController
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/controller/UserManagementController.java`

**Action:**
Create REST controller at `/api/v1/users` with ADMIN-only endpoints:
- `GET /api/v1/users` — List all users (cursor pagination)
- `GET /api/v1/users/{id}` — Get user by ID
- `GET /api/v1/users/search?q={query}` — Search users
- `PATCH /api/v1/users/{id}/activate` — Activate user
- `PATCH /api/v1/users/{id}/deactivate` — Deactivate user
- `PATCH /api/v1/users/{id}/lock` — Lock user account
- `PATCH /api/v1/users/{id}/unlock` — Unlock user account
- `PUT /api/v1/users/{id}/roles` — Update user roles (body: list of role names)
- `DELETE /api/v1/users/{id}` — Soft-delete user

All endpoints secured with `@PreAuthorize("hasRole('ADMIN')")`. All return `ApiResponse<T>`.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-security -q -DskipTests
```

**Done when:** Controller compiles, all endpoints defined, security annotations applied.

### Task 3: Add User Search Repository Methods
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/repository/UserAccountRepository.java`

**Action:**
Add repository methods needed by UserManagementService:
- `findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email)` — for search
- `findByEnabledTrue()` — list active users
- `findByEnabledFalse()` — list deactivated users
- Cursor-based pagination support: `findByIdGreaterThanOrderById(UUID cursor, Pageable pageable)`

If existing repository already has some methods, only add the missing ones.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-security -q -DskipTests
```

**Done when:** Repository compiles with search and pagination methods.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn clean compile -q -DskipTests
```

## Commit Message
```
feat(security): add user account management API

- CRUD operations for user accounts (ADMIN only)
- Activate/deactivate/lock/unlock user accounts
- User search by username or email
- Role management for user accounts
```

---
*Planned: 2026-02-15*
