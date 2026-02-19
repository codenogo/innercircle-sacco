package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.entity.Payout;
import com.innercircle.sacco.payout.entity.PayoutStatus;
import com.innercircle.sacco.payout.entity.PayoutType;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayoutService {

    Payout createPayout(UUID memberId, BigDecimal amount, PayoutType type, String actor);

    Payout approvePayout(UUID payoutId, String actor, String overrideReason, boolean isAdmin);

    Payout processPayout(UUID payoutId, String actor);

    CursorPage<Payout> getPayoutHistory(UUID memberId, String cursor, int limit);

    CursorPage<Payout> getPayoutsByStatus(PayoutStatus status, String cursor, int limit);

    CursorPage<Payout> getAllPayouts(String cursor, int limit);

    Payout getPayoutById(UUID payoutId);
}
