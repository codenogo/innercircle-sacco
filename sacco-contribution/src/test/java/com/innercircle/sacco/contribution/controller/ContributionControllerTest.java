package com.innercircle.sacco.contribution.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.contribution.dto.BulkContributionItemRequest;
import com.innercircle.sacco.contribution.dto.BulkContributionRequest;
import com.innercircle.sacco.contribution.dto.ContributionResponse;
import com.innercircle.sacco.contribution.dto.ContributionSummaryResponse;
import com.innercircle.sacco.contribution.dto.RecordContributionRequest;
import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionCategory;
import com.innercircle.sacco.contribution.entity.ContributionStatus;
import com.innercircle.sacco.contribution.entity.PaymentMode;
import com.innercircle.sacco.contribution.service.ContributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionControllerTest {

    @Mock
    private ContributionService contributionService;

    @InjectMocks
    private ContributionController contributionController;

    private ContributionCategory sharesCategory;
    private Contribution sampleContribution;
    private UUID contributionId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        contributionId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        sharesCategory = new ContributionCategory("Shares", "Monthly shares", true, true);
        sharesCategory.setId(UUID.randomUUID());

        sampleContribution = new Contribution(
                memberId,
                new BigDecimal("1000.00"),
                sharesCategory,
                PaymentMode.MPESA,
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 6, 5),
                "REF-001",
                "Monthly contribution"
        );
        sampleContribution.setId(contributionId);
        sampleContribution.setCreatedAt(Instant.now());
        sampleContribution.setUpdatedAt(Instant.now());
    }

    // -------------------------------------------------------
    // POST /api/v1/contributions
    // -------------------------------------------------------
    @Nested
    @DisplayName("POST /api/v1/contributions")
    class RecordContributionEndpoint {

        @Test
        @DisplayName("should record contribution and return success response")
        void shouldRecordContribution() {
            RecordContributionRequest request = new RecordContributionRequest(
                    memberId, new BigDecimal("1000.00"), sharesCategory.getId(),
                    PaymentMode.MPESA, LocalDate.of(2024, 6, 1),
                    LocalDate.of(2024, 6, 5), "REF-001", "Monthly contribution"
            );

            when(contributionService.recordContribution(any(RecordContributionRequest.class)))
                    .thenReturn(sampleContribution);

            ResponseEntity<ApiResponse<ContributionResponse>> result =
                    contributionController.recordContribution(request);

            assertThat(result).isNotNull();
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData().getMemberId()).isEqualTo(memberId);
            assertThat(result.getBody().getData().getAmount()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(result.getBody().getData().getPaymentMode()).isEqualTo(PaymentMode.MPESA);
            verify(contributionService).recordContribution(any(RecordContributionRequest.class));
        }
    }

    // -------------------------------------------------------
    // POST /api/v1/contributions/bulk
    // -------------------------------------------------------
    @Nested
    @DisplayName("POST /api/v1/contributions/bulk")
    class RecordBulkEndpoint {

        @Test
        @DisplayName("should record bulk contributions and return list")
        void shouldRecordBulk() {
            BulkContributionItemRequest req = new BulkContributionItemRequest(
                    memberId, new BigDecimal("1000.00"),
                    null, null, null, "REF-B1", "Member 1"
            );
            BulkContributionRequest bulk = new BulkContributionRequest(
                    PaymentMode.MPESA, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 5),
                    sharesCategory.getId(), "BATCH-001", List.of(req)
            );

            when(contributionService.recordBulk(any(BulkContributionRequest.class)))
                    .thenReturn(List.of(sampleContribution));

            ResponseEntity<ApiResponse<List<ContributionResponse>>> result =
                    contributionController.recordBulk(bulk);

            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).hasSize(1);
        }
    }

    // -------------------------------------------------------
    // PATCH /api/v1/contributions/{id}/confirm
    // -------------------------------------------------------
    @Nested
    @DisplayName("PATCH /api/v1/contributions/{id}/confirm")
    class ConfirmContributionEndpoint {

        @Test
        @DisplayName("should confirm contribution with provided actor")
        void shouldConfirmContribution() {
            sampleContribution.setStatus(ContributionStatus.CONFIRMED);

            when(contributionService.confirmContribution(contributionId, "treasurer"))
                    .thenReturn(sampleContribution);

            ApiResponse<ContributionResponse> result =
                    contributionController.confirmContribution(contributionId, "treasurer");

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Contribution confirmed successfully");
            assertThat(result.getData().getStatus()).isEqualTo(ContributionStatus.CONFIRMED);
        }
    }

    // -------------------------------------------------------
    // PATCH /api/v1/contributions/{id}/reverse
    // -------------------------------------------------------
    @Nested
    @DisplayName("PATCH /api/v1/contributions/{id}/reverse")
    class ReverseContributionEndpoint {

        @Test
        @DisplayName("should reverse contribution with provided actor")
        void shouldReverseContribution() {
            sampleContribution.setStatus(ContributionStatus.REVERSED);

            when(contributionService.reverseContribution(contributionId, "admin"))
                    .thenReturn(sampleContribution);

            ApiResponse<ContributionResponse> result =
                    contributionController.reverseContribution(contributionId, "admin");

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getStatus()).isEqualTo(ContributionStatus.REVERSED);
        }
    }

    // -------------------------------------------------------
    // GET /api/v1/contributions
    // -------------------------------------------------------
    @Nested
    @DisplayName("GET /api/v1/contributions")
    class ListContributionsEndpoint {

        @Test
        @DisplayName("should list contributions with filters")
        void shouldListContributions() {
            CursorPage<Contribution> page = CursorPage.of(
                    List.of(sampleContribution), null, false
            );

            when(contributionService.list(null, 20, null, null, null)).thenReturn(page);

            ApiResponse<CursorPage<ContributionResponse>> result =
                    contributionController.listContributions(null, 20, null, null, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
            assertThat(result.getData().isHasMore()).isFalse();
        }
    }

    // -------------------------------------------------------
    // GET /api/v1/contributions/member/{memberId}
    // -------------------------------------------------------
    @Nested
    @DisplayName("GET /api/v1/contributions/member/{memberId}")
    class GetMemberContributionsEndpoint {

        @Test
        @DisplayName("should return member contributions")
        void shouldReturnMemberContributions() {
            CursorPage<Contribution> page = CursorPage.of(
                    List.of(sampleContribution), null, false
            );

            when(contributionService.getMemberContributions(memberId, null, 20)).thenReturn(page);

            ApiResponse<CursorPage<ContributionResponse>> result =
                    contributionController.getMemberContributions(memberId, null, 20);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
            assertThat(result.getData().getItems().get(0).getMemberId()).isEqualTo(memberId);
        }
    }

    // -------------------------------------------------------
    // GET /api/v1/contributions/member/{memberId}/summary
    // -------------------------------------------------------
    @Nested
    @DisplayName("GET /api/v1/contributions/member/{memberId}/summary")
    class GetMemberSummaryEndpoint {

        @Test
        @DisplayName("should return member contribution summary")
        void shouldReturnMemberSummary() {
            ContributionSummaryResponse summary = new ContributionSummaryResponse(
                    memberId, new BigDecimal("5000.00"), new BigDecimal("1000.00"),
                    new BigDecimal("200.00"), LocalDate.of(2024, 6, 1)
            );

            when(contributionService.getMemberSummary(memberId)).thenReturn(summary);

            ApiResponse<ContributionSummaryResponse> result =
                    contributionController.getMemberSummary(memberId);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getTotalContributed()).isEqualTo(new BigDecimal("5000.00"));
        }
    }
}
