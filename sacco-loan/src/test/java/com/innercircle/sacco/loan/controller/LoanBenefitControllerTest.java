package com.innercircle.sacco.loan.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.loan.dto.LoanBenefitResponse;
import com.innercircle.sacco.loan.dto.MemberEarningsResponse;
import com.innercircle.sacco.loan.entity.LoanBenefit;
import com.innercircle.sacco.loan.service.LoanBenefitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanBenefitControllerTest {

    @Mock
    private LoanBenefitService benefitService;

    @InjectMocks
    private LoanBenefitController benefitController;

    // -------------------------------------------------------------------------
    // getMemberEarnings
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getMemberEarnings")
    class GetMemberEarnings {

        @Test
        @DisplayName("should return member earnings from service")
        void shouldReturnMemberEarnings() {
            UUID memberId = UUID.randomUUID();
            MemberEarningsResponse earnings = MemberEarningsResponse.builder()
                    .memberId(memberId)
                    .totalEarnings(new BigDecimal("15000.00"))
                    .distributedEarnings(new BigDecimal("10000.00"))
                    .pendingEarnings(new BigDecimal("5000.00"))
                    .totalBenefits(5)
                    .distributedCount(3)
                    .pendingCount(2)
                    .benefits(List.of())
                    .build();

            when(benefitService.getMemberEarnings(memberId)).thenReturn(earnings);

            ApiResponse<MemberEarningsResponse> response =
                    benefitController.getMemberEarnings(memberId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getMemberId()).isEqualTo(memberId);
            assertThat(response.getData().getTotalEarnings())
                    .isEqualByComparingTo(new BigDecimal("15000.00"));
            assertThat(response.getData().getDistributedEarnings())
                    .isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(response.getData().getPendingEarnings())
                    .isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(response.getData().getTotalBenefits()).isEqualTo(5);
            assertThat(response.getData().getDistributedCount()).isEqualTo(3);
            assertThat(response.getData().getPendingCount()).isEqualTo(2);
            verify(benefitService).getMemberEarnings(memberId);
        }

        @Test
        @DisplayName("should pass memberId to service correctly")
        void shouldPassMemberIdCorrectly() {
            UUID memberId = UUID.randomUUID();
            MemberEarningsResponse earnings = MemberEarningsResponse.builder()
                    .memberId(memberId)
                    .totalEarnings(BigDecimal.ZERO)
                    .distributedEarnings(BigDecimal.ZERO)
                    .pendingEarnings(BigDecimal.ZERO)
                    .totalBenefits(0)
                    .distributedCount(0)
                    .pendingCount(0)
                    .benefits(List.of())
                    .build();

            when(benefitService.getMemberEarnings(memberId)).thenReturn(earnings);

            benefitController.getMemberEarnings(memberId);

            verify(benefitService).getMemberEarnings(memberId);
        }

        @Test
        @DisplayName("should return empty benefits list when member has no earnings")
        void shouldReturnEmptyBenefitsForNoEarnings() {
            UUID memberId = UUID.randomUUID();
            MemberEarningsResponse earnings = MemberEarningsResponse.builder()
                    .memberId(memberId)
                    .totalEarnings(BigDecimal.ZERO)
                    .distributedEarnings(BigDecimal.ZERO)
                    .pendingEarnings(BigDecimal.ZERO)
                    .totalBenefits(0)
                    .distributedCount(0)
                    .pendingCount(0)
                    .benefits(List.of())
                    .build();

            when(benefitService.getMemberEarnings(memberId)).thenReturn(earnings);

            ApiResponse<MemberEarningsResponse> response =
                    benefitController.getMemberEarnings(memberId);

            assertThat(response.getData().getTotalEarnings())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getData().getBenefits()).isEmpty();
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
            UUID loanId = UUID.randomUUID();
            LoanBenefitResponse benefit1 = LoanBenefitResponse.builder()
                    .id(UUID.randomUUID())
                    .memberId(UUID.randomUUID())
                    .loanId(loanId)
                    .earnedAmount(new BigDecimal("500.00"))
                    .distributed(true)
                    .build();
            LoanBenefitResponse benefit2 = LoanBenefitResponse.builder()
                    .id(UUID.randomUUID())
                    .memberId(UUID.randomUUID())
                    .loanId(loanId)
                    .earnedAmount(new BigDecimal("300.00"))
                    .distributed(false)
                    .build();

            when(benefitService.getLoanBenefits(loanId))
                    .thenReturn(List.of(benefit1, benefit2));

            ApiResponse<List<LoanBenefitResponse>> response =
                    benefitController.getLoanBenefits(loanId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(2);
            assertThat(response.getData().get(0).getEarnedAmount())
                    .isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(response.getData().get(1).getEarnedAmount())
                    .isEqualByComparingTo(new BigDecimal("300.00"));
            verify(benefitService).getLoanBenefits(loanId);
        }

        @Test
        @DisplayName("should return empty list when no benefits exist")
        void shouldReturnEmptyWhenNoBenefits() {
            UUID loanId = UUID.randomUUID();
            when(benefitService.getLoanBenefits(loanId)).thenReturn(List.of());

            ApiResponse<List<LoanBenefitResponse>> response =
                    benefitController.getLoanBenefits(loanId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("should pass loanId to service correctly")
        void shouldPassLoanIdCorrectly() {
            UUID loanId = UUID.randomUUID();
            when(benefitService.getLoanBenefits(loanId)).thenReturn(List.of());

            benefitController.getLoanBenefits(loanId);

            verify(benefitService).getLoanBenefits(loanId);
        }
    }

    // -------------------------------------------------------------------------
    // getAllBenefits (cursor pagination)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAllBenefits")
    class GetAllBenefits {

        @Test
        @DisplayName("should return paginated benefits with hasMore=true when more exist")
        void shouldReturnPaginatedBenefitsWithHasMore() {
            UUID cursor = null;
            int limit = 2;

            // Service returns limit+1 items to indicate hasMore
            List<LoanBenefitResponse> serviceResults = new ArrayList<>();
            UUID lastId = null;
            for (int i = 0; i < 3; i++) {
                UUID id = UUID.randomUUID();
                if (i == 1) lastId = id; // the last item in the trimmed result
                serviceResults.add(LoanBenefitResponse.builder()
                        .id(id)
                        .memberId(UUID.randomUUID())
                        .loanId(UUID.randomUUID())
                        .earnedAmount(new BigDecimal("100.00"))
                        .build());
            }

            when(benefitService.getAllBenefits(null, 3)).thenReturn(serviceResults);

            ApiResponse<CursorPage<LoanBenefitResponse>> response =
                    benefitController.getAllBenefits(cursor, limit);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().isHasMore()).isTrue();
            assertThat(response.getData().getItems()).hasSize(2);
            assertThat(response.getData().getNextCursor()).isNotNull();
        }

        @Test
        @DisplayName("should return paginated benefits with hasMore=false when no more exist")
        void shouldReturnPaginatedBenefitsWithNoMore() {
            UUID cursor = null;
            int limit = 20;

            List<LoanBenefitResponse> serviceResults = List.of(
                    LoanBenefitResponse.builder()
                            .id(UUID.randomUUID())
                            .memberId(UUID.randomUUID())
                            .loanId(UUID.randomUUID())
                            .earnedAmount(new BigDecimal("100.00"))
                            .build()
            );

            when(benefitService.getAllBenefits(null, 21)).thenReturn(serviceResults);

            ApiResponse<CursorPage<LoanBenefitResponse>> response =
                    benefitController.getAllBenefits(cursor, limit);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().isHasMore()).isFalse();
            assertThat(response.getData().getItems()).hasSize(1);
            assertThat(response.getData().getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should pass cursor to service for subsequent pages")
        void shouldPassCursorToService() {
            UUID cursor = UUID.randomUUID();
            int limit = 10;

            when(benefitService.getAllBenefits(cursor, 11)).thenReturn(List.of());

            benefitController.getAllBenefits(cursor, limit);

            verify(benefitService).getAllBenefits(cursor, 11);
        }

        @Test
        @DisplayName("should request limit+1 items from service to determine hasMore")
        void shouldRequestLimitPlusOne() {
            int limit = 5;

            when(benefitService.getAllBenefits(any(), eq(6))).thenReturn(List.of());

            benefitController.getAllBenefits(null, limit);

            verify(benefitService).getAllBenefits(null, 6);
        }

        @Test
        @DisplayName("should set nextCursor to last item id when hasMore is true")
        void shouldSetNextCursorToLastItemId() {
            int limit = 2;
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();

            List<LoanBenefitResponse> serviceResults = List.of(
                    LoanBenefitResponse.builder().id(id1).memberId(UUID.randomUUID())
                            .loanId(UUID.randomUUID()).earnedAmount(BigDecimal.ONE).build(),
                    LoanBenefitResponse.builder().id(id2).memberId(UUID.randomUUID())
                            .loanId(UUID.randomUUID()).earnedAmount(BigDecimal.ONE).build(),
                    LoanBenefitResponse.builder().id(id3).memberId(UUID.randomUUID())
                            .loanId(UUID.randomUUID()).earnedAmount(BigDecimal.ONE).build()
            );

            when(benefitService.getAllBenefits(null, 3)).thenReturn(serviceResults);

            ApiResponse<CursorPage<LoanBenefitResponse>> response =
                    benefitController.getAllBenefits(null, limit);

            // nextCursor should be the ID of the last item in the trimmed list (id2)
            assertThat(response.getData().getNextCursor()).isEqualTo(id2.toString());
        }

        @Test
        @DisplayName("should return empty page when no benefits exist")
        void shouldReturnEmptyPage() {
            when(benefitService.getAllBenefits(any(), eq(21))).thenReturn(List.of());

            ApiResponse<CursorPage<LoanBenefitResponse>> response =
                    benefitController.getAllBenefits(null, 20);

            assertThat(response.getData().getItems()).isEmpty();
            assertThat(response.getData().isHasMore()).isFalse();
            assertThat(response.getData().getSize()).isZero();
        }

        @Test
        @DisplayName("should set size to number of items in page")
        void shouldSetSizeCorrectly() {
            int limit = 10;
            List<LoanBenefitResponse> serviceResults = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                serviceResults.add(LoanBenefitResponse.builder()
                        .id(UUID.randomUUID())
                        .memberId(UUID.randomUUID())
                        .loanId(UUID.randomUUID())
                        .earnedAmount(BigDecimal.TEN)
                        .build());
            }

            when(benefitService.getAllBenefits(null, 11)).thenReturn(serviceResults);

            ApiResponse<CursorPage<LoanBenefitResponse>> response =
                    benefitController.getAllBenefits(null, limit);

            assertThat(response.getData().getSize()).isEqualTo(5);
        }
    }

    // -------------------------------------------------------------------------
    // refreshBeneficiaries
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("refreshBeneficiaries")
    class RefreshBeneficiaries {

        @Test
        @DisplayName("should refresh beneficiaries and return responses")
        void shouldRefreshAndReturnResponses() {
            UUID loanId = UUID.randomUUID();
            String actor = "admin";

            LoanBenefit benefit1 = createBenefit(loanId, new BigDecimal("500.00"));
            LoanBenefit benefit2 = createBenefit(loanId, new BigDecimal("300.00"));

            when(benefitService.refreshBeneficiaries(loanId, actor))
                    .thenReturn(List.of(benefit1, benefit2));

            ApiResponse<List<LoanBenefitResponse>> response =
                    benefitController.refreshBeneficiaries(loanId, actor);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(2);
            assertThat(response.getMessage()).isEqualTo("Beneficiaries refreshed successfully");
            verify(benefitService).refreshBeneficiaries(loanId, actor);
        }

        @Test
        @DisplayName("should map entity fields to response correctly")
        void shouldMapEntityFieldsToResponse() {
            UUID loanId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            UUID benefitId = UUID.randomUUID();
            String actor = "treasurer";
            Instant distributedAt = Instant.now();
            Instant createdAt = Instant.now().minusSeconds(3600);

            LoanBenefit benefit = new LoanBenefit();
            benefit.setId(benefitId);
            benefit.setMemberId(memberId);
            benefit.setLoanId(loanId);
            benefit.setContributionSnapshot(new BigDecimal("50000.00"));
            benefit.setBenefitsRate(new BigDecimal("25.00"));
            benefit.setEarnedAmount(new BigDecimal("1250.00"));
            benefit.setExpectedEarnings(new BigDecimal("1500.00"));
            benefit.setDistributed(true);
            benefit.setDistributedAt(distributedAt);
            benefit.setCreatedAt(createdAt);

            when(benefitService.refreshBeneficiaries(loanId, actor))
                    .thenReturn(List.of(benefit));

            ApiResponse<List<LoanBenefitResponse>> response =
                    benefitController.refreshBeneficiaries(loanId, actor);

            LoanBenefitResponse dto = response.getData().get(0);
            assertThat(dto.getId()).isEqualTo(benefitId);
            assertThat(dto.getMemberId()).isEqualTo(memberId);
            assertThat(dto.getLoanId()).isEqualTo(loanId);
            assertThat(dto.getContributionSnapshot())
                    .isEqualByComparingTo(new BigDecimal("50000.00"));
            assertThat(dto.getBenefitsRate())
                    .isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(dto.getEarnedAmount())
                    .isEqualByComparingTo(new BigDecimal("1250.00"));
            assertThat(dto.getExpectedEarnings())
                    .isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(dto.isDistributed()).isTrue();
            assertThat(dto.getDistributedAt()).isEqualTo(distributedAt);
            assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("should pass loanId and actor to service")
        void shouldPassLoanIdAndActorToService() {
            UUID loanId = UUID.randomUUID();
            String actor = "system_admin";

            when(benefitService.refreshBeneficiaries(loanId, actor))
                    .thenReturn(List.of());

            benefitController.refreshBeneficiaries(loanId, actor);

            verify(benefitService).refreshBeneficiaries(loanId, actor);
        }

        @Test
        @DisplayName("should return empty list when no beneficiaries created")
        void shouldReturnEmptyWhenNoBeneficiaries() {
            UUID loanId = UUID.randomUUID();

            when(benefitService.refreshBeneficiaries(eq(loanId), any()))
                    .thenReturn(List.of());

            ApiResponse<List<LoanBenefitResponse>> response =
                    benefitController.refreshBeneficiaries(loanId, "admin");

            assertThat(response.getData()).isEmpty();
            assertThat(response.getMessage()).isEqualTo("Beneficiaries refreshed successfully");
        }
    }

    // -------------------------------------------------------------------------
    // Security annotation verification
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Security annotations")
    class SecurityAnnotations {

        @Test
        @DisplayName("getMemberEarnings should require ADMIN, TREASURER, or MEMBER role")
        void getMemberEarningsShouldRequireRoles() throws NoSuchMethodException {
            var annotation = LoanBenefitController.class
                    .getMethod("getMemberEarnings", UUID.class)
                    .getAnnotation(PreAuthorize.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains("ADMIN");
            assertThat(annotation.value()).contains("TREASURER");
            assertThat(annotation.value()).contains("MEMBER");
        }

        @Test
        @DisplayName("getLoanBenefits should require ADMIN, TREASURER, or MEMBER role")
        void getLoanBenefitsShouldRequireRoles() throws NoSuchMethodException {
            var annotation = LoanBenefitController.class
                    .getMethod("getLoanBenefits", UUID.class)
                    .getAnnotation(PreAuthorize.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains("ADMIN");
            assertThat(annotation.value()).contains("TREASURER");
            assertThat(annotation.value()).contains("MEMBER");
        }

        @Test
        @DisplayName("getAllBenefits should require ADMIN or TREASURER role only")
        void getAllBenefitsShouldRequireAdminOrTreasurer() throws NoSuchMethodException {
            var annotation = LoanBenefitController.class
                    .getMethod("getAllBenefits", UUID.class, int.class)
                    .getAnnotation(PreAuthorize.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains("ADMIN");
            assertThat(annotation.value()).contains("TREASURER");
            // MEMBER should NOT be in this annotation
            assertThat(annotation.value()).doesNotContain("MEMBER");
        }

        @Test
        @DisplayName("refreshBeneficiaries should require ADMIN or TREASURER role only")
        void refreshBeneficiariesShouldRequireAdminOrTreasurer() throws NoSuchMethodException {
            var annotation = LoanBenefitController.class
                    .getMethod("refreshBeneficiaries", UUID.class, String.class)
                    .getAnnotation(PreAuthorize.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains("ADMIN");
            assertThat(annotation.value()).contains("TREASURER");
            // MEMBER should NOT be in this annotation
            assertThat(annotation.value()).doesNotContain("MEMBER");
        }

        @Test
        @DisplayName("getMemberEarnings and getLoanBenefits should include MEMBER role")
        void memberAccessibleEndpointsShouldIncludeMemberRole() throws NoSuchMethodException {
            var earningsAnnotation = LoanBenefitController.class
                    .getMethod("getMemberEarnings", UUID.class)
                    .getAnnotation(PreAuthorize.class);
            var loanBenefitsAnnotation = LoanBenefitController.class
                    .getMethod("getLoanBenefits", UUID.class)
                    .getAnnotation(PreAuthorize.class);

            assertThat(earningsAnnotation.value()).contains("MEMBER");
            assertThat(loanBenefitsAnnotation.value()).contains("MEMBER");
        }

        @Test
        @DisplayName("admin-only endpoints should NOT include MEMBER role")
        void adminOnlyEndpointsShouldNotIncludeMemberRole() throws NoSuchMethodException {
            var allBenefitsAnnotation = LoanBenefitController.class
                    .getMethod("getAllBenefits", UUID.class, int.class)
                    .getAnnotation(PreAuthorize.class);
            var refreshAnnotation = LoanBenefitController.class
                    .getMethod("refreshBeneficiaries", UUID.class, String.class)
                    .getAnnotation(PreAuthorize.class);

            assertThat(allBenefitsAnnotation.value()).doesNotContain("MEMBER");
            assertThat(refreshAnnotation.value()).doesNotContain("MEMBER");
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------
    private LoanBenefit createBenefit(UUID loanId, BigDecimal earnedAmount) {
        LoanBenefit benefit = new LoanBenefit();
        benefit.setId(UUID.randomUUID());
        benefit.setMemberId(UUID.randomUUID());
        benefit.setLoanId(loanId);
        benefit.setContributionSnapshot(new BigDecimal("100000.00"));
        benefit.setBenefitsRate(new BigDecimal("10.00"));
        benefit.setEarnedAmount(earnedAmount);
        benefit.setExpectedEarnings(earnedAmount);
        benefit.setDistributed(false);
        benefit.setCreatedAt(Instant.now());
        return benefit;
    }
}
