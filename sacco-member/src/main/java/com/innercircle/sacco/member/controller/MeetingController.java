package com.innercircle.sacco.member.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.member.dto.CreateMeetingRequest;
import com.innercircle.sacco.member.dto.MeetingFineResponse;
import com.innercircle.sacco.member.dto.MeetingResponse;
import com.innercircle.sacco.member.dto.RecordMeetingAttendanceRequest;
import com.innercircle.sacco.member.dto.WaiveMeetingFineRequest;
import com.innercircle.sacco.member.service.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','SECRETARY','CHAIRPERSON','VICE_CHAIRPERSON','VICE_TREASURER')")
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping
    public ApiResponse<List<MeetingResponse>> getAllMeetings() {
        return ApiResponse.ok(
                meetingService.getAllMeetings().stream()
                        .map(MeetingResponse::fromEntity)
                        .toList()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MeetingResponse> createMeeting(
            @Valid @RequestBody CreateMeetingRequest request,
            Authentication authentication) {
        String actor = actor(authentication);
        return ApiResponse.ok(
                MeetingResponse.fromEntity(meetingService.createMeeting(request, actor)),
                "Meeting created successfully"
        );
    }

    @PostMapping("/{id}/attendance")
    public ApiResponse<Void> recordAttendance(
            @PathVariable UUID id,
            @Valid @RequestBody RecordMeetingAttendanceRequest request,
            Authentication authentication) {
        meetingService.recordAttendance(id, request.getEntries(), actor(authentication));
        return ApiResponse.ok(null, "Attendance captured successfully");
    }

    @PostMapping("/{id}/finalize")
    public ApiResponse<MeetingResponse> finalizeMeeting(
            @PathVariable UUID id,
            Authentication authentication) {
        return ApiResponse.ok(
                MeetingResponse.fromEntity(meetingService.finalizeMeeting(id, actor(authentication))),
                "Meeting finalized and fines generated"
        );
    }

    @GetMapping("/fines")
    public ApiResponse<List<MeetingFineResponse>> getFines(
            @RequestParam(required = false) UUID memberId) {
        return ApiResponse.ok(
                meetingService.getFines(memberId).stream()
                        .map(MeetingFineResponse::fromEntity)
                        .toList()
        );
    }

    @PostMapping("/fines/{id}/settle")
    public ApiResponse<MeetingFineResponse> settleFine(
            @PathVariable UUID id,
            Authentication authentication) {
        return ApiResponse.ok(
                MeetingFineResponse.fromEntity(meetingService.settleFine(id, actor(authentication))),
                "Meeting fine settled"
        );
    }

    @PatchMapping("/fines/{id}/waive")
    public ApiResponse<MeetingFineResponse> waiveFine(
            @PathVariable UUID id,
            @RequestBody(required = false) WaiveMeetingFineRequest request,
            Authentication authentication) {
        String reason = request != null ? request.getReason() : null;
        return ApiResponse.ok(
                MeetingFineResponse.fromEntity(meetingService.waiveFine(id, reason, actor(authentication))),
                "Meeting fine waived"
        );
    }

    private String actor(Authentication authentication) {
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "system";
    }
}
