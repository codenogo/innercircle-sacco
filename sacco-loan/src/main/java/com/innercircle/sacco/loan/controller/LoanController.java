package com.innercircle.sacco.loan.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.loan.dto.LoanApplicationRequest;
import com.innercircle.sacco.loan.dto.LoanResponse;
import com.innercircle.sacco.loan.dto.LoanSummaryResponse;
import com.innercircle.sacco.loan.dto.RepaymentRequest;
import com.innercircle.sacco.loan.dto.RepaymentScheduleResponse;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanApplicationRepository loanRepository;

    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoanResponse> applyForLoan(@Valid @RequestBody LoanApplicationRequest request) {
        LoanApplication loan = loanService.applyForLoan(
                request.getMemberId(),
                request.getPrincipalAmount(),
                request.getInterestRate(),
                request.getTermMonths(),
                request.getInterestMethod(),
                request.getPurpose()
        );
        return ApiResponse.ok(LoanResponse.from(loan), "Loan application submitted successfully");
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<LoanResponse> approveLoan(
            @PathVariable UUID id,
            @RequestParam UUID approvedBy) {
        LoanApplication loan = loanService.approveLoan(id, approvedBy);
        return ApiResponse.ok(LoanResponse.from(loan), "Loan approved successfully");
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<LoanResponse> rejectLoan(
            @PathVariable UUID id,
            @RequestParam UUID rejectedBy) {
        LoanApplication loan = loanService.rejectLoan(id, rejectedBy);
        return ApiResponse.ok(LoanResponse.from(loan), "Loan rejected");
    }

    @PatchMapping("/{id}/disburse")
    public ApiResponse<LoanResponse> disburseLoan(
            @PathVariable UUID id,
            @RequestParam String actor) {
        LoanApplication loan = loanService.disburseLoan(id, actor);
        return ApiResponse.ok(LoanResponse.from(loan), "Loan disbursed successfully");
    }

    @PostMapping("/{id}/repay")
    public ApiResponse<Void> recordRepayment(
            @PathVariable UUID id,
            @Valid @RequestBody RepaymentRequest request,
            @RequestParam String actor) {
        LoanRepayment repayment = loanService.recordRepayment(
                id,
                request.getAmount(),
                request.getReferenceNumber(),
                actor
        );
        return ApiResponse.ok(null, "Repayment recorded successfully");
    }

    @GetMapping("/{id}/schedule")
    public ApiResponse<List<RepaymentScheduleResponse>> getLoanSchedule(@PathVariable UUID id) {
        List<RepaymentSchedule> schedules = loanService.getLoanSchedule(id);
        List<RepaymentScheduleResponse> responses = schedules.stream()
                .map(RepaymentScheduleResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.ok(responses);
    }

    @GetMapping
    public ApiResponse<CursorPage<LoanResponse>> listLoans(
            @RequestParam(required = false) UUID memberId,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {

        Pageable pageable = PageRequest.of(0, limit + 1);
        UUID actualCursor = cursor != null ? cursor : new UUID(0L, 0L);

        List<LoanApplication> loans;

        if (memberId != null && status != null) {
            loans = loanRepository.findByMemberIdAndStatusAndIdGreaterThanOrderById(
                    memberId, status, actualCursor, pageable);
        } else if (memberId != null) {
            loans = loanRepository.findByMemberIdAndIdGreaterThanOrderById(memberId, actualCursor, pageable);
        } else if (status != null) {
            loans = loanRepository.findByStatusAndIdGreaterThanOrderById(status, actualCursor, pageable);
        } else {
            loans = loanRepository.findByIdGreaterThanOrderById(actualCursor, pageable);
        }

        boolean hasMore = loans.size() > limit;
        List<LoanApplication> resultLoans = hasMore ? loans.subList(0, limit) : loans;
        String nextCursor = hasMore ? resultLoans.get(resultLoans.size() - 1).getId().toString() : null;

        List<LoanResponse> responses = resultLoans.stream()
                .map(LoanResponse::from)
                .collect(Collectors.toList());

        CursorPage<LoanResponse> page = CursorPage.of(responses, nextCursor, hasMore);
        return ApiResponse.ok(page);
    }

    @GetMapping("/{id}")
    public ApiResponse<LoanResponse> getLoanById(@PathVariable UUID id) {
        LoanApplication loan = loanService.getLoanById(id);
        return ApiResponse.ok(LoanResponse.from(loan));
    }

    @GetMapping("/member/{memberId}/summary")
    public ApiResponse<LoanSummaryResponse> getMemberLoanSummary(@PathVariable UUID memberId) {
        List<LoanApplication> loans = loanService.getMemberLoans(memberId);

        long activeCount = loans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.REPAYING || loan.getStatus() == LoanStatus.DISBURSED)
                .count();

        long closedCount = loans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.CLOSED)
                .count();

        BigDecimal totalBorrowed = loans.stream()
                .filter(loan -> loan.getStatus() != LoanStatus.PENDING && loan.getStatus() != LoanStatus.REJECTED)
                .map(LoanApplication::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRepaid = loans.stream()
                .map(LoanApplication::getTotalRepaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = loans.stream()
                .map(LoanApplication::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LoanResponse> loanResponses = loans.stream()
                .map(LoanResponse::from)
                .collect(Collectors.toList());

        LoanSummaryResponse summary = LoanSummaryResponse.builder()
                .memberId(memberId)
                .totalLoans(loans.size())
                .activeLoans((int) activeCount)
                .closedLoans((int) closedCount)
                .totalBorrowed(totalBorrowed)
                .totalRepaid(totalRepaid)
                .totalOutstanding(totalOutstanding)
                .loans(loanResponses)
                .build();

        return ApiResponse.ok(summary);
    }
}
