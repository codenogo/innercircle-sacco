# Plan 05: Migrate secondary and dashboard tables (LoanWorkflow, LoanBatch, LoanBenefits, LedgerStatements, Dashboard, RoleDashboards) to DataTable

## Goal
Migrate secondary and dashboard tables (LoanWorkflow, LoanBatch, LoanBenefits, LedgerStatements, Dashboard, RoleDashboards) to DataTable

## Tasks

### Task 1: Migrate LoanWorkflow and LoanBatch to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/LoanWorkflow.tsx`, `sacco-ui/src/pages/LoanBatch.tsx`
**Action:**
Replace manual tables with <DataTable> in both files. LoanWorkflow has columns: Description, Type, Date, Amount, Status. LoanBatch has columns: Member, Loan ID, Status, Installment, Arrears, and conditional action column. These are sub-page tables typically with small datasets.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Migrate LoanBenefits and LedgerStatements to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/LoanBenefits.tsx`, `sacco-ui/src/pages/LedgerStatements.tsx`
**Action:**
Replace manual tables with <DataTable>. LoanBenefits has columns: Benefit Type, Description, Eligible, Utilized, Available. LedgerStatements has columns: Date, Description, Debit, Credit — and uses a tfoot for totals which should migrate to stickyTotals prop.

**Verify:**
```bash
cd sacco-ui && npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 3: Migrate Dashboard and RoleDashboards tables to DataTable
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Dashboard.tsx`, `sacco-ui/src/pages/RoleDashboards.tsx`
**Action:**
Replace manual tables with <DataTable>. Dashboard has a small summary table (Item, Description, Value). RoleDashboards has a simple pending-approvals table. These are small static tables — use DataTable for consistency but may not need virtualization overscan tuning.

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
refactor(ui): migrate secondary and dashboard tables to DataTable component
```
