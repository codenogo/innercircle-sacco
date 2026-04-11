package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.WelfareClaimStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ReviewWelfareClaimRequest {
    @NotNull
    private WelfareClaimStatus status;

    private BigDecimal approvedAmount;
    private String decisionSource;
    private String meetingReference;
    private LocalDate decisionDate;
    private String decisionNotes;
}
