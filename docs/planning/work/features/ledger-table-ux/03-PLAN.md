# Plan 03: Add column header filter inputs with debounced server-side filtering and wire to the backend API

## Goal
Add column header filter inputs with debounced server-side filtering and wire to the backend API

## Tasks

### Task 1: Add column filter state and debounced API integration
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Ledger.tsx`
**Action:**
Wire column filtering to the backend: (1) Add columnFilters state to TanStack Table (manualFiltering: true). (2) Create a useDebounce hook (or inline useMemo with setTimeout) — 400ms debounce. (3) When column filters change (after debounce), reset entries, set page to 0, and call loadEntries with the filter values as query params: entryNumber, dateFrom, dateTo, description, transactionType, accountId. (4) Map each column's filter to the correct query param. (5) Account column filter should be a dropdown (populated from /api/v1/ledger/accounts). Other columns use text inputs. Date column uses two small date inputs (from/to). TransactionType uses a dropdown with the 13 enum values. (6) Debit/Credit columns: no server filter (amounts vary per line, filtering at entry level is not meaningful).

**Verify:**
```bash
cd sacco-ui && npm run build
```

**Done when:** [Observable outcome]

### Task 2: Style column header filter inputs
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Ledger.css`
**Action:**
Add CSS for column filter inputs: (1) .ledger-th-filter — second row of thead cells containing filter inputs. (2) .ledger-filter-input — small text input: font-size text-xs, padding space-1, full width, border rule, border-radius radius-sm. (3) .ledger-filter-select — small select/dropdown matching the input style. (4) .ledger-filter-date — paired date inputs with a '–' separator. (5) All filter inputs inherit the design tokens (ink, paper, rule colors). (6) Sticky header must cover both the label row and the filter row.

**Verify:**
```bash
cd sacco-ui && npm run build
```

**Done when:** [Observable outcome]

### Task 3: Update TypeScript types and add useDebounce utility
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/hooks/useDebounce.ts`, `sacco-ui/src/types/ledger.ts`
**Action:**
Create useDebounce hook in hooks/useDebounce.ts: takes a value and delay, returns debounced value using useEffect + setTimeout pattern. Update types/ledger.ts: add JournalEntryFilters interface with optional fields (entryNumber, description, dateFrom, dateTo, transactionType, accountId) matching the backend query params. Add TransactionType union type matching the 13 backend enum values.

**Verify:**
```bash
cd sacco-ui && npm run build
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
cd sacco-ui && npm run build
cd sacco-ui && npm run lint
```

## Commit Message
```
feat(ledger-ui): add column header filters with debounced server-side search
```
