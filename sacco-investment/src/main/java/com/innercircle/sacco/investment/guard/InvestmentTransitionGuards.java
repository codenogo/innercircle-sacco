package com.innercircle.sacco.investment.guard;

import com.innercircle.sacco.common.guard.TransitionGuard;
import com.innercircle.sacco.investment.entity.InvestmentStatus;

import static com.innercircle.sacco.investment.entity.InvestmentStatus.ACTIVE;
import static com.innercircle.sacco.investment.entity.InvestmentStatus.APPROVED;
import static com.innercircle.sacco.investment.entity.InvestmentStatus.CLOSED;
import static com.innercircle.sacco.investment.entity.InvestmentStatus.MATURED;
import static com.innercircle.sacco.investment.entity.InvestmentStatus.PARTIALLY_DISPOSED;
import static com.innercircle.sacco.investment.entity.InvestmentStatus.PROPOSED;
import static com.innercircle.sacco.investment.entity.InvestmentStatus.REJECTED;
import static com.innercircle.sacco.investment.entity.InvestmentStatus.ROLLED_OVER;

public final class InvestmentTransitionGuards {

    public static final TransitionGuard<InvestmentStatus> INVESTMENT =
            TransitionGuard.<InvestmentStatus>builder("Investment")
                    .allow(PROPOSED, APPROVED)
                    .allow(PROPOSED, REJECTED)
                    .allow(APPROVED, ACTIVE)
                    .allow(APPROVED, REJECTED)
                    .allow(ACTIVE, PARTIALLY_DISPOSED)
                    .allow(ACTIVE, MATURED)
                    .allow(ACTIVE, CLOSED)
                    .allow(ACTIVE, ROLLED_OVER)
                    .allow(PARTIALLY_DISPOSED, PARTIALLY_DISPOSED)
                    .allow(PARTIALLY_DISPOSED, MATURED)
                    .allow(PARTIALLY_DISPOSED, CLOSED)
                    .allow(PARTIALLY_DISPOSED, ROLLED_OVER)
                    .allow(MATURED, ROLLED_OVER)
                    .allow(MATURED, CLOSED)
                    .allow(ROLLED_OVER, ACTIVE)
                    .allow(ROLLED_OVER, PARTIALLY_DISPOSED)
                    .allow(ROLLED_OVER, MATURED)
                    .allow(ROLLED_OVER, CLOSED)
                    .build();

    private InvestmentTransitionGuards() {
    }
}
