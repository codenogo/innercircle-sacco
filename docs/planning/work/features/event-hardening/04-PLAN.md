# Plan 04: Create TransitionGuard utility and register all 4 status enum state machines

## Goal
Create TransitionGuard utility and register all 4 status enum state machines

## Tasks

### Task 1: Create TransitionGuard utility and InvalidStateTransitionException
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/guard/TransitionGuard.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/exception/InvalidStateTransitionException.java`
**Action:**
Create InvalidStateTransitionException extending BusinessException with fields: currentState, targetState, entityType. Create TransitionGuard<S extends Enum<S>> class with: (1) a Builder pattern to register allowed transitions: allow(S from, S to), (2) validate(S current, S target) that throws InvalidStateTransitionException if transition is not registered, (3) isAllowed(S current, S target) boolean check, (4) getAllowedTargets(S current) returns Set<S>. The class should be immutable after build.

**Verify:**
```bash
mvn -pl sacco-common compile -q
```

**Done when:** [Observable outcome]

### Task 2: Register TransitionGuard instances for all 4 status enums
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/guard/TransitionGuards.java`
**Action:**
Create TransitionGuards final class with static TransitionGuard instances for: (1) LOAN: PENDING->APPROVED, PENDING->REJECTED, APPROVED->DISBURSED, DISBURSED->REPAYING, REPAYING->CLOSED, REPAYING->DEFAULTED. (2) PAYOUT: PENDING->APPROVED, APPROVED->PROCESSED, APPROVED->FAILED, PENDING->FAILED. (3) CONTRIBUTION: PENDING->CONFIRMED, CONFIRMED->REVERSED. (4) MEMBER: ACTIVE->SUSPENDED, SUSPENDED->ACTIVE, ACTIVE->DEACTIVATED, SUSPENDED->DEACTIVATED. Each guard is a public static final field. Import the status enums from their respective modules — if cross-module dependency is an issue, use String-based keys instead and adapt the guard to work with enum names.

**Verify:**
```bash
mvn -pl sacco-common compile -q
```

**Done when:** [Observable outcome]

### Task 3: Add comprehensive unit tests for TransitionGuard and all 4 registrations
**Files:** `sacco-common/src/test/java/com/innercircle/sacco/common/guard/TransitionGuardTest.java`, `sacco-common/src/test/java/com/innercircle/sacco/common/guard/TransitionGuardsTest.java`
**Action:**
TransitionGuardTest: (1) valid transition passes, (2) invalid transition throws InvalidStateTransitionException, (3) getAllowedTargets returns correct set, (4) isAllowed returns correct boolean. TransitionGuardsTest: test each of the 4 registered guards with (a) every valid transition succeeds, (b) at least one invalid transition per guard throws. Note: since sacco-common cannot import module-specific enums, if TransitionGuards uses String-based approach, test with String state names.

**Verify:**
```bash
mvn -pl sacco-common test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-common compile -q
mvn -pl sacco-common test -q
```

## Commit Message
```
feat(event-hardening): add TransitionGuard utility with registrations for all 4 status enums
```
