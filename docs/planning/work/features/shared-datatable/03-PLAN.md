# Plan 03: Migrate Payouts, PettyCash, and AuditTrail to DataTable component

## Goal
Migrate Payouts, PettyCash, and AuditTrail to DataTable component

## Tasks

### Task 1: Migrate Payouts table to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Payouts.tsx`
**Action:**
Replace manual table with <DataTable>. Define ColumnDef for columns: Member, Type, Date, Status (badge), Amount (KES), and conditional Actions. Keep filter bar and Load More outside.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Migrate PettyCash table to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/PettyCash.tsx`
**Action:**
Replace manual table with <DataTable>. Define ColumnDef for columns: Voucher #, Date, Description, Category, Status (badge), Amount (KES), and conditional Actions. Keep filter bar and Load More outside.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 3: Migrate AuditTrail table to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/AuditTrail.tsx`
**Action:**
Replace manual table with <DataTable>. Define ColumnDef for 5 columns: Timestamp, User, Action, Entity, Details. Keep filter bar and Load More outside.

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
refactor(ui): migrate Payouts, PettyCash, AuditTrail to DataTable component
```
