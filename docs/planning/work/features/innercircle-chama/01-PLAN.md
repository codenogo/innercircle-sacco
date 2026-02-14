# Plan 01: Maven Scaffold + Common Foundation

## Goal
Create the multi-module Maven project scaffold with parent POM, sacco-common (shared types, events, ApiResponse), and sacco-app (Spring Boot main class, Liquibase, Docker Compose).

## Prerequisites
- [ ] None (first plan)

## Tasks

### Task 1: Parent POM + Module POM Stubs
**Files:** `pom.xml`, `sacco-common/pom.xml`, `sacco-security/pom.xml`, `sacco-member/pom.xml`, `sacco-contribution/pom.xml`, `sacco-loan/pom.xml`, `sacco-payout/pom.xml`, `sacco-ledger/pom.xml`, `sacco-reporting/pom.xml`, `sacco-audit/pom.xml`, `sacco-config/pom.xml`, `sacco-app/pom.xml`
**Action:**
1. Create parent POM with `spring-boot-starter-parent` 3.4.x, Java 21, module declarations for all 11 modules
2. Define dependency management: Spring Boot starters, PostgreSQL driver, Liquibase, springdoc-openapi, Testcontainers, Lombok
3. Create each module's POM with dependency on `sacco-common` (except sacco-common itself)
4. sacco-app POM depends on ALL other modules (runtime wiring)
5. Use groupId `com.innercircle` and artifactId prefix `sacco-`

**Verify:**
```bash
mvn validate -q
```

**Done when:** `mvn validate` passes with all 11 modules recognized.

### Task 2: sacco-common Module
**Files:** `sacco-common/src/main/java/com/innercircle/sacco/common/model/BaseEntity.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/dto/ApiResponse.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/dto/PagedResponse.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/dto/CursorPage.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/exception/BusinessException.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/exception/ResourceNotFoundException.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/exception/GlobalExceptionHandler.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/ContributionReceivedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanDisbursedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/LoanRepaymentEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/PayoutProcessedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/PenaltyAppliedEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/event/AuditableEvent.java`, `sacco-common/src/main/java/com/innercircle/sacco/common/util/UuidGenerator.java`
**Action:**
1. `BaseEntity`: abstract class with UUID v7 `id`, `createdAt`, `updatedAt`, `createdBy` fields. Use `@MappedSuperclass`, `@PrePersist`/`@PreUpdate` callbacks.
2. `ApiResponse<T>`: generic wrapper with `boolean success`, `T data`, `String message`, `LocalDateTime timestamp`, `String path`. Static factory methods `ok(data)` and `error(message)`.
3. `CursorPage<T>`: cursor pagination wrapper with `List<T> items`, `String nextCursor`, `boolean hasMore`, `int size`.
4. Domain events: Java records extending `AuditableEvent` marker interface. Each event carries relevant IDs and amounts as `BigDecimal`.
5. Exceptions: `BusinessException` (base), `ResourceNotFoundException`. `GlobalExceptionHandler` returns `ApiResponse` for all errors.
6. `UuidGenerator`: utility to generate UUID v7 (time-ordered).

**Verify:**
```bash
mvn compile -pl sacco-common -q
```

**Done when:** sacco-common compiles cleanly with all shared types.

### Task 3: sacco-app + Infrastructure
**Files:** `sacco-app/src/main/java/com/innercircle/sacco/SaccoApplication.java`, `sacco-app/src/main/resources/application.yml`, `sacco-app/src/main/resources/application-dev.yml`, `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`, `docker-compose.yml`, `.env.example`
**Action:**
1. `SaccoApplication.java`: `@SpringBootApplication(scanBasePackages = "com.innercircle.sacco")` main class.
2. `application.yml`: PostgreSQL datasource config (using env vars), JPA/Hibernate settings (ddl-auto=validate), Liquibase enabled, server port 8080, springdoc config.
3. `application-dev.yml`: Dev profile with relaxed settings, H2 fallback or local PostgreSQL.
4. `db.changelog-master.yaml`: Liquibase master changelog (empty includes list, will be populated by module plans).
5. `docker-compose.yml`: PostgreSQL 15 service with volume, exposed on port 5432.
6. `.env.example`: Template for DB credentials (never commit actual .env).

**Verify:**
```bash
mvn compile -q
```

**Done when:** Full project compiles. `mvn compile` succeeds across all modules.

## Verification

After all tasks:
```bash
mvn compile -q && echo "BUILD SUCCESS"
```

## Commit Message
```
feat(innercircle-chama): scaffold Maven multi-module project

- Parent POM with 11 modules, Java 21, Spring Boot 3.4.x
- sacco-common: BaseEntity, ApiResponse<T>, CursorPage, domain events, exceptions
- sacco-app: Spring Boot main class, application.yml, Liquibase, Docker Compose
```

---
*Planned: 2026-02-14*
