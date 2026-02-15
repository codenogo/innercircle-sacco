# Event & GL Completeness - Implementation Context

## Problem

The event-driven architecture has gaps at three levels:
1. **Missing GL handlers** — events fire but produce no ledger entries (reversals, penalty payments, waivers, benefits)
2. **Missing event publishing** — business operations occur with no event emitted (contribution reversal, loan lifecycle, penalty waiver)
3. **Missing event definitions + payloads** — some events don't exist yet, others lack fields listeners need

These gaps cause ledger imbalances (GL doesn't reconcile with business state) and incomplete audit trails.

## Current Chart of Accounts

| Code | Name | Type |
|------|------|------|
| 1001 | Cash | ASSET |
| 1002 | Loan Receivable | ASSET |
| 1003 | Interest Receivable | ASSET |
| 2001 | Member Shares | LIABILITY |
| 2002 | Member Account | LIABILITY |
| 3001 | Share Capital | EQUITY |
| 3002 | Retained Earnings | EQUITY |
| 4001 | Interest Income | REVENUE |
| 4002 | Contribution Income | REVENUE |
| 4003 | Penalty Income | REVENUE |
| 5001 | Operating Expenses | EXPENSE |
| 5002 | Administrative Expenses | EXPENSE |
| **5003** | **Bad Debt Expense** | **EXPENSE** (new) |

## Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Scope | All gaps: critical GL handlers, medium lifecycle events, low payload enrichment | User chose comprehensive fix to prevent revisiting |
| GL reversal style | Mirror entries (new journal entry with debits/credits swapped) | Standard accounting practice; clear audit trail; no negative amounts |
| Penalty waiver GL | DR Bad Debt Expense (5003) / CR Member Account (2002) | Preserves penalty income recognition; waiver is an explicit write-off expense |
| New account | Add 5003 "Bad Debt Expense" via Liquibase migration | Required for penalty waiver GL entries |
| PenaltyPaidEvent GL | No standalone GL handler needed | All penalty payments currently flow through `LoanRepaymentEvent` which already handles the penalty portion GL. `PenaltyPaidEvent` is audit-only. If standalone penalty payment is added later, revisit. |
| Listener phase | GL handlers use `BEFORE_COMMIT`; audit uses `AFTER_COMMIT` | Consistent with existing pattern in `FinancialEventListener` |
| Lifecycle events | Audit-only (no GL impact) | Loan apply/approve/reject/close and payout create/approve are state transitions, not financial transactions |

## GL Entry Specifications

### Critical: LoanReversalEvent (mirror of LoanRepaymentEvent)
```
DR Loan Receivable (1002)    — principalPortion (reinstate receivable)
DR Interest Receivable (1003) — interestPortion (reinstate receivable)
CR Cash (1001)                — amount (reverse cash receipt)
```
Description: "Reversal of repayment — Reversal ID: {reversalId}"

### Critical: ContributionReversedEvent (mirror of ContributionReceivedEvent)
```
DR Member Shares (2001)  — amount (reduce member shares)
CR Cash (1001)           — amount (cash returned)
```
Description: "Contribution reversed — Ref: {referenceNumber}"

### Critical: PenaltyWaivedEvent (new event + GL)
```
DR Bad Debt Expense (5003)  — amount (write-off expense)
CR Member Account (2002)    — amount (clear member obligation)
```
Description: "Penalty waived — Penalty ID: {penaltyId}"

### Medium: BenefitsDistributedEvent
```
DR Interest Income (4001)   — totalInterestAmount (reduce income pool)
CR Member Account (2002)    — totalInterestAmount (allocate to members)
```
Description: "Interest benefits distributed — Loan ID: {loanId}"

### Lifecycle Events (audit-only, no GL)
New event records needed:
- `LoanApplicationEvent` (apply/approve/reject)
- `LoanStatusChangeEvent` (close/default)
- `ContributionCreatedEvent` (record/bulk)
- `PayoutStatusChangeEvent` (create/approve)
- `PenaltyWaivedEvent` (waive)

## Constraints

- Liquibase migration needed for account 5003 — checksum-sensitive (do not modify existing changesets)
- `LoanReversalEvent` already has `principalPortion` and `interestPortion` fields but no `penaltyPortion` — reversal of penalty portion needs to be handled (add field or derive from amount - principal - interest)
- `ContributionReversedEvent` exists but is never published — wire up in `ContributionServiceImpl.reverseContribution()`
- `ContributionPenaltyServiceImpl.waivePenalty()` exists but throws TODO — needs implementation
- `LoanReversalServiceImpl.reversePenalty()` throws `UnsupportedOperationException` — out of scope for this feature (separate concern)
- Lifecycle events are high-volume; keep payloads minimal for audit efficiency
- Existing tests in `FinancialEventListenerTest` cover 6 handlers — new handlers need matching tests

## Related Code

- `sacco-common/src/main/java/.../event/` — All event record definitions
- `sacco-ledger/src/main/java/.../listener/FinancialEventListener.java` — GL event handlers (6 existing)
- `sacco-audit/src/main/java/.../listener/AuditEventListener.java` — Generic audit listener
- `sacco-ledger/src/main/resources/db/changelog/ledger/002-seed-chart-of-accounts.yaml` — Chart of accounts seed
- `sacco-contribution/src/main/java/.../service/ContributionServiceImpl.java` — Contribution reversal TODO
- `sacco-contribution/src/main/java/.../service/ContributionPenaltyServiceImpl.java` — Penalty waiver TODO
- `sacco-loan/src/main/java/.../service/LoanReversalServiceImpl.java` — Reversal event publisher
- `sacco-loan/src/main/java/.../service/LoanBenefitServiceImpl.java` — Benefits distribution publisher
- `sacco-ledger/src/test/java/.../listener/FinancialEventListenerTest.java` — Existing GL tests

---
*Discussed: 2026-02-15*
