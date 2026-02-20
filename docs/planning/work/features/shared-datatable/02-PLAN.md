# Plan 02: Migrate high-traffic pages (Members, Contributions, Loans) to DataTable component

## Goal
Migrate high-traffic pages (Members, Contributions, Loans) to DataTable component

## Tasks

### Task 1: Migrate Members table to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Members.tsx`
**Action:**
Replace the manual <table className='ledger-table'> in Members.tsx with <DataTable>. Define ColumnDef array for 5 columns: Name (stacked: name + memberNumber + email with Link), Phone, Status (badge), Joined (date), Shares (KES, right-aligned currency). Use getRowClassName for i%2 alternating. Keep filter bar, Load More pagination, and modal outside DataTable. Remove SkeletonTableRows import if no longer needed directly.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Migrate Contributions table to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Contributions.tsx`
**Action:**
Replace manual table with <DataTable>. Define ColumnDef array for 5 columns: Member (text), Category (text), Date (formatted), Status (badge), Amount (KES, right-aligned). Use loading prop, emptyMessage, getRowClassName for alternating. Keep MonthPicker filter bar and Load More outside.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 3: Migrate Loans table to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Loans.tsx`
**Action:**
Replace manual table with <DataTable>. Define ColumnDef array for 7-8 columns (conditional on canManageLoans): Loan ID (truncated), Member ID (truncated), Rate, Status (badge), Disbursed (date), Principal (currency), Balance (currency, conditional negative class), Actions (conditional approve/reject buttons). Handle conditional column count by building columns array dynamically based on canManageLoans. Keep filter bar and Load More outside.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
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
refactor(ui): migrate Members, Contributions, Loans to DataTable component
```
