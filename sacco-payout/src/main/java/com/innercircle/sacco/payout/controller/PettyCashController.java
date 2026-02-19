package com.innercircle.sacco.payout.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.payout.dto.ApprovePettyCashRequest;
import com.innercircle.sacco.payout.dto.CreatePettyCashVoucherRequest;
import com.innercircle.sacco.payout.dto.PettyCashSummaryResponse;
import com.innercircle.sacco.payout.dto.PettyCashVoucherResponse;
import com.innercircle.sacco.payout.dto.RejectPettyCashVoucherRequest;
import com.innercircle.sacco.payout.dto.SettlePettyCashVoucherRequest;
import com.innercircle.sacco.payout.entity.PettyCashVoucher;
import com.innercircle.sacco.payout.entity.PettyCashVoucherStatus;
import com.innercircle.sacco.payout.service.PettyCashService;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/petty-cash")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
public class PettyCashController {

    private final PettyCashService pettyCashService;
    private final MemberAccessHelper memberAccessHelper;

    @PostMapping("/vouchers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PettyCashVoucherResponse> createVoucher(
            @Valid @RequestBody CreatePettyCashVoucherRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        PettyCashVoucher voucher = pettyCashService.createVoucher(
                request.amount(),
                request.purpose(),
                request.expenseType(),
                request.requestDate(),
                request.notes(),
                actor
        );
        return ApiResponse.ok(PettyCashVoucherResponse.from(voucher), "Petty cash voucher created successfully");
    }

    @PutMapping("/vouchers/{voucherId}/approve")
    public ApiResponse<PettyCashVoucherResponse> approveVoucher(
            @PathVariable UUID voucherId,
            @RequestBody(required = false) @Valid ApprovePettyCashRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String overrideReason = request != null ? request.overrideReason() : null;
        PettyCashVoucher voucher = pettyCashService.approveVoucher(voucherId, actor, overrideReason, isAdmin);
        return ApiResponse.ok(PettyCashVoucherResponse.from(voucher), "Voucher approved successfully");
    }

    @PutMapping("/vouchers/{voucherId}/disburse")
    public ApiResponse<PettyCashVoucherResponse> disburseVoucher(
            @PathVariable UUID voucherId,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        PettyCashVoucher voucher = pettyCashService.disburseVoucher(voucherId, actor);
        return ApiResponse.ok(PettyCashVoucherResponse.from(voucher), "Voucher disbursed successfully");
    }

    @PutMapping("/vouchers/{voucherId}/settle")
    public ApiResponse<PettyCashVoucherResponse> settleVoucher(
            @PathVariable UUID voucherId,
            @Valid @RequestBody SettlePettyCashVoucherRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        PettyCashVoucher voucher = pettyCashService.settleVoucher(
                voucherId,
                request.receiptNumber(),
                request.notes(),
                actor
        );
        return ApiResponse.ok(PettyCashVoucherResponse.from(voucher), "Voucher settled successfully");
    }

    @PutMapping("/vouchers/{voucherId}/reject")
    public ApiResponse<PettyCashVoucherResponse> rejectVoucher(
            @PathVariable UUID voucherId,
            @Valid @RequestBody RejectPettyCashVoucherRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        PettyCashVoucher voucher = pettyCashService.rejectVoucher(voucherId, request.reason(), actor);
        return ApiResponse.ok(PettyCashVoucherResponse.from(voucher), "Voucher rejected successfully");
    }

    @GetMapping("/vouchers/{voucherId}")
    public ApiResponse<PettyCashVoucherResponse> getVoucherById(@PathVariable UUID voucherId) {
        return ApiResponse.ok(PettyCashVoucherResponse.from(pettyCashService.getVoucherById(voucherId)));
    }

    @GetMapping("/vouchers")
    public ApiResponse<CursorPage<PettyCashVoucherResponse>> getVouchers(
            @RequestParam(required = false) PettyCashVoucherStatus status,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (month != null && !month.isBlank()) {
            YearMonth yearMonth = parseMonth(month);
            startDate = yearMonth.atDay(1);
            endDate = yearMonth.atEndOfMonth();
        }

        CursorPage<PettyCashVoucher> page = pettyCashService.getVouchers(
                status,
                startDate,
                endDate,
                cursor,
                limit
        );

        CursorPage<PettyCashVoucherResponse> responsePage = CursorPage.of(
                page.getItems().stream().map(PettyCashVoucherResponse::from).toList(),
                page.getNextCursor(),
                page.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }

    @GetMapping("/summary")
    public ApiResponse<PettyCashSummaryResponse> getSummary(
            @RequestParam(required = false) PettyCashVoucherStatus status,
            @RequestParam(required = false) String month
    ) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (month != null && !month.isBlank()) {
            YearMonth yearMonth = parseMonth(month);
            startDate = yearMonth.atDay(1);
            endDate = yearMonth.atEndOfMonth();
        }
        return ApiResponse.ok(pettyCashService.getSummary(status, startDate, endDate));
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Invalid month format. Expected YYYY-MM.");
        }
    }
}
