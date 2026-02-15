# CLAUDE.md

Agent instructions for the InnerCircle SACCO project.

## Project Overview

InnerCircle SACCO is a Kenyan SACCO/Chama management system built as a Java 21 modular monolith on Spring Boot 3.4.2. It handles member management, contributions, loans (with monthly batch interest processing), payouts, double-entry ledger accounting, audit trails, and reporting with PDF/CSV export.

## Quick Reference

```bash
# Build (all modules)
mvn clean install

# Build (skip tests)
mvn clean install -DskipTests

# Test (all modules)
mvn test

# Test (single module)
mvn -pl sacco-loan test

# Run locally
mvn -pl sacco-app spring-boot:run

# Start PostgreSQL
docker compose up -d
```

## Code Organisation

```
innercircle-sacco/
├── sacco-common/          # Shared: BaseEntity, DTOs, events, exceptions, utils
├── sacco-security/        # OAuth2 Authorization Server, JWT, roles, password reset
├── sacco-member/          # Member entity, service, controller
├── sacco-contribution/    # Contributions, categories, bulk recording
├── sacco-loan/            # Loan lifecycle, batch processing, interest accrual, benefits
├── sacco-payout/          # Bank withdrawals, cash disbursements, share withdrawals
├── sacco-ledger/          # Double-entry: chart of accounts, journal entries, statements
├── sacco-reporting/       # Reports, dashboards, analytics, PDF/CSV export
├── sacco-audit/           # Audit events, entity trail tracking
├── sacco-config/          # System config, loan products, contribution schedules, penalties
└── sacco-app/             # Boot app, application.yml, Liquibase master changelog
```

Each module follows the same internal structure:
```
sacco-<module>/src/main/java/com/innercircle/sacco/<module>/
├── controller/    # REST controllers (@RestController)
├── dto/           # Request/response DTOs
├── entity/        # JPA entities and enums
├── repository/    # Spring Data JPA repositories
└── service/       # Service interfaces and implementations
```

## Conventions

### Naming
- Files: `PascalCase.java`
- Classes: `PascalCase`
- Methods/fields: `camelCase`
- Constants/enums: `SCREAMING_SNAKE_CASE`
- Database columns: `snake_case`
- API paths: `/api/v1/<resource>` (kebab-case, plural nouns)

### Code Style
- Lombok for getters/setters/builders (`@Data`, `@Builder`, `@AllArgsConstructor`)
- All entities extend `BaseEntity` (provides `id`, `createdAt`, `updatedAt`, `createdBy`, `version`)
- `@Version` field for optimistic locking on all entities
- Service layer: interface + `*Impl` class
- Constructor injection (no `@Autowired` on fields)

### Git
- Branch naming: `feat/description`, `fix/description`
- Commit format: `type(scope): description`
- PR: Squash and merge

## Architecture Rules

### Do
- Keep module boundaries clean — modules depend on `sacco-common`, not on each other directly
- Use Spring events (`ApplicationEventPublisher`) for cross-module communication
- Write Liquibase migrations for all schema changes (never rely on `ddl-auto`)
- Use DTOs for API request/response — never expose entities directly
- Put new migrations in the owning module's `db/changelog/<module>/` directory
- Register all migrations in `sacco-app/.../db.changelog-master.yaml`
- Use `ConfigService` for runtime-configurable values (not hardcoded constants)
- Use `BigDecimal` for all monetary amounts
- Test services with Mockito, controllers with `@WebMvcTest`

### Don't
- Don't add cross-module JPA relationships (use IDs and service calls instead)
- Don't modify already-applied Liquibase changesets (create new ones)
- Don't hardcode config values — use `SystemConfig` or `application.yml` with env vars
- Don't skip the `version` column on new entities/tables
- Don't use `ddl-auto: update` or `create` — always `validate`
- Don't add `@Autowired` on fields — use constructor injection

## Key Files

| File | Purpose | Notes |
|------|---------|-------|
| `sacco-app/.../application.yml` | All app configuration | Uses env vars with defaults |
| `sacco-app/.../db.changelog-master.yaml` | Liquibase migration index | Add new migrations here |
| `sacco-common/.../entity/BaseEntity.java` | Base class for all entities | Provides id, timestamps, version |
| `sacco-security/.../config/AuthorizationServerConfig.java` | OAuth2 setup | Web + batch client configs |
| `sacco-config/.../service/ConfigService.java` | Runtime config access | Loan products, schedules, penalties |
| `docker-compose.yml` | Local PostgreSQL | Port 5432, db: sacco_db |

## Testing Requirements

- Unit tests required for: all service implementations, controllers, non-trivial entity logic
- Controller tests use `@WebMvcTest` with `@MockBean` for dependencies
- Service tests use `@ExtendWith(MockitoExtension.class)` with `@Mock` / `@InjectMocks`
- H2 in-memory database for repository tests
- 64 test files, 605+ tests across all modules

## Database

- **PostgreSQL 15** with Liquibase YAML migrations
- Hibernate `ddl-auto: validate` — schema must match entities exactly
- 46 changesets across 6 module changelog directories
- All monetary columns: `decimal(19,2)`
- All IDs: `uuid`
- All tables include: `created_at`, `updated_at`, `created_by`, `version`

## Security

- Never commit: secrets, keys, credentials, `.env` files
- All secrets use environment variable placeholders in `application.yml`
- OAuth2 with RSA-signed JWTs for authentication
- Role-based access control (ADMIN, TREASURER, MEMBER)
- Always validate request DTOs with Jakarta validation annotations
- CORS configured via `CORS_ORIGINS` env var
