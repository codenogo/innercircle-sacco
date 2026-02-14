# Plan 08: Loan Management

## Goal
Implement loan products, application/approval/disbursement workflow, repayment schedules (reducing balance + flat rate), penalties, and member earnings from loan interest.

## Prerequisites
- [ ] Plan 01 complete

## Tasks

### Task 1: Loan Entities + Repository
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanApplication.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanStatus.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanRepayment.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/RepaymentStatus.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanGuarantor.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanPenalty.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/RepaymentSchedule.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/LoanApplicationRepository.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/LoanRepaymentRepository.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/LoanGuarantorRepository.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/LoanPenaltyRepository.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/RepaymentScheduleRepository.java`
**Action:**
1. `LoanApplication` entity: memberId, loanProductId (references config), principalAmount, interestRate, interestMethod (REDUCING/FLAT), termMonths, status (PENDING, APPROVED, REJECTED, DISBURSED, REPAYING, CLOSED, DEFAULTED), approvedBy, approvedAt, disbursedAt, totalRepaid, outstandingBalance
2. `RepaymentSchedule`: loanId, installmentNumber, dueDate, principalDue, interestDue, totalDue, paid (boolean)
3. `LoanRepayment`: loanId, amount, principalPortion, interestPortion, paymentDate, referenceNumber
4. `LoanGuarantor`: loanId, guarantorMemberId, guaranteedAmount, status (PENDING, ACCEPTED, REVOKED)
5. `LoanPenalty`: loanId, amount, reason, status (PENDING, PAID, WAIVED), appliedDate
6. All repositories with cursor pagination and filters

**Verify:**
```bash
mvn compile -pl sacco-loan -q
```

**Done when:** All loan entities and repositories compile.

### Task 2: Loan Service + Interest Calculation + Events
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanService.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/InterestCalculator.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/RepaymentScheduleGenerator.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanPenaltyService.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanPenaltyServiceImpl.java`
**Action:**
1. `LoanService`: applyForLoan, approveLoan (single approver — TREASURER/ADMIN), rejectLoan, disburseLoan (publishes `LoanDisbursedEvent`), recordRepayment (publishes `LoanRepaymentEvent`), closeLoan, getMemberLoans, getLoanDetails
2. `InterestCalculator`: calculateReducingBalance(principal, rate, term), calculateFlatRate(principal, rate, term). Returns BigDecimal with 2dp precision.
3. `RepaymentScheduleGenerator`: generates monthly installment schedule based on interest method
4. `LoanPenaltyService`: calculateLatePenalties, applyPenalty (publishes `PenaltyAppliedEvent`), waivePenalty
5. Early repayment support: recalculate remaining schedule

**Verify:**
```bash
mvn compile -pl sacco-loan -q
```

**Done when:** Loan service with interest calculation and events compiles.

### Task 3: Loan REST API + Liquibase
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/controller/LoanController.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/LoanApplicationRequest.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/LoanResponse.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/RepaymentRequest.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/RepaymentScheduleResponse.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/LoanSummaryResponse.java`, `sacco-loan/src/main/resources/db/changelog/loan/001-create-loan-tables.yaml`
**Action:**
1. `LoanController` at `/api/v1/loans`:
   - POST `/` — apply for loan (MEMBER via own account, SECRETARY on behalf)
   - PATCH `/{id}/approve` — approve loan (TREASURER, ADMIN)
   - PATCH `/{id}/reject` — reject loan (TREASURER, ADMIN)
   - PATCH `/{id}/disburse` — disburse loan (TREASURER, ADMIN)
   - POST `/{id}/repayments` — record repayment (TREASURER, SECRETARY)
   - GET `/` — list loans (cursor-paginated, filterable by status/member)
   - GET `/{id}` — loan details with repayment schedule
   - GET `/{id}/schedule` — repayment schedule
   - GET `/member/{memberId}/summary` — member loan summary
2. Liquibase: loan_applications, repayment_schedules, loan_repayments, loan_guarantors, loan_penalties tables
3. Update sacco-app changelog-master

**Verify:**
```bash
mvn compile -pl sacco-loan -q
```

**Done when:** Loan REST API and changelogs complete.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-loan -q
```

## Commit Message
```
feat(loan): implement loan management with configurable interest and approval workflow

- Loan application, approval, disbursement, repayment workflow
- Reducing balance and flat rate interest calculation
- Repayment schedule generation
- Penalties for late repayment with waiver support
- Domain events for ledger integration
```

---
*Planned: 2026-02-14*
