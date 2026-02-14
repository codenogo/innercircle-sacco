package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.entity.CashDisbursement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CashDisbursementService {

    CashDisbursement recordDisbursement(UUID memberId, BigDecimal amount, String receivedBy,
                                       String disbursedBy, String receiptNumber,
                                       LocalDate disbursementDate, String actor);

    CashDisbursement signoff(UUID disbursementId, String signoffBy);

    CursorPage<CashDisbursement> getDisbursementHistory(UUID memberId, String cursor, int limit);

    CursorPage<CashDisbursement> getDisbursementsByDateRange(LocalDate startDate, LocalDate endDate,
                                                            String cursor, int limit);

    CashDisbursement getDisbursementById(UUID disbursementId);

    CashDisbursement getDisbursementByReceipt(String receiptNumber);
}
