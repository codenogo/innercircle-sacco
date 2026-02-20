# Plan 01: Build the shared DataTable<T> component with virtualization, sticky header, scroll container, skeleton loading, and empty state

## Goal
Build the shared DataTable<T> component with virtualization, sticky header, scroll container, skeleton loading, and empty state

## Tasks

### Task 1: Create DataTable.tsx with types and component
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/components/DataTable.tsx`
**Action:**
Create DataTable.tsx exporting: ColumnDef<T> type (key, header, width, render, className, headerClassName, sortable, sortKey), DataTableProps<T> interface (columns, data, getRowKey, loading, skeletonRows, emptyMessage, maxHeight, getRowClassName, onRowClick, sortColumn, sortDirection, onSort, stickyTotals, estimateRowSize, overscan), and the DataTable<T> component. Component renders: outer .datatable-frame div, inner .datatable-scroll-container div (ref for virtualizer), <table> with table-layout:fixed, <colgroup> from column widths, sticky <thead> with optional sort indicators, <tbody> with SkeletonTableRows when loading, empty state when no data, virtual spacer rows + mapped virtual rows using column render functions. Uses @tanstack/react-virtual useVirtualizer. Column widths set as CSS custom properties on the frame div. Import SkeletonTableRows from Skeleton.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Create DataTable.css with shared table styles
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/components/DataTable.css`
**Action:**
Create DataTable.css with styles for: .datatable-frame (border, radius, overflow:hidden, background), .datatable-scroll-container (max-height default, overflow-y:auto), table within frame (width:100%, table-layout:fixed, font-size var(--text-base)), sticky thead (position:sticky, top:0, z-index:10, background var(--paper), border-bottom), th.sortable (cursor, user-select, hover color), td styles (padding, vertical-align, overflow), .datatable-spacer-row td (padding:0, border:0, transparent), .datatable-row--alt (paper-alt background), .datatable-row--alt:hover and regular row:hover (paper-inset background), .datatable-sticky-totals (grid layout, border-top double, background), .datatable-loading-more (flex center, padding, muted text). Mobile responsive at 768px with horizontal scroll. Extract from Ledger.css and components.css patterns.

**Verify:**
```bash
cd sacco-ui && npm run build
```

**Done when:** [Observable outcome]

### Task 3: Smoke-test DataTable with a minimal render in Ledger page
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/components/DataTable.tsx`
**Action:**
Ensure the DataTable component builds cleanly and exports are correct. Run both tsc and vite build to verify no type errors or import issues. Fix any type or import issues found. Do NOT migrate Ledger yet — just verify the component compiles and exports correctly.

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
feat(ui): add shared DataTable component with virtualization and sticky headers
```
