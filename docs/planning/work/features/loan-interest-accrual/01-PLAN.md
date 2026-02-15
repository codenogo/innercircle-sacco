# Plan 01: Config-Driven Loan Application & Entity Migration

## Goal
Wire loan application to LoanProductConfig, migrate interestMethod from String to InterestMethod enum, and add interest tracking fields to LoanApplication entity.

## Prerequisites
- [ ] CONTEXT.md decisions finalized

## Tasks

### Task 1: Add sacco-config dependency and update LoanApplication entity
**Files:** `sacco-loan/pom.xml`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanApplication.java`
**Action:**
1. Add `sacco-config` dependency to `sacco-loan/pom.xml`
2. On `LoanApplication` entity:
   - Add `loanProductId` (UUID, nullable for backward compat with existing data)
   - Change `interestMethod` from `String` to `InterestMethod` enum (import from `sacco-config`)
   - Add `totalInterestAccrued` (BigDecimal, default ZERO, precision 19 scale 2)
   - Add `totalInterestPaid` (BigDecimal, default ZERO, precision 19 scale 2)

**Verify:**
```bash
cd sacco-loan && mvn compile -q
```

**Done when:** LoanApplication compiles with new fields and InterestMethod enum type.

### Task 2: Refactor LoanService and LoanServiceImpl for config-driven application
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanService.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/LoanApplicationRequest.java`
**Action:**
1. Replace `LoanApplicationRequest` fields: remove `interestRate`, `interestMethod`; add `loanProductId` (UUID, @NotNull)
2. Change `LoanService.applyForLoan` signature to: `applyForLoan(UUID memberId, UUID loanProductId, BigDecimal principalAmount, Integer termMonths, String purpose)`
3. In `LoanServiceImpl`:
   - Inject `ConfigService` from sacco-config (or `LoanProductConfigRepository`)
   - Lookup `LoanProductConfig` by `loanProductId`, validate it's active
   - Enforce `maxAmount` and `maxTermMonths` limits from config
   - Set `interestRate` and `interestMethod` from config onto the loan entity
   - Set `loanProductId` on the loan entity
4. In `disburseLoan()`: replace String comparison `"REDUCING_BALANCE".equals(...)` with `InterestMethod.REDUCING_BALANCE == ...`
5. Update `RepaymentScheduleGenerator` to accept `InterestMethod` enum instead of String

**Verify:**
```bash
cd sacco-loan && mvn compile -q
```

**Done when:** LoanServiceImpl looks up config, enforces limits, and uses InterestMethod enum throughout.

### Task 3: Update controller, DTOs, and fix all tests
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/controller/LoanController.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/LoanResponse.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/RepaymentScheduleGenerator.java`, `sacco-loan/src/test/java/com/innercircle/sacco/loan/**/*Test.java`
**Action:**
1. Update `LoanController.applyForLoan` to pass `loanProductId` to service
2. Update `LoanResponse` to include `loanProductId`, `totalInterestAccrued`, `totalInterestPaid`; change `interestMethod` from String to InterestMethod
3. Fix all existing tests in sacco-loan to use new API signatures:
   - Tests that call `applyForLoan(...)` need updated parameters
   - Tests need to mock `ConfigService`/`LoanProductConfigRepository`
   - Tests comparing interestMethod as String need to use enum

**Verify:**
```bash
mvn test -pl sacco-loan -q
```

**Done when:** All sacco-loan tests pass with the config-driven loan application flow.

## Verification

After all tasks:
```bash
mvn test -pl sacco-loan -q
mvn test -pl sacco-config -q
```

## Commit Message
```
feat(loan): wire loan application to LoanProductConfig

- Add sacco-config dependency to sacco-loan module
- Migrate interestMethod from String to InterestMethod enum
- Replace interestRate/interestMethod on LoanApplicationRequest with loanProductId
- Add totalInterestAccrued and totalInterestPaid fields to LoanApplication
- Enforce config limits (maxAmount, maxTermMonths) on application
- Update all tests for new config-driven flow
```

---
*Planned: 2026-02-15*
