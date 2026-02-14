package com.innercircle.sacco.contribution.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Summary of a member's contributions and penalties.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContributionSummaryResponse {

    private UUID memberId;
    private BigDecimal totalContributed;
    private BigDecimal totalPending;
    private BigDecimal totalPenalties;
    private LocalDate lastContributionDate;
}
