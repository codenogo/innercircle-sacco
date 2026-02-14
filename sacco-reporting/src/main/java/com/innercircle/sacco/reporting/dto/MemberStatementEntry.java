package com.innercircle.sacco.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MemberStatementEntry(
        LocalDateTime date,
        String type,
        String description,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal runningBalance,
        UUID referenceId
) {}
