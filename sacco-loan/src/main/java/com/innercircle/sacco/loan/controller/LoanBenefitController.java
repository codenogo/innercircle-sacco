package com.innercircle.sacco.loan.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.loan.dto.LoanBenefitResponse;
import com.innercircle.sacco.loan.dto.MemberEarningsResponse;
import com.innercircle.sacco.loan.entity.LoanBenefit;
import com.innercircle.sacco.loan.service.LoanBenefitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loan-benefits")
@RequiredArgsConstructor
public class LoanBenefitController {

    private final LoanBenefitService benefitService;

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'MEMBER')")
    public ApiResponse<MemberEarningsResponse> getMemberEarnings(@PathVariable UUID memberId) {
        MemberEarningsResponse earnings = benefitService.getMemberEarnings(memberId);
        return ApiResponse.ok(earnings);
    }

    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'MEMBER')")
    public ApiResponse<List<LoanBenefitResponse>> getLoanBenefits(@PathVariable UUID loanId) {
        List<LoanBenefitResponse> benefits = benefitService.getLoanBenefits(loanId);
        return ApiResponse.ok(benefits);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ApiResponse<CursorPage<LoanBenefitResponse>> getAllBenefits(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {

        List<LoanBenefitResponse> benefits = benefitService.getAllBenefits(cursor, limit + 1);

        boolean hasMore = benefits.size() > limit;
        List<LoanBenefitResponse> resultBenefits = hasMore ? benefits.subList(0, limit) : benefits;
        String nextCursor = hasMore ? resultBenefits.get(resultBenefits.size() - 1).getId().toString() : null;

        CursorPage<LoanBenefitResponse> page = CursorPage.of(resultBenefits, nextCursor, hasMore);
        return ApiResponse.ok(page);
    }

    @PostMapping("/refresh/{loanId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ApiResponse<List<LoanBenefitResponse>> refreshBeneficiaries(
            @PathVariable UUID loanId,
            @RequestParam String actor) {

        List<LoanBenefit> benefits = benefitService.refreshBeneficiaries(loanId, actor);
        List<LoanBenefitResponse> responses = benefits.stream()
                .map(LoanBenefitResponse::from)
                .toList();

        return ApiResponse.ok(responses, "Beneficiaries refreshed successfully");
    }
}
