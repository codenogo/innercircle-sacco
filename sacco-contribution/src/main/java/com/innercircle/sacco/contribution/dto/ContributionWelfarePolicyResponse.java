package com.innercircle.sacco.contribution.dto;

import java.math.BigDecimal;

public record ContributionWelfarePolicyResponse(
        boolean enabled,
        BigDecimal fixedAmount
) {}
