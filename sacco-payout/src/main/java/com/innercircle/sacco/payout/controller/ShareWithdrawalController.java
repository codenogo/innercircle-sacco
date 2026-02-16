package com.innercircle.sacco.payout.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.dto.ShareWithdrawalRequest;
import com.innercircle.sacco.payout.dto.ShareWithdrawalResponse;
import com.innercircle.sacco.payout.entity.ShareWithdrawal;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalStatus;
import com.innercircle.sacco.payout.service.ShareWithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/share-withdrawals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')")
public class ShareWithdrawalController {

    private final ShareWithdrawalService shareWithdrawalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<ShareWithdrawalResponse> requestWithdrawal(
            @Valid @RequestBody ShareWithdrawalRequest request,
            @RequestParam(defaultValue = "system") String actor
    ) {
        ShareWithdrawal withdrawal = shareWithdrawalService.requestWithdrawal(
                request.memberId(),
                request.amount(),
                request.withdrawalType(),
                request.currentShareBalance(),
                actor
        );
        return ApiResponse.ok(ShareWithdrawalResponse.from(withdrawal), "Share withdrawal requested successfully");
    }

    @PutMapping("/{withdrawalId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<ShareWithdrawalResponse> approveWithdrawal(
            @PathVariable UUID withdrawalId,
            @RequestParam(defaultValue = "system") String actor
    ) {
        ShareWithdrawal withdrawal = shareWithdrawalService.approveWithdrawal(withdrawalId, actor);
        return ApiResponse.ok(ShareWithdrawalResponse.from(withdrawal), "Share withdrawal approved successfully");
    }

    @PutMapping("/{withdrawalId}/process")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<ShareWithdrawalResponse> processWithdrawal(
            @PathVariable UUID withdrawalId,
            @RequestParam(defaultValue = "system") String actor
    ) {
        ShareWithdrawal withdrawal = shareWithdrawalService.processWithdrawal(withdrawalId, actor);
        return ApiResponse.ok(ShareWithdrawalResponse.from(withdrawal), "Share withdrawal processed successfully");
    }

    @GetMapping("/{withdrawalId}")
    public ApiResponse<ShareWithdrawalResponse> getWithdrawalById(@PathVariable UUID withdrawalId) {
        ShareWithdrawal withdrawal = shareWithdrawalService.getWithdrawalById(withdrawalId);
        return ApiResponse.ok(ShareWithdrawalResponse.from(withdrawal));
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<CursorPage<ShareWithdrawalResponse>> getWithdrawalsByMember(
            @PathVariable UUID memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPage<ShareWithdrawal> withdrawalPage = shareWithdrawalService.getWithdrawalsByMember(
                memberId, cursor, limit
        );
        CursorPage<ShareWithdrawalResponse> responsePage = CursorPage.of(
                withdrawalPage.getItems().stream().map(ShareWithdrawalResponse::from).toList(),
                withdrawalPage.getNextCursor(),
                withdrawalPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }

    @GetMapping("/status/{status}")
    public ApiResponse<CursorPage<ShareWithdrawalResponse>> getWithdrawalsByStatus(
            @PathVariable ShareWithdrawalStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPage<ShareWithdrawal> withdrawalPage = shareWithdrawalService.getWithdrawalsByStatus(
                status, cursor, limit
        );
        CursorPage<ShareWithdrawalResponse> responsePage = CursorPage.of(
                withdrawalPage.getItems().stream().map(ShareWithdrawalResponse::from).toList(),
                withdrawalPage.getNextCursor(),
                withdrawalPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }
}
