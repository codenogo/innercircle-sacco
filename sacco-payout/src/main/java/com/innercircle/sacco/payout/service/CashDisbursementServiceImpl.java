package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.entity.CashDisbursement;
import com.innercircle.sacco.payout.event.CashDisbursementRecordedEvent;
import com.innercircle.sacco.payout.repository.CashDisbursementRepository;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashDisbursementServiceImpl implements CashDisbursementService {

    private final CashDisbursementRepository disbursementRepository;
    private final EventOutboxWriter outboxWriter;

    @Override
    @Transactional
    public CashDisbursement recordDisbursement(UUID memberId, BigDecimal amount, String receivedBy,
                                              String disbursedBy, String receiptNumber,
                                              LocalDate disbursementDate, String actor) {
        // Check if receipt number already exists
        disbursementRepository.findByReceiptNumber(receiptNumber)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Receipt number already exists: " + receiptNumber);
                });

        CashDisbursement disbursement = new CashDisbursement(
                memberId, amount, receivedBy, disbursedBy, receiptNumber, disbursementDate
        );
        disbursement.setCreatedBy(actor);

        CashDisbursement savedDisbursement = disbursementRepository.save(disbursement);

        outboxWriter.write(new CashDisbursementRecordedEvent(
                savedDisbursement.getId(),
                savedDisbursement.getMemberId(),
                savedDisbursement.getAmount(),
                receiptNumber,
                UUID.randomUUID(),
                actor
        ), "CashDisbursement", savedDisbursement.getId());

        return savedDisbursement;
    }

    @Override
    @Transactional
    public CashDisbursement signoff(UUID disbursementId, String signoffBy) {
        CashDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + disbursementId));

        if (disbursement.getSignoffBy() != null) {
            throw new IllegalStateException("Disbursement already signed off");
        }

        disbursement.setSignoffBy(signoffBy);
        return disbursementRepository.save(disbursement);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<CashDisbursement> getDisbursementHistory(UUID memberId, String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<CashDisbursement> disbursements;

        if (cursor == null || cursor.isBlank()) {
            disbursements = disbursementRepository.findByMemberId(memberId, pageRequest);
        } else {
            UUID cursorId = UUID.fromString(cursor);
            disbursements = disbursementRepository.findByMemberIdWithCursor(memberId, cursorId, pageRequest);
        }

        return buildCursorPage(disbursements, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<CashDisbursement> getDisbursementsByDateRange(LocalDate startDate, LocalDate endDate,
                                                                   String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<CashDisbursement> disbursements;

        if (cursor == null || cursor.isBlank()) {
            disbursements = disbursementRepository.findByDisbursementDateBetween(startDate, endDate, pageRequest);
        } else {
            UUID cursorId = UUID.fromString(cursor);
            disbursements = disbursementRepository.findByDisbursementDateBetweenWithCursor(
                    startDate, endDate, cursorId, pageRequest
            );
        }

        return buildCursorPage(disbursements, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CashDisbursement getDisbursementById(UUID disbursementId) {
        return disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + disbursementId));
    }

    @Override
    @Transactional(readOnly = true)
    public CashDisbursement getDisbursementByReceipt(String receiptNumber) {
        return disbursementRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found for receipt: " + receiptNumber));
    }

    private CursorPage<CashDisbursement> buildCursorPage(List<CashDisbursement> disbursements, int limit) {
        boolean hasMore = disbursements.size() > limit;
        List<CashDisbursement> items = hasMore ? disbursements.subList(0, limit) : disbursements;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasMore);
    }
}
