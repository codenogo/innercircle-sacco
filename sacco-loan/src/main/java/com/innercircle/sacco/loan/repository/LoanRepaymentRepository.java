package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.RepaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, UUID> {

    List<LoanRepayment> findByLoanIdOrderByRepaymentDateDesc(UUID loanId);

    List<LoanRepayment> findByMemberIdAndIdGreaterThanOrderById(UUID memberId, UUID cursor, Pageable pageable);

    List<LoanRepayment> findByLoanIdAndStatus(UUID loanId, RepaymentStatus status);
}
