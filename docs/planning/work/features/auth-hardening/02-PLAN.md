# Plan 02: Add @PreAuthorize RBAC to admin-only and internal controllers (config, audit, ledger, export)

## Goal
Add @PreAuthorize RBAC to admin-only and internal controllers (config, audit, ledger, export)

## Tasks

### Task 1: Add spring-security dependency + @PreAuthorize to ConfigController and AuditController
**Files:** `sacco-config/pom.xml`, `sacco-audit/pom.xml`, `sacco-config/src/main/java/com/innercircle/sacco/config/controller/ConfigController.java`, `sacco-audit/src/main/java/com/innercircle/sacco/audit/controller/AuditController.java`
**Action:**
Add <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency> to sacco-config/pom.xml and sacco-audit/pom.xml. Add class-level @PreAuthorize("hasRole('ADMIN')") to ConfigController and AuditController. Import org.springframework.security.access.prepost.PreAuthorize. Existing tests are @ExtendWith(MockitoExtension) so no test changes needed.

**Verify:**
```bash
mvn -pl sacco-config compile -q
mvn -pl sacco-audit compile -q
mvn -pl sacco-config test -q
mvn -pl sacco-audit test -q
```

**Done when:** [Observable outcome]

### Task 2: Add spring-security dependency + @PreAuthorize to LedgerController and fix LedgerControllerTest
**Files:** `sacco-ledger/pom.xml`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/controller/LedgerController.java`, `sacco-ledger/src/test/java/com/innercircle/sacco/ledger/controller/LedgerControllerTest.java`
**Action:**
Add spring-boot-starter-security dependency to sacco-ledger/pom.xml. Add class-level @PreAuthorize("hasAnyRole('ADMIN','TREASURER')") to LedgerController. In LedgerControllerTest (@WebMvcTest): add @AutoConfigureMockMvc(addFilters = false) or @WithMockUser(roles = "ADMIN") to each test class/method so tests pass with security enabled.

**Verify:**
```bash
mvn -pl sacco-ledger test -q
```

**Done when:** [Observable outcome]

### Task 3: Add @PreAuthorize to ExportController
**Files:** `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/controller/ExportController.java`
**Action:**
Add class-level @PreAuthorize("hasAnyRole('ADMIN','TREASURER')") to ExportController. sacco-reporting already has spring-boot-starter-oauth2-resource-server dependency. ExportControllerTest is @ExtendWith(MockitoExtension) so no test changes needed.

**Verify:**
```bash
mvn -pl sacco-reporting test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-config,sacco-audit,sacco-ledger,sacco-reporting test -q
```

## Commit Message
```
feat(auth-hardening): add RBAC to config, audit, ledger, and export controllers
```
