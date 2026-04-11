package com.innercircle.sacco.member.repository;

import com.innercircle.sacco.member.entity.MemberExitRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemberExitRequestRepository extends JpaRepository<MemberExitRequest, UUID> {
    List<MemberExitRequest> findByMemberIdOrderByCreatedAtDesc(UUID memberId);
}
