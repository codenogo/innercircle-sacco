package com.innercircle.sacco.member.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecordMeetingAttendanceRequest {
    @NotEmpty
    @Valid
    private List<MeetingAttendanceEntryRequest> entries;
}
