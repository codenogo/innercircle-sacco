# Loan Interest Accrual - Implementation Context

## Background

Gap analysis of requirements 6.2-6.5 revealed four missing capabilities in the loan module:
- 6.2: Interest rate not sourced from loan product configuration
- 6.3: Interest not accrued monthly on outstanding balance
- 6.4: Cumulative interest not tracked on the loan entity
- 6.5: No dedicated interest accrual history (only repayment-level tracking exists)

### Institutional Perspective

The SACCO/chama issues ~50 loans per month and needs to track interest as institutional income — not just per-loan math. This requires:
- **Accrual-basis accounting**: Interest recognized when earned (monthly), not when cash received
- **Portfolio-level tracking**: Total interest income across all active loans per month
- **Per-member tracking**: Individual interest accrued, paid, and in arrears
- **GL integration**: Monthly accrual journal entries (DR Interest Receivable, CR Interest Income)
- **Repayment allocation**: Interest-first allocation per SACCO standard practice

### Current Gaps

| Area | Current State | Required State |
|------|--------------|----------------|
| Rate source | Raw rate passed in API | Sourced from LoanProductConfig |
| Interest recognition | Cash basis (on repayment only) | Accrual basis (monthly) |
| Interest tracking | Only on repayment records | Dedicated LoanInterestHistory entity |
| Cumulative fields | None on LoanApplication | totalInterestAccrued + totalInterestPaid |
| GL integration | Interest credited on repayment to 4001 | Monthly accrual: DR Interest Receivable, CR Interest Income |
| Repayment allocation | Proportional to schedule | Interest-first per SACCO practice |
| Portfolio reporting | None | Monthly interest income, PAR, arrears |

## Decisions

| # | Area | Decision | Rationale |
|---|------|----------|-----------|
| 1 | Rate source (6.2) | Strict from config — `applyForLoan` takes `loanProductId`, rate/method/limits enforced from `LoanProductConfig` | Single source of truth; prevents rate inconsistency between config and applications |
| 2 | Accrual model (6.3) | Monthly accrual on live outstanding balance via batch job | Reflects actual balance after repayments; industry standard for reducing-balance loans; IFRS compliant |
| 3 | History detail (6.5) | Full audit log — new `LoanInterestHistory` entity with date, amount, balance snapshot, cumulative total, event type | Complete audit trail for compliance and dispute resolution |
| 4 | Module coupling | Direct dependency — `sacco-loan` depends on `sacco-config`, injects `LoanProductConfigRepository` | Simpler than event-driven; acceptable in modular monolith |
| 5 | Schedule update | Keep original repayment schedule from disbursement unchanged | Actual interest tracked separately in `LoanInterestHistory`; schedule serves as the amortization plan |
| 6 | Interest fields | Both `totalInterestAccrued` and `totalInterestPaid` as separate BigDecimal fields on `LoanApplication` | Enables tracking of accrued-vs-paid gap (arrears); essential for accounting reconciliation |
| 7 | API migration | Breaking change — replace `interestRate`/`interestMethod` with `loanProductId` on `LoanApplicationRequest` | Pre-production system; clean API surface; no backward-compat burden |
| 8 | Repayment allocation | Interest-first: overdue interest → current interest → principal | Standard SACCO practice per SASRA/UN SACCO loan agreements |
| 9 | GL accounting | Accrual basis — monthly DR Interest Receivable / CR Interest Income; on repayment DR Cash / CR Interest Receivable / CR Loan Receivable | IFRS compliant; proper revenue recognition |
| 10 | Portfolio reporting | Monthly interest summary across all loans: accrued, received, arrears | Institutional view for treasurer reporting |

## Constraints

- `LoanProductConfig` must exist and be active before a loan can be applied for
- Monthly accrual job runs inside existing `LoanBatchServiceImpl` cron schedule
- All monetary calculations use `BigDecimal` scale 6 intermediate / scale 2 final / `HALF_UP` rounding
- `LoanInterestHistory` records are append-only (no updates or deletes)
- `interestMethod` field on `LoanApplication` changes from `String` to `InterestMethod` enum
- Interest Receivable account (code `1003`) must be seeded in ledger accounts
- `INTEREST_ACCRUAL` must be added to `TransactionType` enum
- `LoanInterestAccrualEvent` must be created in sacco-common for GL posting

## Open Questions

- None — all decisions captured

## Related Code

- `sacco-loan/src/main/java/.../service/InterestCalculator.java` — Pure interest math (reused for monthly accrual)
- `sacco-loan/src/main/java/.../service/RepaymentScheduleGenerator.java` — Schedule generation (unchanged, but migrate to enum)
- `sacco-loan/src/main/java/.../service/LoanServiceImpl.java` — Core service (modified for config lookup + interest tracking)
- `sacco-loan/src/main/java/.../service/LoanBatchServiceImpl.java` — Batch job (extended with accrual logic)
- `sacco-loan/src/main/java/.../entity/LoanApplication.java` — Loan entity (new fields + enum migration)
- `sacco-loan/src/main/java/.../dto/LoanApplicationRequest.java` — API DTO (breaking change)
- `sacco-config/src/main/java/.../entity/LoanProductConfig.java` — Config source for rates
- `sacco-config/src/main/java/.../entity/InterestMethod.java` — Enum to adopt in loan module
- `sacco-config/src/main/java/.../service/ConfigService.java` — Config service interface
- `sacco-ledger/src/main/java/.../listener/FinancialEventListener.java` — Handles GL journal entries
- `sacco-ledger/src/main/java/.../entity/TransactionType.java` — Needs INTEREST_ACCRUAL
- `sacco-common/src/main/java/.../event/*.java` — Event pattern for GL integration

## Research

Key findings from SACCO interest tracking research:
- SASRA requires accrual-basis accounting (IFRS compliant)
- Standard repayment allocation: overdue interest → current interest → principal
- Portfolio at Risk (PAR30/60/90) depends on interest arrears tracking
- Interest in arrears = totalInterestAccrued - totalInterestPaid
- Monthly GL entries: DR Interest Receivable / CR Interest Income
- On repayment: DR Cash / CR Interest Receivable (interest portion) / CR Loan Receivable (principal)

---
*Discussed: 2026-02-15*
