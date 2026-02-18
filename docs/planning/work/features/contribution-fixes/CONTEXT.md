# Contribution Fixes — Context

**Feature:** `contribution-fixes`
**Scope:** Fix 3 backend-frontend contract mismatches in contributions. Frontend-only changes.

## Bugs

### 1. `category` type mismatch → renders `[object Object]`

- **Backend**: `ContributionResponse.category` is `ContributionCategoryResponse` (nested object: `id`, `name`, `description`, `active`, `isMandatory`)
- **Frontend**: `ContributionResponse.category` typed as `string`
- **Symptom**: `Contributions.tsx:221` renders `{c.category}` which shows `[object Object]`
- **Fix**: Update type to nested object, render `c.category.name`

### 2. `contributionMonth` shape → POST will 400

- **Backend**: `RecordContributionRequest.contributionMonth` is `LocalDate` → expects `YYYY-MM-DD`
- **DB**: `contribution_month` column is `type: date`
- **Frontend**: `RecordContributionModal.tsx:88` sends `date.slice(0, 7)` → `"2026-02"` (missing day)
- **Fix**: Send `date.slice(0, 7) + "-01"` (first of month)

### 3. `contributionMonth` filter never matches

- **Backend** returns `contributionMonth: "2026-02-01"` (full date)
- **Frontend** `Contributions.tsx:103` compares `c.contributionMonth === month` where `month` is `"2026-02"`
- **Fix**: Compare `c.contributionMonth.slice(0, 7) === month`

## Cleanup

### Dead `utils/contributions.ts`

- Zero imports from `src/` — only test file references it
- Uses `status === 'PAID'` which isn't in the enum (`CONFIRMED`)
- Superseded by inline summary at `Contributions.tsx:107-113`
- **Delete** both `utils/contributions.ts` and `tests/contributions-utils.test.ts`

## Out of Scope

- `ContributionCategories.tsx` — still hardcoded, needs full CRUD wiring (separate feature)
- `ContributionOperations.tsx` — still hardcoded, needs confirm/reverse/bulk wiring (separate feature)

## Constraints

- No backend changes
- Must pass `tsc --noEmit`, `vite build`, `npm test` after changes
