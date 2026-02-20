# InnerCircle SACCO

A full-stack SACCO (Savings and Credit Cooperative) / Chama management platform built as a Java modular monolith with a React TypeScript frontend. Designed for Kenyan SACCOs to manage members, contributions, loans with automated interest processing, multi-channel payouts, double-entry ledger accounting, audit trails, and financial reporting with PDF/CSV export.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.4.2, Spring Data JPA, Hibernate |
| Auth | Spring Security OAuth2 Authorization Server 1.4.2, RS256 JWT |
| Database | PostgreSQL 15, Liquibase (YAML migrations) |
| Frontend | React 19, TypeScript 5.7, Vite 6.1, React Router 7 |
| Data Tables | TanStack React Table + React Virtual (virtualized scrolling) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5, Mockito, H2 (backend) / Vitest (frontend) |
| Icons | Lucide React |
| Containerization | Docker Compose |

## Architecture

InnerCircle is a **modular monolith** — 11 Maven modules with clean boundaries, communicating via Spring application events and a transactional outbox pattern. Each module owns its domain, migrations, and tests.

```
innercircle-sacco/
├── sacco-common/          # Base entities, DTOs, domain events, exceptions, utilities
├── sacco-security/        # OAuth2 Authorization Server, JWT, RBAC, password reset
├── sacco-member/          # Member registration, lifecycle (active/suspended)
├── sacco-contribution/    # Contributions, dynamic categories, bulk recording
├── sacco-loan/            # Loan lifecycle, batch interest accrual, benefits distribution
├── sacco-payout/          # Bank withdrawals, cash disbursements, share withdrawals, petty cash
├── sacco-ledger/          # Double-entry accounting, chart of accounts, journal entries
├── sacco-reporting/       # Financial reports, dashboards, PDF/CSV export
├── sacco-audit/           # Immutable audit trail, entity history, CSV export
├── sacco-config/          # Runtime system config, loan products, penalty rules
├── sacco-app/             # Spring Boot application, Liquibase master changelog, outbox processor
└── sacco-ui/              # React 19 + TypeScript frontend (28 pages, 18+ components)
```

Each module follows a consistent internal structure:

```
sacco-<module>/src/main/java/com/innercircle/sacco/<module>/
├── controller/    # REST controllers (@RestController, @PreAuthorize)
├── dto/           # Request/response DTOs (Jakarta validation)
├── entity/        # JPA entities extending BaseEntity
├── repository/    # Spring Data JPA repositories
└── service/       # Interface + Impl pattern, constructor injection
```

## Features

### Member Management
- Registration with Kenyan phone number validation
- Lifecycle management (active, suspended, reactivated)
- Individual member profiles with contribution and loan history

### Contributions
- Dynamic contribution categories (Shares, Welfare, Merry-Go-Round, etc.)
- Single and bulk contribution recording with batch references
- Contribution confirmation, reversal, and month-based tracking

### Loan Processing
- Loan application, approval, disbursement, and repayment workflows
- **Automated monthly batch processing** — scheduled interest accrual with configurable processing day
- Interest methods: reducing balance and flat rate
- Late payment penalties with configurable grace period and thresholds
- Automatic loan status transitions (active, defaulted, closed)
- **Interest distribution** — proportional loan interest earnings distributed to members based on share balance

### Payouts (4 Channels)
- **Bank Withdrawals** — external transfers with reconciliation tracking
- **Cash Disbursements** — physical cash payouts with receipt and dual sign-off
- **Share Withdrawals** — partial or full share balance reduction
- **Petty Cash** — voucher workflow (submitted, approved, disbursed, settled/rejected) with expense categorization

### Double-Entry Ledger
- Full chart of accounts (Asset, Liability, Equity, Revenue, Expense)
- Automated journal entry creation from domain events
- Debit/credit balance enforcement with idempotency constraints
- Financial statements and trial balance
- Pre-seeded accounts: Cash, Loans Receivable, Interest Receivable, Share Capital, and more

### Audit Trail
- Immutable event log capturing all domain actions
- Before/after JSON snapshots for change tracking
- 13 audit actions: CREATE, UPDATE, DELETE, APPROVE, REJECT, DISBURSE, SUSPEND, REACTIVATE, LOGIN, LOGOUT, CONFIG_CHANGE, OVERRIDE_APPROVE
- Searchable by entity, actor, action, and date range
- CSV export for compliance

### Reporting & Export
- Financial summary, contribution reports, loan portfolio analysis
- Member statements with opening/closing balances
- PDF and CSV export
- Role-based dashboard analytics (Admin, Treasurer, Member)

### Security
- OAuth2 Authorization Server with RS256-signed JWTs
- Refresh token rotation
- Three roles: **ADMIN**, **TREASURER**, **MEMBER**
- Method-level authorization via `@PreAuthorize`
- Maker-checker pattern for sensitive financial operations
- Password reset via email

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 18+ and npm
- Docker (for PostgreSQL)

## Getting Started

### 1. Start PostgreSQL

```bash
docker compose up -d
```

Starts PostgreSQL 15 on port `5432` with database `sacco_db`.

### 2. Build and Run Backend

```bash
mvn clean install
mvn -pl sacco-app spring-boot:run
```

The backend starts on `http://localhost:8080`. Liquibase applies all 92 migration changesets automatically.

### 3. Start Frontend

```bash
cd sacco-ui
npm install
npm run dev
```

The frontend starts on `http://localhost:3000` with API requests proxied to the backend.

### 4. API Documentation

Open `http://localhost:8080/swagger-ui.html` for interactive API docs.

## API Overview

| Module | Base Path | Description |
|--------|-----------|-------------|
| Members | `/api/v1/members` | Member CRUD, suspend/reactivate |
| Contributions | `/api/v1/contributions` | Record, confirm, reverse contributions |
| Contribution Categories | `/api/v1/contribution-categories` | Dynamic category management |
| Loans | `/api/v1/loans` | Applications, approval, disbursement, repayment |
| Loan Batch | `/api/v1/loans/batch/*` | Monthly interest processing, reversals |
| Loan Benefits | `/api/v1/loan-benefits` | Interest distribution to members |
| Payouts | `/api/v1/payouts` | Payout management |
| Bank Withdrawals | `/api/v1/bank-withdrawals` | Bank transfer processing |
| Cash Disbursements | `/api/v1/cash-disbursements` | Cash disbursement workflow |
| Share Withdrawals | `/api/v1/share-withdrawals` | Share balance withdrawals |
| Ledger | `/api/v1/ledger` | Chart of accounts, journal entries, statements |
| Reports | `/api/v1/reports` | Financial summaries, member statements |
| Export | `/api/v1/export` | PDF and CSV export |
| Dashboard | `/api/v1/dashboard` | Role-based analytics dashboards |
| Config | `/api/v1/config` | System config, loan products, penalty rules |
| Audit | `/api/v1/audit` | Audit events, entity trails, CSV export |
| Auth | `/api/auth` | Authentication, password reset |
| Users | `/api/v1/users` | User management (admin) |

All endpoints return a standard response envelope:

```json
{
  "data": { },
  "message": "Success",
  "status": 200
}
```

Paginated endpoints use cursor-based pagination:

```json
{
  "items": [],
  "nextCursor": "...",
  "hasMore": true
}
```

## Testing

```bash
# Backend — all modules (83 test files, 605+ tests)
mvn test

# Backend — single module
mvn -pl sacco-loan test

# Backend — single test class
mvn -pl sacco-loan test -Dtest=LoanBatchServiceImplTest

# Frontend — unit tests
cd sacco-ui && npm run test

# Frontend — lint
cd sacco-ui && npm run lint
```

## Database Migrations

92 Liquibase changesets in YAML format, organized by module:

```
sacco-<module>/src/main/resources/db/changelog/<module>/
```

All registered in `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml`.

**Conventions:**
- UUID primary keys
- `decimal(19,2)` for all monetary columns
- All tables include `created_at`, `updated_at`, `created_by`, `version` (optimistic locking)
- Hibernate runs in `validate` mode — schema must match entities exactly

To add a new migration:
1. Create a YAML changeset in the owning module's changelog directory
2. Add an include entry in `db.changelog-master.yaml`
3. Rebuild: `mvn clean install -DskipTests`

## Frontend Pages

| Page | Description |
|------|-------------|
| Dashboard | Treasurer metrics, SACCO financial state, key statistics |
| Members | Member list with search, filtering, and profile links |
| Contributions | Contribution history, recording, confirmation |
| Loans | Loan list, applications, approval/disbursement actions |
| Loan Workflow | Step-by-step loan application and schedule views |
| Loan Batch | Monthly batch processing interface |
| Loan Benefits | Interest distribution tracking per member |
| Payouts | Payout management across all channels |
| Petty Cash | Voucher workflow with approval chain |
| Ledger | Journal entries with expandable line items, virtual scrolling |
| Ledger Statements | Trial balance and financial statements |
| Reports | Financial summaries with PDF/CSV export |
| Audit Trail | Searchable event history with entity snapshots |
| Settings | System configuration management |
| Users Admin | User account and role management |

## Project Conventions

- **Module boundaries** — modules depend on `sacco-common`, not on each other. Cross-module communication uses Spring events.
- **Outbox pattern** — domain events are persisted to an outbox table and processed asynchronously with dead-letter retry.
- **DTOs everywhere** — entities are never exposed via API. All requests validated with Jakarta annotations.
- **BigDecimal** — all monetary amounts use `BigDecimal` with `decimal(19,2)` storage.
- **Constructor injection** — no `@Autowired` on fields.
- **Optimistic locking** — `@Version` column on every entity via `BaseEntity`.
- **Commit format** — `type(scope): description` (e.g., `feat(loan): add batch interest accrual`).

## License

Proprietary. All rights reserved.
