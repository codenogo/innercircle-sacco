# Plan 01: Loan Benefits & Earnings Distribution

## Goal
Implement loan benefits system where members earn proportional returns from loan interest, based on their share balance.

## Prerequisites
- [x] Loan module with repayment events implemented
- [x] Ledger module with journal entry support
- [x] Member module with share balance tracking

## Tasks

### Task 1: Create LoanBenefit Entity, Repository, DTOs & Migration
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/entity/LoanBenefit.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/repository/LoanBenefitRepository.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/LoanBenefitResponse.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/dto/MemberEarningsResponse.java`
- `sacco-loan/src/main/resources/db/changelog/loan/003-create-loan-benefits-table.yaml`
- `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml` (add include)

**Action:**
Create `LoanBenefit` entity extending `BaseEntity` with fields: memberId (UUID), loanId (UUID), contributionSnapshot (BigDecimal), benefitsRate (BigDecimal), earnedAmount (BigDecimal), expectedEarnings (BigDecimal), distributed (boolean), distributedAt (Instant). Create Liquibase migration for `loan_benefits` table with indexes on memberId, loanId, and distributed. Create DTOs for benefit responses and member earnings aggregation. Add the changelog include to db.changelog-master.yaml.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-loan -q -DskipTests
```

**Done when:** Entity compiles, migration YAML is valid, DTOs are created.

### Task 2: Create LoanBenefitService with Distribution Logic
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBenefitService.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBenefitServiceImpl.java`
- `sacco-common/src/main/java/com/innercircle/sacco/common/event/BenefitsDistributedEvent.java`

**Action:**
Create service interface and implementation. Key methods:
- `distributeInterestEarnings(UUID loanId, BigDecimal interestAmount)` — queries all active members, calculates proportional share based on each member's shareBalance relative to total shares, creates LoanBenefit records
- `getMemberEarnings(UUID memberId)` — returns aggregated earnings for a member
- `getAllEarnings()` — returns all earnings with pagination
- `refreshBeneficiaries(UUID loanId)` — recalculates benefits when share balances change

The distribution uses `@EventListener` on `LoanRepaymentEvent` to automatically distribute interest portions. Use JdbcTemplate to query member share balances (since sacco-loan cannot depend on sacco-member). Publish `BenefitsDistributedEvent` after distribution.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-common,sacco-loan -q -DskipTests
```

**Done when:** Service compiles, event listener is registered, distribution logic is correct.

### Task 3: Create LoanBenefitController with Endpoints
**Files:**
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/controller/LoanBenefitController.java`

**Action:**
Create REST controller at `/api/v1/loan-benefits` with endpoints:
- `GET /api/v1/loan-benefits/member/{memberId}` — Get member's earnings summary
- `GET /api/v1/loan-benefits/loan/{loanId}` — Get benefits distributed for a loan
- `GET /api/v1/loan-benefits` — List all benefits (cursor pagination)
- `POST /api/v1/loan-benefits/refresh/{loanId}` — Refresh beneficiaries for a loan (ADMIN/TREASURER only)

All endpoints return `ApiResponse<T>`. Use `@PreAuthorize` for admin endpoints.

**Verify:**
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn compile -pl sacco-loan -q -DskipTests
```

**Done when:** Controller compiles, all endpoints defined, security annotations applied.

## Verification

After all tasks:
```bash
cd /Users/arnoldkizzitoh/Documents/haha/innercircle-sacco && mvn clean compile -q -DskipTests
```

## Commit Message
```
feat(loan): add loan benefits and earnings distribution system

- LoanBenefit entity with proportional distribution logic
- Auto-distribute interest earnings on repayment events
- Member earnings aggregation and refresh endpoints
- Liquibase migration for loan_benefits table
```

---
*Planned: 2026-02-15*
