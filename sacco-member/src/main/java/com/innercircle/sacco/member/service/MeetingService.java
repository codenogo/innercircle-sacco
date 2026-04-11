package com.innercircle.sacco.member.service;

import com.innercircle.sacco.member.dto.CreateMeetingRequest;
import com.innercircle.sacco.member.dto.MeetingAttendanceEntryRequest;
import com.innercircle.sacco.member.entity.MeetingFine;
import com.innercircle.sacco.member.entity.MeetingSession;

import java.util.List;
import java.util.UUID;

public interface MeetingService {
    List<MeetingSession> getAllMeetings();
    MeetingSession createMeeting(CreateMeetingRequest request, String actor);
    void recordAttendance(UUID meetingId, List<MeetingAttendanceEntryRequest> entries, String actor);
    MeetingSession finalizeMeeting(UUID meetingId, String actor);
    List<MeetingFine> getFines(UUID memberId);
    MeetingFine settleFine(UUID fineId, String actor);
    MeetingFine waiveFine(UUID fineId, String reason, String actor);
}
