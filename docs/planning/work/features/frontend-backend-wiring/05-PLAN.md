# Plan 05: Wire Reports page with downloads and Settings page with real config data

## Goal
Wire Reports page with downloads and Settings page with real config data

## Tasks

### Task 1: Create report and config services with types
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/types/reports.ts`, `sacco-ui/src/services/reportService.ts`, `sacco-ui/src/types/config.ts`, `sacco-ui/src/services/configService.ts`
**Action:**
Create report types: FinancialSummaryResponse (aggregate contributions, loans, interest, penalties data), MemberStatementResponse (list of MemberStatementEntry with totals). Create reportService.ts: getFinancialSummary(fromDate, toDate), getMemberStatement(memberId, fromDate, toDate), exportMemberStatementPdf(memberId, fromDate, toDate), exportMemberStatementCsv(memberId, fromDate, toDate), exportFinancialSummaryCsv(fromDate, toDate). Export functions return Blob for download. Create config types: SystemConfigResponse (configKey, configValue, description), LoanProductConfig (id, name, maxAmount, minAmount, termMonths, interestRate, interestMethod, requiresCollateral, active), ContributionScheduleConfig (id, name, frequency, amount, gracePeriodDays, active), PenaltyRule (id, name, penaltyType, penaltyAmount, penaltyPercentage, applicableToLoan, active). Create configService.ts: getSystemConfigs(), getLoanProducts(activeOnly?), getContributionSchedules(activeOnly?), getPenaltyRules(activeOnly?).

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Wire Reports page with real stats and download
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Reports.tsx`
**Action:**
Replace hardcoded quickStats with useAuthenticatedApi. Fetch SaccoStateResponse from /api/v1/dashboard/state for 'At a Glance' stats (totalSavings, loansOutstanding, monthCollected, activeMembers). Enable download buttons: on click, call export endpoint (e.g. exportFinancialSummaryCsv), receive blob, create Object URL, trigger download via hidden <a> link. For member-specific reports (Member Statement), add a member search/select before download. Add loading state for stats fetch. Add download-in-progress state per button. Remove hardcoded quickStats array.

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 3: Wire Settings page to real config data
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Settings.tsx`
**Action:**
Replace all hardcoded settings values with useAuthenticatedApi. On mount fetch in parallel: user profile from /api/v1/auth/me (already available via useAuth), loan products from /api/v1/config/loan-products, contribution schedules from /api/v1/config/contribution-schedules, penalty rules from /api/v1/config/penalty-rules, system configs from /api/v1/config/system. Map response data to existing settings-row layout: Profile section from auth user, Contribution Schedule from schedules[0] (amount, dueDate, gracePeriod), Loan Products from products array, Penalties from penalty rules, System section from system configs. Add loading state. Remove all hardcoded values.

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
feat(frontend-backend-wiring): wire reports and settings pages to real API
```
