package com.innercircle.sacco.payout.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.dto.CashDisbursementRequest;
import com.innercircle.sacco.payout.dto.CashDisbursementResponse;
import com.innercircle.sacco.payout.entity.CashDisbursement;
import com.innercircle.sacco.payout.service.CashDisbursementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
public class CashDisbursementController {

    private final CashDisbursementService cashDisbursementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CashDisbursementResponse> recordDisbursement(
            @Valid @RequestBody CashDisbursementRequest request,
            @RequestParam(defaultValue = "system") String actor
    ) {
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

    @PutMapping("/{disbursementId}/signoff")
    public ApiResponse<CashDisbursementResponse> signoff(
            @PathVariable UUID disbursementId,
            @RequestParam String signoffBy
    ) {
        CashDisbursement disbursement = cashDisbursementService.signoff(disbursementId, signoffBy);
        return ApiResponse.ok(CashDisbursementResponse.from(disbursement), "Cash disbursement signed off successfully");
    }

    @GetMapping("/{disbursementId}")
    public ApiResponse<CashDisbursementResponse> getDisbursementById(@PathVariable UUID disbursementId) {
        CashDisbursement disbursement = cashDisbursementService.getDisbursementById(disbursementId);
        return ApiResponse.ok(CashDisbursementResponse.from(disbursement));
    }

    @GetMapping("/receipt/{receiptNumber}")
    public ApiResponse<CashDisbursementResponse> getDisbursementByReceipt(@PathVariable String receiptNumber) {
        CashDisbursement disbursement = cashDisbursementService.getDisbursementByReceipt(receiptNumber);
        return ApiResponse.ok(CashDisbursementResponse.from(disbursement));
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<CursorPage<CashDisbursementResponse>> getDisbursementHistory(
            @PathVariable UUID memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
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
