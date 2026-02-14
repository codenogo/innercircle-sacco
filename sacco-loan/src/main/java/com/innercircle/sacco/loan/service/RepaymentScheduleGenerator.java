package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RepaymentScheduleGenerator {

    private final InterestCalculator interestCalculator;

    public List<RepaymentSchedule> generateSchedule(
            UUID loanId,
            BigDecimal principal,
            BigDecimal annualRate,
            Integer termMonths,
            String interestMethod,
            LocalDate disbursementDate) {

        List<RepaymentSchedule> schedules = new ArrayList<>();

        if ("REDUCING_BALANCE".equals(interestMethod)) {
            schedules = generateReducingBalanceSchedule(loanId, principal, annualRate, termMonths, disbursementDate);
        } else if ("FLAT_RATE".equals(interestMethod)) {
            schedules = generateFlatRateSchedule(loanId, principal, annualRate, termMonths, disbursementDate);
        }

        return schedules;
    }

    private List<RepaymentSchedule> generateReducingBalanceSchedule(
            UUID loanId,
            BigDecimal principal,
            BigDecimal annualRate,
            Integer termMonths,
            LocalDate disbursementDate) {

        List<RepaymentSchedule> schedules = new ArrayList<>();
        BigDecimal monthlyPayment = interestCalculator.calculateReducingBalanceMonthlyPayment(
                principal, annualRate, termMonths);

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = principal;

        for (int i = 1; i <= termMonths; i++) {
            LocalDate dueDate = disbursementDate.plusMonths(i);

            // Interest on remaining principal
            BigDecimal interestAmount = remainingPrincipal.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            // Principal portion = monthly payment - interest
            BigDecimal principalAmount = monthlyPayment.subtract(interestAmount);

            // For last installment, adjust to cover any rounding differences
            if (i == termMonths) {
                principalAmount = remainingPrincipal;
                BigDecimal totalAmount = principalAmount.add(interestAmount);

                RepaymentSchedule schedule = new RepaymentSchedule();
                schedule.setLoanId(loanId);
                schedule.setInstallmentNumber(i);
                schedule.setDueDate(dueDate);
                schedule.setPrincipalAmount(principalAmount);
                schedule.setInterestAmount(interestAmount);
                schedule.setTotalAmount(totalAmount);
                schedule.setPaid(false);

                schedules.add(schedule);
                break;
            }

            remainingPrincipal = remainingPrincipal.subtract(principalAmount);

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoanId(loanId);
            schedule.setInstallmentNumber(i);
            schedule.setDueDate(dueDate);
            schedule.setPrincipalAmount(principalAmount);
            schedule.setInterestAmount(interestAmount);
            schedule.setTotalAmount(monthlyPayment);
            schedule.setPaid(false);

            schedules.add(schedule);
        }

        return schedules;
    }

    private List<RepaymentSchedule> generateFlatRateSchedule(
            UUID loanId,
            BigDecimal principal,
            BigDecimal annualRate,
            Integer termMonths,
            LocalDate disbursementDate) {

        List<RepaymentSchedule> schedules = new ArrayList<>();

        BigDecimal totalInterest = interestCalculator.calculateFlatRate(principal, annualRate, termMonths);
        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyInterest = totalInterest.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = monthlyPrincipal.add(monthlyInterest);

        BigDecimal totalPrincipalAllocated = BigDecimal.ZERO;
        BigDecimal totalInterestAllocated = BigDecimal.ZERO;

        for (int i = 1; i <= termMonths; i++) {
            LocalDate dueDate = disbursementDate.plusMonths(i);

            BigDecimal principalAmount = monthlyPrincipal;
            BigDecimal interestAmount = monthlyInterest;

            // For last installment, adjust for rounding differences
            if (i == termMonths) {
                principalAmount = principal.subtract(totalPrincipalAllocated);
                interestAmount = totalInterest.subtract(totalInterestAllocated);
            }

            totalPrincipalAllocated = totalPrincipalAllocated.add(principalAmount);
            totalInterestAllocated = totalInterestAllocated.add(interestAmount);

            BigDecimal totalAmount = principalAmount.add(interestAmount);

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoanId(loanId);
            schedule.setInstallmentNumber(i);
            schedule.setDueDate(dueDate);
            schedule.setPrincipalAmount(principalAmount);
            schedule.setInterestAmount(interestAmount);
            schedule.setTotalAmount(totalAmount);
            schedule.setPaid(false);

            schedules.add(schedule);
        }

        return schedules;
    }
}
