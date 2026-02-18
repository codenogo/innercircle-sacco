# Plan 02: Wire Loans page and modal to real backend API endpoints

## Goal
Wire Loans page and modal to real backend API endpoints

## Tasks

### Task 1: Create loan service and types
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/types/loans.ts`, `sacco-ui/src/services/loanService.ts`
**Action:**
Create TypeScript types matching backend DTOs: LoanResponse (id, memberId, loanProductId, principalAmount, interestRate, termMonths, interestMethod, status, purpose, approvedBy, approvedAt, disbursedAt, totalRepaid, outstandingBalance, totalInterestAccrued, totalInterestPaid, totalPenalties, createdAt, updatedAt), LoanApplicationRequest (memberId, loanProductId, principalAmount, termMonths, purpose), RepaymentRequest (amount, referenceNumber), LoanSummaryResponse (memberId, totalLoans, activeLoans, closedLoans, totalBorrowed, totalRepaid, totalOutstanding, loans), RepaymentScheduleResponse, MonthlyInterestSummary. LoanStatus union type: 'PENDING'|'APPROVED'|'ACTIVE'|'COMPLETED'|'DEFAULTED'|'REJECTED'. Create loanService.ts exporting: getLoans(cursor?, size?), getLoan(id), applyForLoan(payload), approveLoan(id), rejectLoan(id), disburseLoan(id), repayLoan(id, payload), getLoanSchedule(id), getMemberLoanSummary(memberId), getInterestSummary(month?). All use apiRequest<T>.

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Wire Loans page to real API
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Loans.tsx`
**Action:**
Replace inline mock data with useAuthenticatedApi hook. Import LoanResponse, CursorPage types. Add useState for loans array, loading, error, nextCursor, hasMore. Fetch via request<CursorPage<LoanResponse>>('/api/v1/loans?size=50'). Map backend LoanResponse fields to table: id, memberId (need member name lookup or show ID), interestRate, status, principalAmount, outstandingBalance. Compute portfolio summary (totalDisbursed, totalOutstanding, totalRepaid) from fetched loans. Add cursor-based Load More. Add loading/error states. Remove hardcoded loans array.

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 3: Wire NewLoanModal to submit via API
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/components/NewLoanModal.tsx`
**Action:**
Add onSubmit prop of type (payload: LoanApplicationRequest) => Promise<void> and isSubmitting boolean prop. Fetch loan products from /api/v1/config/loan-products?activeOnly=true to populate product dropdown dynamically (replace hardcoded Standard/Emergency). Product selection should auto-fill interest rate for the estimated repayment calculation. On submit, call onSubmit with LoanApplicationRequest. In Loans.tsx parent: implement handleApplyLoan that calls request<LoanResponse>('/api/v1/loans/apply', { method: 'POST', body }), prepends to loans list, shows success feedback. Pass real member list from /api/v1/members.

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
feat(frontend-backend-wiring): wire loans page and modal to real API
```
