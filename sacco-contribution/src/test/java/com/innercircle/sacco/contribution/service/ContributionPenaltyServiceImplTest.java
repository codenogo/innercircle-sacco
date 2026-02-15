package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.contribution.entity.ContributionPenalty;
import com.innercircle.sacco.contribution.repository.ContributionPenaltyRepository;
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionPenaltyServiceImplTest {

    @Mock
    private ContributionPenaltyRepository penaltyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ContributionPenaltyServiceImpl penaltyService;

    @Captor
    private ArgumentCaptor<PenaltyAppliedEvent> eventCaptor;

    private ContributionPenalty samplePenalty;
    private UUID penaltyId;
    private UUID memberId;
    private UUID contributionId;

    @BeforeEach
    void setUp() {
        penaltyId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        contributionId = UUID.randomUUID();

        samplePenalty = new ContributionPenalty(
                memberId,
                contributionId,
                new BigDecimal("500.00"),
                "Late contribution penalty"
        );
        samplePenalty.setId(penaltyId);
    }

    // -------------------------------------------------------
    // applyPenalty()
    // -------------------------------------------------------
    @Nested
    @DisplayName("applyPenalty()")
    class ApplyPenalty {

        @Test
        @DisplayName("should apply penalty and publish PenaltyAppliedEvent")
        void shouldApplyPenaltyAndPublishEvent() {
            when(penaltyRepository.save(samplePenalty)).thenReturn(samplePenalty);

            ContributionPenalty result = penaltyService.applyPenalty(samplePenalty, "admin");

            assertThat(result).isNotNull();
            assertThat(result.isWaived()).isFalse();
            assertThat(result.getMemberId()).isEqualTo(memberId);
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("500.00"));
            assertThat(result.getReason()).isEqualTo("Late contribution penalty");

            verify(penaltyRepository).save(samplePenalty);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            PenaltyAppliedEvent event = eventCaptor.getValue();
            assertThat(event.penaltyId()).isEqualTo(penaltyId);
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.amount()).isEqualTo(new BigDecimal("500.00"));
            assertThat(event.penaltyType()).isEqualTo("CONTRIBUTION_PENALTY");
            assertThat(event.actor()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should set waived to false even if penalty input has waived=true")
        void shouldForceWaivedToFalse() {
            samplePenalty.setWaived(true);

            when(penaltyRepository.save(samplePenalty)).thenReturn(samplePenalty);

            ContributionPenalty result = penaltyService.applyPenalty(samplePenalty, "admin");

            assertThat(result.isWaived()).isFalse();
        }

        @Test
        @DisplayName("should apply penalty with different actor")
        void shouldApplyPenaltyWithDifferentActor() {
            when(penaltyRepository.save(samplePenalty)).thenReturn(samplePenalty);

            penaltyService.applyPenalty(samplePenalty, "SYSTEM");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().actor()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("should apply penalty without contribution ID")
        void shouldApplyPenaltyWithoutContributionId() {
            ContributionPenalty noContribution = new ContributionPenalty(
                    memberId,
                    null,
                    new BigDecimal("100.00"),
                    "Manual penalty"
            );
            noContribution.setId(UUID.randomUUID());

            when(penaltyRepository.save(noContribution)).thenReturn(noContribution);

            ContributionPenalty result = penaltyService.applyPenalty(noContribution, "treasurer");

            assertThat(result).isNotNull();
            assertThat(result.getContributionId()).isNull();
            verify(penaltyRepository).save(noContribution);
        }
    }

    // -------------------------------------------------------
    // waivePenalty()
    // -------------------------------------------------------
    @Nested
    @DisplayName("waivePenalty()")
    class WaivePenalty {

        @Test
        @DisplayName("should waive an unwaived penalty")
        void shouldWaiveUnwaivedPenalty() {
            samplePenalty.setWaived(false);

            when(penaltyRepository.findById(penaltyId)).thenReturn(Optional.of(samplePenalty));
            when(penaltyRepository.save(samplePenalty)).thenReturn(samplePenalty);

            ContributionPenalty result = penaltyService.waivePenalty(penaltyId, "treasurer");

            assertThat(result.isWaived()).isTrue();
            assertThat(result.getWaivedBy()).isEqualTo("treasurer");
            assertThat(result.getWaivedAt()).isNotNull();
            verify(penaltyRepository).save(samplePenalty);
        }

        @Test
        @DisplayName("should set waivedAt timestamp when waiving penalty")
        void shouldSetWaivedAtTimestamp() {
            Instant before = Instant.now();
            samplePenalty.setWaived(false);

            when(penaltyRepository.findById(penaltyId)).thenReturn(Optional.of(samplePenalty));
            when(penaltyRepository.save(samplePenalty)).thenReturn(samplePenalty);

            ContributionPenalty result = penaltyService.waivePenalty(penaltyId, "admin");
            Instant after = Instant.now();

            assertThat(result.getWaivedAt()).isAfterOrEqualTo(before);
            assertThat(result.getWaivedAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should throw BusinessException when penalty is already waived")
        void shouldThrowWhenAlreadyWaived() {
            samplePenalty.setWaived(true);
            samplePenalty.setWaivedBy("previous-admin");
            samplePenalty.setWaivedAt(Instant.now());

            when(penaltyRepository.findById(penaltyId)).thenReturn(Optional.of(samplePenalty));

            assertThatThrownBy(() -> penaltyService.waivePenalty(penaltyId, "treasurer"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Penalty is already waived");

            verify(penaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when penalty not found")
        void shouldThrowWhenPenaltyNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(penaltyRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> penaltyService.waivePenalty(unknownId, "treasurer"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ContributionPenalty");
        }
    }

    // -------------------------------------------------------
    // getMemberPenalties()
    // -------------------------------------------------------
    @Nested
    @DisplayName("getMemberPenalties()")
    class GetMemberPenalties {

        @Test
        @DisplayName("should return all penalties for a member")
        void shouldReturnAllPenalties() {
            ContributionPenalty penalty2 = new ContributionPenalty(
                    memberId, UUID.randomUUID(),
                    new BigDecimal("300.00"), "Second penalty"
            );
            penalty2.setId(UUID.randomUUID());

            when(penaltyRepository.findByMemberId(memberId))
                    .thenReturn(List.of(samplePenalty, penalty2));

            List<ContributionPenalty> result = penaltyService.getMemberPenalties(memberId);

            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(samplePenalty, penalty2);
            verify(penaltyRepository).findByMemberId(memberId);
        }

        @Test
        @DisplayName("should return empty list when member has no penalties")
        void shouldReturnEmptyListWhenNoPenalties() {
            when(penaltyRepository.findByMemberId(memberId)).thenReturn(Collections.emptyList());

            List<ContributionPenalty> result = penaltyService.getMemberPenalties(memberId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should include both waived and unwaived penalties")
        void shouldIncludeBothWaivedAndUnwaived() {
            ContributionPenalty waivedPenalty = new ContributionPenalty(
                    memberId, UUID.randomUUID(),
                    new BigDecimal("200.00"), "Waived penalty"
            );
            waivedPenalty.setId(UUID.randomUUID());
            waivedPenalty.setWaived(true);
            waivedPenalty.setWaivedBy("admin");
            waivedPenalty.setWaivedAt(Instant.now());

            when(penaltyRepository.findByMemberId(memberId))
                    .thenReturn(List.of(samplePenalty, waivedPenalty));

            List<ContributionPenalty> result = penaltyService.getMemberPenalties(memberId);

            assertThat(result).hasSize(2);
        }
    }

    // -------------------------------------------------------
    // getUnwaivedPenalties()
    // -------------------------------------------------------
    @Nested
    @DisplayName("getUnwaivedPenalties()")
    class GetUnwaivedPenalties {

        @Test
        @DisplayName("should return only unwaived penalties for a member")
        void shouldReturnOnlyUnwaivedPenalties() {
            when(penaltyRepository.findByMemberIdAndWaivedFalse(memberId))
                    .thenReturn(List.of(samplePenalty));

            List<ContributionPenalty> result = penaltyService.getUnwaivedPenalties(memberId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isWaived()).isFalse();
            verify(penaltyRepository).findByMemberIdAndWaivedFalse(memberId);
        }

        @Test
        @DisplayName("should return empty list when all penalties are waived")
        void shouldReturnEmptyWhenAllWaived() {
            when(penaltyRepository.findByMemberIdAndWaivedFalse(memberId))
                    .thenReturn(Collections.emptyList());

            List<ContributionPenalty> result = penaltyService.getUnwaivedPenalties(memberId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when member has no penalties at all")
        void shouldReturnEmptyWhenNoPenalties() {
            UUID unknownMemberId = UUID.randomUUID();

            when(penaltyRepository.findByMemberIdAndWaivedFalse(unknownMemberId))
                    .thenReturn(Collections.emptyList());

            List<ContributionPenalty> result = penaltyService.getUnwaivedPenalties(unknownMemberId);

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------
    // findById()
    // -------------------------------------------------------
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return penalty when found")
        void shouldReturnPenaltyWhenFound() {
            when(penaltyRepository.findById(penaltyId)).thenReturn(Optional.of(samplePenalty));

            ContributionPenalty result = penaltyService.findById(penaltyId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(penaltyId);
            assertThat(result.getMemberId()).isEqualTo(memberId);
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when penalty not found")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(penaltyRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> penaltyService.findById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ContributionPenalty")
                    .hasMessageContaining(unknownId.toString());
        }
    }
}
