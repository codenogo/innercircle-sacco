# Plan 03: Member Management

## Goal
Implement member registration, profiles, and CRUD operations with cursor-paginated listing and role assignment.

## Prerequisites
- [ ] Plan 01 complete

## Tasks

### Task 1: Member Entity + Repository + Service
**Files:** `sacco-member/src/main/java/com/innercircle/sacco/member/entity/Member.java`, `sacco-member/src/main/java/com/innercircle/sacco/member/entity/MemberStatus.java`, `sacco-member/src/main/java/com/innercircle/sacco/member/repository/MemberRepository.java`, `sacco-member/src/main/java/com/innercircle/sacco/member/service/MemberService.java`, `sacco-member/src/main/java/com/innercircle/sacco/member/service/MemberServiceImpl.java`
**Action:**
1. `Member` entity (extends BaseEntity): memberNumber (auto-generated), firstName, lastName, email, phone, nationalId, dateOfBirth, joinDate, status (ACTIVE, SUSPENDED, EXITED), shareBalance (BigDecimal)
2. `MemberStatus` enum: ACTIVE, SUSPENDED, EXITED
3. `MemberRepository`: JPA repository with cursor-based pagination query (WHERE id > :cursor ORDER BY id LIMIT :size)
4. `MemberService` interface + `MemberServiceImpl`: register, update, findById, findAll (cursor-paginated), suspend, reactivate, exit

**Verify:**
```bash
mvn compile -pl sacco-member -q
```

**Done when:** Member entity, repository, and service compile.

### Task 2: Member REST API
**Files:** `sacco-member/src/main/java/com/innercircle/sacco/member/controller/MemberController.java`, `sacco-member/src/main/java/com/innercircle/sacco/member/dto/CreateMemberRequest.java`, `sacco-member/src/main/java/com/innercircle/sacco/member/dto/UpdateMemberRequest.java`, `sacco-member/src/main/java/com/innercircle/sacco/member/dto/MemberResponse.java`, `sacco-member/src/main/java/com/innercircle/sacco/member/mapper/MemberMapper.java`
**Action:**
1. `MemberController`: REST controller at `/api/v1/members`
   - POST `/` — register member (ADMIN, SECRETARY)
   - GET `/` — list members with cursor pagination (all roles)
   - GET `/{id}` — get member by ID (all roles, MEMBER can only see own)
   - PUT `/{id}` — update member (ADMIN, SECRETARY)
   - PATCH `/{id}/suspend` — suspend member (ADMIN)
   - PATCH `/{id}/reactivate` — reactivate member (ADMIN)
2. DTOs: CreateMemberRequest (validated), UpdateMemberRequest, MemberResponse
3. `MemberMapper`: entity ↔ DTO mapping (manual or MapStruct)
4. All endpoints return `ApiResponse<T>`

**Verify:**
```bash
mvn compile -pl sacco-member -q
```

**Done when:** Member REST API compiles with all CRUD endpoints.

### Task 3: Liquibase Changelogs
**Files:** `sacco-member/src/main/resources/db/changelog/member/001-create-members-table.yaml`
**Action:**
1. Create `members` table: id (UUID PK), member_number (VARCHAR UNIQUE), first_name, last_name, email (UNIQUE), phone, national_id (UNIQUE), date_of_birth, join_date, status (VARCHAR), share_balance (NUMERIC 19,2 DEFAULT 0), created_at, updated_at, created_by
2. Indexes on member_number, email, national_id, status
3. Update sacco-app's `db.changelog-master.yaml` to include member changelogs

**Verify:**
```bash
mvn compile -pl sacco-member -q
```

**Done when:** Liquibase changelog is valid and module compiles.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-member -q
```

## Commit Message
```
feat(member): implement member management module

- Member entity with auto-generated member numbers
- CRUD REST API at /api/v1/members with cursor pagination
- Role-based access control on endpoints
- Liquibase changelog for members table
```

---
*Planned: 2026-02-14*
