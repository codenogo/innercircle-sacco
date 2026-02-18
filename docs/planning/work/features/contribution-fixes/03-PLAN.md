# Plan 03: Wire ContributionOperations.tsx to live confirm/reverse endpoints and add bulk contribution form

## Goal
Wire ContributionOperations.tsx to live confirm/reverse endpoints and add bulk contribution form

## Tasks

### Task 1: Wire ContributionOperations to fetch live data with confirm/reverse actions
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/ContributionOperations.tsx`
**Action:**
Rewrite the page: (1) Remove hardcoded mock data and local interfaces. (2) Import useAuthenticatedApi, useCallback, useEffect, useMemo. Import types: ContributionResponse, ContributionStatus, CursorPage, MemberResponse from appropriate type files. (3) Fetch contributions via request<CursorPage<ContributionResponse>>('/api/v1/contributions?size=50'). (4) Fetch members for name resolution (same pattern as Contributions.tsx). (5) Add month filter using MonthPicker — filter by c.contributionMonth.slice(0,7) === month. (6) Add status filter dropdown (All/PENDING/CONFIRMED/REVERSED). (7) Enable Confirm button for PENDING items — on click, PATCH /api/v1/contributions/{id}/confirm, update item in list, show success feedback. (8) Enable Reverse button for PENDING/CONFIRMED items — on click with window.confirm, PATCH /api/v1/contributions/{id}/reverse, update item in list. (9) Show member name via memberMap (same pattern as Contributions.tsx). (10) Show category.name from nested category object. (11) Add loading/error/feedback states. (12) Keep cursor pagination with Load More button.

**Verify:**
```bash
npx tsc --noEmit
npx vite build
```

**Done when:** [Observable outcome]

### Task 2: Add bulk contribution entry form to ContributionOperations
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/ContributionOperations.tsx`
**Action:**
Add a Bulk Processing section below the operations table: (1) Replace disabled Upload CSV / Process Bulk buttons with a functional form. (2) Form has shared defaults: PaymentMode select, Month (DatePicker or MonthPicker), Date, Category select (fetched from /api/v1/contribution-categories?activeOnly=true). (3) Dynamic rows: each row has Member select + Amount input. Add Row / Remove Row buttons. (4) On submit, build BulkContributionRequest with shared defaults + per-item memberId/amount, POST to /api/v1/contributions/bulk. (5) Show success count or per-item errors from response. (6) On success, reload the contributions list above.

**Verify:**
```bash
npx tsc --noEmit
npx vite build
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
npx tsc --noEmit
npx vite build
npm test
```

## Commit Message
```
feat(contribution): wire ContributionOperations with confirm/reverse and bulk entry
```
