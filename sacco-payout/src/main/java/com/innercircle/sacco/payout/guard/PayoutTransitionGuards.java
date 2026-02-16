package com.innercircle.sacco.payout.guard;

import com.innercircle.sacco.common.guard.TransitionGuard;
import com.innercircle.sacco.payout.entity.PayoutStatus;

import static com.innercircle.sacco.payout.entity.PayoutStatus.*;

public final class PayoutTransitionGuards {

    public static final TransitionGuard<PayoutStatus> PAYOUT = TransitionGuard.<PayoutStatus>builder("Payout")
            .allow(PENDING, APPROVED)
            .allow(APPROVED, PROCESSED)
            .allow(APPROVED, FAILED)
            .allow(PENDING, FAILED)
            .build();

    private PayoutTransitionGuards() {
    }
}
