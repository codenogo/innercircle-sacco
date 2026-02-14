package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LoanService {

    LoanApplication applyForLoan(UUID memberId, BigDecimal principalAmount, BigDecimal interestRate,
                                  Integer termMonths, String interestMethod, String purpose);

    LoanApplication approveLoan(UUID loanId, UUID approvedBy);

    LoanApplication rejectLoan(UUID loanId, UUID rejectedBy);

    LoanApplication disburseLoan(UUID loanId, String actor);

    LoanRepayment recordRepayment(UUID loanId, BigDecimal amount, String referenceNumber, String actor);

    LoanApplication closeLoan(UUID loanId);

    List<RepaymentSchedule> getLoanSchedule(UUID loanId);

    LoanApplication getLoanById(UUID loanId);

    List<LoanApplication> getMemberLoans(UUID memberId);
}
