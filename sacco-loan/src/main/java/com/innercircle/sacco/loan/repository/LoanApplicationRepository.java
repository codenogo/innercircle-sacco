package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    List<LoanApplication> findByIdGreaterThanOrderById(UUID cursor, Pageable pageable);

    List<LoanApplication> findByMemberIdAndIdGreaterThanOrderById(UUID memberId, UUID cursor, Pageable pageable);

    List<LoanApplication> findByStatusAndIdGreaterThanOrderById(LoanStatus status, UUID cursor, Pageable pageable);

    List<LoanApplication> findByMemberIdAndStatusAndIdGreaterThanOrderById(
            UUID memberId, LoanStatus status, UUID cursor, Pageable pageable);

    boolean existsByLoanNumber(String loanNumber);

    List<LoanApplication> findByMemberId(UUID memberId);

    List<LoanApplication> findByStatus(LoanStatus status);

    List<LoanApplication> findByStatusAndDisbursedAtAfter(LoanStatus status, Instant instant);

    List<LoanApplication> findByStatusAndDisbursedAtBefore(LoanStatus status, Instant instant);
}
