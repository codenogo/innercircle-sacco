package com.innercircle.sacco.contribution.dto;

import com.innercircle.sacco.contribution.entity.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request to record multiple contributions at once.
 * Shared fields (paymentMode, contributionMonth, contributionDate, categoryId) serve as
 * defaults and can be overridden per item.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BulkContributionRequest {

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @NotNull(message = "Contribution month is required")
    private LocalDate contributionMonth;

    @NotNull(message = "Transaction date is required")
    private LocalDate contributionDate;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @Size(max = 100, message = "Batch reference must not exceed 100 characters")
    private String batchReference;

    @NotEmpty(message = "Contributions list must not be empty")
    @Valid
    private List<BulkContributionItemRequest> contributions;
}
