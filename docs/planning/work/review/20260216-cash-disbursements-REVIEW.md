# Review: Cash Disbursements (Payout Event Publishing)

**Branch:** `feat/custom-auth-api`
**Date:** 2026-02-16
**Verdict:** `warn`

## Summary

The change adds `PayoutStatusChangeEvent` publishing to `createPayout()` and `approvePayout()` in `PayoutServiceImpl`. Previously, only `processPayout()` published an event (`PayoutProcessedEvent`). This enables audit trail tracking for the full payout lifecycle (CREATED → APPROVED → PROCESSED).

## Change Scope

| File | Change |
|------|--------|
| `sacco-payout/.../service/PayoutServiceImpl.java` | Publish `PayoutStatusChangeEvent` on create and approve |

**Scope verdict:** Clean. No drive-by refactors, no unrelated changes.

## Test Results

- **128/128 payout module tests pass** (after rebuilding `sacco-common`)
- Initial `NoClassDefFoundError` was a stale local Maven artifact, not a code issue

## Warnings

### 1. Missing test coverage for new event publishing (Medium)

`PayoutServiceImplTest` does not verify the new `PayoutStatusChangeEvent` publishing in `createPayout()` or `approvePayout()`. The existing `eventCaptor` is typed to `PayoutProcessedEvent`, so only the `processPayout()` event is asserted. Tests pass because the mock `eventPublisher` silently accepts any event — but the new behavior has zero assertion coverage.

**Recommendation:** Add test cases verifying `PayoutStatusChangeEvent` is published with correct `action` ("CREATED"/"APPROVED"), `payoutId`, `memberId`, and `actor`.

### 2. No per-endpoint security annotations (Low, systemic)

No `@PreAuthorize` or equivalent annotations on `PayoutController` or `CashDisbursementController` endpoints. This is consistent across other modules (e.g., `ContributionController`) and appears to be a project-wide pattern — security may be configured at a different layer. Not introduced by this change.

## Checks Passed

- Version column: Handled by `common/001-add-version-column.yaml` (retroactive migration)
- Contract compatibility: `PayoutStatusChangeEvent` implements `AuditableEvent` — triggers audit only, no GL impact
- Failure behavior: Event publishing is within `@Transactional`, so failures roll back atomically
- Scope hygiene: Minimal, targeted change

## Next Action

**Fix** warning #1 (add test coverage for new events), then `/ship`.
