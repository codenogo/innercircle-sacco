package com.innercircle.sacco.member.repository;

import com.innercircle.sacco.member.entity.WelfareClaim;
import com.innercircle.sacco.member.entity.WelfareClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface WelfareClaimRepository extends JpaRepository<WelfareClaim, UUID> {
    List<WelfareClaim> findByMemberId(UUID memberId);
    long countByStatusIn(List<WelfareClaimStatus> statuses);
    long countByMemberIdAndEventCodeAndStatusInAndEventDateBetween(
            UUID memberId,
            String eventCode,
            List<WelfareClaimStatus> statuses,
            LocalDate startDate,
            LocalDate endDate
    );
}
