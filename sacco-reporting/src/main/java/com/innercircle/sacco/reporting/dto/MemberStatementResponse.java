package com.innercircle.sacco.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MemberStatementResponse(
        UUID memberId,
        String memberName,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal totalContributions,
        BigDecimal totalLoansReceived,
        BigDecimal totalRepayments,
        BigDecimal totalPayouts,
        BigDecimal totalPenalties,
        List<MemberStatementEntry> entries
) {}
