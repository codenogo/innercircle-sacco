package com.innercircle.sacco.payout.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.payout.dto.ApproveCashDisbursementRequest;
import com.innercircle.sacco.payout.dto.CashDisbursementRequest;
import com.innercircle.sacco.payout.dto.CashDisbursementResponse;
import com.innercircle.sacco.payout.entity.CashDisbursement;
import com.innercircle.sacco.payout.service.CashDisbursementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cash-disbursements")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')")
public class CashDisbursementController {

    private final CashDisbursementService cashDisbursementService;
    private final MemberAccessHelper memberAccessHelper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CashDisbursementResponse> recordDisbursement(
            @Valid @RequestBody CashDisbursementRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        CashDisbursement disbursement = cashDisbursementService.recordDisbursement(
                request.memberId(),
                request.amount(),
                request.receivedBy(),
                request.disbursedBy(),
                request.receiptNumber(),
                request.disbursementDate(),
                actor
        );
        return ApiResponse.ok(CashDisbursementResponse.from(disbursement), "Cash disbursement recorded successfully");
    }

    @PutMapping("/{disbursementId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CashDisbursementResponse> approveDisbursement(
            @PathVariable UUID disbursementId,
            @RequestBody(required = false) ApproveCashDisbursementRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        String overrideReason = request != null ? request.overrideReason() : null;
        CashDisbursement disbursement = cashDisbursementService.approveDisbursement(
                disbursementId, actor, overrideReason, isAdmin
        );
        return ApiResponse.ok(CashDisbursementResponse.from(disbursement), "Cash disbursement approved successfully");
    }

    @PutMapping("/{disbursementId}/record")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CashDisbursementResponse> recordDisbursementComplete(
            @PathVariable UUID disbursementId,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        CashDisbursement disbursement = cashDisbursementService.completeDisbursement(disbursementId, actor);
        return ApiResponse.ok(CashDisbursementResponse.from(disbursement), "Cash disbursement recorded successfully");
    }

    @PutMapping("/{disbursementId}/signoff")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CashDisbursementResponse> signoff(
            @PathVariable UUID disbursementId,
            Authentication authentication
    ) {
        String signoffBy = memberAccessHelper.currentActor(authentication);
        CashDisbursement disbursement = cashDisbursementService.signoff(disbursementId, signoffBy);
        return ApiResponse.ok(CashDisbursementResponse.from(disbursement), "Cash disbursement signed off successfully");
    }

    @GetMapping("/{disbursementId}")
    public ApiResponse<CashDisbursementResponse> getDisbursementById(
            @PathVariable UUID disbursementId,
            Authentication authentication) {
        CashDisbursement disbursement = cashDisbursementService.getDisbursementById(disbursementId);
        memberAccessHelper.assertAccessToMember(disbursement.getMemberId(), authentication);
        return ApiResponse.ok(CashDisbursementResponse.from(disbursement));
    }

    @GetMapping("/receipt/{receiptNumber}")
    public ApiResponse<CashDisbursementResponse> getDisbursementByReceipt(
            @PathVariable String receiptNumber,
            Authentication authentication) {
        CashDisbursement disbursement = cashDisbursementService.getDisbursementByReceipt(receiptNumber);
        memberAccessHelper.assertAccessToMember(disbursement.getMemberId(), authentication);
        return ApiResponse.ok(CashDisbursementResponse.from(disbursement));
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<CursorPage<CashDisbursementResponse>> getDisbursementHistory(
            @PathVariable UUID memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        memberAccessHelper.assertAccessToMember(memberId, authentication);
        CursorPage<CashDisbursement> disbursementPage = cashDisbursementService.getDisbursementHistory(
                memberId, cursor, limit
        );
        CursorPage<CashDisbursementResponse> responsePage = CursorPage.of(
                disbursementPage.getItems().stream().map(CashDisbursementResponse::from).toList(),
                disbursementPage.getNextCursor(),
                disbursementPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CursorPage<CashDisbursementResponse>> getDisbursementsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPage<CashDisbursement> disbursementPage = cashDisbursementService.getDisbursementsByDateRange(
                startDate, endDate, cursor, limit
        );
        CursorPage<CashDisbursementResponse> responsePage = CursorPage.of(
                disbursementPage.getItems().stream().map(CashDisbursementResponse::from).toList(),
                disbursementPage.getNextCursor(),
                disbursementPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }
}
