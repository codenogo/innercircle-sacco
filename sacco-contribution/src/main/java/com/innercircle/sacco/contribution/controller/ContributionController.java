package com.innercircle.sacco.contribution.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.contribution.dto.BulkContributionRequest;
import com.innercircle.sacco.contribution.dto.ContributionObligationResponse;
import com.innercircle.sacco.contribution.dto.ContributionPenaltyResponse;
import com.innercircle.sacco.contribution.dto.ContributionResponse;
import com.innercircle.sacco.contribution.dto.ContributionSummaryResponse;
import com.innercircle.sacco.contribution.dto.ContributionWelfarePolicyResponse;
import com.innercircle.sacco.contribution.dto.RecordContributionRequest;
import com.innercircle.sacco.contribution.dto.WaivePenaltyRequest;
import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionStatus;
import com.innercircle.sacco.contribution.service.ContributionObligationService;
import com.innercircle.sacco.contribution.service.ContributionPenaltyService;
import com.innercircle.sacco.contribution.service.ContributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for contribution management.
 * Base path: /api/v1/contributions
 */
@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','MEMBER')")
public class ContributionController {

    private final ContributionService contributionService;
    private final ContributionPenaltyService contributionPenaltyService;
    private final ContributionObligationService contributionObligationService;
    private final MemberAccessHelper memberAccessHelper;

    /**
     * Record a new contribution (PENDING status).
     * POST /api/v1/contributions
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<ContributionResponse>> recordContribution(
            @Valid @RequestBody RecordContributionRequest request) {

        Contribution saved = contributionService.recordContribution(request);
        return ResponseEntity.created(URI.create("/api/v1/contributions/" + saved.getId()))
                .body(ApiResponse.ok(ContributionResponse.fromEntity(saved), "Contribution recorded successfully"));
    }

    /**
     * Record multiple contributions at once.
     * POST /api/v1/contributions/bulk
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> recordBulk(
            @Valid @RequestBody BulkContributionRequest request) {

        List<Contribution> saved = contributionService.recordBulk(request);
        List<ContributionResponse> responses = saved.stream()
                .map(ContributionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(responses, "Bulk contributions recorded successfully"));
    }

    /**
     * Confirm a contribution and trigger ledger integration.
     * PATCH /api/v1/contributions/{id}/confirm
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<ContributionResponse> confirmContribution(
            @PathVariable UUID id,
            Authentication authentication) {
        String actor = memberAccessHelper.currentActor(authentication);

        Contribution confirmed = contributionService.confirmContribution(id, actor);
        return ApiResponse.ok(ContributionResponse.fromEntity(confirmed), "Contribution confirmed successfully");
    }

    /**
     * Reverse a contribution.
     * PATCH /api/v1/contributions/{id}/reverse
     */
    @PatchMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<ContributionResponse> reverseContribution(
            @PathVariable UUID id,
            Authentication authentication) {
        String actor = memberAccessHelper.currentActor(authentication);

        Contribution reversed = contributionService.reverseContribution(id, actor);
        return ApiResponse.ok(ContributionResponse.fromEntity(reversed), "Contribution reversed successfully");
    }

    /**
     * List contributions with cursor pagination and optional filters.
     * GET /api/v1/contributions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<CursorPage<ContributionResponse>> listContributions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ContributionStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID memberId,
            @RequestParam(required = false) LocalDate contributionMonth) {

        CursorPage<Contribution> page = contributionService.list(
                cursor,
                size,
                status,
                categoryId,
                memberId,
                contributionMonth
        );
        CursorPage<ContributionResponse> responsePage = CursorPage.of(
                page.getItems().stream()
                        .map(ContributionResponse::fromEntity)
                        .toList(),
                page.getNextCursor(),
                page.isHasMore()
        );

        return ApiResponse.ok(responsePage);
    }

    /**
     * Get contributions for a specific member.
     * GET /api/v1/contributions/member/{memberId}
     */
    @GetMapping("/member/{memberId}")
    public ApiResponse<CursorPage<ContributionResponse>> getMemberContributions(
            @PathVariable UUID memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) LocalDate contributionMonth,
            Authentication authentication) {
        memberAccessHelper.assertAccessToMember(memberId, authentication);

        CursorPage<Contribution> page = contributionService.getMemberContributions(
                memberId,
                cursor,
                size,
                contributionMonth
        );
        CursorPage<ContributionResponse> responsePage = CursorPage.of(
                page.getItems().stream()
                        .map(ContributionResponse::fromEntity)
                        .toList(),
                page.getNextCursor(),
                page.isHasMore()
        );

        return ApiResponse.ok(responsePage);
    }

    /**
     * Get member contribution summary (total contributed, pending, penalties).
     * GET /api/v1/contributions/member/{memberId}/summary
     */
    @GetMapping("/member/{memberId}/summary")
    public ApiResponse<ContributionSummaryResponse> getMemberSummary(
            @PathVariable UUID memberId,
            Authentication authentication) {
        memberAccessHelper.assertAccessToMember(memberId, authentication);
        ContributionSummaryResponse summary = contributionService.getMemberSummary(memberId);
        return ApiResponse.ok(summary);
    }

    /**
     * Get welfare split policy for contributions.
     * GET /api/v1/contributions/welfare-policy
     */
    @GetMapping("/welfare-policy")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<ContributionWelfarePolicyResponse> getWelfarePolicy() {
        return ApiResponse.ok(contributionService.getWelfarePolicy());
    }

    /**
     * List generated monthly contribution obligations.
     * GET /api/v1/contributions/obligations?month=YYYY-MM
     */
    @GetMapping("/obligations")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<List<ContributionObligationResponse>> getObligations(
            @RequestParam String month,
            @RequestParam(required = false) UUID memberId) {
        YearMonth targetMonth = parseMonth(month);
        List<ContributionObligationResponse> response = contributionObligationService
                .getObligations(targetMonth, memberId)
                .stream()
                .map(ContributionObligationResponse::fromEntity)
                .toList();
        return ApiResponse.ok(response);
    }

    /**
     * Manually generate monthly obligations and penalties.
     * POST /api/v1/contributions/obligations/run?month=YYYY-MM
     */
    @PostMapping("/obligations/run")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<List<ContributionObligationResponse>> runObligations(
            @RequestParam String month,
            Authentication authentication) {
        YearMonth targetMonth = parseMonth(month);
        String actor = memberAccessHelper.currentActor(authentication);
        List<ContributionObligationResponse> response = contributionObligationService
                .runMonthlyObligations(targetMonth, actor)
                .stream()
                .map(ContributionObligationResponse::fromEntity)
                .toList();
        return ApiResponse.ok(response, "Contribution obligations generated");
    }

    /**
     * List contribution penalties with optional month filter.
     * GET /api/v1/contributions/penalties?memberId=&month=
     */
    @GetMapping("/penalties")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<List<ContributionPenaltyResponse>> getPenalties(
            @RequestParam UUID memberId,
            @RequestParam(required = false) String month) {
        YearMonth parsedMonth = month == null || month.isBlank() ? null : parseMonth(month);
        List<ContributionPenaltyResponse> response = contributionPenaltyService
                .getPenalties(memberId, parsedMonth)
                .stream()
                .map(ContributionPenaltyResponse::fromEntity)
                .toList();
        return ApiResponse.ok(response);
    }

    /**
     * Waive a contribution penalty.
     * PATCH /api/v1/contributions/penalties/{id}/waive
     */
    @PatchMapping("/penalties/{id}/waive")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    public ApiResponse<ContributionPenaltyResponse> waivePenalty(
            @PathVariable UUID id,
            @RequestBody(required = false) WaivePenaltyRequest request,
            Authentication authentication) {
        String actor = memberAccessHelper.currentActor(authentication);
        String reason = request != null ? request.getReason() : null;
        return ApiResponse.ok(
                ContributionPenaltyResponse.fromEntity(contributionPenaltyService.waivePenalty(id, reason, actor)),
                "Contribution penalty waived successfully"
        );
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (RuntimeException ex) {
            throw new BusinessException("Invalid month format. Use YYYY-MM");
        }
    }
}
