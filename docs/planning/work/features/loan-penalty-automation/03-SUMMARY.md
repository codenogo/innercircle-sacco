# Plan 03 Summary

## Outcome
✅ Complete

## Changes Made
| File | Change |
|------|--------|
| `sacco-loan/src/main/java/.../service/LoanPenaltyService.java` | Added `markPenaltyPaid()` and `payPenalties()` interface methods |
| `sacco-loan/src/main/java/.../service/LoanPenaltyServiceImpl.java` | Implemented atomic penalty payment with oldest-first ordering |
| `sacco-loan/src/main/java/.../repository/LoanPenaltyRepository.java` | Added `findByLoanIdAndPaidFalseOrderByAppliedAtAsc` query |
| `sacco-common/src/main/java/.../event/PenaltyPaidEvent.java` | New event record for penalty payment tracking |
| `sacco-common/src/main/java/.../event/LoanRepaymentEvent.java` | Added `penaltyPortion` field (8th constructor param) |
| `sacco-loan/src/main/java/.../entity/LoanRepayment.java` | Added `penaltyPortion` field |
| `sacco-loan/src/main/resources/db/changelog/loan/008-add-penalty-portion-to-repayments.yaml` | Migration for penalty_portion column |
| `sacco-app/src/main/resources/db/changelog/db.changelog-master.yaml` | Registered new migration |
| `sacco-loan/src/main/java/.../service/LoanServiceImpl.java` | Inserted penalty allocation step in `recordRepayment()` between interest and principal |
| `sacco-ledger/src/main/java/.../listener/FinancialEventListener.java` | Added CR Penalty Income journal line in repayment handler |
| `sacco-loan/src/main/java/.../dto/LoanResponse.java` | Added `totalPenalties` field and mapping in `from()` |
| `sacco-loan/src/test/java/.../service/LoanServiceImplTest.java` | Added LoanPenaltyService mock, LoanInterestHistoryRepository mock, and 2 penalty allocation tests |
| `sacco-common/src/test/java/.../event/LoanRepaymentEventTest.java` | Updated constructor calls for 8-arg record |
| `sacco-loan/src/test/java/.../service/LoanBenefitServiceImplTest.java` | Updated constructor calls for 8-arg record |
| `sacco-ledger/src/test/java/.../listener/FinancialEventListenerTest.java` | Updated constructor calls for 8-arg record + penalty journal line |

## Verification Results
- Task 2 (Penalty payment methods): ✅ `mvn compile` passed
- Task 1 (Repayment allocation): ✅ `mvn compile` passed, `mvn test -pl sacco-loan` 256 tests passed
- Task 3 (DTO + tests): ✅ `mvn test -pl sacco-loan,sacco-config,sacco-ledger -am -q` passed
- Plan verification: ✅ All tests pass

## Issues Encountered
- `LoanRepaymentEvent` is a Java record — adding `penaltyPortion` broke all callers (6+ test files). Fixed by updating all constructor calls.
- New penalty allocation tests triggered NPE on `interestHistoryRepository.save()` because `@InjectMocks` injected null for the unmocked repository. Fixed by adding `@Mock LoanInterestHistoryRepository`.
- Used `lenient()` stubs for `loanPenaltyService.payPenalties()` in `@BeforeEach` to avoid Mockito strict-stubs errors for tests that throw before reaching penalty code.

## Commit
`pending` - feat(loan-penalty): repayment allocation with Interest -> Penalties -> Principal

---
*Implemented: 2026-02-15*
