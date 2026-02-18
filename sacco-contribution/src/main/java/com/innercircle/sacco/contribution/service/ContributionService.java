package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.contribution.dto.BulkContributionRequest;
import com.innercircle.sacco.contribution.dto.ContributionSummaryResponse;
import com.innercircle.sacco.contribution.dto.RecordContributionRequest;
import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing member contributions.
 */
public interface ContributionService {

    /**
     * Record a new contribution from a request DTO.
     */
    Contribution recordContribution(RecordContributionRequest request);

    /**
     * Record multiple contributions at once.
     */
    List<Contribution> recordBulk(BulkContributionRequest request);

    /**
     * Confirm a contribution and publish ContributionReceivedEvent.
     */
    Contribution confirmContribution(UUID contributionId, String actor);

    /**
     * Reverse a contribution.
     */
    Contribution reverseContribution(UUID contributionId, String actor);

    /**
     * Find contribution by ID.
     */
    Contribution findById(UUID contributionId);

    /**
     * List contributions with cursor pagination and optional filters.
     */
    CursorPage<Contribution> list(
            String cursor,
            int size,
            ContributionStatus status,
            UUID categoryId,
            UUID memberId,
            LocalDate contributionMonth
    );

    /**
     * Get contributions for a specific member.
     */
    CursorPage<Contribution> getMemberContributions(UUID memberId, String cursor, int size, LocalDate contributionMonth);

    /**
     * Get contribution summary for a member.
     */
    ContributionSummaryResponse getMemberSummary(UUID memberId);
}
