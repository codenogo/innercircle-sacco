package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.common.event.PenaltyAppliedEvent;
import com.innercircle.sacco.common.event.PenaltyWaivedEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.contribution.entity.ContributionPenalty;
import com.innercircle.sacco.contribution.repository.ContributionPenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of ContributionPenaltyService.
 */
@Service
@RequiredArgsConstructor
public class ContributionPenaltyServiceImpl implements ContributionPenaltyService {

    private final ContributionPenaltyRepository penaltyRepository;
    private final EventOutboxWriter outboxWriter;

    @Override
    @Transactional
    public ContributionPenalty applyPenalty(ContributionPenalty penalty, String actor) {
        penalty.setWaived(false);
        if (penalty.getPenaltyDate() == null) {
            penalty.setPenaltyDate(LocalDate.now());
        }
        if (penalty.getPenaltyCode() == null || penalty.getPenaltyCode().isBlank()) {
            penalty.setPenaltyCode("PEN-" + UUID.randomUUID());
        }
        ContributionPenalty savedPenalty = penaltyRepository.save(penalty);

        outboxWriter.write(new PenaltyAppliedEvent(
                savedPenalty.getId(),
                savedPenalty.getMemberId(),
                savedPenalty.getAmount(),
                "CONTRIBUTION_PENALTY",
                UUID.randomUUID(),
                actor
        ), "ContributionPenalty", savedPenalty.getId());

        return savedPenalty;
    }

    @Override
    @Transactional
    public ContributionPenalty waivePenalty(UUID penaltyId, String reason, String actor) {
        ContributionPenalty penalty = findById(penaltyId);
        String waiverReason = (reason == null || reason.isBlank()) ? "Manual waiver" : reason.trim();

        if (penalty.isWaived()) {
            throw new BusinessException("Penalty is already waived");
        }

        penalty.setWaived(true);
        penalty.setWaivedReason(waiverReason);
        penalty.setWaivedBy(actor);
        penalty.setWaivedAt(Instant.now());

        ContributionPenalty waivedPenalty = penaltyRepository.save(penalty);

        outboxWriter.write(new PenaltyWaivedEvent(
                waivedPenalty.getId(),
                waivedPenalty.getMemberId(),
                waivedPenalty.getAmount(),
                waiverReason,
                UUID.randomUUID(),
                actor
        ), "ContributionPenalty", waivedPenalty.getId());

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
    public List<ContributionPenalty> getPenalties(UUID memberId, YearMonth month) {
        if (month == null) {
            return penaltyRepository.findByMemberId(memberId);
        }
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        return penaltyRepository.findByMemberIdAndPenaltyDateBetween(memberId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionPenalty findById(UUID penaltyId) {
        return penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new ResourceNotFoundException("ContributionPenalty", penaltyId));
    }
}
