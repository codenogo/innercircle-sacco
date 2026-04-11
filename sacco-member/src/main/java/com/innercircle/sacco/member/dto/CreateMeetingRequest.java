package com.innercircle.sacco.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateMeetingRequest {
    @NotBlank
    private String title;

    @NotNull
    private LocalDate meetingDate;

    private Integer lateThresholdMinutes;
    private BigDecimal absenceFineAmount;
    private BigDecimal lateFineAmount;
    private BigDecimal unpaidDailyPenaltyAmount;
    private String notes;
}
