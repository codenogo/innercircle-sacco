package com.innercircle.sacco.member.repository;

import com.innercircle.sacco.member.entity.MemberExitInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberExitInstallmentRepository extends JpaRepository<MemberExitInstallment, UUID> {
    List<MemberExitInstallment> findByExitRequestIdOrderByInstallmentNumberAsc(UUID exitRequestId);
    Optional<MemberExitInstallment> findFirstByExitRequestIdAndProcessedFalseOrderByInstallmentNumberAsc(UUID exitRequestId);
}
