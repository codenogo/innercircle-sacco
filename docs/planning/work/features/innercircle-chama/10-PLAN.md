# Plan 10: Reporting & Dashboard

## Goal
Implement dashboard endpoints for members, treasurers, and admins, plus financial reports and PDF/CSV export for member statements.

## Prerequisites
- [ ] Plan 01 complete
- [ ] Plans 02-09 recommended (reports query data from all modules)

## Tasks

### Task 1: Report Services
**Files:** `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/MemberStatementService.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/MemberStatementServiceImpl.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/FinancialReportService.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/FinancialReportServiceImpl.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/dto/MemberStatementEntry.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/dto/MemberStatementResponse.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/dto/FinancialSummaryResponse.java`
**Action:**
1. `MemberStatementService`: generateStatement(memberId, dateRange) — combines contributions, loans, repayments, payouts, penalties into a chronological statement with running balance
2. `FinancialReportService`: overallSummary(dateRange) — total contributions, total loans disbursed, total repayments, total payouts, net position
3. DTOs for statement entries and summaries

**Verify:**
```bash
mvn compile -pl sacco-reporting -q
```

**Done when:** Report services compile.

### Task 2: Dashboard Endpoints
**Files:** `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/controller/DashboardController.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/controller/ReportController.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/dto/MemberDashboardResponse.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/dto/TreasurerDashboardResponse.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/dto/AdminDashboardResponse.java`
**Action:**
1. `DashboardController` at `/api/v1/dashboard`:
   - GET `/member` — member's own summary: share balance, active loans, recent contributions, upcoming repayments (MEMBER)
   - GET `/treasurer` — financial overview: total collections, pending approvals, overdue loans, cash position (TREASURER)
   - GET `/admin` — system overview: member count, total assets, config summary, recent audit events (ADMIN)
2. `ReportController` at `/api/v1/reports`:
   - GET `/member-statement/{memberId}` — full member statement (ADMIN, TREASURER, or own MEMBER)
   - GET `/financial-summary` — overall financial summary (ADMIN, TREASURER)
   - GET `/loan-portfolio` — loan portfolio summary (ADMIN, TREASURER)
   - GET `/contribution-summary` — contribution collection summary (ADMIN, TREASURER)

**Verify:**
```bash
mvn compile -pl sacco-reporting -q
```

**Done when:** Dashboard and report endpoints compile.

### Task 3: PDF/CSV Export
**Files:** `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/ExportService.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/ExportServiceImpl.java`, `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/controller/ExportController.java`
**Action:**
1. `ExportService`: exportToCsv(data, headers), exportToPdf(data, template)
2. CSV export using OpenCSV or manual StringBuilder
3. PDF export using Apache PDFBox or iText (simple table layout)
4. `ExportController` at `/api/v1/export`:
   - GET `/member-statement/{memberId}/pdf` — member statement as PDF
   - GET `/member-statement/{memberId}/csv` — member statement as CSV
   - GET `/financial-summary/csv` — financial summary as CSV
   - GET `/trial-balance/csv` — trial balance as CSV
5. Response with Content-Disposition header for download

**Verify:**
```bash
mvn compile -pl sacco-reporting -q
```

**Done when:** Export service and controller compile.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-reporting -q
```

## Commit Message
```
feat(reporting): implement dashboards, reports, and PDF/CSV export

- Member, treasurer, admin dashboard endpoints
- Member statement generation with running balance
- Financial summary and loan portfolio reports
- PDF and CSV export for statements and reports
```

---
*Planned: 2026-02-14*
