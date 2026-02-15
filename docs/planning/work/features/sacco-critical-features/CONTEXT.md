# SACCO Critical Missing Features - Implementation Context

## Overview

Implement critical features missing from the innercircle-sacco project, identified by comparing with the older production SACCO system. These features are essential for a functional SACCO management system.

## Features

| # | Feature | Priority | Module |
|---|---------|----------|--------|
| 1 | Loan Benefits/Earnings Distribution | HIGH | sacco-loan |
| 2 | Batch Loan Processing | HIGH | sacco-loan |
| 3 | Email Integration | HIGH | sacco-common |
| 4 | Password Reset Flow | HIGH | sacco-security |
| 5 | Loan Transaction Reversals | MEDIUM | sacco-loan |
| 6 | Unpaid Loan Detection | MEDIUM | sacco-loan |
| 7 | User Account Management API | MEDIUM | sacco-security |
| 8 | Enhanced Dashboard Analytics | MEDIUM | sacco-reporting |

## Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Loan Benefits | Proportional distribution based on member share balance | Matches SACCO cooperative model where members earn from loan interest proportionally |
| Benefits Calculation | Calculate on loan repayment events | Real-time calculation when interest is received, not batch |
| Batch Processing | Spring @Scheduled with configurable cron | Simple, reliable, no external job scheduler needed |
| Batch Frequency | Monthly processing (configurable via system_configs) | Standard SACCO monthly cycle |
| Email Provider | Spring Mail with SMTP (externalized config) | Framework-native, easy to swap providers |
| Email Templates | Plain text initially, Thymeleaf templates optional | Simplicity first; can add HTML templates later |
| Password Reset | Time-limited token (24h) stored in DB | Standard secure pattern |
| Reset Token Format | UUID-based token with expiry timestamp | Simple, secure, no JWT complexity for reset tokens |
| Loan Reversals | Full reversal with compensating journal entries | Maintains audit trail; never delete ledger entries |
| Reversal Authorization | ADMIN or TREASURER role required | Sensitive operation needs privileged access |
| Dashboard Analytics | JdbcTemplate with aggregate SQL queries | Consistent with existing FinancialReportServiceImpl pattern |
| Time-Series Data | Monthly aggregation by year | Matches old system's proven approach |

## Constraints

- All modules depend only on `sacco-common` (no cross-module dependencies)
- Inter-module communication via Spring Application Events only
- All money as BigDecimal with NUMERIC(19,2)
- Every financial operation must create balanced journal entries
- Audit trail for all state changes
- API response format: `ApiResponse<T>` wrapper
- Cursor-based pagination for list endpoints
- UUID v7 for all entity IDs

## Related Code

- `sacco-loan/src/main/java/.../service/LoanServiceImpl.java` - Existing loan service with event publishing
- `sacco-loan/src/main/java/.../entity/LoanApplication.java` - Loan entity pattern
- `sacco-contribution/src/main/java/.../service/ContributionServiceImpl.java` - Event publishing pattern
- `sacco-security/src/main/java/.../entity/UserAccount.java` - User entity for password reset
- `sacco-reporting/src/main/java/.../service/FinancialReportServiceImpl.java` - JdbcTemplate reporting pattern
- `sacco-ledger/src/main/java/.../service/LedgerServiceImpl.java` - Journal entry creation pattern
- `sacco-audit/src/main/java/.../service/AuditServiceImpl.java` - Audit event pattern

## Open Questions

None - all decisions made based on analysis of old system and current architecture.

---
*Discussed: 2026-02-15*
