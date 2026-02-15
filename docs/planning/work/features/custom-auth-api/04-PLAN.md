# Plan 04: Me Endpoint + Admin User Creation

## Goal
Add the current-user endpoint (`GET /api/v1/me`) and admin-only user creation (`POST /api/v1/admin/users`) with password-reset trigger.

## Prerequisites
- [ ] Plan 01 complete (memberId on UserAccount)
- [ ] Plan 02 complete (SecurityConfig, JwtService)

## Tasks

### Task 1: Create MeController with GET /api/v1/me
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/MeResponse.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/MemberSummary.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/controller/MeController.java`

**Action:**

**MemberSummary.java:**
```java
public record MemberSummary(
    UUID id,
    String firstName,
    String lastName,
    String memberNumber
) {}
```

**MeResponse.java:**
```java
public record MeResponse(
    UUID id,
    String username,
    String email,
    Boolean enabled,
    Set<String> roles,
    MemberSummary member,   // null if not linked
    Instant createdAt,
    Instant updatedAt
) {}
```

**MeController.java:**
- `@RestController @RequestMapping("/api/v1/me")`
- Inject `UserAccountRepository` and `MemberRepository` (from sacco-member module — add sacco-member dependency to sacco-security pom.xml)
- `GET /api/v1/me`:
  - Extract username from `SecurityContextHolder.getContext().getAuthentication().getName()`
  - Load UserAccount by username
  - If `memberId` is set, load Member and build `MemberSummary`
  - Return `ApiResponse<MeResponse>`
- Add `sacco-member` dependency to `sacco-security/pom.xml`:
  ```xml
  <dependency>
      <groupId>com.innercircle</groupId>
      <artifactId>sacco-member</artifactId>
  </dependency>
  ```

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q
```

**Done when:** GET /api/v1/me returns current user profile with optional member data.

### Task 2: Create CreateUserRequest DTO + Admin User Creation Service
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/dto/CreateUserRequest.java`
- `sacco-security/src/main/java/com/innercircle/sacco/security/service/UserManagementService.java` (extend interface)
- `sacco-security/src/main/java/com/innercircle/sacco/security/service/UserManagementServiceImpl.java` (implement)

**Action:**

**CreateUserRequest.java:**
```java
public record CreateUserRequest(
    @NotBlank @Size(min = 3, max = 100) String username,
    @NotBlank @Email String email,
    @NotEmpty Set<String> roleNames,
    UUID memberId,                         // optional
    @Builder.Default Boolean sendPasswordResetEmail  // default true
) {}
```
Use a regular class instead of record if `@Builder.Default` is needed.

**Extend UserManagementService interface:**
- Add `UserResponse createUser(CreateUserRequest request)`

**Implement in UserManagementServiceImpl:**
- Validate username/email uniqueness (throw ConflictException if exists)
- Validate roleNames against existing roles
- If memberId provided, validate it exists via MemberRepository
- Create UserAccount with:
  - Random temporary password (BCrypt-encoded UUID — user will never use it)
  - enabled = true, accountNonLocked = true
  - Link roles by name lookup
  - Set memberId
- Save UserAccount
- If sendPasswordResetEmail is true (default), call `PasswordResetService.requestPasswordReset(email)` to trigger reset email
- Return UserResponse

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q
```

**Done when:** createUser method compiles with validation, user creation, and password reset trigger.

### Task 3: Create UserAdminController
**Files:**
- `sacco-security/src/main/java/com/innercircle/sacco/security/controller/UserAdminController.java`

**Action:**
- `@RestController @RequestMapping("/api/v1/admin/users") @PreAuthorize("hasRole('ADMIN')")`
- Inject `UserManagementService`, `PasswordResetService`
- Endpoints:
  - `POST /api/v1/admin/users`:
    - Accepts `@Valid @RequestBody CreateUserRequest`
    - Returns `ResponseEntity<ApiResponse<UserResponse>>` with 201 Created
    - Delegates to `userManagementService.createUser()`
  - `POST /api/v1/admin/users/{id}/password-reset`:
    - Admin triggers password reset for a specific user
    - Load user by ID, call `passwordResetService.requestPasswordReset(user.getEmail())`
    - Returns `ResponseEntity<ApiResponse<Void>>` with 200 OK and "Password reset email sent"

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q && mvn -pl sacco-security test -q
```

**Done when:** Admin user creation and password reset trigger endpoints compile and existing tests pass.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn -pl sacco-security -am compile -q && mvn -pl sacco-security test -q
```

## Commit Message
```
feat(custom-auth-api): add /api/v1/me endpoint and admin user creation

- Create MeController with GET /api/v1/me returning user profile + member data
- Create CreateUserRequest DTO for admin user creation
- Extend UserManagementService with createUser method
- Create UserAdminController with POST /api/v1/admin/users
- Add admin-triggered password reset endpoint
```

---
*Planned: 2026-02-15*
