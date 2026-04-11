package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.contribution.entity.ContributionPenalty;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing contribution penalties.
 */
public interface ContributionPenaltyService {

    /**
     * Apply a penalty to a member and publish PenaltyAppliedEvent.
     */
    ContributionPenalty applyPenalty(ContributionPenalty penalty, String actor);

    /**
     * Waive a penalty (requires TREASURER/ADMIN authorization).
     */
    ContributionPenalty waivePenalty(UUID penaltyId, String reason, String actor);

    /**
     * Get all penalties for a member.
     */
    List<ContributionPenalty> getMemberPenalties(UUID memberId);

    /**
     * Get unwaived penalties for a member.
     */
    List<ContributionPenalty> getUnwaivedPenalties(UUID memberId);

    /**
     * Get penalties with optional month filter.
     */
    List<ContributionPenalty> getPenalties(UUID memberId, YearMonth month);

    /**
     * Get a penalty by ID.
     */
    ContributionPenalty findById(UUID penaltyId);
}
