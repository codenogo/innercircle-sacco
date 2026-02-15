package com.innercircle.sacco.contribution.repository;

import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository for Contribution entities with cursor-based pagination support.
 */
@Repository
public interface ContributionRepository extends JpaRepository<Contribution, UUID>, JpaSpecificationExecutor<Contribution> {

    /**
     * Find contributions with cursor pagination.
     */
    List<Contribution> findByIdGreaterThanOrderById(UUID cursor, Pageable pageable);

    /**
     * Find contributions by member with cursor pagination.
     */
    List<Contribution> findByMemberIdAndIdGreaterThanOrderById(
            UUID memberId, UUID cursor, Pageable pageable);

    /**
     * Find contributions by member and status with cursor pagination.
     */
    List<Contribution> findByMemberIdAndStatusAndIdGreaterThanOrderById(
            UUID memberId, ContributionStatus status, UUID cursor, Pageable pageable);

    /**
     * Find contributions by status with cursor pagination.
     */
    List<Contribution> findByStatusAndIdGreaterThanOrderById(
            ContributionStatus status, UUID cursor, Pageable pageable);

    /**
     * Find contributions by member within a date range.
     */
    List<Contribution> findByMemberIdAndContributionDateBetween(
            UUID memberId, LocalDate startDate, LocalDate endDate);

    /**
     * Find by reference number.
     */
    Optional<Contribution> findByReferenceNumber(String referenceNumber);

    /**
     * Check if reference number exists.
     */
    boolean existsByReferenceNumber(String referenceNumber);

    /**
     * Calculate total confirmed contributions for a member.
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c " +
           "WHERE c.memberId = :memberId AND c.status = 'CONFIRMED'")
    BigDecimal sumConfirmedContributionsByMember(@Param("memberId") UUID memberId);

    /**
     * Calculate total pending contributions for a member.
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c " +
           "WHERE c.memberId = :memberId AND c.status = 'PENDING'")
    BigDecimal sumPendingContributionsByMember(@Param("memberId") UUID memberId);

    /**
     * Get last contribution date for a member.
     */
    @Query("SELECT MAX(c.contributionDate) FROM Contribution c " +
           "WHERE c.memberId = :memberId AND c.status = 'CONFIRMED'")
    Optional<LocalDate> findLastContributionDate(@Param("memberId") UUID memberId);
}
