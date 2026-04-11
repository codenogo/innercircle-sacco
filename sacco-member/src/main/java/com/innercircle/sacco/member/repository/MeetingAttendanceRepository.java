package com.innercircle.sacco.member.repository;

import com.innercircle.sacco.member.entity.MeetingAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingAttendanceRepository extends JpaRepository<MeetingAttendance, UUID> {
    List<MeetingAttendance> findByMeetingId(UUID meetingId);
    Optional<MeetingAttendance> findByMeetingIdAndMemberId(UUID meetingId, UUID memberId);
}
