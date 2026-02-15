package com.innercircle.sacco.reporting.dto;

import java.math.BigDecimal;

public record MonthlyDataPoint(
        int month,
        String monthName,
        BigDecimal amount
) {}
