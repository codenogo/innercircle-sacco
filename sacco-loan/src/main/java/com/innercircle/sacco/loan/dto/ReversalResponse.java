package com.innercircle.sacco.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReversalResponse {

    private UUID reversalId;
    private String reversalType; // REPAYMENT or PENALTY
    private UUID originalTransactionId;
    private UUID loanId;
    private UUID memberId;
    private BigDecimal amount;
    private String reason;
    private String actor;
    private Instant reversedAt;
    private String message;
}
