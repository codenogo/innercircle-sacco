package com.innercircle.sacco.member.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.member.dto.CreateMemberExitRequestRequest;
import com.innercircle.sacco.member.dto.MemberExitInstallmentResponse;
import com.innercircle.sacco.member.dto.MemberExitRequestResponse;
import com.innercircle.sacco.member.dto.ReviewMemberExitRequestRequest;
import com.innercircle.sacco.member.service.MemberExitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members/{memberId}/exit-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER','CHAIRPERSON','VICE_CHAIRPERSON','VICE_TREASURER')")
public class MemberExitController {

    private final MemberExitService memberExitService;
    private final MemberAccessHelper memberAccessHelper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberExitRequestResponse> createExitRequest(
            @PathVariable UUID memberId,
            @Valid @RequestBody CreateMemberExitRequestRequest request,
            Authentication authentication) {
        memberAccessHelper.assertAccessToMember(memberId, authentication);
        return ApiResponse.ok(
                MemberExitRequestResponse.fromEntity(memberExitService.createExitRequest(memberId, request, actor(authentication))),
                "Member exit request created"
        );
    }

    @PatchMapping("/{requestId}/review")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER','CHAIRPERSON','VICE_CHAIRPERSON','VICE_TREASURER')")
    public ApiResponse<MemberExitRequestResponse> reviewExitRequest(
            @PathVariable UUID memberId,
            @PathVariable UUID requestId,
            @Valid @RequestBody ReviewMemberExitRequestRequest request,
            Authentication authentication) {
        return ApiResponse.ok(
                MemberExitRequestResponse.fromEntity(
                        memberExitService.reviewExitRequest(memberId, requestId, request, actor(authentication))
                ),
                "Member exit review updated"
        );
    }

    @PostMapping("/{requestId}/process-installment")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<MemberExitInstallmentResponse> processInstallment(
            @PathVariable UUID memberId,
            @PathVariable UUID requestId,
            Authentication authentication) {
        return ApiResponse.ok(
                MemberExitInstallmentResponse.fromEntity(
                        memberExitService.processInstallment(memberId, requestId, actor(authentication), isAdmin(authentication))
                ),
                "Exit installment processed"
        );
    }

    @GetMapping
    public ApiResponse<List<MemberExitRequestResponse>> getExitRequests(
            @PathVariable UUID memberId,
            Authentication authentication) {
        memberAccessHelper.assertAccessToMember(memberId, authentication);
        return ApiResponse.ok(
                memberExitService.getExitRequests(memberId).stream()
                        .map(MemberExitRequestResponse::fromEntity)
                        .toList()
        );
    }

    private String actor(Authentication authentication) {
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "system";
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
