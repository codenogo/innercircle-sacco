package com.innercircle.sacco.member.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateWelfareClaimRequest {
    @NotNull
    private UUID memberId;

    private UUID beneficiaryId;
    private UUID benefitCatalogId;

    @NotBlank
    private String eventCode;

    @NotNull
    private LocalDate eventDate;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal requestedAmount;
}
