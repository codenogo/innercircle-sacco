package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.ContributionReceivedEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.InvalidStateTransitionException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.contribution.dto.BulkContributionItemRequest;
import com.innercircle.sacco.contribution.dto.BulkContributionRequest;
import com.innercircle.sacco.contribution.dto.ContributionSummaryResponse;
import com.innercircle.sacco.contribution.dto.RecordContributionRequest;
import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionCategory;
import com.innercircle.sacco.contribution.entity.ContributionStatus;
import com.innercircle.sacco.contribution.entity.PaymentMode;
import com.innercircle.sacco.contribution.repository.ContributionCategoryRepository;
import com.innercircle.sacco.contribution.repository.ContributionPenaltyRepository;
import com.innercircle.sacco.contribution.repository.ContributionRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionServiceImplTest {

    @Mock
    private ContributionRepository contributionRepository;

    @Mock
    private ContributionPenaltyRepository penaltyRepository;

    @Mock
    private ContributionCategoryRepository categoryRepository;

    @Mock
    private EventOutboxWriter outboxWriter;

    @InjectMocks
    private ContributionServiceImpl contributionService;

    @Captor
    private ArgumentCaptor<ContributionReceivedEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<Contribution> contributionCaptor;

    private ContributionCategory sharesCategory;
    private ContributionCategory welfareCategory;
    private Contribution sampleContribution;
    private UUID contributionId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        contributionId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        sharesCategory = new ContributionCategory("Shares", "Monthly shares", true, true);
        sharesCategory.setId(UUID.randomUUID());

        welfareCategory = new ContributionCategory("Welfare", "Welfare fund", true, false);
        welfareCategory.setId(UUID.randomUUID());

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
    // recordContribution()
    // -------------------------------------------------------
    @Nested
    @DisplayName("recordContribution()")
    class RecordContribution {

        @Test
        @DisplayName("should record contribution with PENDING status")
        void shouldRecordContributionWithPendingStatus() {
            RecordContributionRequest request = new RecordContributionRequest(
                    memberId, new BigDecimal("1000.00"), sharesCategory.getId(),
                    PaymentMode.MPESA, LocalDate.of(2024, 6, 1),
                    LocalDate.of(2024, 6, 5), "REF-001", "Monthly contribution"
            );

            when(categoryRepository.findById(sharesCategory.getId())).thenReturn(Optional.of(sharesCategory));
            when(contributionRepository.existsByReferenceNumber("REF-001")).thenReturn(false);
            when(contributionRepository.save(any(Contribution.class))).thenAnswer(inv -> {
                Contribution c = inv.getArgument(0);
                c.setId(contributionId);
                return c;
            });

            Contribution result = contributionService.recordContribution(request);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ContributionStatus.PENDING);
            assertThat(result.getMemberId()).isEqualTo(memberId);
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(result.getCategory()).isEqualTo(sharesCategory);
            assertThat(result.getPaymentMode()).isEqualTo(PaymentMode.MPESA);
            assertThat(result.getContributionMonth()).isEqualTo(LocalDate.of(2024, 6, 1));
            verify(contributionRepository).save(any(Contribution.class));
        }

        @Test
        @DisplayName("should throw when reference number already exists")
        void shouldThrowWhenReferenceNumberExists() {
            RecordContributionRequest request = new RecordContributionRequest(
                    memberId, new BigDecimal("1000.00"), sharesCategory.getId(),
                    PaymentMode.MPESA, LocalDate.of(2024, 6, 1),
                    LocalDate.of(2024, 6, 5), "REF-001", "Monthly contribution"
            );

            when(contributionRepository.existsByReferenceNumber("REF-001")).thenReturn(true);

            assertThatThrownBy(() -> contributionService.recordContribution(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Reference number already exists");

            verify(contributionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should record contribution without reference number")
        void shouldRecordContributionWithoutReferenceNumber() {
            RecordContributionRequest request = new RecordContributionRequest(
                    memberId, new BigDecimal("500.00"), welfareCategory.getId(),
                    PaymentMode.CASH, LocalDate.of(2024, 7, 1),
                    LocalDate.of(2024, 7, 5), null, null
            );

            when(categoryRepository.findById(welfareCategory.getId())).thenReturn(Optional.of(welfareCategory));
            when(contributionRepository.save(any(Contribution.class))).thenAnswer(inv -> {
                Contribution c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            Contribution result = contributionService.recordContribution(request);

            assertThat(result).isNotNull();
            assertThat(result.getReferenceNumber()).isNull();
            assertThat(result.getNotes()).isNull();
        }

        @Test
        @DisplayName("should throw when category not found")
        void shouldThrowWhenCategoryNotFound() {
            UUID badCategoryId = UUID.randomUUID();
            RecordContributionRequest request = new RecordContributionRequest(
                    memberId, new BigDecimal("1000.00"), badCategoryId,
                    PaymentMode.MPESA, LocalDate.of(2024, 6, 1),
                    LocalDate.of(2024, 6, 5), null, null
            );

            when(categoryRepository.findById(badCategoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contributionService.recordContribution(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Category not found");
        }

        @Test
        @DisplayName("should throw when category is inactive")
        void shouldThrowWhenCategoryIsInactive() {
            ContributionCategory inactive = new ContributionCategory("Old", "Disabled", false, false);
            inactive.setId(UUID.randomUUID());

            RecordContributionRequest request = new RecordContributionRequest(
                    memberId, new BigDecimal("1000.00"), inactive.getId(),
                    PaymentMode.BANK, LocalDate.of(2024, 6, 1),
                    LocalDate.of(2024, 6, 5), null, null
            );

            when(categoryRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> contributionService.recordContribution(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Category is not active");
        }
    }

    // -------------------------------------------------------
    // recordBulk()
    // -------------------------------------------------------
    @Nested
    @DisplayName("recordBulk()")
    class RecordBulk {

        @Test
        @DisplayName("should record multiple contributions in bulk")
        void shouldRecordBulk() {
            BulkContributionItemRequest req1 = new BulkContributionItemRequest(
                    memberId, new BigDecimal("1000.00"),
                    null, null, null, "REF-B1", "Member 1"
            );
            UUID member2 = UUID.randomUUID();
            BulkContributionItemRequest req2 = new BulkContributionItemRequest(
                    member2, new BigDecimal("2000.00"),
                    null, null, null, "REF-B2", "Member 2"
            );

            BulkContributionRequest bulk = new BulkContributionRequest(
                    PaymentMode.MPESA,
                    LocalDate.of(2024, 6, 1),
                    LocalDate.of(2024, 6, 5),
                    sharesCategory.getId(),
                    "BATCH-001",
                    List.of(req1, req2)
            );

            when(categoryRepository.findById(sharesCategory.getId())).thenReturn(Optional.of(sharesCategory));
            when(contributionRepository.existsByReferenceNumber(any())).thenReturn(false);
            when(contributionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<Contribution> results = contributionService.recordBulk(bulk);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getMemberId()).isEqualTo(memberId);
            assertThat(results.get(1).getMemberId()).isEqualTo(member2);
            assertThat(results.get(0).getPaymentMode()).isEqualTo(PaymentMode.MPESA);
        }

        @Test
        @DisplayName("should throw if any reference number in batch already exists")
        void shouldThrowOnDuplicateRefInBatch() {
            BulkContributionItemRequest req = new BulkContributionItemRequest(
                    memberId, new BigDecimal("1000.00"),
                    null, null, null, "REF-EXISTS", null
            );

            BulkContributionRequest bulk = new BulkContributionRequest(
                    PaymentMode.CASH, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 5),
                    sharesCategory.getId(), "BATCH-002", List.of(req)
            );

            when(categoryRepository.findById(sharesCategory.getId())).thenReturn(Optional.of(sharesCategory));
            when(contributionRepository.existsByReferenceNumber("REF-EXISTS")).thenReturn(true);

            assertThatThrownBy(() -> contributionService.recordBulk(bulk))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Reference number already exists");
        }
    }

    // -------------------------------------------------------
    // confirmContribution()
    // -------------------------------------------------------
    @Nested
    @DisplayName("confirmContribution()")
    class ConfirmContribution {

        @Test
        @DisplayName("should confirm a pending contribution and publish event")
        void shouldConfirmPendingContribution() {
            sampleContribution.setStatus(ContributionStatus.PENDING);

            when(contributionRepository.findById(contributionId))
                    .thenReturn(Optional.of(sampleContribution));
            when(contributionRepository.save(sampleContribution))
                    .thenReturn(sampleContribution);

            Contribution result = contributionService.confirmContribution(contributionId, "treasurer");

            assertThat(result.getStatus()).isEqualTo(ContributionStatus.CONFIRMED);
            verify(outboxWriter).write(eventCaptor.capture(), eq("Contribution"), any(UUID.class));

            ContributionReceivedEvent event = eventCaptor.getValue();
            assertThat(event.contributionId()).isEqualTo(contributionId);
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.amount()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(event.actor()).isEqualTo("treasurer");
        }

        @Test
        @DisplayName("should throw when contribution is already confirmed")
        void shouldThrowWhenAlreadyConfirmed() {
            sampleContribution.setStatus(ContributionStatus.CONFIRMED);

            when(contributionRepository.findById(contributionId))
                    .thenReturn(Optional.of(sampleContribution));

            assertThatThrownBy(() -> contributionService.confirmContribution(contributionId, "actor"))
                    .isInstanceOf(InvalidStateTransitionException.class);

            verify(outboxWriter, never()).write(any(), any(), any());
        }

        @Test
        @DisplayName("should throw when trying to confirm a reversed contribution")
        void shouldThrowWhenConfirmingReversed() {
            sampleContribution.setStatus(ContributionStatus.REVERSED);

            when(contributionRepository.findById(contributionId))
                    .thenReturn(Optional.of(sampleContribution));

            assertThatThrownBy(() -> contributionService.confirmContribution(contributionId, "actor"))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for unknown ID")
        void shouldThrowNotFoundWhenConfirming() {
            UUID unknownId = UUID.randomUUID();
            when(contributionRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contributionService.confirmContribution(unknownId, "actor"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Contribution");
        }
    }

    // -------------------------------------------------------
    // reverseContribution()
    // -------------------------------------------------------
    @Nested
    @DisplayName("reverseContribution()")
    class ReverseContribution {

        @Test
        @DisplayName("should reverse a contribution")
        void shouldReverseContribution() {
            sampleContribution.setStatus(ContributionStatus.CONFIRMED);

            when(contributionRepository.findById(contributionId))
                    .thenReturn(Optional.of(sampleContribution));
            when(contributionRepository.save(sampleContribution))
                    .thenReturn(sampleContribution);

            Contribution result = contributionService.reverseContribution(contributionId, "admin");

            assertThat(result.getStatus()).isEqualTo(ContributionStatus.REVERSED);
            verify(contributionRepository).save(sampleContribution);
        }

        @Test
        @DisplayName("should throw when already reversed")
        void shouldThrowWhenAlreadyReversed() {
            sampleContribution.setStatus(ContributionStatus.REVERSED);

            when(contributionRepository.findById(contributionId))
                    .thenReturn(Optional.of(sampleContribution));

            assertThatThrownBy(() -> contributionService.reverseContribution(contributionId, "admin"))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    // -------------------------------------------------------
    // findById()
    // -------------------------------------------------------
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return contribution when found")
        void shouldReturnContributionWhenFound() {
            when(contributionRepository.findById(contributionId))
                    .thenReturn(Optional.of(sampleContribution));

            Contribution result = contributionService.findById(contributionId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(contributionId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(contributionRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contributionService.findById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Contribution");
        }
    }

    // -------------------------------------------------------
    // list()
    // -------------------------------------------------------
    @Nested
    @DisplayName("list()")
    class ListContributions {

        @Test
        @DisplayName("should list contributions without filters")
        @SuppressWarnings("unchecked")
        void shouldListWithoutFilters() {
            when(contributionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleContribution)));

            CursorPage<Contribution> result = contributionService.list(null, 20, null, null, null, null);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasMore()).isFalse();
        }

        @Test
        @DisplayName("should return empty page when no contributions")
        @SuppressWarnings("unchecked")
        void shouldReturnEmptyPage() {
            when(contributionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            CursorPage<Contribution> result = contributionService.list(null, 20, null, null, null, null);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasMore()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }
    }

    // -------------------------------------------------------
    // getMemberSummary()
    // -------------------------------------------------------
    @Nested
    @DisplayName("getMemberSummary()")
    class GetMemberSummary {

        @Test
        @DisplayName("should return correct member summary")
        void shouldReturnCorrectSummary() {
            when(contributionRepository.sumConfirmedContributionsByMember(memberId))
                    .thenReturn(new BigDecimal("5000.00"));
            when(contributionRepository.sumPendingContributionsByMember(memberId))
                    .thenReturn(new BigDecimal("1000.00"));
            when(penaltyRepository.sumUnwaivedPenaltiesByMember(memberId))
                    .thenReturn(new BigDecimal("200.00"));
            when(contributionRepository.findLastContributionDate(memberId))
                    .thenReturn(Optional.of(LocalDate.of(2024, 6, 1)));

            ContributionSummaryResponse summary = contributionService.getMemberSummary(memberId);

            assertThat(summary.getMemberId()).isEqualTo(memberId);
            assertThat(summary.getTotalContributed()).isEqualTo(new BigDecimal("5000.00"));
            assertThat(summary.getTotalPending()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(summary.getTotalPenalties()).isEqualTo(new BigDecimal("200.00"));
            assertThat(summary.getLastContributionDate()).isEqualTo(LocalDate.of(2024, 6, 1));
        }

        @Test
        @DisplayName("should return zero summary for new member")
        void shouldReturnZeroSummaryForNewMember() {
            when(contributionRepository.sumConfirmedContributionsByMember(memberId))
                    .thenReturn(BigDecimal.ZERO);
            when(contributionRepository.sumPendingContributionsByMember(memberId))
                    .thenReturn(BigDecimal.ZERO);
            when(penaltyRepository.sumUnwaivedPenaltiesByMember(memberId))
                    .thenReturn(BigDecimal.ZERO);
            when(contributionRepository.findLastContributionDate(memberId))
                    .thenReturn(Optional.empty());

            ContributionSummaryResponse summary = contributionService.getMemberSummary(memberId);

            assertThat(summary.getTotalContributed()).isEqualTo(BigDecimal.ZERO);
            assertThat(summary.getLastContributionDate()).isNull();
        }
    }
}
