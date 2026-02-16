package com.innercircle.sacco.loan.guard;

import com.innercircle.sacco.common.guard.TransitionGuard;
import com.innercircle.sacco.loan.entity.LoanStatus;

import static com.innercircle.sacco.loan.entity.LoanStatus.*;

public final class LoanTransitionGuards {

    public static final TransitionGuard<LoanStatus> LOAN = TransitionGuard.<LoanStatus>builder("LoanApplication")
            .allow(PENDING, APPROVED)
            .allow(PENDING, REJECTED)
            .allow(APPROVED, DISBURSED)
            .allow(DISBURSED, REPAYING)
            .allow(REPAYING, CLOSED)
            .allow(REPAYING, DEFAULTED)
            .build();

    private LoanTransitionGuards() {
    }
}
