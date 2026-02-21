package com.innercircle.sacco.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchProcessingResult {

    private Integer processedLoans;
    private Integer penalizedLoans;
    private Integer closedLoans;
    private Integer interestAccruedLoans;
    private java.math.BigDecimal totalInterestAccrued;
    private Instant processedAt;
    private String message;
    private List<String> warnings;
    private String processingDate;
}
