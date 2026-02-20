package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.LoanDisbursedEvent;
import com.innercircle.sacco.common.event.LoanRepaymentEvent;
import com.innercircle.sacco.common.exception.InvalidStateTransitionException;
import com.innercircle.sacco.common.exception.MakerCheckerViolationException;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.service.ConfigService;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.entity.RepaymentStatus;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.LoanInterestHistoryRepository;
import com.innercircle.sacco.loan.repository.LoanRepaymentRepository;
import com.innercircle.sacco.loan.repository.RepaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock
    private LoanApplicationRepository loanRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private LoanRepaymentRepository repaymentRepository;

    @Mock
    private LoanInterestHistoryRepository interestHistoryRepository;

    @Mock
    private InterestCalculator interestCalculator;

    @Mock
    private RepaymentScheduleGenerator scheduleGenerator;

    @Mock
    private EventOutboxWriter outboxWriter;

    @Mock
    private ConfigService configService;

    @Mock
    private LoanPenaltyService loanPenaltyService;

    @InjectMocks
    private LoanServiceImpl loanService;

    @Captor
    private ArgumentCaptor<LoanApplication> loanCaptor;

    @Captor
    private ArgumentCaptor<LoanRepayment> repaymentCaptor;

    private UUID memberId;
    private UUID loanId;
    private UUID approverId;
    private UUID loanProductId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        loanId = UUID.randomUUID();
        approverId = UUID.randomUUID();
        loanProductId = UUID.randomUUID();
    }

    private LoanProductConfig createLoanProduct(InterestMethod method, BigDecimal rate,
                                                  BigDecimal maxAmount, int maxTermMonths) {
        LoanProductConfig product = new LoanProductConfig();
        product.setId(loanProductId);
        product.setName("Test Loan Product");
        product.setInterestMethod(method);
        product.setAnnualInterestRate(rate);
        product.setMaxAmount(maxAmount);
        product.setMaxTermMonths(maxTermMonths);
        product.setActive(true);
        return product;
    }

    // -------------------------------------------------------------------------
    // applyForLoan
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("applyForLoan")
    class ApplyForLoan {

        @Test
        @DisplayName("should create loan application with PENDING status")
        void shouldCreateLoanWithPendingStatus() {
            LoanProductConfig product = createLoanProduct(InterestMethod.FLAT_RATE,
                    new BigDecimal("12"), new BigDecimal("500000"), 24);
            when(configService.getLoanProduct(loanProductId)).thenReturn(product);
            when(loanRepository.existsByLoanNumber(anyString())).thenReturn(false);
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = loanService.applyForLoan(
                    memberId, loanProductId, new BigDecimal("100000"), 12, "Business", "treasurer");

            verify(loanRepository).save(loanCaptor.capture());
            LoanApplication saved = loanCaptor.getValue();

            assertThat(saved.getMemberId()).isEqualTo(memberId);
            assertThat(saved.getLoanProductId()).isEqualTo(loanProductId);
            assertThat(saved.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(saved.getInterestRate()).isEqualByComparingTo(new BigDecimal("12"));
            assertThat(saved.getTermMonths()).isEqualTo(12);
            assertThat(saved.getInterestMethod()).isEqualTo(InterestMethod.FLAT_RATE);
            assertThat(saved.getPurpose()).isEqualTo("Business");
            assertThat(saved.getStatus()).isEqualTo(LoanStatus.PENDING);
            assertThat(saved.getTotalRepaid()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getLoanNumber()).startsWith("LN-");
            assertThat(saved.getLoanNumber()).hasSize(11);
        }

        @Test
        @DisplayName("should set createdBy to actor when applying for loan")
        void shouldSetCreatedByToActor() {
            LoanProductConfig product = createLoanProduct(InterestMethod.FLAT_RATE,
                    new BigDecimal("12"), new BigDecimal("500000"), 24);
            when(configService.getLoanProduct(loanProductId)).thenReturn(product);
            when(loanRepository.existsByLoanNumber(anyString())).thenReturn(false);
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.applyForLoan(memberId, loanProductId, new BigDecimal("100000"), 12, "Business", "maker@sacco");

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getCreatedBy()).isEqualTo("maker@sacco");
        }

        @Test
        @DisplayName("should accept REDUCING_BALANCE interest method from product config")
        void shouldAcceptReducingBalance() {
            LoanProductConfig product = createLoanProduct(InterestMethod.REDUCING_BALANCE,
                    new BigDecimal("12"), new BigDecimal("500000"), 24);
            when(configService.getLoanProduct(loanProductId)).thenReturn(product);
            when(loanRepository.existsByLoanNumber(anyString())).thenReturn(false);
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = loanService.applyForLoan(
                    memberId, loanProductId, new BigDecimal("100000"), 12, null, "treasurer");

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getInterestMethod()).isEqualTo(InterestMethod.REDUCING_BALANCE);
        }

        @Test
        @DisplayName("should retry loan number generation after collision")
        void shouldRetryLoanNumberGenerationAfterCollision() {
            LoanProductConfig product = createLoanProduct(InterestMethod.FLAT_RATE,
                    new BigDecimal("12"), new BigDecimal("500000"), 24);
            when(configService.getLoanProduct(loanProductId)).thenReturn(product);
            when(loanRepository.existsByLoanNumber(anyString()))
                    .thenReturn(true, false);
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = loanService.applyForLoan(
                    memberId, loanProductId, new BigDecimal("100000"), 12, "Business", "treasurer");

            assertThat(result.getLoanNumber()).startsWith("LN-");
            verify(loanRepository).save(any(LoanApplication.class));
        }

        @Test
        @DisplayName("should throw for zero principal amount")
        void shouldThrowForZeroPrincipal() {
            assertThatThrownBy(() ->
                    loanService.applyForLoan(memberId, loanProductId, BigDecimal.ZERO, 12, null, "treasurer"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Principal amount must be greater than zero");
        }

        @Test
        @DisplayName("should throw for negative principal amount")
        void shouldThrowForNegativePrincipal() {
            assertThatThrownBy(() ->
                    loanService.applyForLoan(memberId, loanProductId, new BigDecimal("-1000"), 12, null, "treasurer"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Principal amount must be greater than zero");
        }

        @Test
        @DisplayName("should throw for zero term months")
        void shouldThrowForZeroTermMonths() {
            assertThatThrownBy(() ->
                    loanService.applyForLoan(memberId, loanProductId, new BigDecimal("100000"), 0, null, "treasurer"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Term months must be greater than zero");
        }

        @Test
        @DisplayName("should throw for negative term months")
        void shouldThrowForNegativeTermMonths() {
            assertThatThrownBy(() ->
                    loanService.applyForLoan(memberId, loanProductId, new BigDecimal("100000"), -5, null, "treasurer"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Term months must be greater than zero");
        }

        @Test
        @DisplayName("should throw for inactive loan product")
        void shouldThrowForInactiveLoanProduct() {
            LoanProductConfig product = createLoanProduct(InterestMethod.FLAT_RATE,
                    new BigDecimal("12"), new BigDecimal("500000"), 24);
            product.setActive(false);
            when(configService.getLoanProduct(loanProductId)).thenReturn(product);

            assertThatThrownBy(() ->
                    loanService.applyForLoan(memberId, loanProductId, new BigDecimal("100000"), 12, null, "treasurer"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan product is not active");
        }

        @Test
        @DisplayName("should throw when principal exceeds product max amount")
        void shouldThrowWhenPrincipalExceedsMax() {
            LoanProductConfig product = createLoanProduct(InterestMethod.FLAT_RATE,
                    new BigDecimal("12"), new BigDecimal("50000"), 24);
            when(configService.getLoanProduct(loanProductId)).thenReturn(product);

            assertThatThrownBy(() ->
                    loanService.applyForLoan(memberId, loanProductId, new BigDecimal("100000"), 12, null, "treasurer"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Principal amount exceeds maximum allowed");
        }

        @Test
        @DisplayName("should throw when term exceeds product max term")
        void shouldThrowWhenTermExceedsMax() {
            LoanProductConfig product = createLoanProduct(InterestMethod.FLAT_RATE,
                    new BigDecimal("12"), new BigDecimal("500000"), 12);
            when(configService.getLoanProduct(loanProductId)).thenReturn(product);

            assertThatThrownBy(() ->
                    loanService.applyForLoan(memberId, loanProductId, new BigDecimal("100000"), 24, null, "treasurer"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Term exceeds maximum allowed");
        }

        @Test
        @DisplayName("should throw when loan number generation is exhausted")
        void shouldThrowWhenLoanNumberGenerationExhausted() {
            LoanProductConfig product = createLoanProduct(InterestMethod.FLAT_RATE,
                    new BigDecimal("12"), new BigDecimal("500000"), 24);
            when(configService.getLoanProduct(loanProductId)).thenReturn(product);
            when(loanRepository.existsByLoanNumber(anyString())).thenReturn(true);

            assertThatThrownBy(() ->
                    loanService.applyForLoan(memberId, loanProductId, new BigDecimal("100000"), 12, "Business", "treasurer"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unable to generate unique loan number");
        }
    }

    // -------------------------------------------------------------------------
    // approveLoan
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("approveLoan")
    class ApproveLoan {

        @Test
        @DisplayName("should approve a pending loan by a different user")
        void shouldApprovePendingLoan() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            loan.setCreatedBy("maker");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = loanService.approveLoan(loanId, approverId, "checker", null, false);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
            assertThat(result.getApprovedBy()).isEqualTo(approverId);
            assertThat(result.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw MakerCheckerViolationException when creator tries to approve own loan")
        void shouldThrowWhenCreatorApprovesOwnLoan() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            loan.setCreatedBy("maker");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.approveLoan(loanId, approverId, "maker", null, false))
                    .isInstanceOf(MakerCheckerViolationException.class);
        }

        @Test
        @DisplayName("should allow ADMIN override when creator approves own loan with reason")
        void shouldAllowAdminOverrideWithReason() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            loan.setCreatedBy("admin");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = loanService.approveLoan(loanId, approverId, "admin",
                    "Emergency approval - sole officer on duty", true);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
        }

        @Test
        @DisplayName("should throw when ADMIN creator tries to approve own loan without reason")
        void shouldThrowWhenAdminApprovesSelfWithNoReason() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            loan.setCreatedBy("admin");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.approveLoan(loanId, approverId, "admin", null, true))
                    .isInstanceOf(MakerCheckerViolationException.class);
        }

        @Test
        @DisplayName("should throw for non-pending loan")
        void shouldThrowForNonPendingLoan() {
            LoanApplication loan = createLoan(LoanStatus.APPROVED);
            loan.setCreatedBy("maker");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.approveLoan(loanId, approverId, "checker", null, false))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("should throw for non-existent loan")
        void shouldThrowForNonExistentLoan() {
            when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.approveLoan(loanId, approverId, "checker", null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found");
        }

        @Test
        @DisplayName("should throw when trying to approve a rejected loan")
        void shouldThrowForRejectedLoan() {
            LoanApplication loan = createLoan(LoanStatus.REJECTED);
            loan.setCreatedBy("maker");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.approveLoan(loanId, approverId, "checker", null, false))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("should throw when trying to approve a repaying loan")
        void shouldThrowForRepayingLoan() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setCreatedBy("maker");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.approveLoan(loanId, approverId, "checker", null, false))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    // -------------------------------------------------------------------------
    // rejectLoan
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("rejectLoan")
    class RejectLoan {

        @Test
        @DisplayName("should reject a pending loan by a different user")
        void shouldRejectPendingLoan() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            loan.setCreatedBy("maker");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = loanService.rejectLoan(loanId, approverId, "checker", null, false);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.REJECTED);
            assertThat(result.getApprovedBy()).isEqualTo(approverId);
            assertThat(result.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw MakerCheckerViolationException when creator tries to reject own loan")
        void shouldThrowWhenCreatorRejectsOwnLoan() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            loan.setCreatedBy("maker");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.rejectLoan(loanId, approverId, "maker", null, false))
                    .isInstanceOf(MakerCheckerViolationException.class);
        }

        @Test
        @DisplayName("should throw for non-pending loan")
        void shouldThrowForNonPendingLoan() {
            LoanApplication loan = createLoan(LoanStatus.APPROVED);
            loan.setCreatedBy("maker");
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.rejectLoan(loanId, approverId, "checker", null, false))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("should throw for non-existent loan")
        void shouldThrowForNonExistentLoan() {
            when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.rejectLoan(loanId, approverId, "checker", null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found");
        }
    }

    // -------------------------------------------------------------------------
    // disburseLoan
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("disburseLoan")
    class DisburseLoan {

        @Test
        @DisplayName("should disburse an approved loan with FLAT_RATE")
        void shouldDisburseApprovedLoanFlatRate() {
            LoanApplication loan = createLoan(LoanStatus.APPROVED);
            loan.setInterestMethod(InterestMethod.FLAT_RATE);
            loan.setPrincipalAmount(new BigDecimal("100000"));
            loan.setInterestRate(new BigDecimal("12"));
            loan.setTermMonths(12);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(interestCalculator.calculateFlatRate(any(), any(), any()))
                    .thenReturn(new BigDecimal("12000"));
            when(scheduleGenerator.generateSchedule(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = loanService.disburseLoan(loanId, "admin");

            assertThat(result.getStatus()).isEqualTo(LoanStatus.REPAYING);
            assertThat(result.getDisbursedAt()).isNotNull();
            assertThat(result.getOutstandingBalance()).isEqualByComparingTo(new BigDecimal("112000"));

            verify(scheduleRepository).saveAll(any());
            verify(outboxWriter).write(any(LoanDisbursedEvent.class), eq("LoanApplication"), any(UUID.class));
        }

        @Test
        @DisplayName("should disburse an approved loan with REDUCING_BALANCE")
        void shouldDisburseApprovedLoanReducingBalance() {
            LoanApplication loan = createLoan(LoanStatus.APPROVED);
            loan.setInterestMethod(InterestMethod.REDUCING_BALANCE);
            loan.setPrincipalAmount(new BigDecimal("100000"));
            loan.setInterestRate(new BigDecimal("12"));
            loan.setTermMonths(12);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(interestCalculator.calculateReducingBalance(any(), any(), any()))
                    .thenReturn(new BigDecimal("6618.55"));
            when(scheduleGenerator.generateSchedule(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = loanService.disburseLoan(loanId, "admin");

            assertThat(result.getStatus()).isEqualTo(LoanStatus.REPAYING);
            assertThat(result.getOutstandingBalance())
                    .isEqualByComparingTo(new BigDecimal("106618.55"));

            verify(interestCalculator).calculateReducingBalance(any(), any(), any());
            verify(interestCalculator, never()).calculateFlatRate(any(), any(), any());
        }

        @Test
        @DisplayName("should throw for non-approved loan")
        void shouldThrowForNonApprovedLoan() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.disburseLoan(loanId, "admin"))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("should throw for already disbursed loan")
        void shouldThrowForAlreadyDisbursedLoan() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.disburseLoan(loanId, "admin"))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("should publish LoanDisbursedEvent with correct data")
        void shouldPublishDisbursedEvent() {
            LoanApplication loan = createLoan(LoanStatus.APPROVED);
            loan.setInterestMethod(InterestMethod.FLAT_RATE);
            loan.setPrincipalAmount(new BigDecimal("50000"));
            loan.setInterestRate(new BigDecimal("10"));
            loan.setTermMonths(6);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(interestCalculator.calculateFlatRate(any(), any(), any()))
                    .thenReturn(new BigDecimal("2500"));
            when(scheduleGenerator.generateSchedule(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.disburseLoan(loanId, "treasurer");

            ArgumentCaptor<LoanDisbursedEvent> eventCaptor = ArgumentCaptor.forClass(LoanDisbursedEvent.class);
            verify(outboxWriter).write(eventCaptor.capture(), eq("LoanApplication"), any(UUID.class));

            LoanDisbursedEvent event = eventCaptor.getValue();
            assertThat(event.loanId()).isEqualTo(loanId);
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.principalAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(event.interestAmount()).isEqualByComparingTo(new BigDecimal("2500"));
            assertThat(event.actor()).isEqualTo("treasurer");
        }
    }

    // -------------------------------------------------------------------------
    // recordRepayment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("recordRepayment")
    class RecordRepayment {

        @BeforeEach
        void setUpPenaltyStub() {
            lenient().when(loanPenaltyService.payPenalties(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should record a valid repayment and update loan balances")
        void shouldRecordValidRepayment() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            loan.setTotalRepaid(BigDecimal.ZERO);

            RepaymentSchedule schedule = createSchedule(1, new BigDecimal("10000"),
                    new BigDecimal("8000"), new BigDecimal("2000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanRepayment result = loanService.recordRepayment(loanId, new BigDecimal("10000"), "REF001", "user");

            assertThat(result.getLoanId()).isEqualTo(loanId);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(result.getStatus()).isEqualTo(RepaymentStatus.CONFIRMED);
            assertThat(result.getReferenceNumber()).isEqualTo("REF001");

            // Verify loan balance updates
            verify(loanRepository).save(loanCaptor.capture());
            LoanApplication savedLoan = loanCaptor.getValue();
            assertThat(savedLoan.getTotalRepaid()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(savedLoan.getOutstandingBalance()).isEqualByComparingTo(new BigDecimal("90000"));
        }

        @Test
        @DisplayName("should close loan when fully repaid")
        void shouldCloseLoanWhenFullyRepaid() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("5000"));
            loan.setTotalRepaid(new BigDecimal("95000"));

            RepaymentSchedule schedule = createSchedule(12, new BigDecimal("5000"),
                    new BigDecimal("4000"), new BigDecimal("1000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.recordRepayment(loanId, new BigDecimal("5000"), "REF_FINAL", "user");

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.CLOSED);
            assertThat(loanCaptor.getValue().getOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should mark schedule as paid when fully covered")
        void shouldMarkScheduleAsPaid() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            loan.setTotalRepaid(BigDecimal.ZERO);

            RepaymentSchedule schedule = createSchedule(1, new BigDecimal("10000"),
                    new BigDecimal("8000"), new BigDecimal("2000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.recordRepayment(loanId, new BigDecimal("10000"), "REF001", "user");

            verify(scheduleRepository).save(any(RepaymentSchedule.class));
            assertThat(schedule.getPaid()).isTrue();
            assertThat(schedule.getAmountPaid()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("should handle partial schedule payment")
        void shouldHandlePartialSchedulePayment() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            loan.setTotalRepaid(BigDecimal.ZERO);

            RepaymentSchedule schedule = createSchedule(1, new BigDecimal("10000"),
                    new BigDecimal("8000"), new BigDecimal("2000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.recordRepayment(loanId, new BigDecimal("5000"), "REF001", "user");

            assertThat(schedule.getPaid()).isFalse();
            assertThat(schedule.getAmountPaid()).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("should allocate across multiple schedules")
        void shouldAllocateAcrossMultipleSchedules() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            loan.setTotalRepaid(BigDecimal.ZERO);

            RepaymentSchedule schedule1 = createSchedule(1, new BigDecimal("5000"),
                    new BigDecimal("4000"), new BigDecimal("1000"));
            RepaymentSchedule schedule2 = createSchedule(2, new BigDecimal("5000"),
                    new BigDecimal("4000"), new BigDecimal("1000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule1, schedule2)));
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.recordRepayment(loanId, new BigDecimal("8000"), "REF001", "user");

            // First schedule fully paid
            assertThat(schedule1.getPaid()).isTrue();
            assertThat(schedule1.getAmountPaid()).isEqualByComparingTo(new BigDecimal("5000"));
            // Second schedule partially paid
            assertThat(schedule2.getPaid()).isFalse();
            assertThat(schedule2.getAmountPaid()).isEqualByComparingTo(new BigDecimal("3000"));
        }

        @Test
        @DisplayName("should throw for non-repayable loan status")
        void shouldThrowForNonRepayableLoan() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() ->
                    loanService.recordRepayment(loanId, new BigDecimal("1000"), "REF", "user"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Loan is not in a repayable state");
        }

        @Test
        @DisplayName("should accept repayment for DISBURSED status")
        void shouldAcceptRepaymentForDisbursedStatus() {
            LoanApplication loan = createLoan(LoanStatus.DISBURSED);
            loan.setOutstandingBalance(new BigDecimal("50000"));
            loan.setTotalRepaid(BigDecimal.ZERO);

            RepaymentSchedule schedule = createSchedule(1, new BigDecimal("5000"),
                    new BigDecimal("4000"), new BigDecimal("1000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanRepayment result = loanService.recordRepayment(loanId, new BigDecimal("5000"), "REF", "user");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw for zero repayment amount")
        void shouldThrowForZeroAmount() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() ->
                    loanService.recordRepayment(loanId, BigDecimal.ZERO, "REF", "user"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Repayment amount must be greater than zero");
        }

        @Test
        @DisplayName("should throw for negative repayment amount")
        void shouldThrowForNegativeAmount() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() ->
                    loanService.recordRepayment(loanId, new BigDecimal("-100"), "REF", "user"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Repayment amount must be greater than zero");
        }

        @Test
        @DisplayName("should throw when repayment exceeds outstanding balance")
        void shouldThrowWhenExceedsBalance() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("5000"));
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() ->
                    loanService.recordRepayment(loanId, new BigDecimal("5001"), "REF", "user"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Repayment amount exceeds outstanding balance");
        }

        @Test
        @DisplayName("should publish LoanRepaymentEvent")
        void shouldPublishRepaymentEvent() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            loan.setTotalRepaid(BigDecimal.ZERO);

            RepaymentSchedule schedule = createSchedule(1, new BigDecimal("10000"),
                    new BigDecimal("8000"), new BigDecimal("2000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.recordRepayment(loanId, new BigDecimal("10000"), "REF001", "cashier");

            ArgumentCaptor<LoanRepaymentEvent> eventCaptor = ArgumentCaptor.forClass(LoanRepaymentEvent.class);
            verify(outboxWriter).write(eventCaptor.capture(), eq("LoanApplication"), any(UUID.class));

            LoanRepaymentEvent event = eventCaptor.getValue();
            assertThat(event.loanId()).isEqualTo(loanId);
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(event.actor()).isEqualTo("cashier");
        }

        @Test
        @DisplayName("should allocate repayment as Interest -> Penalties -> Principal")
        void shouldAllocateInterestThenPenaltiesThenPrincipal() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            loan.setTotalRepaid(BigDecimal.ZERO);
            loan.setTotalInterestAccrued(new BigDecimal("2000"));
            loan.setTotalInterestPaid(BigDecimal.ZERO);

            RepaymentSchedule schedule = createSchedule(1, new BigDecimal("10000"),
                    new BigDecimal("8000"), new BigDecimal("2000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));
            // Penalties: 500 paid out of available amount
            when(loanPenaltyService.payPenalties(eq(loanId), any(), eq("user")))
                    .thenReturn(new BigDecimal("500"));
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanRepayment result = loanService.recordRepayment(loanId, new BigDecimal("10000"), "REF001", "user");

            // Interest: 2000, Penalties: 500, Principal: 7500
            assertThat(result.getInterestPortion()).isEqualByComparingTo(new BigDecimal("2000"));
            assertThat(result.getPenaltyPortion()).isEqualByComparingTo(new BigDecimal("500"));
            assertThat(result.getPrincipalPortion()).isEqualByComparingTo(new BigDecimal("7500"));
        }

        @Test
        @DisplayName("should skip penalties when insufficient for any single penalty")
        void shouldSkipPenaltiesWhenInsufficientForAnySinglePenalty() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            loan.setTotalRepaid(BigDecimal.ZERO);
            loan.setTotalInterestAccrued(new BigDecimal("9500"));
            loan.setTotalInterestPaid(BigDecimal.ZERO);

            RepaymentSchedule schedule = createSchedule(1, new BigDecimal("10000"),
                    new BigDecimal("8000"), new BigDecimal("2000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));
            // No penalties could be paid (atomic: each penalty requires full amount)
            when(loanPenaltyService.payPenalties(eq(loanId), any(), eq("user")))
                    .thenReturn(BigDecimal.ZERO);
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanRepayment result = loanService.recordRepayment(loanId, new BigDecimal("10000"), "REF001", "user");

            // Interest takes 9500, no penalties paid, principal gets 500
            assertThat(result.getInterestPortion()).isEqualByComparingTo(new BigDecimal("9500"));
            assertThat(result.getPenaltyPortion()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPrincipalPortion()).isEqualByComparingTo(new BigDecimal("500"));
        }

        @Test
        @DisplayName("should not change status when partially repaid")
        void shouldNotChangeStatusWhenPartiallyRepaid() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("100000"));
            loan.setTotalRepaid(BigDecimal.ZERO);

            RepaymentSchedule schedule = createSchedule(1, new BigDecimal("10000"),
                    new BigDecimal("8000"), new BigDecimal("2000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdAndPaidFalseOrderByDueDate(loanId))
                    .thenReturn(new ArrayList<>(List.of(schedule)));
            when(repaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> {
                LoanRepayment r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.recordRepayment(loanId, new BigDecimal("10000"), "REF001", "user");

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.REPAYING);
        }
    }

    // -------------------------------------------------------------------------
    // closeLoan
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("closeLoan")
    class CloseLoan {

        @Test
        @DisplayName("should close loan with zero outstanding balance")
        void shouldCloseLoanWithZeroBalance() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(BigDecimal.ZERO);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanApplication result = loanService.closeLoan(loanId);

            assertThat(result.getStatus()).isEqualTo(LoanStatus.CLOSED);
        }

        @Test
        @DisplayName("should throw when outstanding balance is not zero")
        void shouldThrowWhenBalanceNotZero() {
            LoanApplication loan = createLoan(LoanStatus.REPAYING);
            loan.setOutstandingBalance(new BigDecimal("1000"));

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.closeLoan(loanId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot close loan with outstanding balance");
        }

        @Test
        @DisplayName("should throw for non-existent loan")
        void shouldThrowForNonExistentLoan() {
            when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.closeLoan(loanId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found");
        }
    }

    // -------------------------------------------------------------------------
    // getLoanSchedule
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getLoanSchedule")
    class GetLoanSchedule {

        @Test
        @DisplayName("should return schedules for a loan")
        void shouldReturnSchedules() {
            List<RepaymentSchedule> expectedSchedules = List.of(
                    createSchedule(1, new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000")),
                    createSchedule(2, new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("1000"))
            );
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId))
                    .thenReturn(expectedSchedules);

            List<RepaymentSchedule> result = loanService.getLoanSchedule(loanId);

            assertThat(result).hasSize(2);
            verify(scheduleRepository).findByLoanIdOrderByInstallmentNumber(loanId);
        }

        @Test
        @DisplayName("should return empty list when no schedules exist")
        void shouldReturnEmptyList() {
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId))
                    .thenReturn(List.of());

            List<RepaymentSchedule> result = loanService.getLoanSchedule(loanId);
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getLoanById
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getLoanById")
    class GetLoanById {

        @Test
        @DisplayName("should return loan when found")
        void shouldReturnLoanWhenFound() {
            LoanApplication loan = createLoan(LoanStatus.PENDING);
            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

            LoanApplication result = loanService.getLoanById(loanId);
            assertThat(result).isEqualTo(loan);
        }

        @Test
        @DisplayName("should throw when loan not found")
        void shouldThrowWhenNotFound() {
            when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.getLoanById(loanId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found with id: " + loanId);
        }
    }

    // -------------------------------------------------------------------------
    // getMemberLoans
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getMemberLoans")
    class GetMemberLoans {

        @Test
        @DisplayName("should return member loans")
        void shouldReturnMemberLoans() {
            LoanApplication loan1 = createLoan(LoanStatus.REPAYING);
            LoanApplication loan2 = createLoan(LoanStatus.CLOSED);
            when(loanRepository.findByMemberId(memberId)).thenReturn(List.of(loan1, loan2));

            List<LoanApplication> result = loanService.getMemberLoans(memberId);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no loans exist")
        void shouldReturnEmptyList() {
            when(loanRepository.findByMemberId(memberId)).thenReturn(List.of());

            List<LoanApplication> result = loanService.getMemberLoans(memberId);
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------
    private LoanApplication createLoan(LoanStatus status) {
        LoanApplication loan = new LoanApplication();
        loan.setId(loanId);
        loan.setMemberId(memberId);
        loan.setLoanNumber("LN-ABCDEF12");
        loan.setLoanProductId(loanProductId);
        loan.setPrincipalAmount(new BigDecimal("100000"));
        loan.setInterestRate(new BigDecimal("12"));
        loan.setTermMonths(12);
        loan.setInterestMethod(InterestMethod.FLAT_RATE);
        loan.setStatus(status);
        loan.setTotalRepaid(BigDecimal.ZERO);
        loan.setOutstandingBalance(BigDecimal.ZERO);
        loan.setTotalInterestAccrued(BigDecimal.ZERO);
        loan.setTotalInterestPaid(BigDecimal.ZERO);
        return loan;
    }

    private RepaymentSchedule createSchedule(int installmentNumber, BigDecimal totalAmount,
                                              BigDecimal principalAmount, BigDecimal interestAmount) {
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setLoanId(loanId);
        schedule.setInstallmentNumber(installmentNumber);
        schedule.setTotalAmount(totalAmount);
        schedule.setPrincipalAmount(principalAmount);
        schedule.setInterestAmount(interestAmount);
        schedule.setAmountPaid(BigDecimal.ZERO);
        schedule.setPaid(false);
        return schedule;
    }
}
