package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.entity.ShareWithdrawal;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalStatus;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalType;

import java.math.BigDecimal;
import java.util.UUID;

public interface ShareWithdrawalService {

    ShareWithdrawal requestWithdrawal(UUID memberId, BigDecimal amount, ShareWithdrawalType withdrawalType,
                                     BigDecimal currentShareBalance, String actor);

    ShareWithdrawal approveWithdrawal(UUID withdrawalId, String actor, String overrideReason, boolean isAdmin);

    ShareWithdrawal processWithdrawal(UUID withdrawalId, String actor);

    CursorPage<ShareWithdrawal> getWithdrawalsByMember(UUID memberId, String cursor, int limit);

    CursorPage<ShareWithdrawal> getWithdrawalsByStatus(ShareWithdrawalStatus status, String cursor, int limit);

    ShareWithdrawal getWithdrawalById(UUID withdrawalId);
}
