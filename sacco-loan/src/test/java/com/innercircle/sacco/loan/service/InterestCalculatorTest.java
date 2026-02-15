package com.innercircle.sacco.loan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InterestCalculatorTest {

    private InterestCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new InterestCalculator();
    }

    // -------------------------------------------------------------------------
    // calculateFlatRate
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("calculateFlatRate")
    class CalculateFlatRate {

        @Test
        @DisplayName("should calculate flat rate interest for standard loan")
        void shouldCalculateFlatRateInterest() {
            // Principal = 100,000, Rate = 12%, Term = 12 months
            // Interest = 100,000 * 0.12 * (12/12) = 12,000
            BigDecimal result = calculator.calculateFlatRate(
                    new BigDecimal("100000"), new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(new BigDecimal("12000.00"));
        }

        @Test
        @DisplayName("should calculate flat rate for partial year term")
        void shouldCalculateFlatRatePartialYear() {
            // Principal = 100,000, Rate = 12%, Term = 6 months
            // Interest = 100,000 * 0.12 * (6/12) = 6,000
            BigDecimal result = calculator.calculateFlatRate(
                    new BigDecimal("100000"), new BigDecimal("12"), 6);
            assertThat(result).isEqualByComparingTo(new BigDecimal("6000.00"));
        }

        @Test
        @DisplayName("should calculate flat rate for multi-year term")
        void shouldCalculateFlatRateMultiYear() {
            // Principal = 50,000, Rate = 10%, Term = 24 months
            // Interest = 50,000 * 0.10 * (24/12) = 10,000
            BigDecimal result = calculator.calculateFlatRate(
                    new BigDecimal("50000"), new BigDecimal("10"), 24);
            assertThat(result).isEqualByComparingTo(new BigDecimal("10000.00"));
        }

        @Test
        @DisplayName("should return zero for zero principal")
        void shouldReturnZeroForZeroPrincipal() {
            BigDecimal result = calculator.calculateFlatRate(
                    BigDecimal.ZERO, new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for negative principal")
        void shouldReturnZeroForNegativePrincipal() {
            BigDecimal result = calculator.calculateFlatRate(
                    new BigDecimal("-100000"), new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for zero term months")
        void shouldReturnZeroForZeroTermMonths() {
            BigDecimal result = calculator.calculateFlatRate(
                    new BigDecimal("100000"), new BigDecimal("12"), 0);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for negative term months")
        void shouldReturnZeroForNegativeTermMonths() {
            BigDecimal result = calculator.calculateFlatRate(
                    new BigDecimal("100000"), new BigDecimal("12"), -1);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should handle zero interest rate")
        void shouldHandleZeroInterestRate() {
            BigDecimal result = calculator.calculateFlatRate(
                    new BigDecimal("100000"), BigDecimal.ZERO, 12);
            assertThat(result).isEqualByComparingTo(new BigDecimal("0.00"));
        }

        @Test
        @DisplayName("should handle very small amounts with proper scale")
        void shouldHandleVerySmallAmounts() {
            BigDecimal result = calculator.calculateFlatRate(
                    new BigDecimal("100"), new BigDecimal("5"), 1);
            // Interest = 100 * 0.05 * (1/12) = 0.42
            assertThat(result.scale()).isEqualTo(2);
            assertThat(result.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        }
    }

    // -------------------------------------------------------------------------
    // calculateReducingBalance
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("calculateReducingBalance")
    class CalculateReducingBalance {

        @Test
        @DisplayName("should calculate reducing balance interest for standard loan")
        void shouldCalculateReducingBalanceInterest() {
            BigDecimal result = calculator.calculateReducingBalance(
                    new BigDecimal("100000"), new BigDecimal("12"), 12);
            // Reducing balance produces less interest than flat rate
            assertThat(result).isGreaterThan(BigDecimal.ZERO);
            BigDecimal flatRateInterest = calculator.calculateFlatRate(
                    new BigDecimal("100000"), new BigDecimal("12"), 12);
            assertThat(result).isLessThan(flatRateInterest);
        }

        @Test
        @DisplayName("should return zero for zero principal")
        void shouldReturnZeroForZeroPrincipal() {
            BigDecimal result = calculator.calculateReducingBalance(
                    BigDecimal.ZERO, new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for negative principal")
        void shouldReturnZeroForNegativePrincipal() {
            BigDecimal result = calculator.calculateReducingBalance(
                    new BigDecimal("-100000"), new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for zero term months")
        void shouldReturnZeroForZeroTermMonths() {
            BigDecimal result = calculator.calculateReducingBalance(
                    new BigDecimal("100000"), new BigDecimal("12"), 0);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for negative term months")
        void shouldReturnZeroForNegativeTermMonths() {
            BigDecimal result = calculator.calculateReducingBalance(
                    new BigDecimal("100000"), new BigDecimal("12"), -5);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should produce proper scale of 2")
        void shouldProduceProperScale() {
            BigDecimal result = calculator.calculateReducingBalance(
                    new BigDecimal("100000"), new BigDecimal("12"), 12);
            assertThat(result.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle large principal and long term")
        void shouldHandleLargePrincipalAndLongTerm() {
            BigDecimal result = calculator.calculateReducingBalance(
                    new BigDecimal("10000000"), new BigDecimal("18"), 60);
            assertThat(result).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle single month term")
        void shouldHandleSingleMonthTerm() {
            BigDecimal result = calculator.calculateReducingBalance(
                    new BigDecimal("100000"), new BigDecimal("12"), 1);
            // For 1 month, reducing balance interest = principal * monthlyRate
            // monthlyRate = 12/100/12 = 0.01 => interest = 100000 * 0.01 = 1000
            assertThat(result).isGreaterThan(BigDecimal.ZERO);
        }
    }

    // -------------------------------------------------------------------------
    // calculateReducingBalanceMonthlyPayment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("calculateReducingBalanceMonthlyPayment")
    class CalculateReducingBalanceMonthlyPayment {

        @Test
        @DisplayName("should calculate monthly payment for standard loan")
        void shouldCalculateMonthlyPayment() {
            BigDecimal result = calculator.calculateReducingBalanceMonthlyPayment(
                    new BigDecimal("100000"), new BigDecimal("12"), 12);
            assertThat(result).isGreaterThan(BigDecimal.ZERO);
            // Monthly payment * 12 should exceed principal (includes interest)
            BigDecimal total = result.multiply(BigDecimal.valueOf(12));
            assertThat(total).isGreaterThan(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("should return zero for zero principal")
        void shouldReturnZeroForZeroPrincipal() {
            BigDecimal result = calculator.calculateReducingBalanceMonthlyPayment(
                    BigDecimal.ZERO, new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for negative principal")
        void shouldReturnZeroForNegativePrincipal() {
            BigDecimal result = calculator.calculateReducingBalanceMonthlyPayment(
                    new BigDecimal("-100"), new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for zero term")
        void shouldReturnZeroForZeroTerm() {
            BigDecimal result = calculator.calculateReducingBalanceMonthlyPayment(
                    new BigDecimal("100000"), new BigDecimal("12"), 0);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should have scale of 2")
        void shouldHaveScaleOfTwo() {
            BigDecimal result = calculator.calculateReducingBalanceMonthlyPayment(
                    new BigDecimal("100000"), new BigDecimal("12"), 12);
            assertThat(result.scale()).isEqualTo(2);
        }
    }

    // -------------------------------------------------------------------------
    // calculateFlatRateMonthlyPayment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("calculateFlatRateMonthlyPayment")
    class CalculateFlatRateMonthlyPayment {

        @Test
        @DisplayName("should calculate flat rate monthly payment")
        void shouldCalculateFlatRateMonthlyPayment() {
            // Principal = 120,000, Rate = 12%, Term = 12 months
            // Total interest = 120,000 * 0.12 = 14,400
            // Total amount = 134,400
            // Monthly = 134,400 / 12 = 11,200
            BigDecimal result = calculator.calculateFlatRateMonthlyPayment(
                    new BigDecimal("120000"), new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(new BigDecimal("11200.00"));
        }

        @Test
        @DisplayName("should return zero for zero principal")
        void shouldReturnZeroForZeroPrincipal() {
            BigDecimal result = calculator.calculateFlatRateMonthlyPayment(
                    BigDecimal.ZERO, new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for negative principal")
        void shouldReturnZeroForNegativePrincipal() {
            BigDecimal result = calculator.calculateFlatRateMonthlyPayment(
                    new BigDecimal("-100"), new BigDecimal("12"), 12);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero for zero term")
        void shouldReturnZeroForZeroTerm() {
            BigDecimal result = calculator.calculateFlatRateMonthlyPayment(
                    new BigDecimal("100000"), new BigDecimal("12"), 0);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should have scale of 2")
        void shouldHaveScaleOfTwo() {
            BigDecimal result = calculator.calculateFlatRateMonthlyPayment(
                    new BigDecimal("100000"), new BigDecimal("12"), 12);
            assertThat(result.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("monthly payment times term should equal total")
        void monthlyPaymentTimesTermShouldEqualTotal() {
            BigDecimal principal = new BigDecimal("120000");
            BigDecimal rate = new BigDecimal("12");
            int termMonths = 12;

            BigDecimal monthlyPayment = calculator.calculateFlatRateMonthlyPayment(principal, rate, termMonths);
            BigDecimal totalInterest = calculator.calculateFlatRate(principal, rate, termMonths);
            BigDecimal expectedTotal = principal.add(totalInterest);

            BigDecimal actualTotal = monthlyPayment.multiply(BigDecimal.valueOf(termMonths));
            assertThat(actualTotal).isEqualByComparingTo(expectedTotal);
        }
    }

    // -------------------------------------------------------------------------
    // Cross-method comparisons
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Cross-method comparisons")
    class CrossMethodComparisons {

        @Test
        @DisplayName("reducing balance should produce less total interest than flat rate")
        void reducingBalanceShouldBeLessThanFlatRate() {
            BigDecimal principal = new BigDecimal("100000");
            BigDecimal rate = new BigDecimal("15");
            int term = 24;

            BigDecimal reducingInterest = calculator.calculateReducingBalance(principal, rate, term);
            BigDecimal flatInterest = calculator.calculateFlatRate(principal, rate, term);

            assertThat(reducingInterest).isLessThan(flatInterest);
        }

        @Test
        @DisplayName("reducing balance monthly payment should be less than flat rate monthly payment")
        void reducingBalanceMonthlyPaymentShouldBeLessThanFlatRate() {
            BigDecimal principal = new BigDecimal("100000");
            BigDecimal rate = new BigDecimal("15");
            int term = 24;

            BigDecimal reducingMonthly = calculator.calculateReducingBalanceMonthlyPayment(principal, rate, term);
            BigDecimal flatMonthly = calculator.calculateFlatRateMonthlyPayment(principal, rate, term);

            assertThat(reducingMonthly).isLessThan(flatMonthly);
        }
    }
}
