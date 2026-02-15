package com.innercircle.sacco.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchProcessingResult {

    private Integer processedLoans;
    private Integer penalizedLoans;
    private Integer closedLoans;
    private Instant processedAt;
    private String message;
}
