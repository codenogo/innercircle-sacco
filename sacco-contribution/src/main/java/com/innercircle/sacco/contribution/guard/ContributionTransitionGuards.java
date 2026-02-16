package com.innercircle.sacco.contribution.guard;

import com.innercircle.sacco.common.guard.TransitionGuard;
import com.innercircle.sacco.contribution.entity.ContributionStatus;

import static com.innercircle.sacco.contribution.entity.ContributionStatus.*;

public final class ContributionTransitionGuards {

    public static final TransitionGuard<ContributionStatus> CONTRIBUTION =
            TransitionGuard.<ContributionStatus>builder("Contribution")
                    .allow(PENDING, CONFIRMED)
                    .allow(CONFIRMED, REVERSED)
                    .build();

    private ContributionTransitionGuards() {
    }
}
