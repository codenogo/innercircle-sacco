# Plan 01: Create the shared MakerCheckerGuard, exception, and OVERRIDE_APPROVE audit action as foundation for all maker-checker enforcement

## Goal
Create the shared MakerCheckerGuard, exception, and OVERRIDE_APPROVE audit action as foundation for all maker-checker enforcement

## Tasks

### Task 1: Create MakerCheckerGuard and MakerCheckerViolationException
**CWD:** `sacco-common`
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/guard/MakerCheckerGuard.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/exception/MakerCheckerViolationException.java`
**Action:**
Create MakerCheckerGuard with two static methods: (1) assertDifferentActor(String maker, String checker, String entityType, UUID entityId) throws MakerCheckerViolationException if maker.equals(checker). (2) assertOrOverride(String maker, String checker, String overrideReason, boolean isAdmin) which allows same-actor only if isAdmin=true and overrideReason is non-blank, otherwise throws. Create MakerCheckerViolationException extending RuntimeException with entityType and entityId fields. Ensure the exception is mapped to HTTP 403 in the global exception handler (check existing GlobalExceptionHandler or equivalent).

**Verify:**
```bash
mvn -pl sacco-common compile -q
```

**Done when:** [Observable outcome]

### Task 2: Add OVERRIDE_APPROVE audit action and update audit listener mapping
**CWD:** `sacco-audit`
**Files:** `sacco-audit/src/main/java/com/innercircle/sacco/audit/entity/AuditAction.java`, `sacco-audit/src/main/java/com/innercircle/sacco/audit/listener/AuditEventListener.java`
**Action:**
Add OVERRIDE_APPROVE to AuditAction enum. Update the event-type-to-AuditAction mapping in AuditEventListener to map 'OVERRIDE_APPROVED' string to OVERRIDE_APPROVE action. This ensures override approvals get their own distinct audit trail entry.

**Verify:**
```bash
mvn -pl sacco-audit compile -q
```

**Done when:** [Observable outcome]

### Task 3: Unit tests for MakerCheckerGuard
**CWD:** `sacco-common`
**Files:** `sacco-common/src/test/java/com/innercircle/sacco/common/guard/MakerCheckerGuardTest.java`
**Action:**
Write tests covering: (1) different actors passes silently, (2) same actor without override throws MakerCheckerViolationException, (3) same actor with ADMIN role and valid reason passes, (4) same actor with ADMIN role but blank reason throws, (5) same actor with non-ADMIN role and reason still throws, (6) null/empty maker or checker edge cases.

**Verify:**
```bash
mvn -pl sacco-common test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-common,sacco-audit test -q
```

## Commit Message
```
feat(maker-checker-controls): add MakerCheckerGuard, exception, and OVERRIDE_APPROVE audit action
```
