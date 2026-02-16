package com.innercircle.sacco.member.guard;

import com.innercircle.sacco.common.guard.TransitionGuard;
import com.innercircle.sacco.member.entity.MemberStatus;

import static com.innercircle.sacco.member.entity.MemberStatus.*;

public final class MemberTransitionGuards {

    public static final TransitionGuard<MemberStatus> MEMBER = TransitionGuard.<MemberStatus>builder("Member")
            .allow(ACTIVE, SUSPENDED)
            .allow(SUSPENDED, ACTIVE)
            .allow(ACTIVE, DEACTIVATED)
            .allow(SUSPENDED, DEACTIVATED)
            .build();

    private MemberTransitionGuards() {
    }
}
