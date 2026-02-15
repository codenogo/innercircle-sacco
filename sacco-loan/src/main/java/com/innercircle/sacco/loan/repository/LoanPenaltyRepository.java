package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.LoanPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanPenaltyRepository extends JpaRepository<LoanPenalty, UUID> {

    List<LoanPenalty> findByLoanId(UUID loanId);

    List<LoanPenalty> findByMemberId(UUID memberId);

    List<LoanPenalty> findByLoanIdAndAppliedTrue(UUID loanId);

    List<LoanPenalty> findByLoanIdAndAppliedFalse(UUID loanId);

    List<LoanPenalty> findByLoanIdAndPaidFalse(UUID loanId);

    List<LoanPenalty> findByLoanIdAndPaidFalseOrderByAppliedAtAsc(UUID loanId);

    List<LoanPenalty> findByLoanIdAndScheduleId(UUID loanId, UUID scheduleId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM LoanPenalty p WHERE p.loanId = :loanId AND p.paid = false")
    BigDecimal sumUnpaidAmountByLoanId(@Param("loanId") UUID loanId);
}
