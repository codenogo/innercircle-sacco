package com.innercircle.sacco.payout.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.payout.dto.ApprovePayoutRequest;
import com.innercircle.sacco.payout.dto.PayoutRequest;
import com.innercircle.sacco.payout.dto.PayoutResponse;
import com.innercircle.sacco.payout.entity.Payout;
import com.innercircle.sacco.payout.entity.PayoutStatus;
import com.innercircle.sacco.payout.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')")
public class PayoutController {

    private final PayoutService payoutService;
    private final MemberAccessHelper memberAccessHelper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<PayoutResponse> createPayout(
            @Valid @RequestBody PayoutRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        Payout payout = payoutService.createPayout(
                request.memberId(),
                request.amount(),
                request.type(),
                actor
        );
        return ApiResponse.ok(PayoutResponse.from(payout), "Payout created successfully");
    }

    @PutMapping("/{payoutId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<PayoutResponse> approvePayout(
            @PathVariable UUID payoutId,
            @org.springframework.web.bind.annotation.RequestBody(required = false) @jakarta.validation.Valid ApprovePayoutRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String overrideReason = request != null ? request.overrideReason() : null;
        Payout payout = payoutService.approvePayout(payoutId, actor, overrideReason, isAdmin);
        return ApiResponse.ok(PayoutResponse.from(payout), "Payout approved successfully");
    }

    @PutMapping("/{payoutId}/process")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<PayoutResponse> processPayout(
            @PathVariable UUID payoutId,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        Payout payout = payoutService.processPayout(payoutId, actor);
        return ApiResponse.ok(PayoutResponse.from(payout), "Payout processed successfully");
    }

    @GetMapping("/{payoutId}")
    public ApiResponse<PayoutResponse> getPayoutById(
            @PathVariable UUID payoutId,
            Authentication authentication) {
        Payout payout = payoutService.getPayoutById(payoutId);
        memberAccessHelper.assertAccessToMember(payout.getMemberId(), authentication);
        return ApiResponse.ok(PayoutResponse.from(payout));
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<CursorPage<PayoutResponse>> getPayoutHistory(
            @PathVariable UUID memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        memberAccessHelper.assertAccessToMember(memberId, authentication);
        CursorPage<Payout> payoutPage = payoutService.getPayoutHistory(memberId, cursor, limit);
        CursorPage<PayoutResponse> responsePage = CursorPage.of(
                payoutPage.getItems().stream().map(PayoutResponse::from).toList(),
                payoutPage.getNextCursor(),
                payoutPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CursorPage<PayoutResponse>> getPayoutsByStatus(
            @PathVariable PayoutStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPage<Payout> payoutPage = payoutService.getPayoutsByStatus(status, cursor, limit);
        CursorPage<PayoutResponse> responsePage = CursorPage.of(
                payoutPage.getItems().stream().map(PayoutResponse::from).toList(),
                payoutPage.getNextCursor(),
                payoutPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CursorPage<PayoutResponse>> getAllPayouts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPage<Payout> payoutPage = payoutService.getAllPayouts(cursor, limit);
        CursorPage<PayoutResponse> responsePage = CursorPage.of(
                payoutPage.getItems().stream().map(PayoutResponse::from).toList(),
                payoutPage.getNextCursor(),
                payoutPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }
}
