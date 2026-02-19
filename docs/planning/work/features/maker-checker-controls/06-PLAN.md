# Plan 06: Add maker-checker override reason UI to frontend approval actions across all financial flows

## Goal
Add maker-checker override reason UI to frontend approval actions across all financial flows

## Tasks

### Task 1: Add maker-checker types and API error handling
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/types/makerChecker.ts`, `sacco-ui/src/services/loanService.ts`, `sacco-ui/src/services/payoutService.ts`, `sacco-ui/src/services/pettyCashService.ts`
**Action:**
Create makerChecker.ts with ApprovalRequest type ({ overrideReason?: string }). Update service functions for loan approve, payout approve, share withdrawal approve, petty cash approve, bank withdrawal approve, and cash disbursement approve to accept optional overrideReason in request body. Add error handling for 403 MakerCheckerViolation responses — parse the error to detect self-approval violations.

**Verify:**
```bash
cd sacco-ui && npm run build
```

**Done when:** [Observable outcome]

### Task 2: Add override reason input to approval modals/actions
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Operations.tsx`, `sacco-ui/src/pages/ContributionOperations.tsx`, `sacco-ui/src/pages/PettyCash.tsx`
**Action:**
In approval action dialogs/buttons: (1) After clicking approve, if the current user matches the item's createdBy, show a confirmation dialog with a required override reason textarea (only for ADMIN users). (2) For non-ADMIN users attempting self-approval, show an error message explaining they cannot approve their own items. (3) Pass overrideReason to the approve service call. (4) Show success/error toast based on response.

**Verify:**
```bash
cd sacco-ui && npm run build
```

**Done when:** [Observable outcome]

### Task 3: Frontend error handling and user feedback for maker-checker violations
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/components/MakerCheckerWarning.tsx`
**Action:**
Create a reusable MakerCheckerWarning component that displays when a 403 maker-checker violation is returned from the API. Component shows: (1) clear message that self-approval is not allowed, (2) for ADMIN users, an option to provide an override reason and retry, (3) for non-ADMIN users, instruction to have a different user approve. Integrate into all approval flows.

**Verify:**
```bash
cd sacco-ui && npm run build
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
cd sacco-ui && npm run build && npm run lint
```

## Commit Message
```
feat(maker-checker-controls): add frontend override reason UI and maker-checker error handling
```
