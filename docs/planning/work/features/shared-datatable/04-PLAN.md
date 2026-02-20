# Plan 04: Migrate ContributionOperations, UsersAdmin, and ContributionCategories to DataTable component

## Goal
Migrate ContributionOperations, UsersAdmin, and ContributionCategories to DataTable component

## Tasks

### Task 1: Migrate ContributionOperations table to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/ContributionOperations.tsx`
**Action:**
Replace manual table with <DataTable>. Define ColumnDef for columns matching existing thead: Member, Category, Date, Status, Amount (KES), and any action columns. Keep filter bar outside.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Migrate UsersAdmin table to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/UsersAdmin.tsx`
**Action:**
Replace manual table with <DataTable>. Define ColumnDef for 5 columns matching existing thead. Keep filter bar and modal outside.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 3: Migrate ContributionCategories table to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/ContributionCategories.tsx`
**Action:**
Replace manual table with <DataTable>. Define ColumnDef for 4 columns matching existing thead. Keep filter bar outside.

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
refactor(ui): migrate ContributionOps, UsersAdmin, ContributionCategories to DataTable
```
