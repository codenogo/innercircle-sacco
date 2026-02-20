# Plan 06: Convert Ledger page to use DataTable component and clean up deprecated table CSS

## Goal
Convert Ledger page to use DataTable component and clean up deprecated table CSS

## Tasks

### Task 1: Migrate Ledger.tsx to use DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Ledger.tsx`
**Action:**
Replace the manual table in Ledger.tsx with <DataTable>. This is the most complex migration because Ledger uses expandable rows (entry + sub-rows), custom row types (LedgerDisplayRow discriminated union), and sticky totals. Define ColumnDef for 6 columns: Ref (with expand button), Date (sortable), Type, Description (with imbalance badge), Debit, Credit. Pass stickyTotals prop for the loaded totals footer. The displayRows flattening and virtualizer setup move into DataTable — Ledger passes flat data with getRowKey and estimateRowSize. Keep all filter bar, data fetching, and pagination logic in Ledger.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Remove deprecated ledger-table styles from components.css
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/styles/components.css`, `sacco-ui/src/pages/Ledger.css`
**Action:**
Remove the old .ledger-table, .ledger-table th, .ledger-table th.sortable styles from components.css (lines ~357-373) since all tables now use DataTable's own CSS classes. Remove any Ledger.css rules that are now handled by DataTable.css (table frame, scroll container, sticky thead, spacer rows). Keep Ledger-specific styles that aren't shared (expand button, journal-ref, journal-desc, subrow indent, entry-status, filter bar overrides, amount color classes).

**Verify:**
```bash
cd sacco-ui && npm run build
```

**Done when:** [Observable outcome]

### Task 3: Final build and visual verification checklist
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/components/DataTable.tsx`, `sacco-ui/src/components/DataTable.css`
**Action:**
Run full TypeScript check and Vite build. Fix any unused imports or dead CSS found. Confirm DataTable.tsx exports are clean and DataTable.css has no orphaned selectors. Verify the complete build succeeds with all 16 tables migrated.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
cd sacco-ui && npm run build
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
cd sacco-ui && npx tsc --noEmit
cd sacco-ui && npm run build
```

## Commit Message
```
refactor(ui): convert Ledger to DataTable and remove deprecated table CSS
```
