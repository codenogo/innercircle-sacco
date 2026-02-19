package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LoanService {

    LoanApplication applyForLoan(UUID memberId, UUID loanProductId, BigDecimal principalAmount,
                                  Integer termMonths, String purpose, String actor);

    LoanApplication approveLoan(UUID loanId, UUID approvedBy, String actor,
                                 String overrideReason, boolean isAdmin);

    LoanApplication rejectLoan(UUID loanId, UUID rejectedBy, String actor,
                                String overrideReason, boolean isAdmin);

    LoanApplication disburseLoan(UUID loanId, String actor);

    LoanRepayment recordRepayment(UUID loanId, BigDecimal amount, String referenceNumber, String actor);

    LoanApplication closeLoan(UUID loanId);

    List<RepaymentSchedule> getLoanSchedule(UUID loanId);

    LoanApplication getLoanById(UUID loanId);

    List<LoanApplication> getMemberLoans(UUID memberId);
}
