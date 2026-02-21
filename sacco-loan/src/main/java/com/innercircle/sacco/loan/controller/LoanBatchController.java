package com.innercircle.sacco.loan.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.loan.dto.BatchProcessingResult;
import com.innercircle.sacco.loan.dto.ReversalRequest;
import com.innercircle.sacco.loan.dto.ReversalResponse;
import com.innercircle.sacco.loan.service.LoanBatchService;
import com.innercircle.sacco.loan.service.LoanReversalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanBatchController {

    private final LoanBatchService batchService;
    private final LoanReversalService reversalService;

    /**
     * Trigger batch processing of outstanding loans for a specific date.
     * If targetDate is not provided, auto-determines the next date to process.
     * Requires ADMIN or TREASURER role.
     */
    @PostMapping("/batch/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ApiResponse<BatchProcessingResult> processBatch(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {

        String triggeredBy = getCurrentUsername();

        if (targetDate == null) {
            // Auto-determine: delegate to processOutstandingLoans which figures out the date
            BatchProcessingResult result = batchService.processOutstandingLoans();
            return ApiResponse.ok(result, result.getMessage());
        }

        BatchProcessingResult result = batchService.processDailyLoans(targetDate, triggeredBy);
        return ApiResponse.ok(result, result.getMessage());
    }

    /**
     * Detect unpaid loans for a specific month.
     * Requires ADMIN or TREASURER role.
     */
    @GetMapping("/batch/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ApiResponse<List<Map<String, Object>>> detectUnpaidLoans(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {

        // Normalize to first day of month
        LocalDate normalizedMonth = month.withDayOfMonth(1);

        List<Map<String, Object>> unpaidLoans = batchService.detectUnpaidLoans(normalizedMonth);
        String message = String.format("Found %d unpaid loan installments for %s",
            unpaidLoans.size(), normalizedMonth);

        return ApiResponse.ok(unpaidLoans, message);
    }

    /**
     * Reverse a loan repayment.
     * Requires ADMIN or TREASURER role.
     */
    @PostMapping("/reversals/repayment/{repaymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ApiResponse<ReversalResponse> reverseRepayment(
            @PathVariable UUID repaymentId,
            @Valid @RequestBody ReversalRequest request) {

        String actor = getCurrentUsername();
        ReversalResponse response = reversalService.reverseRepayment(
            repaymentId, request.getReason(), actor);

        return ApiResponse.ok(response, response.getMessage());
    }

    /**
     * Reverse a penalty.
     * Requires ADMIN or TREASURER role.
     */
    @PostMapping("/reversals/penalty/{penaltyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ApiResponse<ReversalResponse> reversePenalty(
            @PathVariable UUID penaltyId,
            @Valid @RequestBody ReversalRequest request) {

        String actor = getCurrentUsername();
        ReversalResponse response = reversalService.reversePenalty(
            penaltyId, request.getReason(), actor);

        return ApiResponse.ok(response, response.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
