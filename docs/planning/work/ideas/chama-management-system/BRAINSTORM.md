# Brainstorm: Kenyan Chama Management System

**Date:** 2026-02-14

## Problem / Opportunity

Kenyan chamas (informal savings groups) manage contributions, loans, payouts, and accounting using spreadsheets, WhatsApp, and paper ledgers. This leads to disputes, lack of transparency, and poor record-keeping. InnerCircle SACCO aims to provide a clean, configurable digital platform for a single chama to manage its full financial lifecycle with proper audit trails and role-based access.

## Constraints

- Single-chama deployment (not multi-tenant SaaS)
- Java 17+ / Spring Boot / Maven multi-module
- PostgreSQL database
- Spring Authorization Server for OAuth2/OIDC
- REST APIs designed to serve both web dashboard and future mobile apps
- Financial accuracy is non-negotiable (double-entry ledger, audit trail on all mutations)
- Configurable (contribution schedules, loan terms, penalty rates, payout rules)

## Feature Requirements

| # | Feature | Description |
|---|---------|-------------|
| 1 | **Contribution Management** | Member contributions (recurring/one-off), schedules, arrears tracking, penalties for late payment |
| 2 | **Withdrawal Management** | Share withdrawals with approval workflow, balance validation, partial/full withdrawal |
| 3 | **Loan Management** | Loan products, application, guarantors, approval workflow, disbursement, repayment schedules (reducing/flat balance), early repayment |
| 4 | **Loan Penalties** | Configurable penalty rules (late repayment, default), auto-calculation, waiver workflow |
| 5 | **Loan Benefits / Member Earnings** | Interest earned on savings, loan interest distribution, dividend calculation, member earnings statements |
| 6 | **Payout Management** | Scheduled payouts (merry-go-round), ad-hoc payouts, approval workflow, payout history |
| 7 | **Ledger / Accounting** | Double-entry general ledger, chart of accounts, journal entries, trial balance, P&L, balance sheet |
| 8 | **Bank Withdrawals** | Track bank-to-member withdrawals, bank reconciliation, transaction references |
| 9 | **Cash Disbursements** | Cash payout tracking, receipt generation, signoff by treasurer |
| 10 | **Dashboard & Reporting** | Member dashboard, treasurer/admin dashboard, financial reports, member statements, export (PDF/CSV) |
| 11 | **Audit Trail** | Immutable log of all data mutations, who/what/when, queryable, exportable |
| 12 | **Configuration** | Contribution amounts/schedules, loan products/terms, penalty rates, payout rules, approval thresholds, roles/permissions |
| 13 | **OAuth2 / Security** | Spring Authorization Server, JWT tokens, role-based access (Admin, Treasurer, Member), refresh tokens |

## Candidates

### Option A: Modular Monolith (Recommended)

**Summary:** Single Spring Boot application with well-separated Maven modules per domain. Shared PostgreSQL database. Clean module boundaries enforced by Maven dependency rules. Simple to deploy, test, and reason about.

**Architecture:**

```
innercircle-sacco/
├── pom.xml (parent POM)
├── sacco-common/              # Shared: DTOs, exceptions, utils, audit
├── sacco-security/            # OAuth2 server + resource server config
├── sacco-member/              # Member registration, profiles, roles
├── sacco-contribution/        # Contributions, schedules, arrears
├── sacco-loan/                # Loans, repayments, penalties, benefits
├── sacco-payout/              # Payouts, merry-go-round, cash disbursements
├── sacco-ledger/              # Double-entry GL, journal, chart of accounts
├── sacco-reporting/           # Dashboards, reports, PDF/CSV export
├── sacco-audit/               # Audit trail (immutable event log)
├── sacco-config/              # System configuration, loan products, rules
└── sacco-app/                 # Main Spring Boot app, wires everything together
```

**In scope:**
- All 13 features above
- REST APIs (JSON) for all operations
- Server-side rendered admin dashboard or SPA-ready API
- Flyway/Liquibase for schema migrations
- Comprehensive audit trail via JPA entity listeners or Spring AOP
- Role-based access: ADMIN, TREASURER, SECRETARY, MEMBER
- Configurable business rules (contribution amounts, loan terms, penalty rates)

**Out of scope (Phase 1):**
- M-Pesa integration (manual recording first)
- Mobile app (API-ready but no native app)
- Multi-tenancy
- SMS/email notifications

**Risks:**
| Risk | Mitigation |
|------|------------|
| Module coupling creeps in over time | Enforce Maven dependency rules; modules only depend on `sacco-common` and their own domain |
| Ledger accuracy | Double-entry with balance assertions; every transaction must balance to zero |
| Auth complexity | Spring Authorization Server is newer; stick to standard OAuth2 flows |

**MVP slice (build first):**
1. `sacco-common` + `sacco-security` + `sacco-member` (auth + members)
2. `sacco-contribution` + `sacco-ledger` + `sacco-audit` (money in + accounting)
3. `sacco-loan` (money out + repayments)
4. `sacco-payout` + `sacco-reporting` + `sacco-config` (operations + visibility)

---

### Option B: Microservices

**Summary:** Each domain as an independent Spring Boot service. API Gateway (Spring Cloud Gateway). Service-to-service communication via REST or messaging. Independent databases per service.

**Architecture:**

```
Each module becomes a standalone service:
  member-service, contribution-service, loan-service,
  ledger-service, payout-service, reporting-service

+ API Gateway (Spring Cloud Gateway)
+ Service Discovery (Eureka or Consul)
+ Config Server (Spring Cloud Config)
+ Message Broker (RabbitMQ/Kafka) for inter-service events
```

**In scope:** Same features as Option A

**Out of scope:** Same as Option A

**Risks:**
| Risk | Mitigation |
|------|------------|
| Massive operational complexity for a single-chama app | Accept it or don't choose this option |
| Distributed transactions across ledger + loans | Saga pattern, but adds significant complexity |
| Deployment overhead (10+ services) | Docker Compose / K8s, but overkill for target |

**MVP slice:** Same order but each step is a standalone deployable service.

**Verdict:** Over-engineered for a single-chama instance. The operational cost (service discovery, distributed tracing, saga patterns) far outweighs the benefits at this scale.

---

### Option C: Modular Monolith with Event-Driven Internals

**Summary:** Like Option A, but modules communicate via Spring Application Events internally. This creates a clear pathway to extract into microservices later if needed, while keeping deployment simple now.

**Architecture:** Same Maven structure as Option A, but:
- Modules publish domain events (e.g., `LoanDisbursedEvent`, `ContributionReceivedEvent`)
- Ledger module listens to financial events and auto-creates journal entries
- Audit module listens to all mutation events
- Async processing via `@Async` + `@TransactionalEventListener`

**In scope:** Same as Option A + event-driven module communication

**Out of scope:** Same as Option A

**Risks:**
| Risk | Mitigation |
|------|------------|
| Event ordering and consistency | Use `@TransactionalEventListener` to tie events to transaction commit |
| Debugging event chains | Structured logging with correlation IDs |
| Slightly more abstract than direct service calls | Keep events simple; don't over-abstract early |

**MVP slice:** Same as Option A, with events added incrementally as modules interact.

---

## Recommendation

**Primary: Option A — Modular Monolith**

For a single-chama deployment, this is the right level of complexity. Clean Maven module boundaries give you separation without distributed systems overhead. You can always evolve toward Option C's event-driven patterns later as the codebase grows.

**Backup: Option C — Modular Monolith with Events**

If you anticipate multiple chamas or a SaaS pivot in the future, start with Option C. The event-driven internals make extraction to microservices straightforward later.

## Tech Stack Summary

| Layer | Technology |
|-------|------------|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| Build | Maven (multi-module) |
| Auth | Spring Authorization Server + Spring Security OAuth2 Resource Server |
| Database | PostgreSQL 15+ |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| API | REST (JSON), OpenAPI 3.0 (springdoc) |
| Audit | JPA EntityListeners + Hibernate Envers or custom |
| Reporting | JasperReports or Apache POI (PDF/CSV) |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| API Docs | springdoc-openapi |

## Next Step

Run:
`/discuss "InnerCircle Chama — Modular Monolith"`
