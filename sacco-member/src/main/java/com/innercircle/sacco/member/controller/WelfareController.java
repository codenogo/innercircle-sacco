package com.innercircle.sacco.member.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.member.dto.CreateWelfareBeneficiaryRequest;
import com.innercircle.sacco.member.dto.CreateWelfareClaimRequest;
import com.innercircle.sacco.member.dto.ReviewWelfareClaimRequest;
import com.innercircle.sacco.member.dto.WelfareBeneficiaryResponse;
import com.innercircle.sacco.member.dto.WelfareClaimResponse;
import com.innercircle.sacco.member.dto.WelfareFundSummaryResponse;
import com.innercircle.sacco.member.service.WelfareService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/welfare")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','CHAIRPERSON','VICE_CHAIRPERSON','VICE_TREASURER')")
public class WelfareController {

    private final WelfareService welfareService;

    @PostMapping("/beneficiaries")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WelfareBeneficiaryResponse> createBeneficiary(
            @Valid @RequestBody CreateWelfareBeneficiaryRequest request,
            Authentication authentication) {
        return ApiResponse.ok(
                WelfareBeneficiaryResponse.fromEntity(welfareService.createBeneficiary(request, actor(authentication))),
                "Welfare beneficiary created"
        );
    }

    @GetMapping("/beneficiaries/member/{memberId}")
    public ApiResponse<java.util.List<WelfareBeneficiaryResponse>> getBeneficiariesForMember(
            @PathVariable UUID memberId) {
        return ApiResponse.ok(
                welfareService.getBeneficiariesForMember(memberId).stream()
                        .map(WelfareBeneficiaryResponse::fromEntity)
                        .toList()
        );
    }

    @PostMapping("/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WelfareClaimResponse> createClaim(
            @Valid @RequestBody CreateWelfareClaimRequest request,
            Authentication authentication) {
        return ApiResponse.ok(
                WelfareClaimResponse.fromEntity(welfareService.createClaim(request, actor(authentication))),
                "Welfare claim submitted"
        );
    }

    @PatchMapping("/claims/{id}/review")
    public ApiResponse<WelfareClaimResponse> reviewClaim(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewWelfareClaimRequest request,
            Authentication authentication) {
        return ApiResponse.ok(
                WelfareClaimResponse.fromEntity(welfareService.reviewClaim(id, request, actor(authentication))),
                "Welfare claim review recorded"
        );
    }

    @PostMapping("/claims/{id}/process")
    public ApiResponse<WelfareClaimResponse> processClaim(
            @PathVariable UUID id,
            Authentication authentication) {
        return ApiResponse.ok(
                WelfareClaimResponse.fromEntity(welfareService.processClaim(
                        id,
                        actor(authentication),
                        isAdmin(authentication)
                )),
                "Welfare claim processed"
        );
    }

    @GetMapping("/fund-summary")
    public ApiResponse<WelfareFundSummaryResponse> getFundSummary() {
        return ApiResponse.ok(welfareService.getFundSummary());
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
