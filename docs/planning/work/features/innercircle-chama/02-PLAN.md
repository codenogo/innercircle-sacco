# Plan 02: Security & Authentication

## Goal
Implement OAuth2 security with Spring Authorization Server, JWT tokens, and role-based access control (ADMIN, TREASURER, SECRETARY, MEMBER).

## Prerequisites
- [ ] Plan 01 complete

## Tasks

### Task 1: Spring Authorization Server Setup
**Files:** `sacco-security/src/main/java/com/innercircle/sacco/security/config/AuthorizationServerConfig.java`, `sacco-security/src/main/java/com/innercircle/sacco/security/config/SecurityConfig.java`, `sacco-security/src/main/java/com/innercircle/sacco/security/entity/UserAccount.java`, `sacco-security/src/main/java/com/innercircle/sacco/security/entity/Role.java`, `sacco-security/src/main/java/com/innercircle/sacco/security/repository/UserAccountRepository.java`, `sacco-security/src/main/java/com/innercircle/sacco/security/repository/RoleRepository.java`
**Action:**
1. Configure Spring Authorization Server with registered clients (web-app: auth code + PKCE, batch-client: client credentials)
2. Create `UserAccount` entity (extends BaseEntity): username, password (BCrypt), email, enabled, accountNonLocked + ManyToMany roles
3. Create `Role` entity: name (enum: ADMIN, TREASURER, SECRETARY, MEMBER), description
4. JPA repositories for both entities
5. `SecurityConfig`: resource server with JWT decoder, CORS config, CSRF for API

**Verify:**
```bash
mvn compile -pl sacco-security -q
```

**Done when:** Security module compiles with Authorization Server and entity classes.

### Task 2: UserDetailsService + JWT Customization
**Files:** `sacco-security/src/main/java/com/innercircle/sacco/security/service/SaccoUserDetailsService.java`, `sacco-security/src/main/java/com/innercircle/sacco/security/config/JwtTokenCustomizer.java`, `sacco-security/src/main/java/com/innercircle/sacco/security/dto/RegisterRequest.java`, `sacco-security/src/main/java/com/innercircle/sacco/security/dto/UserResponse.java`
**Action:**
1. `SaccoUserDetailsService` implements `UserDetailsService`: loads user from DB, maps roles to granted authorities
2. `JwtTokenCustomizer` implements `OAuth2TokenCustomizer<JwtEncodingContext>`: adds user ID, roles, and member ID to JWT claims
3. DTOs for registration and user response

**Verify:**
```bash
mvn compile -pl sacco-security -q
```

**Done when:** Custom UserDetailsService and JWT customizer compile.

### Task 3: Liquibase Changelogs + Role Seed Data
**Files:** `sacco-security/src/main/resources/db/changelog/security/001-create-user-tables.yaml`, `sacco-security/src/main/resources/db/changelog/security/002-seed-roles.yaml`, `sacco-security/src/main/resources/db/changelog/security/003-create-oauth2-tables.yaml`
**Action:**
1. Changelog 001: Create `user_accounts` table (id UUID PK, username, password_hash, email, enabled, account_non_locked, created_at, updated_at), `roles` table (id UUID PK, name VARCHAR UNIQUE, description), `user_roles` join table
2. Changelog 002: Seed 4 roles (ADMIN, TREASURER, SECRETARY, MEMBER) + default admin user
3. Changelog 003: Spring Authorization Server registered client tables (oauth2_registered_client, oauth2_authorization, oauth2_authorization_consent)
4. Update sacco-app's `db.changelog-master.yaml` to include security changelogs

**Verify:**
```bash
mvn compile -pl sacco-security -q
```

**Done when:** Liquibase changelogs are valid YAML and module compiles.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-security -q
```

## Commit Message
```
feat(security): implement OAuth2 with Spring Authorization Server

- Authorization Code (PKCE) + Client Credentials flows
- JWT with custom claims (userId, roles, memberId)
- UserAccount + Role entities with BCrypt passwords
- Liquibase changelogs for security tables + role seed data
```

---
*Planned: 2026-02-14*
