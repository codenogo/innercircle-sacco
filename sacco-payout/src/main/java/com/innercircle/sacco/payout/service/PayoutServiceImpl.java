package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.PayoutProcessedEvent;
import com.innercircle.sacco.common.event.PayoutStatusChangeEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.guard.MakerCheckerGuard;
import com.innercircle.sacco.payout.entity.Payout;
import com.innercircle.sacco.payout.entity.PayoutStatus;
import com.innercircle.sacco.payout.entity.PayoutType;
import com.innercircle.sacco.payout.guard.PayoutTransitionGuards;
import com.innercircle.sacco.payout.repository.PayoutRepository;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.common.util.SecureIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private static final int MAX_REFERENCE_NUMBER_ATTEMPTS = 10;

    private final PayoutRepository payoutRepository;
    private final EventOutboxWriter outboxWriter;

    @Override
    @Transactional
    public Payout createPayout(
            UUID memberId,
            BigDecimal amount,
            PayoutType type,
            String sourceType,
            UUID sourceId,
            Integer installmentNumber,
            String actor
    ) {
        Payout payout = new Payout(memberId, amount, type);
        payout.setSourceType(sourceType);
        payout.setSourceId(sourceId);
        payout.setInstallmentNumber(installmentNumber);
        payout.setCreatedBy(actor);
        Payout saved = payoutRepository.save(payout);

        outboxWriter.write(new PayoutStatusChangeEvent(
                saved.getId(),
                saved.getMemberId(),
                "CREATED",
                UUID.randomUUID(),
                actor
        ), "Payout", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public Payout approvePayout(UUID payoutId, String actor, String overrideReason, boolean isAdmin) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));

        PayoutTransitionGuards.PAYOUT.validate(payout.getStatus(), PayoutStatus.APPROVED);

        boolean overrideUsed = MakerCheckerGuard.assertOrOverride(
                payout.getCreatedBy(), actor, overrideReason, isAdmin, "Payout", payout.getId()
        );

        payout.setStatus(PayoutStatus.APPROVED);
        payout.setApprovedBy(actor);

        Payout approved = payoutRepository.save(payout);

        String action = overrideUsed ? "OVERRIDE_APPROVED" : "APPROVED";
        outboxWriter.write(new PayoutStatusChangeEvent(
                approved.getId(),
                approved.getMemberId(),
                action,
                UUID.randomUUID(),
                actor
        ), "Payout", approved.getId());

        return approved;
    }

    @Override
    @Transactional
    public Payout processPayout(UUID payoutId, String actor) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));

        PayoutTransitionGuards.PAYOUT.validate(payout.getStatus(), PayoutStatus.PROCESSED);

        payout.setStatus(PayoutStatus.PROCESSED);
        payout.setProcessedAt(Instant.now());
        payout.setReferenceNumber(generateUniqueReferenceNumber());

        Payout savedPayout = payoutRepository.save(payout);

        outboxWriter.write(new PayoutProcessedEvent(
                savedPayout.getId(),
                savedPayout.getMemberId(),
                savedPayout.getAmount(),
                savedPayout.getType().name(),
                savedPayout.getSourceType(),
                savedPayout.getSourceId(),
                savedPayout.getInstallmentNumber(),
                UUID.randomUUID(),
                actor
        ), "Payout", savedPayout.getId());

        return savedPayout;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Payout> getPayoutHistory(UUID memberId, String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<Payout> payouts;

        if (cursor == null || cursor.isBlank()) {
            payouts = payoutRepository.findByMemberId(memberId, pageRequest);
        } else {
            UUID cursorId = UUID.fromString(cursor);
            payouts = payoutRepository.findByMemberIdWithCursor(memberId, cursorId, pageRequest);
        }

        return buildCursorPage(payouts, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Payout> getPayoutsByStatus(PayoutStatus status, String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<Payout> payouts;

        if (cursor == null || cursor.isBlank()) {
            payouts = payoutRepository.findByStatus(status, pageRequest);
        } else {
            UUID cursorId = UUID.fromString(cursor);
            payouts = payoutRepository.findByStatusWithCursor(status, cursorId, pageRequest);
        }

        return buildCursorPage(payouts, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Payout> getAllPayouts(String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<Payout> payouts;

        if (cursor == null || cursor.isBlank()) {
            payouts = payoutRepository.findAllPaged(pageRequest);
        } else {
            UUID cursorId = UUID.fromString(cursor);
            payouts = payoutRepository.findAllWithCursor(cursorId, pageRequest);
        }

        return buildCursorPage(payouts, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Payout getPayoutById(UUID payoutId) {
        return payoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
    }

    private CursorPage<Payout> buildCursorPage(List<Payout> payouts, int limit) {
        boolean hasMore = payouts.size() > limit;
        List<Payout> items = hasMore ? payouts.subList(0, limit) : payouts;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasMore);
    }

    private String generateUniqueReferenceNumber() {
        for (int attempt = 0; attempt < MAX_REFERENCE_NUMBER_ATTEMPTS; attempt++) {
            String candidate = SecureIdGenerator.generate("PAY");
            if (!payoutRepository.existsByReferenceNumber(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("Unable to generate unique reference number");
    }
}
