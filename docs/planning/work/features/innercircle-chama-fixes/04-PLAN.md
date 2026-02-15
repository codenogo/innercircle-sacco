# Plan 04: Authorization & IDOR

## Goal
Fix IDOR vulnerabilities in reporting controllers and add date range validation.

## Prerequisites
- [ ] Plan 03 complete (SecurityConfig must have `.authenticated()` in place)

## Tasks

### Task 1: Create authorization helper for reporting module (B6)
**Files:** `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/security/ReportingAuthHelper.java` (new)
**Action:**
Create a helper class that resolves the current user's memberId from the JWT `email` claim:
1. Inject `JdbcTemplate` (reporting module already uses JDBC).
2. Method `resolveCurrentMemberId(Authentication auth)`:
   - Extract `email` from JWT claims (the `JwtTokenCustomizer` adds it as a claim).
   - Query `SELECT id FROM members WHERE email = ?`.
   - Return the UUID.
3. Method `assertAccessToMember(UUID requestedMemberId, Authentication auth)`:
   - If user has `ROLE_ADMIN` or `ROLE_TREASURER`, allow access to any member.
   - Otherwise, resolve current user's memberId and compare. Throw `AccessDeniedException` if mismatch.

**Verify:**
```bash
mvn compile -pl sacco-reporting -q
```

**Done when:** `ReportingAuthHelper` compiles and provides member-aware authorization.

### Task 2: Add authorization to DashboardController and ReportController (B6, W15)
**Files:** `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/controller/DashboardController.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/controller/ReportController.java`
**Action:**
1. In `DashboardController.memberDashboard()` (line 27): inject `ReportingAuthHelper`, add `Authentication` parameter, call `authHelper.assertAccessToMember(memberId, auth)` before proceeding.
2. In `ReportController.memberStatement()` (line 33): same pattern — inject helper, add `Authentication`, call `assertAccessToMember(memberId, auth)`.
3. In `ReportController.memberStatement()`: add date validation — if `fromDate.isAfter(toDate)`, throw `BusinessException("fromDate must be before toDate")`.
4. In `ReportController.financialSummary()` (line 42): add same date validation.

**Verify:**
```bash
mvn compile -pl sacco-reporting -q
```

**Done when:** MEMBER role can only access own dashboard/statement. TREASURER/ADMIN can access any. Date ranges are validated.

### Task 3: Add authorization to ExportController (B6, W15)
**Files:** `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/controller/ExportController.java`
**Action:**
1. Inject `ReportingAuthHelper`.
2. In `memberStatementPdf()` (line 38) and `memberStatementCsv()` (line 52): add `Authentication` parameter, call `authHelper.assertAccessToMember(memberId, auth)`.
3. Add date validation to both member statement export methods and `financialSummaryCsv()`.

**Verify:**
```bash
mvn compile -pl sacco-reporting -q
```

**Done when:** All export endpoints enforce member-level authorization and date validation.

## Verification

After all tasks:
```bash
mvn compile -q
```

## Commit Message
```
fix(reporting): add IDOR protection and date validation to reporting endpoints

- B6: Extract member ID from JWT, enforce MEMBER can only access own data
- B6: TREASURER/ADMIN can access any member's data
- W15: Validate fromDate < toDate on all date-range endpoints
```

---
*Planned: 2026-02-15*
