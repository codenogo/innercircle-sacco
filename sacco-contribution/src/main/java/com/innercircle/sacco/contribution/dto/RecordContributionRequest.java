package com.innercircle.sacco.contribution.dto;

import com.innercircle.sacco.contribution.entity.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to record a new contribution.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecordContributionRequest {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "CategoryID is required")
    private UUID categoryId;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @NotNull(message = "Contribution month is required")
    private LocalDate contributionMonth;

    @NotNull(message = "Transaction date is required")
    private LocalDate contributionDate;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
