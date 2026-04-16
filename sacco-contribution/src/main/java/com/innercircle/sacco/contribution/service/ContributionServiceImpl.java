package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.ContributionCreatedEvent;
import com.innercircle.sacco.common.event.ContributionReceivedEvent;
import com.innercircle.sacco.common.event.ContributionReversedEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.contribution.dto.BulkContributionItemRequest;
import com.innercircle.sacco.contribution.dto.BulkContributionRequest;
import com.innercircle.sacco.contribution.dto.ContributionSummaryResponse;
import com.innercircle.sacco.contribution.dto.ContributionWelfarePolicyResponse;
import com.innercircle.sacco.contribution.dto.RecordContributionRequest;
import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionCategory;
import com.innercircle.sacco.contribution.entity.ContributionStatus;
import com.innercircle.sacco.contribution.guard.ContributionTransitionGuards;
import com.innercircle.sacco.contribution.repository.ContributionCategoryRepository;
import com.innercircle.sacco.contribution.repository.ContributionPenaltyRepository;
import com.innercircle.sacco.contribution.repository.ContributionRepository;
import com.innercircle.sacco.config.service.PolicyConfigResolver;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.common.util.SecureIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of ContributionService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContributionServiceImpl implements ContributionService {

    private static final String WELFARE_FIXED_AMOUNT_CONFIG_KEY = "contribution.welfare.fixed_amount";
    private static final int MAX_REFERENCE_NUMBER_ATTEMPTS = 10;

    private final ContributionRepository contributionRepository;
    private final ContributionPenaltyRepository penaltyRepository;
    private final ContributionCategoryRepository categoryRepository;
    private final PolicyConfigResolver policyConfigResolver;
    private final ContributionObligationService obligationService;
    private final EventOutboxWriter outboxWriter;

    private record ContributionSplit(BigDecimal contributionAmount, BigDecimal welfareAmount) {}

    @Override
    @Transactional
    public Contribution recordContribution(RecordContributionRequest request) {
        String referenceNumber = resolveReferenceNumber(request.getReferenceNumber());

        // Resolve and validate category
        ContributionCategory category = resolveCategory(request.getCategoryId());
        BigDecimal fixedWelfareAmount = getWelfareFixedAmount();
        ContributionSplit split = calculateSplit(request.getAmount(), category, fixedWelfareAmount);

        Contribution contribution = new Contribution(
                request.getMemberId(),
                request.getAmount(),
                category,
                request.getPaymentMode(),
                request.getContributionMonth(),
                request.getContributionDate(),
                referenceNumber,
                request.getNotes()
        );
        contribution.setContributionAmount(split.contributionAmount());
        contribution.setWelfareAmount(split.welfareAmount());
        contribution.setObligationId(
                obligationService.resolveObligationIdForContribution(
                        contribution.getMemberId(),
                        contribution.getContributionMonth()
                )
        );

        Contribution saved = contributionRepository.save(contribution);

        outboxWriter.write(new ContributionCreatedEvent(
                saved.getId(),
                saved.getMemberId(),
                saved.getAmount(),
                saved.getReferenceNumber(),
                UUID.randomUUID(),
                "system"
        ), "Contribution", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public List<Contribution> recordBulk(BulkContributionRequest request) {
        ContributionCategory category = resolveCategory(request.getCategoryId());
        BigDecimal fixedWelfareAmount = getWelfareFixedAmount();

        List<Contribution> contributions = new ArrayList<>();
        for (BulkContributionItemRequest req : request.getContributions()) {
            String referenceNumber = resolveReferenceNumber(req.getReferenceNumber());

            Contribution c = new Contribution(
                    req.getMemberId(),
                    req.getAmount(),
                    category,
                    req.getPaymentMode() != null ? req.getPaymentMode() : request.getPaymentMode(),
                    req.getContributionMonth() != null ? req.getContributionMonth() : request.getContributionMonth(),
                    req.getContributionDate() != null ? req.getContributionDate() : request.getContributionDate(),
                    referenceNumber,
                    req.getNotes() != null ? req.getNotes() : "Bulk entry: " + request.getBatchReference()
            );
            ContributionSplit split = calculateSplit(req.getAmount(), category, fixedWelfareAmount);
            c.setContributionAmount(split.contributionAmount());
            c.setWelfareAmount(split.welfareAmount());
            c.setObligationId(
                    obligationService.resolveObligationIdForContribution(
                            c.getMemberId(),
                            c.getContributionMonth()
                    )
            );

            contributions.add(c);
        }

        List<Contribution> saved = contributionRepository.saveAll(contributions);

        for (Contribution c : saved) {
            outboxWriter.write(new ContributionCreatedEvent(
                    c.getId(),
                    c.getMemberId(),
                    c.getAmount(),
                    c.getReferenceNumber(),
                    UUID.randomUUID(),
                    "system"
            ), "Contribution", c.getId());
        }

        return saved;
    }

    @Override
    @Transactional
    public Contribution confirmContribution(UUID contributionId, String actor) {
        Contribution contribution = findById(contributionId);

        ContributionTransitionGuards.CONTRIBUTION.validate(contribution.getStatus(), ContributionStatus.CONFIRMED);

        contribution.setStatus(ContributionStatus.CONFIRMED);
        Contribution confirmed = contributionRepository.save(contribution);
        obligationService.applyConfirmedContribution(confirmed);

        outboxWriter.write(new ContributionReceivedEvent(
                confirmed.getId(),
                confirmed.getMemberId(),
                confirmed.getAmount(),
                confirmed.getCategory().getId(),
                confirmed.getContributionAmount(),
                confirmed.getWelfareAmount(),
                confirmed.getReferenceNumber(),
                UUID.randomUUID(),
                actor
        ), "Contribution", confirmed.getId());

        return confirmed;
    }

    @Override
    @Transactional
    public Contribution reverseContribution(UUID contributionId, String actor) {
        Contribution contribution = findById(contributionId);

        ContributionTransitionGuards.CONTRIBUTION.validate(contribution.getStatus(), ContributionStatus.REVERSED);

        contribution.setStatus(ContributionStatus.REVERSED);
        Contribution reversed = contributionRepository.save(contribution);
        obligationService.reverseConfirmedContribution(reversed);

        outboxWriter.write(new ContributionReversedEvent(
                reversed.getId(),
                reversed.getMemberId(),
                reversed.getAmount(),
                reversed.getCategory().getId(),
                reversed.getContributionAmount(),
                reversed.getWelfareAmount(),
                reversed.getReferenceNumber(),
                UUID.randomUUID(),
                actor
        ), "Contribution", reversed.getId());

        return reversed;
    }

    @Override
    @Transactional(readOnly = true)
    public Contribution findById(UUID contributionId) {
        return contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution", contributionId));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Contribution> list(
            String cursor,
            int size,
            ContributionStatus status,
            UUID categoryId,
            UUID memberId,
            LocalDate contributionMonth
    ) {
        UUID cursorId = (cursor != null && !cursor.isEmpty())
                ? UUID.fromString(cursor)
                : new UUID(0L, 0L);

        Specification<Contribution> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (memberId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("memberId"), memberId));
        }
        if (contributionMonth != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("contributionMonth"), contributionMonth));
        }
        spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("id"), cursorId));

        List<Contribution> contributions = contributionRepository.findAll(spec,
                PageRequest.of(0, size + 1, Sort.by("id").ascending())).getContent();

        boolean hasMore = contributions.size() > size;
        if (hasMore) {
            contributions = new ArrayList<>(contributions).subList(0, size);
        }

        String nextCursor = hasMore && !contributions.isEmpty()
                ? contributions.get(contributions.size() - 1).getId().toString()
                : null;

        return CursorPage.of(contributions, nextCursor, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Contribution> getMemberContributions(
            UUID memberId,
            String cursor,
            int size,
            LocalDate contributionMonth
    ) {
        return list(cursor, size, null, null, memberId, contributionMonth);
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionSummaryResponse getMemberSummary(UUID memberId) {
        BigDecimal totalConfirmed = contributionRepository.sumConfirmedContributionsByMember(memberId);
        BigDecimal totalPending = contributionRepository.sumPendingContributionsByMember(memberId);
        BigDecimal totalPenalties = penaltyRepository.sumUnwaivedPenaltiesByMember(memberId);
        LocalDate lastContributionDate = contributionRepository.findLastContributionDate(memberId)
                .orElse(null);

        return new ContributionSummaryResponse(
                memberId,
                totalConfirmed,
                totalPending,
                totalPenalties,
                lastContributionDate
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionWelfarePolicyResponse getWelfarePolicy() {
        BigDecimal fixedAmount = getWelfareFixedAmount();
        return new ContributionWelfarePolicyResponse(
                fixedAmount.compareTo(BigDecimal.ZERO) > 0,
                fixedAmount
        );
    }

    // -------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------

    private ContributionCategory resolveCategory(UUID categoryId) {
        ContributionCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("Category not found: " + categoryId));
        if (!category.isActive()) {
            throw new BusinessException("Category is not active: " + category.getName());
        }
        return category;
    }

    private ContributionSplit calculateSplit(BigDecimal grossAmount, ContributionCategory category, BigDecimal fixedWelfareAmount) {
        if (!category.isWelfareEligible() || fixedWelfareAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new ContributionSplit(grossAmount, BigDecimal.ZERO);
        }

        if (grossAmount.compareTo(fixedWelfareAmount) < 0) {
            throw new BusinessException("Contribution amount cannot be lower than welfare fixed amount: " + fixedWelfareAmount);
        }

        return new ContributionSplit(grossAmount.subtract(fixedWelfareAmount), fixedWelfareAmount);
    }

    private BigDecimal getWelfareFixedAmount() {
        BigDecimal amount = policyConfigResolver.requireNonNegativeDecimal(WELFARE_FIXED_AMOUNT_CONFIG_KEY);
        return amount.compareTo(BigDecimal.ZERO) > 0 ? amount : BigDecimal.ZERO;
    }

    private String resolveReferenceNumber(String referenceNumber) {
        if (referenceNumber != null && !referenceNumber.isBlank()) {
            String normalizedReferenceNumber = referenceNumber.trim();
            if (contributionRepository.existsByReferenceNumber(normalizedReferenceNumber)) {
                throw new BusinessException("Reference number already exists: " + normalizedReferenceNumber);
            }
            return normalizedReferenceNumber;
        }

        for (int attempt = 0; attempt < MAX_REFERENCE_NUMBER_ATTEMPTS; attempt++) {
            String candidate = SecureIdGenerator.generate("CN");
            if (!contributionRepository.existsByReferenceNumber(candidate)) {
                return candidate;
            }
        }

        throw new BusinessException("Unable to generate unique reference number");
    }
}
