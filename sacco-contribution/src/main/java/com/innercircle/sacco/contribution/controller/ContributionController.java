package com.innercircle.sacco.contribution.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.contribution.dto.ContributionResponse;
import com.innercircle.sacco.contribution.dto.ContributionSummaryResponse;
import com.innercircle.sacco.contribution.dto.RecordContributionRequest;
import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionStatus;
import com.innercircle.sacco.contribution.service.ContributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.UUID;

/**
 * REST controller for contribution management.
 * Base path: /api/v1/contributions
 */
@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;

    /**
     * Record a new contribution (PENDING status).
     * POST /api/v1/contributions
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContributionResponse> recordContribution(
            @Valid @RequestBody RecordContributionRequest request) {

        Contribution contribution = new Contribution(
                request.getMemberId(),
                request.getAmount(),
                request.getType(),
                request.getContributionDate(),
                request.getReferenceNumber(),
                request.getNotes()
        );

        Contribution saved = contributionService.recordContribution(contribution);
        return ApiResponse.ok(ContributionResponse.fromEntity(saved), "Contribution recorded successfully");
    }

    /**
     * Confirm a contribution and trigger ledger integration.
     * PATCH /api/v1/contributions/{id}/confirm
     */
    @PatchMapping("/{id}/confirm")
    public ApiResponse<ContributionResponse> confirmContribution(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "SYSTEM") String actor) {

        Contribution confirmed = contributionService.confirmContribution(id, actor);
        return ApiResponse.ok(ContributionResponse.fromEntity(confirmed), "Contribution confirmed successfully");
    }

    /**
     * Reverse a contribution.
     * PATCH /api/v1/contributions/{id}/reverse
     */
    @PatchMapping("/{id}/reverse")
    public ApiResponse<ContributionResponse> reverseContribution(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "SYSTEM") String actor) {

        Contribution reversed = contributionService.reverseContribution(id, actor);
        return ApiResponse.ok(ContributionResponse.fromEntity(reversed), "Contribution reversed successfully");
    }

    /**
     * List contributions with cursor pagination and optional status filter.
     * GET /api/v1/contributions
     */
    @GetMapping
    public ApiResponse<CursorPage<ContributionResponse>> listContributions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ContributionStatus status) {

        CursorPage<Contribution> page = contributionService.list(cursor, size, status);
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
            @RequestParam(defaultValue = "20") int size) {

        CursorPage<Contribution> page = contributionService.getMemberContributions(memberId, cursor, size);
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
    public ApiResponse<ContributionSummaryResponse> getMemberSummary(@PathVariable UUID memberId) {
        ContributionSummaryResponse summary = contributionService.getMemberSummary(memberId);
        return ApiResponse.ok(summary);
    }
}
