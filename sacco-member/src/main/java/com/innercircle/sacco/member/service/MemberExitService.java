package com.innercircle.sacco.member.service;

import com.innercircle.sacco.member.dto.CreateMemberExitRequestRequest;
import com.innercircle.sacco.member.dto.ReviewMemberExitRequestRequest;
import com.innercircle.sacco.member.entity.MemberExitInstallment;
import com.innercircle.sacco.member.entity.MemberExitRequest;

import java.util.List;
import java.util.UUID;

public interface MemberExitService {
    MemberExitRequest createExitRequest(UUID memberId, CreateMemberExitRequestRequest request, String actor);
    MemberExitRequest reviewExitRequest(UUID memberId, UUID requestId, ReviewMemberExitRequestRequest request, String actor);
    MemberExitInstallment processInstallment(UUID memberId, UUID requestId, String actor, boolean isAdmin);
    List<MemberExitRequest> getExitRequests(UUID memberId);
    List<MemberExitInstallment> getInstallments(UUID requestId);
}
