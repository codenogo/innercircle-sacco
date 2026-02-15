# Plan 05: Enhanced Dashboard Analytics

## Goal
Enhance dashboard endpoints with time-series data (monthly aggregations by year) for loans, contributions, repayments, and interest — matching the old system's comprehensive analytics.

## Prerequisites
- [x] Reporting module with dashboard endpoints
- [x] FinancialReportServiceImpl with JdbcTemplate

## Tasks

### Task 1: Create Time-Series Analytics DTOs
**Files:**
- `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/dto/MonthlyDataPoint.java`
- `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/dto/DashboardAnalyticsResponse.java`
- `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/dto/SaccoStateResponse.java`

**Action:**
Create DTOs:
- `MonthlyDataPoint` — record with fields: month (int), monthName (String), amount (BigDecimal). Represents one month's data point.
- `DashboardAnalyticsResponse` — record with: year (int), loansDisbursed (List<MonthlyDataPoint>), amountRepaid (List<MonthlyDataPoint>), interestAccrued (List<MonthlyDataPoint>), contributionsReceived (List<MonthlyDataPoint>), payoutsProcessed (List<MonthlyDataPoint>).
- `SaccoStateResponse` — record with: totalMembers (int), activeMembers (int), totalShareCapital (BigDecimal), totalOutstandingLoans (BigDecimal), totalContributions (BigDecimal), totalPayouts (BigDecimal), loanRecoveryRate (BigDecimal), memberGrowthRate (BigDecimal).

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-reporting -q -DskipTests
```

**Done when:** All DTOs compile as records.

### Task 2: Add Analytics Methods to FinancialReportService
**Files:**
- `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/FinancialReportService.java`
- `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/FinancialReportServiceImpl.java`

**Action:**
Add methods to the existing service interface and implementation:
- `getDashboardAnalytics(int year)` — returns `DashboardAnalyticsResponse` with monthly breakdowns using JdbcTemplate aggregate queries:
  - Loans disbursed per month: `SELECT EXTRACT(MONTH FROM disbursed_at) AS month, SUM(principal_amount) FROM loan_applications WHERE status IN ('REPAYING','CLOSED') AND EXTRACT(YEAR FROM disbursed_at) = ? GROUP BY month`
  - Amount repaid per month: `SELECT EXTRACT(MONTH FROM repayment_date) AS month, SUM(amount) FROM loan_repayments WHERE EXTRACT(YEAR FROM repayment_date) = ? GROUP BY month`
  - Interest accrued per month: `SELECT EXTRACT(MONTH FROM repayment_date) AS month, SUM(interest_portion) FROM loan_repayments WHERE EXTRACT(YEAR FROM repayment_date) = ? GROUP BY month`
  - Contributions per month: `SELECT EXTRACT(MONTH FROM contribution_date) AS month, SUM(amount) FROM contributions WHERE status = 'CONFIRMED' AND EXTRACT(YEAR FROM contribution_date) = ? GROUP BY month`
  - Payouts per month: similar query on payouts table
- `getSaccoState()` — returns `SaccoStateResponse` with current aggregate state of the SACCO

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-reporting -q -DskipTests
```

**Done when:** Service compiles with analytics methods, SQL queries are correct.

### Task 3: Add Analytics Endpoints to DashboardController
**Files:**
- `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/controller/DashboardController.java`

**Action:**
Add new endpoints to the existing DashboardController:
- `GET /api/v1/dashboard/analytics?year={year}` — Full analytics dashboard with monthly breakdowns (defaults to current year). Requires ADMIN or TREASURER role.
- `GET /api/v1/dashboard/analytics/loans?year={year}` — Loans disbursed per month.
- `GET /api/v1/dashboard/analytics/repayments?year={year}` — Repayments per month.
- `GET /api/v1/dashboard/analytics/interest?year={year}` — Interest accrued per month.
- `GET /api/v1/dashboard/analytics/contributions?year={year}` — Contributions per month.
- `GET /api/v1/dashboard/state` — Current SACCO state/health overview.

All return `ApiResponse<T>`. Analytics endpoints require `@PreAuthorize("hasAnyRole('ADMIN','TREASURER')")`.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-reporting -q -DskipTests
```

**Done when:** Controller compiles with new endpoints, security annotations applied.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn clean compile -q -DskipTests
```

## Commit Message
```
feat(reporting): add enhanced dashboard analytics with time-series data

- Monthly breakdowns for loans, repayments, interest, contributions
- SACCO state overview endpoint
- Year-based analytics filtering
- ADMIN/TREASURER authorization for analytics
```

---
*Planned: 2026-02-15*
