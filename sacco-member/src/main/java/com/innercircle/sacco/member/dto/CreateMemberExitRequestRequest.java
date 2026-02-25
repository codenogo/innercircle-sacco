package com.innercircle.sacco.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateMemberExitRequestRequest {
    @NotNull
    private LocalDate noticeDate;

    @NotNull
    private LocalDate effectiveDate;
}
