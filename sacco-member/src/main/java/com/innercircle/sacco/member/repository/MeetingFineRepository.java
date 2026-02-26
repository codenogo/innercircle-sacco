package com.innercircle.sacco.member.repository;

import com.innercircle.sacco.member.entity.MeetingFine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingFineRepository extends JpaRepository<MeetingFine, UUID> {
    List<MeetingFine> findByMemberId(UUID memberId);
    List<MeetingFine> findBySettledFalseAndWaivedFalse();
}
