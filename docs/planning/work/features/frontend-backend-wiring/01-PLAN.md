# Plan 01: Wire Contributions page and modal to real backend API endpoints

## Goal
Wire Contributions page and modal to real backend API endpoints

## Tasks

### Task 1: Create contribution service and types
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/types/contributions.ts`, `sacco-ui/src/services/contributionService.ts`
**Action:**
Create TypeScript types matching backend DTOs: ContributionResponse (id, memberId, amount, category, paymentMode, contributionMonth, status, contributionDate, referenceNumber, notes, createdAt, updatedAt), ContributionSummaryResponse (memberId, totalContributed, totalPending, totalPenalties, lastContributionDate), RecordContributionRequest (memberId, amount, categoryId, paymentMode, contributionMonth, contributionDate, referenceNumber, notes), ContributionCategoryResponse (id, name, description, mandatory, active). Create contributionService.ts exporting: getContributions(cursor?, size?), getMemberContributions(memberId, cursor?, size?), getMemberContributionSummary(memberId), recordContribution(payload), confirmContribution(id), reverseContribution(id), getCategories(activeOnly?). All functions use apiRequest<T> from apiClient.ts. List endpoints use CursorPage<ContributionResponse>.

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Wire Contributions page to real API
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Contributions.tsx`
**Action:**
Replace inline mock data with useAuthenticatedApi hook. Import ContributionResponse and CursorPage types. Add useState for contributions array, loading, error, nextCursor, hasMore states. Add useEffect to fetch contributions via request<CursorPage<ContributionResponse>>('/api/v1/contributions?size=50'). Add cursor-based 'Load More' button like Members page. Replace inline Contribution interface with ContributionResponse type. Map backend fields (contributionMonth, contributionDate, status, amount) to table columns. Keep MonthPicker for client-side filtering of fetched data. Update page-summary stats to compute from real data. Add loading/error states following Members.tsx pattern. Remove hardcoded contributions array and mock data import for contributions.

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 3: Wire RecordContributionModal to submit via API
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/components/RecordContributionModal.tsx`
**Action:**
Add onSubmit prop of type (payload: RecordContributionRequest) => Promise<void> and isSubmitting boolean prop. Fetch contribution categories from /api/v1/contribution-categories?activeOnly=true to populate category dropdown (replace hardcoded Monthly/Special/Registration). On form submit, call onSubmit with RecordContributionRequest payload. Add loading state to submit button. In Contributions.tsx parent: implement handleRecordContribution that calls request<ContributionResponse>('/api/v1/contributions', { method: 'POST', body }) then prepends result to contributions list and shows success feedback. Pass real member list from /api/v1/members (fetched on page load) instead of mock members.

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
feat(frontend-backend-wiring): wire contributions page and modal to real API
```
