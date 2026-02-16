# Member Account Setup on Registration

## Summary

When a member is added to the SACCO, automatically create per-member sub-accounts in the ledger. System accounts (Bad Debt, Interest Receivable, etc.) remain seeded via Liquibase migrations.

## Decisions

### 1. Per-Member Sub-Accounts (not consolidated)

Create individual ledger accounts per member rather than relying solely on the consolidated chart of accounts. This enables per-member trial balance from the ledger and a proper audit trail.

**On member registration**, create:
- **Member Shares** sub-account (parent: 2001) — e.g., `2001-M001`
- **Member Savings** sub-account (parent: 2002) — e.g., `2002-M001`

**On first loan**, create:
- **Loan Receivable** sub-account (parent: 1002) — e.g., `1002-M001`

### 2. Account Code Format: Parent-MemberNumber

Sub-account codes follow the pattern `{parentCode}-{memberNumber}`. Example: member `M001` gets accounts `2001-M001` and `2002-M001`. Human-readable and leverages the existing unique member number.

### 3. MemberCreatedEvent

Publish `MemberCreatedEvent` from `MemberServiceImpl.create()` (currently a TODO at line 41). The event carries `memberId` and `memberNumber`. A new `@EventListener` in `FinancialEventListener` (sacco-ledger) handles the event and creates the two sub-accounts.

### 4. System Accounts: Liquibase Only

The current 13 system accounts seeded via Liquibase are complete. No additional system accounts needed. Future system accounts are added as new Liquibase changesets.

## Schema Changes Required

The `accounts` table needs two new nullable columns:
- `parent_account_code VARCHAR(20)` — links sub-account to parent (e.g., `2001`)
- `member_id UUID` — associates the account with a specific member

## Open Questions

1. Should existing members get retroactive sub-accounts via a one-time data migration?
2. Should per-member account balance become the source of truth, eventually replacing `members.shareBalance`?
3. Should journal entries reference per-member sub-accounts or continue using consolidated parent accounts?

## Related Code

| File | Relevance |
|------|-----------|
| `sacco-member/.../MemberServiceImpl.java` | Publishes MemberCreatedEvent (TODO line 41) |
| `sacco-member/.../Member.java` | Member entity with `memberNumber` field |
| `sacco-ledger/.../Account.java` | Account entity — needs `parentAccountCode` and `memberId` fields |
| `sacco-ledger/.../FinancialEventListener.java` | Handles financial events — add MemberCreatedEvent handler |
| `sacco-common/.../event/` | Event records — add MemberCreatedEvent |
| `sacco-ledger/.../002-seed-chart-of-accounts.yaml` | Existing system account seeds |
| `sacco-loan/.../LoanServiceImpl.java` | Loan application — trigger loan sub-account creation |
