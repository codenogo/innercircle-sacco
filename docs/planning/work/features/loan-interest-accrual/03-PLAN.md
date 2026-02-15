# Plan 03: Interest-First Repayment Allocation & Portfolio Interest Reporting

## Goal
Refactor repayment allocation to interest-first (SACCO standard), update GL entries for accrual-basis repayment, and add portfolio-level interest reporting for institutional tracking.

## Prerequisites
- [ ] Plan 01 complete (config-driven loan application, entity migration)
- [ ] Plan 02 complete (LoanInterestHistory, monthly accrual batch, GL integration)

## Tasks

### Task 1: Refactor repayment allocation to interest-first and track totalInterestPaid
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanInterestHistory.java`

**Action:**
1. Refactor `recordRepayment()` in LoanServiceImpl — replace proportional allocation with interest-first:
   - **Step 1**: Calculate total accrued but unpaid interest = `loan.getTotalInterestAccrued() - loan.getTotalInterestPaid()`
   - **Step 2**: Allocate to overdue interest first (interest arrears), then current period interest, then principal
   - **Step 3**: Update `loan.setTotalInterestPaid()` with interest portion allocated
   - **Step 4**: Update schedule payments — mark schedules paid based on interest+principal allocation
2. Create `LoanInterestHistory` record with eventType `REPAYMENT_APPLIED` when interest is paid via repayment:
   - `interestAmount` = negative (reducing outstanding interest)
   - `description` = "Interest paid via repayment REF-xxx"
3. Remove old proportional allocation logic (lines 189-194 of current LoanServiceImpl)

**Verify:**
```bash
cd sacco-loan && mvn compile -q
```

**Done when:** Repayment allocates interest-first (overdue interest → current interest → principal), updates totalInterestPaid, and creates audit history record.

### Task 2: Update FinancialEventListener repayment handler for accrual-basis GL entries
**Files:**
- `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/FinancialEventListener.java`

**Action:**
1. Modify `handleLoanRepayment()` to use accrual-basis accounting:
   - **Current (cash basis)**: DR Cash / CR Loan Receivable / CR Interest Income
   - **New (accrual basis)**: DR Cash / CR Interest Receivable (1003) / CR Loan Receivable (1002)
   - The interest portion credits Interest Receivable (reverses the monthly accrual), NOT Interest Income
   - Interest Income was already recognized during monthly accrual (Plan 02)
2. Change the interest portion credit line:
   - From: `creditInterest.setAccount(interestIncomeAccount)` (4001)
   - To: `creditInterest.setAccount(interestReceivableAccount)` (1003)
   - Description: "Interest receivable settled - Repayment ID: {repaymentId}"
3. The principal portion continues to credit Loan Receivable (1002) — no change there

**Verify:**
```bash
cd sacco-ledger && mvn compile -q
```

**Done when:** Loan repayment GL entry credits Interest Receivable (1003) for interest portion, completing the accrual-basis accounting cycle.

### Task 3: Create InterestReportingService with portfolio-level interest summary API
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/InterestReportingService.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/InterestReportingServiceImpl.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/MonthlyInterestSummary.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/MemberInterestSummary.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/controller/LoanController.java`

**Action:**
1. Create `MonthlyInterestSummary` DTO:
   - `month` (YearMonth)
   - `totalInterestAccrued` (BigDecimal — total interest earned this month across all loans)
   - `totalInterestReceived` (BigDecimal — total interest cash collected this month)
   - `totalInterestArrears` (BigDecimal — accrued minus received, cumulative)
   - `activeLoansCount` (int)
   - `loansWithArrearsCount` (int)
2. Create `MemberInterestSummary` DTO:
   - `memberId` (UUID)
   - `loanId` (UUID)
   - `totalInterestAccrued`, `totalInterestPaid`, `interestArrears` (BigDecimal)
   - `lastAccrualDate` (LocalDate)
3. Create `InterestReportingService` interface:
   - `getMonthlyInterestSummary(YearMonth month)` → `MonthlyInterestSummary`
   - `getMemberInterestSummary(UUID memberId)` → `List<MemberInterestSummary>`
   - `getPortfolioInterestArrears()` → `List<MemberInterestSummary>` (all loans with arrears > 0)
4. Create `InterestReportingServiceImpl`:
   - Use `LoanInterestHistoryRepository` for monthly accrual data
   - Use `LoanApplicationRepository` for cumulative fields (totalInterestAccrued, totalInterestPaid)
   - Calculate arrears: `totalInterestAccrued - totalInterestPaid` per loan
5. Add REST endpoints to `LoanController`:
   - `GET /api/v1/loans/interest/summary?month=2026-02` → monthly portfolio summary
   - `GET /api/v1/loans/interest/member/{memberId}` → member interest summary
   - `GET /api/v1/loans/interest/arrears` → all loans with interest arrears

**Verify:**
```bash
mvn test -pl sacco-loan -q
```

**Done when:** Portfolio-level interest reporting API returns monthly accrued/received/arrears across all loans, per-member summaries, and arrears report.

## Verification

After all tasks:
```bash
mvn test -pl sacco-loan -q
mvn test -pl sacco-ledger -q
```

## Commit Message
```
feat(loan): interest-first repayment allocation and portfolio interest reporting

- Refactor repayment to allocate interest-first per SACCO practice
- Update GL entries for accrual-basis: repayment credits Interest Receivable
- Add InterestReportingService for portfolio-level interest tracking
- Add REST endpoints for monthly summary, member summary, and arrears report
- Track totalInterestPaid on each repayment with audit trail
```

---
*Planned: 2026-02-15*
