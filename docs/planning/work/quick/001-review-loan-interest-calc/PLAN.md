# Quick: Review Loan Interest Calculation

## Goal
Review the loan interest calculation logic for correctness, edge case handling, and precision.

## Files
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/InterestCalculator.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/RepaymentScheduleGenerator.java`
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`
- `sacco-loan/src/test/java/com/innercircle/sacco/loan/service/InterestCalculatorTest.java`
- `sacco-loan/src/test/java/com/innercircle/sacco/loan/service/RepaymentScheduleGeneratorTest.java`

## Approach
Read-only code review of all interest calculation and repayment schedule code. Verify mathematical correctness of flat-rate and reducing-balance formulas, edge case handling, BigDecimal precision, and test coverage.

## Verify
```bash
mvn test -pl sacco-loan -q
```
