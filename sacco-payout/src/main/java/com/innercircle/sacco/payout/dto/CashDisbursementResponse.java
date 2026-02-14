package com.innercircle.sacco.payout.dto;

import com.innercircle.sacco.payout.entity.CashDisbursement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CashDisbursementResponse(
        UUID id,
        UUID memberId,
        BigDecimal amount,
        String receivedBy,
        String disbursedBy,
        String signoffBy,
        String receiptNumber,
        LocalDate disbursementDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static CashDisbursementResponse from(CashDisbursement disbursement) {
        return new CashDisbursementResponse(
                disbursement.getId(),
                disbursement.getMemberId(),
                disbursement.getAmount(),
                disbursement.getReceivedBy(),
                disbursement.getDisbursedBy(),
                disbursement.getSignoffBy(),
                disbursement.getReceiptNumber(),
                disbursement.getDisbursementDate(),
                disbursement.getCreatedAt(),
                disbursement.getUpdatedAt()
        );
    }
}
