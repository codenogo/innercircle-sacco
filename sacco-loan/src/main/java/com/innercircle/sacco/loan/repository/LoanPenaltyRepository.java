package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.LoanPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanPenaltyRepository extends JpaRepository<LoanPenalty, UUID> {

    List<LoanPenalty> findByLoanId(UUID loanId);

    List<LoanPenalty> findByMemberId(UUID memberId);

    List<LoanPenalty> findByLoanIdAndAppliedTrue(UUID loanId);

    List<LoanPenalty> findByLoanIdAndAppliedFalse(UUID loanId);
}
