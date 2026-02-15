# InnerCircle SACCO

A SACCO/Chama management system built as a modular monolith with Spring Boot. Handles member management, contributions, loans, payouts, double-entry ledger accounting, and reporting.

## Tech Stack

- **Java 21** / **Spring Boot 3.4.2**
- **PostgreSQL 15** with Liquibase migrations
- **Spring Security** with OAuth2 Authorization Server
- **Spring Data JPA** / Hibernate (validate mode)
- **SpringDoc OpenAPI** for API documentation
- **Lombok** for boilerplate reduction
- **JUnit 5** / Mockito / H2 for testing

## Project Structure

```
innercircle-sacco/
├── sacco-common/          # Shared entities, DTOs, events, exceptions
├── sacco-security/        # OAuth2, JWT, user accounts, roles, password reset
├── sacco-member/          # Member registration and lifecycle
├── sacco-contribution/    # Contributions, categories, bulk recording
├── sacco-loan/            # Loan applications, repayments, batch processing, interest accrual
├── sacco-payout/          # Bank withdrawals, cash disbursements, share withdrawals
├── sacco-ledger/          # Double-entry accounting, chart of accounts, journal entries
├── sacco-reporting/       # Reports, dashboards, PDF/CSV export
├── sacco-audit/           # Audit trail and event logging
├── sacco-config/          # System config, loan products, contribution schedules, penalty rules
└── sacco-app/             # Spring Boot application, migrations, application.yml
```

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+

## Getting Started

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts PostgreSQL with database `sacco_db`, user `sacco`, password `sacco` on port `5432`.

### 2. Build

```bash
mvn clean install
```

### 3. Run

```bash
mvn -pl sacco-app spring-boot:run
```

The application starts on `http://localhost:8080`. Liquibase runs all migrations automatically on startup.

### 4. API Docs

Open `http://localhost:8080/swagger-ui.html` for the interactive API documentation.

## API Overview

| Module | Base Path | Description |
|--------|-----------|-------------|
| Members | `/api/v1/members` | Member CRUD, suspend/reactivate |
| Contributions | `/api/v1/contributions` | Record, confirm, reverse contributions |
| Contribution Categories | `/api/v1/contribution-categories` | Category management |
| Loans | `/api/v1/loans` | Applications, approval, disbursement, repayment |
| Loan Batch | `/api/v1/loans/batch/*` | Monthly interest processing, reversals |
| Loan Benefits | `/api/v1/loan-benefits` | Member earnings and beneficiaries |
| Payouts | `/api/v1/payouts` | Payout management |
| Bank Withdrawals | `/api/v1/bank-withdrawals` | Bank withdrawal processing |
| Cash Disbursements | `/api/v1/cash-disbursements` | Cash disbursement processing |
| Share Withdrawals | `/api/v1/share-withdrawals` | Share withdrawal processing |
| Ledger | `/api/v1/ledger` | Chart of accounts, journal entries, financial statements |
| Reports | `/api/v1/reports` | Member statements, financial summaries |
| Export | `/api/v1/export` | PDF and CSV export |
| Dashboard | `/api/v1/dashboard` | Member, treasurer, admin dashboards |
| Config | `/api/v1/config` | System config, loan products, schedules, penalties |
| Audit | `/api/v1/audit` | Audit events, entity trails, CSV export |
| Auth | `/api/auth` | Password reset |
| Users | `/api/v1/users` | User management (admin) |

## Configuration

All configuration uses environment variables with sensible defaults:

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Server port |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `sacco_db` | Database name |
| `DB_USERNAME` | `sacco` | Database user |
| `DB_PASSWORD` | `sacco` | Database password |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | | SMTP username |
| `MAIL_PASSWORD` | | SMTP password |
| `MAIL_FROM` | `noreply@innercircle-sacco.com` | From address |
| `OAUTH2_WEB_SECRET` | `changeme` | OAuth2 web client secret |
| `OAUTH2_BATCH_SECRET` | `changeme` | OAuth2 batch client secret |
| `OAUTH2_RSA_PUBLIC_KEY` | | JWT RSA public key |
| `OAUTH2_RSA_PRIVATE_KEY` | | JWT RSA private key |
| `CORS_ORIGINS` | `http://localhost:3000,http://localhost:8080` | Allowed CORS origins |

## Testing

```bash
# Run all tests
mvn test

# Run tests for a specific module
mvn -pl sacco-loan test

# Run a single test class
mvn -pl sacco-loan test -Dtest=LoanBatchServiceImplTest
```

64 test files with 605+ tests across all modules.

## Database Migrations

Migrations are managed with Liquibase in YAML format. Each module owns its migrations under:

```
sacco-<module>/src/main/resources/db/changelog/<module>/
```

All migrations are included via `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`.

To add a new migration:
1. Create a new YAML file in the appropriate module's changelog directory
2. Add an include entry in `db.changelog-master.yaml`
3. Rebuild with `mvn clean install -DskipTests`

## License

Proprietary. All rights reserved.
