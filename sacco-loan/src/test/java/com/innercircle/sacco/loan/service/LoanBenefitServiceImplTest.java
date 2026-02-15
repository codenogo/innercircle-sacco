package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.BenefitsDistributedEvent;
import com.innercircle.sacco.common.event.LoanRepaymentEvent;
import com.innercircle.sacco.loan.dto.LoanBenefitResponse;
import com.innercircle.sacco.loan.dto.MemberEarningsResponse;
import com.innercircle.sacco.loan.entity.LoanBenefit;
import com.innercircle.sacco.loan.repository.LoanBenefitRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanBenefitServiceImplTest {

    @Mock
    private LoanBenefitRepository benefitRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LoanBenefitServiceImpl benefitService;

    @Captor
    private ArgumentCaptor<List<LoanBenefit>> benefitListCaptor;

    private UUID loanId;
    private UUID memberId1;
    private UUID memberId2;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();
        memberId1 = UUID.randomUUID();
        memberId2 = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // handleLoanRepayment (event listener)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("handleLoanRepayment")
    class HandleLoanRepayment {

        @Test
        @DisplayName("should distribute interest when interest portion is positive")
        void shouldDistributeWhenInterestPositive() {
            UUID repaymentId = UUID.randomUUID();
            LoanRepaymentEvent event = new LoanRepaymentEvent(
                    loanId, memberId1, repaymentId,
                    new BigDecimal("10000"), new BigDecimal("8000"),
                    new BigDecimal("2000"), "user");

            Map<String, Object> member = new HashMap<>();
            member.put("id", memberId1);
            member.put("share_balance", new BigDecimal("10000"));

            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(member));
            when(benefitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            benefitService.handleLoanRepayment(event);

            verify(benefitRepository).saveAll(anyList());
            verify(eventPublisher).publishEvent(any(BenefitsDistributedEvent.class));
        }

        @Test
        @DisplayName("should not distribute when interest portion is zero")
        void shouldNotDistributeWhenInterestZero() {
            UUID repaymentId = UUID.randomUUID();
            LoanRepaymentEvent event = new LoanRepaymentEvent(
                    loanId, memberId1, repaymentId,
                    new BigDecimal("10000"), new BigDecimal("10000"),
                    BigDecimal.ZERO, "user");

            benefitService.handleLoanRepayment(event);

            verify(benefitRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should not distribute when interest portion is negative")
        void shouldNotDistributeWhenInterestNegative() {
            UUID repaymentId = UUID.randomUUID();
            LoanRepaymentEvent event = new LoanRepaymentEvent(
                    loanId, memberId1, repaymentId,
                    new BigDecimal("10000"), new BigDecimal("10000"),
                    new BigDecimal("-100"), "user");

            benefitService.handleLoanRepayment(event);

            verify(benefitRepository, never()).saveAll(anyList());
        }
    }

    // -------------------------------------------------------------------------
    // distributeInterestEarnings
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("distributeInterestEarnings")
    class DistributeInterestEarnings {

        @Test
        @DisplayName("should distribute proportionally to share balances")
        void shouldDistributeProportionally() {
            Map<String, Object> member1 = new HashMap<>();
            member1.put("id", memberId1);
            member1.put("share_balance", new BigDecimal("30000"));

            Map<String, Object> member2 = new HashMap<>();
            member2.put("id", memberId2);
            member2.put("share_balance", new BigDecimal("70000"));

            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(member1, member2));
            when(benefitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<LoanBenefit> result = benefitService.distributeInterestEarnings(
                    loanId, new BigDecimal("1000"), "admin");

            verify(benefitRepository).saveAll(benefitListCaptor.capture());
            List<LoanBenefit> savedBenefits = benefitListCaptor.getValue();

            assertThat(savedBenefits).hasSize(2);

            // member1 has 30% share -> 300
            LoanBenefit benefit1 = savedBenefits.stream()
                    .filter(b -> b.getMemberId().equals(memberId1)).findFirst().orElseThrow();
            assertThat(benefit1.getEarnedAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(benefit1.getContributionSnapshot()).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(benefit1.isDistributed()).isTrue();
            assertThat(benefit1.getDistributedAt()).isNotNull();

            // member2 has 70% share -> 700
            LoanBenefit benefit2 = savedBenefits.stream()
                    .filter(b -> b.getMemberId().equals(memberId2)).findFirst().orElseThrow();
            assertThat(benefit2.getEarnedAmount()).isEqualByComparingTo(new BigDecimal("700.00"));
        }

        @Test
        @DisplayName("should throw for zero interest amount")
        void shouldThrowForZeroInterest() {
            assertThatThrownBy(() ->
                    benefitService.distributeInterestEarnings(loanId, BigDecimal.ZERO, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Interest amount must be greater than zero");
        }

        @Test
        @DisplayName("should throw for negative interest amount")
        void shouldThrowForNegativeInterest() {
            assertThatThrownBy(() ->
                    benefitService.distributeInterestEarnings(loanId, new BigDecimal("-100"), "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Interest amount must be greater than zero");
        }

        @Test
        @DisplayName("should return empty list when no active members exist")
        void shouldReturnEmptyWhenNoActiveMembers() {
            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

            List<LoanBenefit> result = benefitService.distributeInterestEarnings(
                    loanId, new BigDecimal("1000"), "admin");

            assertThat(result).isEmpty();
            verify(benefitRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should set correct loanId on all benefits")
        void shouldSetCorrectLoanId() {
            Map<String, Object> member = new HashMap<>();
            member.put("id", memberId1);
            member.put("share_balance", new BigDecimal("10000"));

            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(member));
            when(benefitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            benefitService.distributeInterestEarnings(loanId, new BigDecimal("500"), "admin");

            verify(benefitRepository).saveAll(benefitListCaptor.capture());
            assertThat(benefitListCaptor.getValue())
                    .allSatisfy(b -> assertThat(b.getLoanId()).isEqualTo(loanId));
        }

        @Test
        @DisplayName("should set expected earnings equal to earned amount")
        void shouldSetExpectedEarnings() {
            Map<String, Object> member = new HashMap<>();
            member.put("id", memberId1);
            member.put("share_balance", new BigDecimal("10000"));

            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(member));
            when(benefitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            benefitService.distributeInterestEarnings(loanId, new BigDecimal("500"), "admin");

            verify(benefitRepository).saveAll(benefitListCaptor.capture());
            assertThat(benefitListCaptor.getValue())
                    .allSatisfy(b -> assertThat(b.getExpectedEarnings()).isEqualTo(b.getEarnedAmount()));
        }

        @Test
        @DisplayName("should publish BenefitsDistributedEvent")
        void shouldPublishEvent() {
            Map<String, Object> member = new HashMap<>();
            member.put("id", memberId1);
            member.put("share_balance", new BigDecimal("10000"));

            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(member));
            when(benefitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            benefitService.distributeInterestEarnings(loanId, new BigDecimal("500"), "admin");

            ArgumentCaptor<BenefitsDistributedEvent> eventCaptor =
                    ArgumentCaptor.forClass(BenefitsDistributedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            BenefitsDistributedEvent event = eventCaptor.getValue();
            assertThat(event.loanId()).isEqualTo(loanId);
            assertThat(event.totalInterestAmount()).isEqualByComparingTo(new BigDecimal("500"));
            assertThat(event.beneficiaryCount()).isEqualTo(1);
            assertThat(event.actor()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should handle single member receiving all interest")
        void shouldHandleSingleMember() {
            Map<String, Object> member = new HashMap<>();
            member.put("id", memberId1);
            member.put("share_balance", new BigDecimal("50000"));

            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(member));
            when(benefitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            benefitService.distributeInterestEarnings(loanId, new BigDecimal("1000"), "admin");

            verify(benefitRepository).saveAll(benefitListCaptor.capture());
            assertThat(benefitListCaptor.getValue()).hasSize(1);
            assertThat(benefitListCaptor.getValue().get(0).getEarnedAmount())
                    .isEqualByComparingTo(new BigDecimal("1000.00"));
        }
    }

    // -------------------------------------------------------------------------
    // getMemberEarnings
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getMemberEarnings")
    class GetMemberEarnings {

        @Test
        @DisplayName("should calculate totals correctly with mixed distributed/pending")
        void shouldCalculateTotalsCorrectly() {
            LoanBenefit benefit1 = createBenefit(memberId1, new BigDecimal("500"), true);
            LoanBenefit benefit2 = createBenefit(memberId1, new BigDecimal("300"), true);
            LoanBenefit benefit3 = createBenefit(memberId1, new BigDecimal("200"), false);

            when(benefitRepository.findByMemberIdOrderByCreatedAtDesc(memberId1))
                    .thenReturn(List.of(benefit1, benefit2, benefit3));

            MemberEarningsResponse response = benefitService.getMemberEarnings(memberId1);

            assertThat(response.getMemberId()).isEqualTo(memberId1);
            assertThat(response.getTotalEarnings()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(response.getDistributedEarnings()).isEqualByComparingTo(new BigDecimal("800"));
            assertThat(response.getPendingEarnings()).isEqualByComparingTo(new BigDecimal("200"));
            assertThat(response.getTotalBenefits()).isEqualTo(3);
            assertThat(response.getDistributedCount()).isEqualTo(2);
            assertThat(response.getPendingCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return zero totals when no benefits exist")
        void shouldReturnZeroTotalsWhenNoBenefits() {
            when(benefitRepository.findByMemberIdOrderByCreatedAtDesc(memberId1))
                    .thenReturn(List.of());

            MemberEarningsResponse response = benefitService.getMemberEarnings(memberId1);

            assertThat(response.getTotalEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getDistributedEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getPendingEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalBenefits()).isEqualTo(0);
            assertThat(response.getDistributedCount()).isEqualTo(0);
            assertThat(response.getPendingCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return all distributed when none pending")
        void shouldReturnAllDistributedWhenNonePending() {
            LoanBenefit benefit1 = createBenefit(memberId1, new BigDecimal("500"), true);
            LoanBenefit benefit2 = createBenefit(memberId1, new BigDecimal("300"), true);

            when(benefitRepository.findByMemberIdOrderByCreatedAtDesc(memberId1))
                    .thenReturn(List.of(benefit1, benefit2));

            MemberEarningsResponse response = benefitService.getMemberEarnings(memberId1);

            assertThat(response.getPendingEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getPendingCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should include benefit details in response")
        void shouldIncludeBenefitDetails() {
            LoanBenefit benefit = createBenefit(memberId1, new BigDecimal("500"), true);
            benefit.setId(UUID.randomUUID());
            benefit.setCreatedAt(Instant.now());

            when(benefitRepository.findByMemberIdOrderByCreatedAtDesc(memberId1))
                    .thenReturn(List.of(benefit));

            MemberEarningsResponse response = benefitService.getMemberEarnings(memberId1);

            assertThat(response.getBenefits()).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // getLoanBenefits
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getLoanBenefits")
    class GetLoanBenefits {

        @Test
        @DisplayName("should return benefits for a loan")
        void shouldReturnBenefitsForLoan() {
            LoanBenefit benefit = createBenefit(memberId1, new BigDecimal("500"), true);
            benefit.setId(UUID.randomUUID());
            benefit.setCreatedAt(Instant.now());

            when(benefitRepository.findByLoanIdOrderByEarnedAmountDesc(loanId))
                    .thenReturn(List.of(benefit));

            List<LoanBenefitResponse> result = benefitService.getLoanBenefits(loanId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLoanId()).isEqualTo(loanId);
        }

        @Test
        @DisplayName("should return empty list when no benefits exist")
        void shouldReturnEmptyList() {
            when(benefitRepository.findByLoanIdOrderByEarnedAmountDesc(loanId))
                    .thenReturn(List.of());

            List<LoanBenefitResponse> result = benefitService.getLoanBenefits(loanId);

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getAllBenefits
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAllBenefits")
    class GetAllBenefits {

        @Test
        @DisplayName("should return paginated benefits with cursor")
        void shouldReturnPaginatedBenefitsWithCursor() {
            UUID cursor = UUID.randomUUID();
            LoanBenefit benefit = createBenefit(memberId1, new BigDecimal("500"), true);
            benefit.setId(UUID.randomUUID());
            benefit.setCreatedAt(Instant.now());

            when(benefitRepository.findByIdGreaterThanOrderById(eq(cursor), any(Pageable.class)))
                    .thenReturn(List.of(benefit));

            List<LoanBenefitResponse> result = benefitService.getAllBenefits(cursor, 20);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should use zero UUID when cursor is null")
        void shouldUseZeroUuidWhenCursorNull() {
            LoanBenefit benefit = createBenefit(memberId1, new BigDecimal("500"), true);
            benefit.setId(UUID.randomUUID());
            benefit.setCreatedAt(Instant.now());

            UUID zeroUuid = new UUID(0L, 0L);
            when(benefitRepository.findByIdGreaterThanOrderById(eq(zeroUuid), any(Pageable.class)))
                    .thenReturn(List.of(benefit));

            List<LoanBenefitResponse> result = benefitService.getAllBenefits(null, 20);

            assertThat(result).hasSize(1);
            verify(benefitRepository).findByIdGreaterThanOrderById(eq(zeroUuid), any(Pageable.class));
        }

        @Test
        @DisplayName("should return empty list when no benefits exist")
        void shouldReturnEmptyList() {
            when(benefitRepository.findByIdGreaterThanOrderById(any(), any(Pageable.class)))
                    .thenReturn(List.of());

            List<LoanBenefitResponse> result = benefitService.getAllBenefits(null, 20);
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // refreshBeneficiaries
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("refreshBeneficiaries")
    class RefreshBeneficiaries {

        @Test
        @DisplayName("should mark existing benefits as distributed and redistribute")
        void shouldMarkExistingAndRedistribute() {
            LoanBenefit existingBenefit = createBenefit(memberId1, new BigDecimal("500"), false);
            existingBenefit.setExpectedEarnings(new BigDecimal("500"));

            when(benefitRepository.findByLoanId(loanId)).thenReturn(List.of(existingBenefit));
            when(benefitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> member = new HashMap<>();
            member.put("id", memberId1);
            member.put("share_balance", new BigDecimal("10000"));
            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(member));

            benefitService.refreshBeneficiaries(loanId, "admin");

            // Verify existing benefits are marked as distributed
            assertThat(existingBenefit.isDistributed()).isTrue();
            assertThat(existingBenefit.getDistributedAt()).isNotNull();
        }

        @Test
        @DisplayName("should return empty list when no existing benefits found")
        void shouldReturnEmptyWhenNoExistingBenefits() {
            when(benefitRepository.findByLoanId(loanId)).thenReturn(List.of());

            List<LoanBenefit> result = benefitService.refreshBeneficiaries(loanId, "admin");

            assertThat(result).isEmpty();
            verify(jdbcTemplate, never()).queryForList(anyString());
        }

        @Test
        @DisplayName("should redistribute total expected earnings from existing benefits")
        void shouldRedistributeTotalExpectedEarnings() {
            LoanBenefit existing1 = createBenefit(memberId1, new BigDecimal("300"), false);
            existing1.setExpectedEarnings(new BigDecimal("300"));
            LoanBenefit existing2 = createBenefit(memberId2, new BigDecimal("700"), true);
            existing2.setExpectedEarnings(new BigDecimal("700"));

            when(benefitRepository.findByLoanId(loanId)).thenReturn(List.of(existing1, existing2));
            when(benefitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> member = new HashMap<>();
            member.put("id", memberId1);
            member.put("share_balance", new BigDecimal("10000"));
            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(member));

            benefitService.refreshBeneficiaries(loanId, "admin");

            // saveAll is called twice: once for marking existing as distributed, once for new distribution
            verify(benefitRepository, times(2)).saveAll(benefitListCaptor.capture());
            List<List<LoanBenefit>> allCalls = benefitListCaptor.getAllValues();

            // First call: mark existing as distributed
            assertThat(allCalls.get(0)).hasSize(2);
            assertThat(allCalls.get(0)).allSatisfy(b -> assertThat(b.isDistributed()).isTrue());

            // Second call: redistribute total expected (300 + 700 = 1000) to 1 member
            assertThat(allCalls.get(1)).hasSize(1);
            assertThat(allCalls.get(1).get(0).getEarnedAmount())
                    .isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("should not re-mark already distributed benefits")
        void shouldNotReMarkAlreadyDistributed() {
            Instant originalDistributedAt = Instant.now().minusSeconds(3600);
            LoanBenefit existing = createBenefit(memberId1, new BigDecimal("500"), true);
            existing.setExpectedEarnings(new BigDecimal("500"));
            existing.setDistributedAt(originalDistributedAt);

            when(benefitRepository.findByLoanId(loanId)).thenReturn(List.of(existing));
            when(benefitRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> member = new HashMap<>();
            member.put("id", memberId1);
            member.put("share_balance", new BigDecimal("10000"));
            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(member));

            benefitService.refreshBeneficiaries(loanId, "admin");

            // Already distributed, so distributedAt should remain original
            assertThat(existing.getDistributedAt()).isEqualTo(originalDistributedAt);
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------
    private LoanBenefit createBenefit(UUID memberId, BigDecimal earnedAmount, boolean distributed) {
        LoanBenefit benefit = new LoanBenefit();
        benefit.setMemberId(memberId);
        benefit.setLoanId(loanId);
        benefit.setContributionSnapshot(new BigDecimal("10000"));
        benefit.setBenefitsRate(new BigDecimal("50.00"));
        benefit.setEarnedAmount(earnedAmount);
        benefit.setExpectedEarnings(earnedAmount);
        benefit.setDistributed(distributed);
        if (distributed) {
            benefit.setDistributedAt(Instant.now());
        }
        return benefit;
    }
}
