# Maker-Checker Controls

**Feature:** `maker-checker-controls`
**Date:** 2026-02-18

## Summary

Add maker-checker (dual-authorization) controls to all financial flows in the SACCO system. The person who creates a loan, payout, or other financial transaction cannot be the same person who approves it.

## Decisions

### Scope: All Financial Flows
Every financial entity requires maker-checker enforcement:
- **Loan Applications** (PENDING -> APPROVED)
- **Payouts** (PENDING -> APPROVED)
- **Share Withdrawals** (PENDING -> APPROVED)
- **Petty Cash Vouchers** (SUBMITTED -> APPROVED)
- **Bank Withdrawals** (needs new approval step added)
- **Cash Disbursements** (needs new status workflow added)

### Depth: Approval-Only Separation
- Creator cannot approve their own record
- Approver CAN disburse/process (no three-person requirement)
- Keeps the system practical for small SACCOs with 2-3 privileged users

### Override: ADMIN Self-Approve with Reason
- ADMIN users can override the maker-checker rule
- Must provide a mandatory reason (free text, max 500 chars)
- Override is logged as an audit event with the reason
- TREASURER role cannot override

### Architecture: Shared Guard in sacco-common
```java
// sacco-common/.../guard/MakerCheckerGuard.java
MakerCheckerGuard.assertOrOverride(maker, checker, overrideReason, auth)
```
- Throws `MakerCheckerViolationException` if same actor and no valid override
- All services call this guard during approval methods

### API: Optional overrideReason on Approve DTOs
```
PATCH /api/v1/loans/{id}/approve
{ "overrideReason": "Emergency: only admin available" }
```
- Field is optional; required only when creator == approver and user is ADMIN
- Server-side validation in `MakerCheckerGuard`

## Constraints

1. Additive changes only — must not break existing approval workflows
2. Fix `LoanApplication.createdBy` not being set (prerequisite bug)
3. ADMIN overrides always produce audit events with reason
4. New Liquibase migrations needed for Bank Withdrawal and Cash Disbursement status columns
5. HTTP 403 with clear message on maker-checker violations
6. Frontend shows override reason input for ADMINs on self-approval

## Open Questions

1. New `OVERRIDE_APPROVE` audit action vs. reusing `APPROVE` with metadata?
2. Frontend: proactive warning vs. post-failure reason prompt for self-approval?
3. Cash Disbursement new workflow shape: PENDING->APPROVED->RECORDED?

## Related Code

| File | Relevance |
|------|-----------|
| `BaseEntity.java` | `createdBy` field used for maker identity |
| `MemberAccessHelper.java` | Resolves current user/actor from Authentication |
| `LoanServiceImpl.java` | Loan approval — needs guard call added |
| `LoanTransitionGuards.java` | Loan status transitions |
| `PayoutServiceImpl.java` | Payout approval — needs guard call added |
| `BankWithdrawalServiceImpl.java` | Needs new approval step + guard |
| `CashDisbursementServiceImpl.java` | Needs new status workflow + guard |
| `ShareWithdrawalServiceImpl.java` | Share withdrawal approval — needs guard |
| `PettyCashServiceImpl.java` | Petty cash approval — needs guard |
| `AuditAction.java` | May need new OVERRIDE_APPROVE action |
