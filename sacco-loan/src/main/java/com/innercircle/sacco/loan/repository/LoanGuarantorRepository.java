package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.GuarantorStatus;
import com.innercircle.sacco.loan.entity.LoanGuarantor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanGuarantorRepository extends JpaRepository<LoanGuarantor, UUID> {

    List<LoanGuarantor> findByLoanId(UUID loanId);

    List<LoanGuarantor> findByGuarantorMemberId(UUID guarantorMemberId);

    List<LoanGuarantor> findByLoanIdAndStatus(UUID loanId, GuarantorStatus status);
}
