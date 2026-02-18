# Plan 01: Fix contribution type mismatches (category, contributionMonth, BulkContributionRequest) and delete dead utils

## Goal
Fix contribution type mismatches (category, contributionMonth, BulkContributionRequest) and delete dead utils

## Tasks

### Task 1: Fix ContributionResponse.category type and BulkContributionRequest type
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/types/contributions.ts`
**Action:**
In types/contributions.ts: (1) Change ContributionResponse.category from `string` to `ContributionCategoryResponse`. (2) Replace BulkContributionRequest with backend-matching shape: shared defaults (paymentMode: PaymentMode, contributionMonth: string, contributionDate: string, categoryId: string, batchReference?: string) plus contributions: BulkContributionItemRequest[]. (3) Add BulkContributionItemRequest interface (memberId: string, amount: number, paymentMode?: PaymentMode, contributionMonth?: string, contributionDate?: string, referenceNumber?: string, notes?: string). (4) Add ContributionCategoryRequest interface (name: string, description?: string, active?: boolean, isMandatory?: boolean).

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Fix category rendering, contributionMonth send, and month filter
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Contributions.tsx`, `sacco-ui/src/components/RecordContributionModal.tsx`
**Action:**
In Contributions.tsx: (1) Change line 221 from `{c.category}` to `{typeof c.category === 'string' ? c.category : c.category.name}` or just `{c.category.name}`. (2) Change line 103 month filter from `c.contributionMonth === month` to `c.contributionMonth.slice(0, 7) === month`. In RecordContributionModal.tsx: (3) Change line 88 from `contributionMonth: date.slice(0, 7)` to `contributionMonth: date.slice(0, 7) + '-01'`.

**Verify:**
```bash
npx tsc --noEmit
npx vite build
```

**Done when:** [Observable outcome]

### Task 3: Delete dead utils/contributions.ts and its test
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/utils/contributions.ts`, `sacco-ui/tests/contributions-utils.test.ts`
**Action:**
Delete both files. They have zero imports from src/ and use 'PAID' status which doesn't exist in the ContributionStatus enum. The functionality is superseded by inline summary in Contributions.tsx:107-113.

**Verify:**
```bash
npx tsc --noEmit
npm test
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
fix(contribution): fix category/month type mismatches and remove dead utils
```
