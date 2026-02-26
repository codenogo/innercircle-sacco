package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.MemberExitRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewMemberExitRequestRequest {
    @NotNull
    private MemberExitRequestStatus status;

    private String reviewNotes;
}
