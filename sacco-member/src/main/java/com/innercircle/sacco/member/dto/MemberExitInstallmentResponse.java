package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.MemberExitInstallment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MemberExitInstallmentResponse(
        UUID id,
        UUID exitRequestId,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        boolean processed,
        Instant processedAt,
        UUID payoutId,
        Instant createdAt,
        Instant updatedAt
) {
    public static MemberExitInstallmentResponse fromEntity(MemberExitInstallment installment) {
        return new MemberExitInstallmentResponse(
                installment.getId(),
                installment.getExitRequestId(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getAmount(),
                installment.isProcessed(),
                installment.getProcessedAt(),
                installment.getPayoutId(),
                installment.getCreatedAt(),
                installment.getUpdatedAt()
        );
    }
}
