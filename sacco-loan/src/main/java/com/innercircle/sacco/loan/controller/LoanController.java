package com.innercircle.sacco.loan.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.loan.dto.LoanApplicationRequest;
import com.innercircle.sacco.loan.dto.LoanResponse;
import com.innercircle.sacco.loan.dto.LoanSummaryResponse;
import com.innercircle.sacco.loan.dto.MemberInterestSummary;
import com.innercircle.sacco.loan.dto.MonthlyInterestSummary;
import com.innercircle.sacco.loan.dto.RepaymentRequest;
import com.innercircle.sacco.loan.dto.RepaymentScheduleResponse;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanRepayment;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.service.InterestReportingService;
import com.innercircle.sacco.loan.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')")
public class LoanController {

    private final LoanService loanService;
    private final InterestReportingService interestReportingService;
    private final LoanApplicationRepository loanRepository;
    private final MemberAccessHelper memberAccessHelper;

    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<LoanResponse> applyForLoan(@Valid @RequestBody LoanApplicationRequest request) {
        LoanApplication loan = loanService.applyForLoan(
                request.getMemberId(),
                request.getLoanProductId(),
                request.getPrincipalAmount(),
                request.getTermMonths(),
                request.getPurpose()
        );
        return ApiResponse.ok(LoanResponse.from(loan), "Loan application submitted successfully");
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<LoanResponse> approveLoan(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID approvedBy = memberAccessHelper.resolveCurrentUserId(authentication);
        LoanApplication loan = loanService.approveLoan(id, approvedBy);
        return ApiResponse.ok(LoanResponse.from(loan), "Loan approved successfully");
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<LoanResponse> rejectLoan(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID rejectedBy = memberAccessHelper.resolveCurrentUserId(authentication);
        LoanApplication loan = loanService.rejectLoan(id, rejectedBy);
        return ApiResponse.ok(LoanResponse.from(loan), "Loan rejected");
    }

    @PatchMapping("/{id}/disburse")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<LoanResponse> disburseLoan(
            @PathVariable UUID id,
            Authentication authentication) {
        String actor = memberAccessHelper.currentActor(authentication);
        LoanApplication loan = loanService.disburseLoan(id, actor);
        return ApiResponse.ok(LoanResponse.from(loan), "Loan disbursed successfully");
    }

    @PostMapping("/{id}/repay")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<Void> recordRepayment(
            @PathVariable UUID id,
            @Valid @RequestBody RepaymentRequest request,
            Authentication authentication) {
        String actor = memberAccessHelper.currentActor(authentication);
        LoanRepayment repayment = loanService.recordRepayment(
                id,
                request.getAmount(),
                request.getReferenceNumber(),
                actor
        );
        return ApiResponse.ok(null, "Repayment recorded successfully");
    }

    @GetMapping("/{id}/schedule")
    public ApiResponse<List<RepaymentScheduleResponse>> getLoanSchedule(
            @PathVariable UUID id,
            Authentication authentication) {
        LoanApplication loan = loanService.getLoanById(id);
        memberAccessHelper.assertAccessToMember(loan.getMemberId(), authentication);

        List<RepaymentSchedule> schedules = loanService.getLoanSchedule(id);
        List<RepaymentScheduleResponse> responses = schedules.stream()
                .map(RepaymentScheduleResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.ok(responses);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
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
    public ApiResponse<LoanResponse> getLoanById(
            @PathVariable UUID id,
            Authentication authentication) {
        LoanApplication loan = loanService.getLoanById(id);
        memberAccessHelper.assertAccessToMember(loan.getMemberId(), authentication);
        return ApiResponse.ok(LoanResponse.from(loan));
    }

    @GetMapping("/member/{memberId}/summary")
    public ApiResponse<LoanSummaryResponse> getMemberLoanSummary(
            @PathVariable UUID memberId,
            Authentication authentication) {
        memberAccessHelper.assertAccessToMember(memberId, authentication);
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

    @GetMapping("/interest/summary")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<MonthlyInterestSummary> getMonthlyInterestSummary(
            @RequestParam String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        MonthlyInterestSummary summary = interestReportingService.getMonthlyInterestSummary(yearMonth);
        return ApiResponse.ok(summary);
    }

    @GetMapping("/interest/member/{memberId}")
    public ApiResponse<List<MemberInterestSummary>> getMemberInterestSummary(
            @PathVariable UUID memberId,
            Authentication authentication) {
        memberAccessHelper.assertAccessToMember(memberId, authentication);
        List<MemberInterestSummary> summaries = interestReportingService.getMemberInterestSummary(memberId);
        return ApiResponse.ok(summaries);
    }

    @GetMapping("/interest/arrears")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<List<MemberInterestSummary>> getPortfolioInterestArrears() {
        List<MemberInterestSummary> arrears = interestReportingService.getPortfolioInterestArrears();
        return ApiResponse.ok(arrears);
    }
}
