package com.innercircle.sacco.member.repository;

import com.innercircle.sacco.member.entity.MeetingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingSessionRepository extends JpaRepository<MeetingSession, UUID> {
    List<MeetingSession> findAllByOrderByMeetingDateDesc();
}
