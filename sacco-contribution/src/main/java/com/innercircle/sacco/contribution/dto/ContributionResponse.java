package com.innercircle.sacco.contribution.dto;

import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionStatus;
import com.innercircle.sacco.contribution.entity.ContributionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response containing contribution details.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContributionResponse {

    private UUID id;
    private UUID memberId;
    private BigDecimal amount;
    private ContributionType type;
    private ContributionStatus status;
    private LocalDate contributionDate;
    private String referenceNumber;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public static ContributionResponse fromEntity(Contribution contribution) {
        return new ContributionResponse(
                contribution.getId(),
                contribution.getMemberId(),
                contribution.getAmount(),
                contribution.getType(),
                contribution.getStatus(),
                contribution.getContributionDate(),
                contribution.getReferenceNumber(),
                contribution.getNotes(),
                contribution.getCreatedAt(),
                contribution.getUpdatedAt()
        );
    }
}
