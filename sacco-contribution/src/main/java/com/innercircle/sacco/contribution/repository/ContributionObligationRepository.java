package com.innercircle.sacco.contribution.repository;

import com.innercircle.sacco.contribution.entity.ContributionObligation;
import com.innercircle.sacco.contribution.entity.ContributionObligationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContributionObligationRepository extends JpaRepository<ContributionObligation, UUID> {

    List<ContributionObligation> findByObligationMonthOrderByDueDateAsc(LocalDate obligationMonth);

    List<ContributionObligation> findByMemberIdAndObligationMonthOrderByDueDateAsc(UUID memberId, LocalDate obligationMonth);

    List<ContributionObligation> findByMemberIdAndStatusInOrderByDueDateAsc(
            UUID memberId,
            Collection<ContributionObligationStatus> statuses
    );

    Optional<ContributionObligation> findByMemberIdAndScheduleConfigIdAndObligationMonth(
            UUID memberId,
            UUID scheduleConfigId,
            LocalDate obligationMonth
    );

    boolean existsByMemberIdAndScheduleConfigIdAndObligationMonth(
            UUID memberId,
            UUID scheduleConfigId,
            LocalDate obligationMonth
    );
}
