package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RepaymentScheduleGeneratorTest {

    private RepaymentScheduleGenerator generator;
    private InterestCalculator interestCalculator;

    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final LocalDate DISBURSEMENT_DATE = LocalDate.of(2025, 1, 15);

    @BeforeEach
    void setUp() {
        interestCalculator = new InterestCalculator();
        generator = new RepaymentScheduleGenerator(interestCalculator);
    }

    // -------------------------------------------------------------------------
    // Flat Rate Schedule
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("generateSchedule - FLAT_RATE")
    class FlatRateSchedule {

        @Test
        @DisplayName("should generate correct number of installments")
        void shouldGenerateCorrectNumberOfInstallments() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("120000"), new BigDecimal("12"),
                    12, "FLAT_RATE", DISBURSEMENT_DATE);
            assertThat(schedules).hasSize(12);
        }

        @Test
        @DisplayName("should set loan ID on all schedules")
        void shouldSetLoanIdOnAllSchedules() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("120000"), new BigDecimal("12"),
                    12, "FLAT_RATE", DISBURSEMENT_DATE);
            assertThat(schedules).allSatisfy(s -> assertThat(s.getLoanId()).isEqualTo(LOAN_ID));
        }

        @Test
        @DisplayName("should set installment numbers sequentially from 1")
        void shouldSetInstallmentNumbersSequentially() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("120000"), new BigDecimal("12"),
                    6, "FLAT_RATE", DISBURSEMENT_DATE);
            for (int i = 0; i < schedules.size(); i++) {
                assertThat(schedules.get(i).getInstallmentNumber()).isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("should set due dates monthly from disbursement date")
        void shouldSetDueDatesMonthly() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("120000"), new BigDecimal("12"),
                    6, "FLAT_RATE", DISBURSEMENT_DATE);
            for (int i = 0; i < schedules.size(); i++) {
                assertThat(schedules.get(i).getDueDate())
                        .isEqualTo(DISBURSEMENT_DATE.plusMonths(i + 1));
            }
        }

        @Test
        @DisplayName("should mark all installments as unpaid")
        void shouldMarkAllAsUnpaid() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("120000"), new BigDecimal("12"),
                    12, "FLAT_RATE", DISBURSEMENT_DATE);
            assertThat(schedules).allSatisfy(s -> assertThat(s.getPaid()).isFalse());
        }

        @Test
        @DisplayName("total of all principal amounts should equal principal")
        void totalPrincipalShouldEqualLoanPrincipal() {
            BigDecimal principal = new BigDecimal("120000");
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, principal, new BigDecimal("12"),
                    12, "FLAT_RATE", DISBURSEMENT_DATE);
            BigDecimal totalPrincipal = schedules.stream()
                    .map(RepaymentSchedule::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalPrincipal).isEqualByComparingTo(principal);
        }

        @Test
        @DisplayName("total of all interest amounts should equal flat rate interest")
        void totalInterestShouldEqualFlatRateInterest() {
            BigDecimal principal = new BigDecimal("120000");
            BigDecimal rate = new BigDecimal("12");
            int term = 12;
            BigDecimal expectedInterest = interestCalculator.calculateFlatRate(principal, rate, term);

            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, principal, rate, term, "FLAT_RATE", DISBURSEMENT_DATE);
            BigDecimal totalInterest = schedules.stream()
                    .map(RepaymentSchedule::getInterestAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalInterest).isEqualByComparingTo(expectedInterest);
        }

        @Test
        @DisplayName("totalAmount should equal principalAmount + interestAmount for each installment")
        void totalAmountShouldEqualPrincipalPlusInterest() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("120000"), new BigDecimal("12"),
                    12, "FLAT_RATE", DISBURSEMENT_DATE);
            for (RepaymentSchedule schedule : schedules) {
                BigDecimal expected = schedule.getPrincipalAmount().add(schedule.getInterestAmount());
                assertThat(schedule.getTotalAmount()).isEqualByComparingTo(expected);
            }
        }

        @Test
        @DisplayName("should handle single month term")
        void shouldHandleSingleMonthTerm() {
            BigDecimal principal = new BigDecimal("100000");
            BigDecimal rate = new BigDecimal("12");
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, principal, rate, 1, "FLAT_RATE", DISBURSEMENT_DATE);
            assertThat(schedules).hasSize(1);
            assertThat(schedules.get(0).getPrincipalAmount()).isEqualByComparingTo(principal);
        }

        @Test
        @DisplayName("should handle rounding adjustments in last installment")
        void shouldHandleRoundingInLastInstallment() {
            // Use values that cause rounding issues (e.g., 7 months)
            BigDecimal principal = new BigDecimal("100000");
            BigDecimal rate = new BigDecimal("10");
            int term = 7;

            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, principal, rate, term, "FLAT_RATE", DISBURSEMENT_DATE);

            assertThat(schedules).hasSize(term);
            BigDecimal totalPrincipal = schedules.stream()
                    .map(RepaymentSchedule::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalPrincipal).isEqualByComparingTo(principal);
        }
    }

    // -------------------------------------------------------------------------
    // Reducing Balance Schedule
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("generateSchedule - REDUCING_BALANCE")
    class ReducingBalanceSchedule {

        @Test
        @DisplayName("should generate correct number of installments")
        void shouldGenerateCorrectNumberOfInstallments() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    12, "REDUCING_BALANCE", DISBURSEMENT_DATE);
            assertThat(schedules).hasSize(12);
        }

        @Test
        @DisplayName("should set loan ID on all schedules")
        void shouldSetLoanIdOnAllSchedules() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    12, "REDUCING_BALANCE", DISBURSEMENT_DATE);
            assertThat(schedules).allSatisfy(s -> assertThat(s.getLoanId()).isEqualTo(LOAN_ID));
        }

        @Test
        @DisplayName("should set installment numbers sequentially")
        void shouldSetInstallmentNumbersSequentially() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    6, "REDUCING_BALANCE", DISBURSEMENT_DATE);
            for (int i = 0; i < schedules.size(); i++) {
                assertThat(schedules.get(i).getInstallmentNumber()).isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("should set due dates monthly from disbursement date")
        void shouldSetDueDatesMonthly() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    6, "REDUCING_BALANCE", DISBURSEMENT_DATE);
            for (int i = 0; i < schedules.size(); i++) {
                assertThat(schedules.get(i).getDueDate())
                        .isEqualTo(DISBURSEMENT_DATE.plusMonths(i + 1));
            }
        }

        @Test
        @DisplayName("should mark all installments as unpaid")
        void shouldMarkAllAsUnpaid() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    12, "REDUCING_BALANCE", DISBURSEMENT_DATE);
            assertThat(schedules).allSatisfy(s -> assertThat(s.getPaid()).isFalse());
        }

        @Test
        @DisplayName("interest portion should decrease over installments")
        void interestPortionShouldDecrease() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    12, "REDUCING_BALANCE", DISBURSEMENT_DATE);
            // Check that interest generally decreases (last vs first, skip last since adjusted)
            BigDecimal firstInterest = schedules.get(0).getInterestAmount();
            BigDecimal secondToLastInterest = schedules.get(schedules.size() - 2).getInterestAmount();
            assertThat(firstInterest).isGreaterThan(secondToLastInterest);
        }

        @Test
        @DisplayName("principal portion should increase over installments")
        void principalPortionShouldIncrease() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    12, "REDUCING_BALANCE", DISBURSEMENT_DATE);
            BigDecimal firstPrincipal = schedules.get(0).getPrincipalAmount();
            BigDecimal secondToLastPrincipal = schedules.get(schedules.size() - 2).getPrincipalAmount();
            assertThat(secondToLastPrincipal).isGreaterThan(firstPrincipal);
        }

        @Test
        @DisplayName("should handle single month term")
        void shouldHandleSingleMonthTerm() {
            BigDecimal principal = new BigDecimal("100000");
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, principal, new BigDecimal("12"),
                    1, "REDUCING_BALANCE", DISBURSEMENT_DATE);
            assertThat(schedules).hasSize(1);
            // For single month, principal portion of the last installment = full principal
            assertThat(schedules.get(0).getPrincipalAmount()).isEqualByComparingTo(principal);
        }

        @Test
        @DisplayName("totalAmount should equal principalAmount + interestAmount for each installment")
        void totalAmountShouldEqualPrincipalPlusInterest() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    12, "REDUCING_BALANCE", DISBURSEMENT_DATE);
            for (RepaymentSchedule schedule : schedules) {
                BigDecimal expected = schedule.getPrincipalAmount().add(schedule.getInterestAmount());
                assertThat(schedule.getTotalAmount()).isEqualByComparingTo(expected);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Invalid / unknown interest methods
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("generateSchedule - invalid method")
    class InvalidMethod {

        @Test
        @DisplayName("should return empty list for unknown interest method")
        void shouldReturnEmptyForUnknownMethod() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    12, "UNKNOWN_METHOD", DISBURSEMENT_DATE);
            assertThat(schedules).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for null interest method")
        void shouldReturnEmptyForNullMethod() {
            List<RepaymentSchedule> schedules = generator.generateSchedule(
                    LOAN_ID, new BigDecimal("100000"), new BigDecimal("12"),
                    12, null, DISBURSEMENT_DATE);
            assertThat(schedules).isEmpty();
        }
    }
}
