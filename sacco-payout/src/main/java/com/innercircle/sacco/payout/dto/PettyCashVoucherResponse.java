package com.innercircle.sacco.payout.dto;

import com.innercircle.sacco.payout.entity.PettyCashExpenseType;
import com.innercircle.sacco.payout.entity.PettyCashVoucher;
import com.innercircle.sacco.payout.entity.PettyCashVoucherStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PettyCashVoucherResponse(
        UUID id,
        String referenceNumber,
        BigDecimal amount,
        String purpose,
        PettyCashExpenseType expenseType,
        PettyCashVoucherStatus status,
        LocalDate requestDate,
        String approvedBy,
        String disbursedBy,
        String settledBy,
        String rejectedBy,
        Instant disbursedAt,
        Instant settledAt,
        Instant rejectedAt,
        String receiptNumber,
        String rejectionReason,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {
    public static PettyCashVoucherResponse from(PettyCashVoucher voucher) {
        return new PettyCashVoucherResponse(
                voucher.getId(),
                voucher.getReferenceNumber(),
                voucher.getAmount(),
                voucher.getPurpose(),
                voucher.getExpenseType(),
                voucher.getStatus(),
                voucher.getRequestDate(),
                voucher.getApprovedBy(),
                voucher.getDisbursedBy(),
                voucher.getSettledBy(),
                voucher.getRejectedBy(),
                voucher.getDisbursedAt(),
                voucher.getSettledAt(),
                voucher.getRejectedAt(),
                voucher.getReceiptNumber(),
                voucher.getRejectionReason(),
                voucher.getNotes(),
                voucher.getCreatedAt(),
                voucher.getUpdatedAt(),
                voucher.getCreatedBy()
        );
    }
}
