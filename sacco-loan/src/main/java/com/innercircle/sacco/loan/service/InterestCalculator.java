package com.innercircle.sacco.loan.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class InterestCalculator {

    /**
     * Calculate total interest for reducing balance method.
     * Formula: Uses monthly reducing balance where interest is calculated on outstanding principal.
     */
    public BigDecimal calculateReducingBalance(BigDecimal principal, BigDecimal annualRate, Integer termMonths) {
        if (principal.compareTo(BigDecimal.ZERO) <= 0 || termMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        // Calculate monthly payment using amortization formula
        // M = P * [r(1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = onePlusRate.pow(termMonths);

        BigDecimal monthlyPayment = principal
                .multiply(monthlyRate.multiply(onePlusRatePowerN))
                .divide(onePlusRatePowerN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

        // Total interest = (monthly payment * term) - principal
        BigDecimal totalPayment = monthlyPayment.multiply(BigDecimal.valueOf(termMonths));
        return totalPayment.subtract(principal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total interest for flat rate method.
     * Formula: Interest = Principal * Rate * (Term / 12)
     */
    public BigDecimal calculateFlatRate(BigDecimal principal, BigDecimal annualRate, Integer termMonths) {
        if (principal.compareTo(BigDecimal.ZERO) <= 0 || termMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = annualRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal termInYears = BigDecimal.valueOf(termMonths)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        return principal.multiply(rate).multiply(termInYears).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate monthly payment amount for reducing balance.
     */
    public BigDecimal calculateReducingBalanceMonthlyPayment(BigDecimal principal, BigDecimal annualRate, Integer termMonths) {
        if (principal.compareTo(BigDecimal.ZERO) <= 0 || termMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = onePlusRate.pow(termMonths);

        return principal
                .multiply(monthlyRate.multiply(onePlusRatePowerN))
                .divide(onePlusRatePowerN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate monthly payment amount for flat rate.
     */
    public BigDecimal calculateFlatRateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, Integer termMonths) {
        if (principal.compareTo(BigDecimal.ZERO) <= 0 || termMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalInterest = calculateFlatRate(principal, annualRate, termMonths);
        BigDecimal totalAmount = principal.add(totalInterest);

        return totalAmount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
    }
}
