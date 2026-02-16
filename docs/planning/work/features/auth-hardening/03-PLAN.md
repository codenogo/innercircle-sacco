# Plan 03: Add @PreAuthorize RBAC to member-facing controllers (member, contribution, payout) and fix affected tests

## Goal
Add @PreAuthorize RBAC to member-facing controllers (member, contribution, payout) and fix affected tests

## Tasks

### Task 1: Add spring-security dependency + @PreAuthorize to MemberController, ContributionController, ContributionCategoryController
**Files:** `sacco-member/pom.xml`, `sacco-contribution/pom.xml`, `sacco-member/src/main/java/com/innercircle/sacco/member/controller/MemberController.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/controller/ContributionController.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/controller/ContributionCategoryController.java`
**Action:**
Add spring-boot-starter-security dependency to sacco-member/pom.xml and sacco-contribution/pom.xml. MemberController: class-level @PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')") for reads (GET list, GET by id), method-level @PreAuthorize("hasRole('ADMIN')") on POST (create), PUT (update), PATCH suspend, PATCH reactivate. ContributionController: method-level @PreAuthorize("hasAnyRole('ADMIN','TREASURER')") on POST record and POST bulk-record; @PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')") on GET endpoints. ContributionCategoryController: class-level @PreAuthorize("hasRole('ADMIN')"). Tests are @ExtendWith(MockitoExtension) so no test changes.

**Verify:**
```bash
mvn -pl sacco-member test -q
mvn -pl sacco-contribution test -q
```

**Done when:** [Observable outcome]

### Task 2: Add spring-security dependency + @PreAuthorize to payout controllers
**Files:** `sacco-payout/pom.xml`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/PayoutController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/CashDisbursementController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/BankWithdrawalController.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/controller/ShareWithdrawalController.java`
**Action:**
Add spring-boot-starter-security dependency to sacco-payout/pom.xml. PayoutController: method-level @PreAuthorize("hasAnyRole('ADMIN','TREASURER')") on POST create, POST approve, POST process; @PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')") on GET endpoints. CashDisbursementController: @PreAuthorize("hasAnyRole('ADMIN','TREASURER')") on POST record and PUT signoff; @PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')") on GET endpoints. BankWithdrawalController and ShareWithdrawalController: same pattern (writes=ADMIN,TREASURER; reads=all roles).

**Verify:**
```bash
mvn -pl sacco-payout compile -q
```

**Done when:** [Observable outcome]

### Task 3: Fix payout @WebMvcTest controller tests for security
**Files:** `sacco-payout/src/test/java/com/innercircle/sacco/payout/controller/PayoutControllerTest.java`, `sacco-payout/src/test/java/com/innercircle/sacco/payout/controller/CashDisbursementControllerTest.java`, `sacco-payout/src/test/java/com/innercircle/sacco/payout/controller/BankWithdrawalControllerTest.java`, `sacco-payout/src/test/java/com/innercircle/sacco/payout/controller/ShareWithdrawalControllerTest.java`
**Action:**
All 4 tests use @WebMvcTest which auto-configures Spring Security. Add @AutoConfigureMockMvc(addFilters = false) to each test class to disable security filters in controller unit tests (security is tested separately via integration tests). Alternatively, add class-level @WithMockUser(roles = "ADMIN") if we want security annotations validated in unit tests. Preference: @AutoConfigureMockMvc(addFilters = false) for isolation.

**Verify:**
```bash
mvn -pl sacco-payout test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-member,sacco-contribution,sacco-payout test -q
```

## Commit Message
```
feat(auth-hardening): add RBAC to member, contribution, and payout controllers
```
