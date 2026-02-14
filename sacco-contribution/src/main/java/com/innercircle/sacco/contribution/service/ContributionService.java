package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.contribution.dto.ContributionSummaryResponse;
import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionStatus;

import java.util.UUID;

/**
 * Service for managing member contributions.
 */
public interface ContributionService {

    /**
     * Record a new contribution (status: PENDING).
     */
    Contribution recordContribution(Contribution contribution);

    /**
     * Confirm a contribution and publish ContributionReceivedEvent.
     */
    Contribution confirmContribution(UUID contributionId, String actor);

    /**
     * Reverse a contribution.
     */
    Contribution reverseContribution(UUID contributionId, String actor);

    /**
     * Get a contribution by ID.
     */
    Contribution findById(UUID contributionId);

    /**
     * Get contributions with cursor pagination.
     */
    CursorPage<Contribution> list(String cursor, int size, ContributionStatus status);

    /**
     * Get member contributions with cursor pagination.
     */
    CursorPage<Contribution> getMemberContributions(UUID memberId, String cursor, int size);

    /**
     * Get member contribution summary.
     */
    ContributionSummaryResponse getMemberSummary(UUID memberId);
}
