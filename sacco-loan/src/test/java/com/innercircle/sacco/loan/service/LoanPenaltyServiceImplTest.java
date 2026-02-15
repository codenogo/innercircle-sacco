package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.loan.entity.LoanPenalty;
import com.innercircle.sacco.loan.repository.LoanPenaltyRepository;
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
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanPenaltyServiceImplTest {

    @Mock
    private LoanPenaltyRepository penaltyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LoanPenaltyServiceImpl penaltyService;

    @Captor
    private ArgumentCaptor<LoanPenalty> penaltyCaptor;

    private UUID loanId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();
        memberId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // applyPenalty
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("applyPenalty")
    class ApplyPenalty {

        @Test
        @DisplayName("should create penalty with correct fields")
        void shouldCreatePenaltyWithCorrectFields() {
            UUID savedPenaltyId = UUID.randomUUID();
            when(penaltyRepository.save(any(LoanPenalty.class))).thenAnswer(inv -> {
                LoanPenalty p = inv.getArgument(0);
                p.setId(savedPenaltyId);
                return p;
            });

            LoanPenalty result = penaltyService.applyPenalty(
                    loanId, memberId, new BigDecimal("500"), "Late payment", "admin");

            verify(penaltyRepository).save(penaltyCaptor.capture());
            LoanPenalty saved = penaltyCaptor.getValue();

            assertThat(saved.getLoanId()).isEqualTo(loanId);
            assertThat(saved.getMemberId()).isEqualTo(memberId);
            assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("500"));
            assertThat(saved.getReason()).isEqualTo("Late payment");
            assertThat(saved.getApplied()).isTrue();
            assertThat(saved.getAppliedAt()).isNotNull();
        }

        @Test
        @DisplayName("should publish PenaltyAppliedEvent with correct data")
        void shouldPublishPenaltyAppliedEvent() {
            UUID savedPenaltyId = UUID.randomUUID();
            when(penaltyRepository.save(any(LoanPenalty.class))).thenAnswer(inv -> {
                LoanPenalty p = inv.getArgument(0);
                p.setId(savedPenaltyId);
                return p;
            });

            penaltyService.applyPenalty(loanId, memberId, new BigDecimal("1000"), "Overdue", "treasurer");

            ArgumentCaptor<PenaltyAppliedEvent> eventCaptor =
                    ArgumentCaptor.forClass(PenaltyAppliedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            PenaltyAppliedEvent event = eventCaptor.getValue();
            assertThat(event.penaltyId()).isEqualTo(savedPenaltyId);
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(event.penaltyType()).isEqualTo("LOAN_LATE_REPAYMENT");
            assertThat(event.actor()).isEqualTo("treasurer");
        }

        @Test
        @DisplayName("should throw for zero penalty amount")
        void shouldThrowForZeroAmount() {
            assertThatThrownBy(() ->
                    penaltyService.applyPenalty(loanId, memberId, BigDecimal.ZERO, "reason", "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty amount must be greater than zero");
        }

        @Test
        @DisplayName("should throw for negative penalty amount")
        void shouldThrowForNegativeAmount() {
            assertThatThrownBy(() ->
                    penaltyService.applyPenalty(loanId, memberId, new BigDecimal("-100"), "reason", "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty amount must be greater than zero");
        }

        @Test
        @DisplayName("should return saved penalty entity")
        void shouldReturnSavedPenalty() {
            when(penaltyRepository.save(any(LoanPenalty.class))).thenAnswer(inv -> {
                LoanPenalty p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            LoanPenalty result = penaltyService.applyPenalty(
                    loanId, memberId, new BigDecimal("250"), "reason", "admin");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getLoanId()).isEqualTo(loanId);
        }

        @Test
        @DisplayName("should handle very small penalty amount")
        void shouldHandleVerySmallAmount() {
            when(penaltyRepository.save(any(LoanPenalty.class))).thenAnswer(inv -> {
                LoanPenalty p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            LoanPenalty result = penaltyService.applyPenalty(
                    loanId, memberId, new BigDecimal("0.01"), "Minimal penalty", "admin");

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("0.01"));
        }

        @Test
        @DisplayName("should handle large penalty amount")
        void shouldHandleLargeAmount() {
            when(penaltyRepository.save(any(LoanPenalty.class))).thenAnswer(inv -> {
                LoanPenalty p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            LoanPenalty result = penaltyService.applyPenalty(
                    loanId, memberId, new BigDecimal("1000000.00"), "Large penalty", "admin");

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        }
    }

    // -------------------------------------------------------------------------
    // getLoanPenalties
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getLoanPenalties")
    class GetLoanPenalties {

        @Test
        @DisplayName("should return penalties for a loan")
        void shouldReturnPenaltiesForLoan() {
            LoanPenalty penalty1 = createPenalty(new BigDecimal("500"));
            LoanPenalty penalty2 = createPenalty(new BigDecimal("300"));
            when(penaltyRepository.findByLoanId(loanId)).thenReturn(List.of(penalty1, penalty2));

            List<LoanPenalty> result = penaltyService.getLoanPenalties(loanId);

            assertThat(result).hasSize(2);
            verify(penaltyRepository).findByLoanId(loanId);
        }

        @Test
        @DisplayName("should return empty list when no penalties exist")
        void shouldReturnEmptyWhenNoPenalties() {
            when(penaltyRepository.findByLoanId(loanId)).thenReturn(List.of());

            List<LoanPenalty> result = penaltyService.getLoanPenalties(loanId);

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getMemberPenalties
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getMemberPenalties")
    class GetMemberPenalties {

        @Test
        @DisplayName("should return penalties for a member")
        void shouldReturnPenaltiesForMember() {
            LoanPenalty penalty = createPenalty(new BigDecimal("500"));
            when(penaltyRepository.findByMemberId(memberId)).thenReturn(List.of(penalty));

            List<LoanPenalty> result = penaltyService.getMemberPenalties(memberId);

            assertThat(result).hasSize(1);
            verify(penaltyRepository).findByMemberId(memberId);
        }

        @Test
        @DisplayName("should return empty list when no member penalties exist")
        void shouldReturnEmptyWhenNoMemberPenalties() {
            when(penaltyRepository.findByMemberId(memberId)).thenReturn(List.of());

            List<LoanPenalty> result = penaltyService.getMemberPenalties(memberId);

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------
    private LoanPenalty createPenalty(BigDecimal amount) {
        LoanPenalty penalty = new LoanPenalty();
        penalty.setId(UUID.randomUUID());
        penalty.setLoanId(loanId);
        penalty.setMemberId(memberId);
        penalty.setAmount(amount);
        penalty.setReason("Late payment");
        penalty.setApplied(true);
        return penalty;
    }
}
