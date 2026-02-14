package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, UUID> {

    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumber(UUID loanId);

    List<RepaymentSchedule> findByLoanIdAndPaidFalseOrderByDueDate(UUID loanId);

    List<RepaymentSchedule> findByDueDateBeforeAndPaidFalse(LocalDate date);
}
