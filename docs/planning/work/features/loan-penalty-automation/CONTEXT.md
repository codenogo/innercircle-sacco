# Loan Penalty Automation - Implementation Context

## Problem Statement

Three gaps exist in the loan processing pipeline:

1. **Batch processing detects but never applies penalties** - `LoanBatchServiceImpl` increments `penalizedCount` and logs warnings, but never calls `LoanPenaltyService.applyPenalty()` or reads `PenaltyRule` config.
2. **Repayment allocation ignores penalties** - `LoanServiceImpl.recordRepayment()` allocates interest -> principal only. `LoanPenalty` has no `paid` field. `LoanApplication` has no penalty balance tracking.
3. **Penalty config disconnected** - `PenaltyRule` entity with `LOAN_DEFAULT` type and `FLAT`/`PERCENTAGE` calculation methods exists but is never used by the loan module. Grace period (30d) and default threshold (90d) are hardcoded.

## Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Repayment allocation | Interest -> Penalties -> Principal | Standard SACCO policy: accrued interest settled first, then outstanding penalties, then principal reduction |
| Auto-penalty | Batch job auto-applies via PenaltyRule | Reads active `LOAN_DEFAULT` PenaltyRule, calculates amount (FLAT or PERCENTAGE of overdue installment), calls `applyPenalty()` |
| Compounding | Configurable per PenaltyRule | Add `compounding` boolean to `PenaltyRule`. If true, penalty re-applies each month installment remains unpaid. If false, one-time per installment |
| Grace period | Move to SystemConfig | `loan.penalty.grace_period_days` (default 30). Admins can adjust without code changes |
| Default threshold | Move to SystemConfig | `loan.penalty.default_threshold_days` (default 90). Days overdue before marking DEFAULTED |
| LoanPenalty tracking | Add `paid`/`paidAt` fields | Required for repayment allocation to mark penalties as settled |
| LoanApplication tracking | Add `totalPenalties` field | Tracks total outstanding penalty balance, analogous to `totalInterestAccrued`/`totalInterestPaid` |
| Idempotency | Track penalized installments | Prevent double-penalizing same installment in same batch run. `LoanPenalty` needs `installmentNumber` or `scheduleId` reference |

## Constraints

- Must not break existing repayment flow or GL accounting
- PenaltyRule lives in `sacco-config` module; `LoanBatchServiceImpl` in `sacco-loan` - use `ConfigService` to bridge
- Penalty GL entries already handled by `FinancialEventListener` via `PenaltyAppliedEvent`
- Liquibase migrations needed for schema changes (new columns on `loan_penalties`, `loan_applications`, `penalty_rules`)
- Interest-first allocation (Plan 03) must remain intact; penalties slot in after interest

## Related Code

- `sacco-loan/.../service/LoanBatchServiceImpl.java` - batch processing (needs penalty wiring)
- `sacco-loan/.../service/LoanServiceImpl.java` - repayment allocation (needs penalty step)
- `sacco-loan/.../service/LoanPenaltyServiceImpl.java` - penalty application (needs `paid` support)
- `sacco-loan/.../entity/LoanPenalty.java` - entity (needs `paid`, `paidAt`, `scheduleId` fields)
- `sacco-loan/.../entity/LoanApplication.java` - entity (needs `totalPenalties` field)
- `sacco-config/.../entity/PenaltyRule.java` - config entity (needs `compounding` field)
- `sacco-config/.../repository/PenaltyRuleRepository.java` - has `findByPenaltyTypeAndActiveTrue()`
- `sacco-config/.../service/ConfigServiceImpl.java` - penalty rule lookup
- `sacco-ledger/.../listener/FinancialEventListener.java` - GL entries for penalty events

## Open Questions

- None - all gray areas resolved

---
*Discussed: 2026-02-15*
