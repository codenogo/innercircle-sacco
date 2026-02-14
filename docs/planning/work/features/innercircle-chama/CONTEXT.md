# InnerCircle Chama — Modular Monolith - Implementation Context

## Summary

A single-chama management platform built as a Java 21 / Spring Boot 3.x modular monolith with Maven multi-module structure. Handles contributions, loans, payouts, accounting, and reporting with proper OAuth2 security and audit trails.

## Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Architecture | Modular monolith (single Spring Boot app, Maven modules) | Right complexity for single-chama. Simple deploy, test, debug. |
| Module dependencies | Strict: modules only depend on `sacco-common` | Maximum isolation. Shared interfaces/DTOs in common module. No cross-module dependencies. |
| Ledger integration | Spring Application Events | Modules publish domain events (e.g., `ContributionReceivedEvent`), ledger listens and auto-creates journal entries. Decoupled write path. |
| API response format | Custom `ApiResponse<T>` wrapper | `{data, message, timestamp, path}` — standardized across all endpoints. Errors use same envelope. |
| Pagination | Cursor-based (keyset) | Better for mobile clients and concurrent writes. No skipped/duplicate rows. |
| OAuth2 flows | Authorization Code (PKCE) + Client Credentials | Auth code for web/mobile users. Client credentials for batch jobs (penalty calculation, scheduled tasks). |
| RBAC model | Role-based, 4 fixed roles | ADMIN, TREASURER, SECRETARY, MEMBER. Each role has fixed permissions. Simple, covers chama structures. |
| Audit trail | Custom audit event table | AuditEvent entity: actor, action, entityType, entityId, before/after JSON snapshots, timestamp. Queryable and exportable. |
| Configuration storage | Database-backed (config tables) | `loan_products`, `penalty_rules`, `contribution_schedules`, `system_config` tables. Changeable at runtime via admin API. |
| Interest calculation | Configurable: reducing balance + flat rate | Per loan product configuration. Reducing balance (interest on outstanding principal) or flat rate (interest on original amount). |
| Loan approval | Single approver (Treasurer/Admin) | Workflow: PENDING → APPROVED/REJECTED → DISBURSED. Authorized role approves. |
| Schema migrations | Liquibase | XML/YAML changelog. Supports rollback and multi-DB if needed later. |
| Java version | Java 21 (LTS) | Virtual threads, pattern matching, record patterns. Latest LTS. |
| Database | PostgreSQL 15+ | ACID compliance for financial data. JSON support for flexible fields. |
| ORM | Spring Data JPA / Hibernate | Standard Spring ecosystem. JPA repositories per module. |
| API documentation | springdoc-openapi (OpenAPI 3.0) | Auto-generated Swagger UI from annotations. |
| Testing | JUnit 5 + Mockito + Testcontainers | Testcontainers for PostgreSQL integration tests. |

## Maven Module Structure

```
innercircle-sacco/
├── pom.xml                    # Parent POM (dependency management, plugin config)
├── sacco-common/              # Shared: ApiResponse<T>, DTOs, exceptions, base entities, event interfaces
├── sacco-security/            # Spring Authorization Server, OAuth2 resource server, role definitions
├── sacco-member/              # Member registration, profiles, role assignment
├── sacco-contribution/        # Contributions, schedules, arrears tracking
├── sacco-loan/                # Loan products, application, approval, disbursement, repayment, penalties, benefits
├── sacco-payout/              # Payouts (merry-go-round, ad-hoc), cash disbursements, bank withdrawals
├── sacco-ledger/              # Double-entry GL, chart of accounts, journal entries, financial statements
├── sacco-reporting/           # Dashboards, reports, PDF/CSV export, member statements
├── sacco-audit/               # Immutable audit event log, queryable, exportable
├── sacco-config/              # System configuration, loan products config, rules engine
└── sacco-app/                 # Main Spring Boot application, wires all modules, Liquibase migrations
```

## Module Dependency Rules

```
sacco-app → all modules (runtime wiring only)
sacco-member → sacco-common
sacco-contribution → sacco-common
sacco-loan → sacco-common
sacco-payout → sacco-common
sacco-ledger → sacco-common (listens to events from common interfaces)
sacco-reporting → sacco-common
sacco-audit → sacco-common (listens to all mutation events)
sacco-config → sacco-common
sacco-security → sacco-common
```

No module depends on another module directly. All inter-module communication happens via:
- **Events** (defined in `sacco-common`) for write operations (ledger entries, audit logs)
- **Shared interfaces** (defined in `sacco-common`) for read/query operations

## Roles & Permissions

| Role | Permissions |
|------|------------|
| ADMIN | Full system access. User management, configuration, all approvals, reporting. |
| TREASURER | Financial operations: approve loans, process payouts, manage contributions, view ledger, generate reports. |
| SECRETARY | Member management, record contributions, create loan applications, view reports. |
| MEMBER | View own dashboard, apply for loans, view own statements, view own contribution history. |

## API Conventions

- Base path: `/api/v1/`
- Response wrapper: `ApiResponse<T>` with `data`, `message`, `timestamp`, `path`
- Error format: Same `ApiResponse<T>` with `error` object containing `code`, `message`, `details`
- Cursor pagination: `?cursor=<encoded>&size=20` → response includes `nextCursor`
- Date format: ISO 8601 (`2026-02-14T10:30:00Z`)
- Money: `BigDecimal` with 2 decimal places, stored as `NUMERIC(19,2)` in PostgreSQL
- IDs: UUID v7 (time-ordered) for all entities

## Event-Driven Patterns

Domain events defined in `sacco-common`:
- `ContributionReceivedEvent` → Ledger creates journal entry (debit: bank/cash, credit: member savings)
- `LoanDisbursedEvent` → Ledger creates journal entry (debit: loans receivable, credit: bank/cash)
- `LoanRepaymentEvent` → Ledger creates journal entry (debit: bank/cash, credit: loans receivable + interest income)
- `PayoutProcessedEvent` → Ledger creates journal entry (debit: member savings, credit: bank/cash)
- `PenaltyAppliedEvent` → Ledger creates journal entry (debit: member account, credit: penalty income)
- All mutation events → Audit module captures actor, action, before/after snapshots

## Constraints

- Single-chama deployment (not multi-tenant)
- Financial accuracy non-negotiable: every ledger entry must balance to zero
- All money operations must be within database transactions
- Audit trail is append-only (no updates/deletes on audit records)
- Configuration changes must be audited

## Out of Scope (Phase 1)

- M-Pesa / Daraja API integration (manual recording for now)
- Native mobile app (API-ready for future)
- Multi-tenancy
- SMS/email notifications
- File attachments (loan documents, receipts)

## Open Questions

- Chart of accounts: use a predefined template for chamas or make fully configurable?
- Report templates: which financial reports are must-have for MVP (trial balance, P&L, balance sheet, member statements)?
- Merry-go-round scheduling: fixed rotation order or configurable rotation rules?

---
*Discussed: 2026-02-14*
