package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.entity.BankWithdrawal;
import com.innercircle.sacco.payout.entity.WithdrawalStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface BankWithdrawalService {

    BankWithdrawal initiateWithdrawal(UUID memberId, BigDecimal amount, String bankName,
                                     String accountNumber, String actor);

    BankWithdrawal approveWithdrawal(UUID withdrawalId, String actor, String overrideReason, boolean isAdmin);

    BankWithdrawal confirmWithdrawal(UUID withdrawalId, String referenceNumber, String actor);

    BankWithdrawal markReconciled(UUID withdrawalId, String actor);

    CursorPage<BankWithdrawal> getUnreconciled(String cursor, int limit);

    CursorPage<BankWithdrawal> getWithdrawalsByMember(UUID memberId, String cursor, int limit);

    CursorPage<BankWithdrawal> getWithdrawalsByStatus(WithdrawalStatus status, String cursor, int limit);

    BankWithdrawal getWithdrawalById(UUID withdrawalId);
}
