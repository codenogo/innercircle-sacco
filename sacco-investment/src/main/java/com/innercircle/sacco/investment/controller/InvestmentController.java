package com.innercircle.sacco.investment.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.investment.dto.CreateInvestmentRequest;
import com.innercircle.sacco.investment.dto.DisposeInvestmentRequest;
import com.innercircle.sacco.investment.dto.InvestmentIncomeResponse;
import com.innercircle.sacco.investment.dto.InvestmentResponse;
import com.innercircle.sacco.investment.dto.InvestmentSummaryResponse;
import com.innercircle.sacco.investment.dto.InvestmentValuationResponse;
import com.innercircle.sacco.investment.dto.RecordIncomeRequest;
import com.innercircle.sacco.investment.dto.RecordValuationRequest;
import com.innercircle.sacco.investment.dto.RejectInvestmentRequest;
import com.innercircle.sacco.investment.dto.RollOverRequest;
import com.innercircle.sacco.investment.entity.Investment;
import com.innercircle.sacco.investment.entity.InvestmentIncome;
import com.innercircle.sacco.investment.entity.InvestmentValuation;
import com.innercircle.sacco.investment.service.InvestmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final MemberAccessHelper memberAccessHelper;

    @GetMapping
    public ApiResponse<List<InvestmentResponse>> getInvestments() {
        List<InvestmentResponse> investments = investmentService.listInvestments().stream()
                .map(InvestmentResponse::from)
                .toList();
        return ApiResponse.ok(investments);
    }

    @GetMapping("/summary")
    public ApiResponse<InvestmentSummaryResponse> getSummary() {
        return ApiResponse.ok(investmentService.getSummary());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<InvestmentResponse> getInvestment(@PathVariable UUID id) {
        return ApiResponse.ok(InvestmentResponse.from(investmentService.getInvestment(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<InvestmentResponse>> createInvestment(
            @Valid @RequestBody CreateInvestmentRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        Investment created = investmentService.createInvestment(request, actor);

        return ResponseEntity.created(URI.create("/api/v1/investments/" + created.getId()))
                .body(ApiResponse.ok(InvestmentResponse.from(created), "Investment created successfully"));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<InvestmentResponse> approveInvestment(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        Investment updated = investmentService.approveInvestment(id, actor);
        return ApiResponse.ok(InvestmentResponse.from(updated), "Investment approved successfully");
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<InvestmentResponse> rejectInvestment(
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid RejectInvestmentRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        String reason = request != null ? request.reason() : null;
        Investment updated = investmentService.rejectInvestment(id, reason, actor);
        return ApiResponse.ok(InvestmentResponse.from(updated), "Investment rejected successfully");
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<InvestmentResponse> activateInvestment(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        Investment updated = investmentService.activateInvestment(id, actor);
        return ApiResponse.ok(InvestmentResponse.from(updated), "Investment activated successfully");
    }

    @PostMapping("/{id}/dispose")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<InvestmentResponse> disposeInvestment(
            @PathVariable UUID id,
            @Valid @RequestBody DisposeInvestmentRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        Investment updated = investmentService.disposeInvestment(id, request, actor);
        return ApiResponse.ok(InvestmentResponse.from(updated), "Investment disposal recorded successfully");
    }

    @PostMapping("/{id}/rollover")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<InvestmentResponse> rollOverInvestment(
            @PathVariable UUID id,
            @Valid @RequestBody RollOverRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        Investment updated = investmentService.rollOverInvestment(id, request, actor);
        return ApiResponse.ok(InvestmentResponse.from(updated), "Investment rolled over successfully");
    }

    @GetMapping("/{id}/income")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<List<InvestmentIncomeResponse>> getIncomeHistory(@PathVariable UUID id) {
        List<InvestmentIncomeResponse> incomeRecords = investmentService.getIncomeHistory(id).stream()
                .map(InvestmentIncomeResponse::from)
                .toList();
        return ApiResponse.ok(incomeRecords);
    }

    @PostMapping("/{id}/income")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<InvestmentIncomeResponse> recordIncome(
            @PathVariable UUID id,
            @Valid @RequestBody RecordIncomeRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        InvestmentIncome saved = investmentService.recordIncome(id, request, actor);
        return ApiResponse.ok(InvestmentIncomeResponse.from(saved), "Investment income recorded successfully");
    }

    @GetMapping("/{id}/valuations")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<List<InvestmentValuationResponse>> getValuationHistory(@PathVariable UUID id) {
        List<InvestmentValuationResponse> valuations = investmentService.getValuationHistory(id).stream()
                .map(InvestmentValuationResponse::from)
                .toList();
        return ApiResponse.ok(valuations);
    }

    @PostMapping("/{id}/valuations")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<InvestmentValuationResponse> recordValuation(
            @PathVariable UUID id,
            @Valid @RequestBody RecordValuationRequest request,
            Authentication authentication
    ) {
        String actor = memberAccessHelper.currentActor(authentication);
        InvestmentValuation saved = investmentService.recordValuation(id, request, actor);
        return ApiResponse.ok(InvestmentValuationResponse.from(saved), "Investment valuation recorded successfully");
    }
}
