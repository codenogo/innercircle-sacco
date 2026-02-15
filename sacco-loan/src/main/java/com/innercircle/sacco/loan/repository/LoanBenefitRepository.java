package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.LoanBenefit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanBenefitRepository extends JpaRepository<LoanBenefit, UUID> {

    List<LoanBenefit> findByMemberId(UUID memberId);

    List<LoanBenefit> findByLoanId(UUID loanId);

    List<LoanBenefit> findByIdGreaterThanOrderById(UUID cursor, Pageable pageable);

    @Query("SELECT lb FROM LoanBenefit lb WHERE lb.memberId = :memberId ORDER BY lb.createdAt DESC")
    List<LoanBenefit> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") UUID memberId);

    @Query("SELECT lb FROM LoanBenefit lb WHERE lb.loanId = :loanId ORDER BY lb.earnedAmount DESC")
    List<LoanBenefit> findByLoanIdOrderByEarnedAmountDesc(@Param("loanId") UUID loanId);
}
