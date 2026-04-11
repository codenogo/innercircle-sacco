package com.innercircle.sacco.contribution.repository;

import com.innercircle.sacco.contribution.entity.ContributionPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ContributionPenalty entities.
 */
@Repository
public interface ContributionPenaltyRepository extends JpaRepository<ContributionPenalty, UUID> {

    boolean existsByPenaltyCode(String penaltyCode);

    /**
     * Find all penalties for a member.
     */
    List<ContributionPenalty> findByMemberId(UUID memberId);

    /**
     * Find unwaived penalties for a member.
     */
    List<ContributionPenalty> findByMemberIdAndWaivedFalse(UUID memberId);

    /**
     * Find penalties by contribution ID.
     */
    List<ContributionPenalty> findByContributionId(UUID contributionId);

    List<ContributionPenalty> findByMemberIdAndPenaltyDateBetween(UUID memberId, LocalDate startDate, LocalDate endDate);

    List<ContributionPenalty> findByPenaltyDateBetween(LocalDate startDate, LocalDate endDate);

    List<ContributionPenalty> findByObligationIdAndRuleTierIdAndPenaltyDate(UUID obligationId, UUID ruleTierId, LocalDate penaltyDate);

    /**
     * Calculate total unwaived penalties for a member.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM ContributionPenalty p " +
           "WHERE p.memberId = :memberId AND p.waived = false")
    BigDecimal sumUnwaivedPenaltiesByMember(@Param("memberId") UUID memberId);

    /**
     * Calculate total penalties (including waived) for a member.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM ContributionPenalty p " +
           "WHERE p.memberId = :memberId")
    BigDecimal sumAllPenaltiesByMember(@Param("memberId") UUID memberId);
}
