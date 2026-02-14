package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.contribution.entity.ContributionPenalty;
import com.innercircle.sacco.contribution.repository.ContributionPenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of ContributionPenaltyService.
 */
@Service
@RequiredArgsConstructor
public class ContributionPenaltyServiceImpl implements ContributionPenaltyService {

    private final ContributionPenaltyRepository penaltyRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ContributionPenalty applyPenalty(ContributionPenalty penalty, String actor) {
        penalty.setWaived(false);
        ContributionPenalty savedPenalty = penaltyRepository.save(penalty);

        // Publish PenaltyAppliedEvent
        eventPublisher.publishEvent(new PenaltyAppliedEvent(
                savedPenalty.getId(),
                savedPenalty.getMemberId(),
                savedPenalty.getAmount(),
                "CONTRIBUTION_PENALTY",
                actor
        ));

        return savedPenalty;
    }

    @Override
    @Transactional
    public ContributionPenalty waivePenalty(UUID penaltyId, String actor) {
        ContributionPenalty penalty = findById(penaltyId);

        if (penalty.isWaived()) {
            throw new BusinessException("Penalty is already waived");
        }

        penalty.setWaived(true);
        penalty.setWaivedBy(actor);
        penalty.setWaivedAt(Instant.now());

        ContributionPenalty waivedPenalty = penaltyRepository.save(penalty);

        // TODO: Publish PenaltyWaivedEvent when defined

        return waivedPenalty;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributionPenalty> getMemberPenalties(UUID memberId) {
        return penaltyRepository.findByMemberId(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributionPenalty> getUnwaivedPenalties(UUID memberId) {
        return penaltyRepository.findByMemberIdAndWaivedFalse(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionPenalty findById(UUID penaltyId) {
        return penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new ResourceNotFoundException("ContributionPenalty", penaltyId));
    }
}
