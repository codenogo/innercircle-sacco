package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.ContributionReceivedEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.contribution.dto.ContributionSummaryResponse;
import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionStatus;
import com.innercircle.sacco.contribution.repository.ContributionPenaltyRepository;
import com.innercircle.sacco.contribution.repository.ContributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of ContributionService.
 */
@Service
@RequiredArgsConstructor
public class ContributionServiceImpl implements ContributionService {

    private final ContributionRepository contributionRepository;
    private final ContributionPenaltyRepository penaltyRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Contribution recordContribution(Contribution contribution) {
        // Validate reference number uniqueness if provided
        if (contribution.getReferenceNumber() != null
                && contributionRepository.existsByReferenceNumber(contribution.getReferenceNumber())) {
            throw new BusinessException("Reference number already exists: " + contribution.getReferenceNumber());
        }

        // Ensure status is PENDING for new contributions
        contribution.setStatus(ContributionStatus.PENDING);

        return contributionRepository.save(contribution);
    }

    @Override
    @Transactional
    public Contribution confirmContribution(UUID contributionId, String actor) {
        Contribution contribution = findById(contributionId);

        if (contribution.getStatus() == ContributionStatus.CONFIRMED) {
            throw new BusinessException("Contribution is already confirmed");
        }

        if (contribution.getStatus() == ContributionStatus.REVERSED) {
            throw new BusinessException("Cannot confirm a reversed contribution");
        }

        contribution.setStatus(ContributionStatus.CONFIRMED);
        Contribution confirmed = contributionRepository.save(contribution);

        // Publish ContributionReceivedEvent for ledger integration
        eventPublisher.publishEvent(new ContributionReceivedEvent(
                confirmed.getId(),
                confirmed.getMemberId(),
                confirmed.getAmount(),
                confirmed.getReferenceNumber(),
                actor
        ));

        return confirmed;
    }

    @Override
    @Transactional
    public Contribution reverseContribution(UUID contributionId, String actor) {
        Contribution contribution = findById(contributionId);

        if (contribution.getStatus() == ContributionStatus.REVERSED) {
            throw new BusinessException("Contribution is already reversed");
        }

        contribution.setStatus(ContributionStatus.REVERSED);
        Contribution reversed = contributionRepository.save(contribution);

        // TODO: Publish ContributionReversedEvent when defined

        return reversed;
    }

    @Override
    @Transactional(readOnly = true)
    public Contribution findById(UUID contributionId) {
        return contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution", contributionId));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Contribution> list(String cursor, int size, ContributionStatus status) {
        UUID cursorId = (cursor != null && !cursor.isEmpty())
                ? UUID.fromString(cursor)
                : new UUID(0L, 0L);

        List<Contribution> contributions;
        if (status != null) {
            contributions = contributionRepository.findByStatusAndIdGreaterThanOrderById(
                    status, cursorId, PageRequest.of(0, size + 1)
            );
        } else {
            contributions = contributionRepository.findByIdGreaterThanOrderById(
                    cursorId, PageRequest.of(0, size + 1)
            );
        }

        boolean hasMore = contributions.size() > size;
        if (hasMore) {
            contributions = contributions.subList(0, size);
        }

        String nextCursor = hasMore && !contributions.isEmpty()
                ? contributions.get(contributions.size() - 1).getId().toString()
                : null;

        return CursorPage.of(contributions, nextCursor, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Contribution> getMemberContributions(UUID memberId, String cursor, int size) {
        UUID cursorId = (cursor != null && !cursor.isEmpty())
                ? UUID.fromString(cursor)
                : new UUID(0L, 0L);

        List<Contribution> contributions = contributionRepository
                .findByMemberIdAndIdGreaterThanOrderById(
                        memberId, cursorId, PageRequest.of(0, size + 1)
                );

        boolean hasMore = contributions.size() > size;
        if (hasMore) {
            contributions = contributions.subList(0, size);
        }

        String nextCursor = hasMore && !contributions.isEmpty()
                ? contributions.get(contributions.size() - 1).getId().toString()
                : null;

        return CursorPage.of(contributions, nextCursor, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionSummaryResponse getMemberSummary(UUID memberId) {
        BigDecimal totalConfirmed = contributionRepository.sumConfirmedContributionsByMember(memberId);
        BigDecimal totalPending = contributionRepository.sumPendingContributionsByMember(memberId);
        BigDecimal totalPenalties = penaltyRepository.sumUnwaivedPenaltiesByMember(memberId);
        LocalDate lastContributionDate = contributionRepository.findLastContributionDate(memberId)
                .orElse(null);

        return new ContributionSummaryResponse(
                memberId,
                totalConfirmed,
                totalPending,
                totalPenalties,
                lastContributionDate
        );
    }
}
