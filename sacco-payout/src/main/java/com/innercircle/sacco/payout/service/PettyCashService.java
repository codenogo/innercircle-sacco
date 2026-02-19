package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.dto.PettyCashSummaryResponse;
import com.innercircle.sacco.payout.entity.PettyCashExpenseType;
import com.innercircle.sacco.payout.entity.PettyCashVoucher;
import com.innercircle.sacco.payout.entity.PettyCashVoucherStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface PettyCashService {

    PettyCashVoucher createVoucher(BigDecimal amount,
                                   String purpose,
                                   PettyCashExpenseType expenseType,
                                   LocalDate requestDate,
                                   String notes,
                                   String actor);

    PettyCashVoucher approveVoucher(UUID voucherId, String actor, String overrideReason, boolean isAdmin);

    PettyCashVoucher disburseVoucher(UUID voucherId, String actor);

    PettyCashVoucher settleVoucher(UUID voucherId, String receiptNumber, String notes, String actor);

    PettyCashVoucher rejectVoucher(UUID voucherId, String reason, String actor);

    PettyCashVoucher getVoucherById(UUID voucherId);

    CursorPage<PettyCashVoucher> getVouchers(PettyCashVoucherStatus status,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             String cursor,
                                             int limit);

    PettyCashSummaryResponse getSummary(PettyCashVoucherStatus status,
                                        LocalDate startDate,
                                        LocalDate endDate);
}
