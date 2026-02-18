package com.innercircle.sacco.payout.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.payout.dto.BankWithdrawalRequest;
import com.innercircle.sacco.payout.dto.BankWithdrawalResponse;
import com.innercircle.sacco.payout.entity.BankWithdrawal;
import com.innercircle.sacco.payout.entity.WithdrawalStatus;
import com.innercircle.sacco.payout.service.BankWithdrawalService;
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
@RequestMapping("/api/v1/bank-withdrawals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')")
public class BankWithdrawalController {

    private final BankWithdrawalService bankWithdrawalService;
    private final MemberAccessHelper memberAccessHelper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<BankWithdrawalResponse> initiateWithdrawal(
            @Valid @RequestBody BankWithdrawalRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        BankWithdrawal withdrawal = bankWithdrawalService.initiateWithdrawal(
                request.memberId(),
                request.amount(),
                request.bankName(),
                request.accountNumber(),
                actor
        );
        return ApiResponse.ok(BankWithdrawalResponse.from(withdrawal), "Bank withdrawal initiated successfully");
    }

    @PutMapping("/{withdrawalId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<BankWithdrawalResponse> confirmWithdrawal(
            @PathVariable UUID withdrawalId,
            @RequestParam String referenceNumber,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        BankWithdrawal withdrawal = bankWithdrawalService.confirmWithdrawal(withdrawalId, referenceNumber, actor);
        return ApiResponse.ok(BankWithdrawalResponse.from(withdrawal), "Bank withdrawal confirmed successfully");
    }

    @PutMapping("/{withdrawalId}/reconcile")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<BankWithdrawalResponse> markReconciled(
            @PathVariable UUID withdrawalId,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        BankWithdrawal withdrawal = bankWithdrawalService.markReconciled(withdrawalId, actor);
        return ApiResponse.ok(BankWithdrawalResponse.from(withdrawal), "Bank withdrawal marked as reconciled");
    }

    @GetMapping("/{withdrawalId}")
    public ApiResponse<BankWithdrawalResponse> getWithdrawalById(
            @PathVariable UUID withdrawalId,
            Authentication authentication) {
        BankWithdrawal withdrawal = bankWithdrawalService.getWithdrawalById(withdrawalId);
        memberAccessHelper.assertAccessToMember(withdrawal.getMemberId(), authentication);
        return ApiResponse.ok(BankWithdrawalResponse.from(withdrawal));
    }

    @GetMapping("/unreconciled")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CursorPage<BankWithdrawalResponse>> getUnreconciled(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPage<BankWithdrawal> withdrawalPage = bankWithdrawalService.getUnreconciled(cursor, limit);
        CursorPage<BankWithdrawalResponse> responsePage = CursorPage.of(
                withdrawalPage.getItems().stream().map(BankWithdrawalResponse::from).toList(),
                withdrawalPage.getNextCursor(),
                withdrawalPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<CursorPage<BankWithdrawalResponse>> getWithdrawalsByMember(
            @PathVariable UUID memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        memberAccessHelper.assertAccessToMember(memberId, authentication);
        CursorPage<BankWithdrawal> withdrawalPage = bankWithdrawalService.getWithdrawalsByMember(memberId, cursor, limit);
        CursorPage<BankWithdrawalResponse> responsePage = CursorPage.of(
                withdrawalPage.getItems().stream().map(BankWithdrawalResponse::from).toList(),
                withdrawalPage.getNextCursor(),
                withdrawalPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CursorPage<BankWithdrawalResponse>> getWithdrawalsByStatus(
            @PathVariable WithdrawalStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPage<BankWithdrawal> withdrawalPage = bankWithdrawalService.getWithdrawalsByStatus(status, cursor, limit);
        CursorPage<BankWithdrawalResponse> responsePage = CursorPage.of(
                withdrawalPage.getItems().stream().map(BankWithdrawalResponse::from).toList(),
                withdrawalPage.getNextCursor(),
                withdrawalPage.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }
}
