package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.LoanInterestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanInterestHistoryRepository extends JpaRepository<LoanInterestHistory, UUID> {

    List<LoanInterestHistory> findByLoanIdOrderByAccrualDateDesc(UUID loanId);

    List<LoanInterestHistory> findByLoanIdAndAccrualDateBetween(UUID loanId, LocalDate start, LocalDate end);

    List<LoanInterestHistory> findByAccrualDateBetween(LocalDate start, LocalDate end);

    List<LoanInterestHistory> findByMemberIdOrderByAccrualDateDesc(UUID memberId);
}
