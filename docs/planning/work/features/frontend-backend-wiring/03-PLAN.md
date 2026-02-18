# Plan 03: Wire Payouts page/modal and Dashboard to real backend API endpoints

## Goal
Wire Payouts page/modal and Dashboard to real backend API endpoints

## Tasks

### Task 1: Create payout and dashboard services with types
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/types/payouts.ts`, `sacco-ui/src/services/payoutService.ts`, `sacco-ui/src/types/dashboard.ts`, `sacco-ui/src/services/dashboardService.ts`
**Action:**
Create payout types: PayoutResponse (id, memberId, amount, type, status, approvedBy, processedAt, referenceNumber, createdAt, updatedAt), PayoutRequest (memberId, amount, type), PayoutStatus union ('PENDING'|'APPROVED'|'PROCESSING'|'COMPLETED'|'REJECTED'), PayoutType union. Create payoutService.ts: getPayouts(cursor?, size?), getPayoutsByStatus(status, cursor?, size?), getMemberPayouts(memberId, cursor?, size?), createPayout(payload), approvePayout(id), processPayout(id). Create dashboard types: TreasurerDashboardResponse, AdminDashboardResponse, SaccoStateResponse, MonthlyDataPoint (month, amount). Fields for TreasurerDashboard: match what backend returns (fund summary, collection stats, recent entries, member counts). Create dashboardService.ts: getTreasurerDashboard(), getAdminDashboard(), getAnalytics(year?), getSaccoState(). All use apiRequest<T>.

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Wire Payouts page and modal to real API
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Payouts.tsx`, `sacco-ui/src/components/NewPayoutModal.tsx`
**Action:**
In Payouts.tsx: Replace inline mock data with useAuthenticatedApi. Fetch payouts via CursorPage<PayoutResponse>. Map PayoutResponse fields to table columns. Compute summary stats from real data. Add cursor pagination, loading/error states. Remove hardcoded payouts array and mock import. In NewPayoutModal.tsx: Add onSubmit/isSubmitting props. On submit call onSubmit with PayoutRequest payload. In Payouts.tsx parent: implement handleCreatePayout that POST to /api/v1/payouts, prepend result, show feedback. Fetch real members for dropdown.

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 3: Wire Dashboard page to real API
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Dashboard.tsx`
**Action:**
Replace all inline mock data with useAuthenticatedApi. Fetch TreasurerDashboardResponse from /api/v1/dashboard/treasurer on mount. Map response fields to: Fund Summary section (totalSavings, activeLoans, availableBalance), Monthly Collections section (expected, collected, outstanding, rate), Recent Entries table, Member Summary counts. Add loading skeleton state and error handling. If user role is ADMIN, optionally also fetch /api/v1/dashboard/admin for additional stats. Remove all hardcoded mock constants (fundSummary, monthlyCollection, recentEntries, memberSummary).

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
npx tsc --noEmit
npx vite build
```

## Commit Message
```
feat(frontend-backend-wiring): wire payouts and dashboard pages to real API
```
