package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.PettyCashWorkflowEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.common.guard.MakerCheckerGuard;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.common.util.SecureIdGenerator;
import com.innercircle.sacco.payout.dto.PettyCashSummaryResponse;
import com.innercircle.sacco.payout.entity.PettyCashExpenseType;
import com.innercircle.sacco.payout.entity.PettyCashVoucher;
import com.innercircle.sacco.payout.entity.PettyCashVoucherStatus;
import com.innercircle.sacco.payout.repository.PettyCashVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PettyCashServiceImpl implements PettyCashService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final PettyCashVoucherRepository voucherRepository;
    private final EventOutboxWriter outboxWriter;

    @Override
    @Transactional
    public PettyCashVoucher createVoucher(BigDecimal amount,
                                          String purpose,
                                          PettyCashExpenseType expenseType,
                                          LocalDate requestDate,
                                          String notes,
                                          String actor) {
        PettyCashVoucher voucher = new PettyCashVoucher(
                generateUniqueReferenceNumber(),
                amount,
                purpose,
                expenseType,
                requestDate != null ? requestDate : LocalDate.now(),
                notes
        );
        voucher.setCreatedBy(actor);

        PettyCashVoucher saved = voucherRepository.save(voucher);
        publishWorkflowEvent(saved, "CREATED", null, actor);
        return saved;
    }

    @Override
    @Transactional
    public PettyCashVoucher approveVoucher(UUID voucherId, String actor, String overrideReason, boolean isAdmin) {
        PettyCashVoucher voucher = getExistingVoucher(voucherId);
        ensureStatus(voucher, PettyCashVoucherStatus.SUBMITTED, "Only submitted vouchers can be approved.");

        boolean overrideUsed = MakerCheckerGuard.assertOrOverride(
                voucher.getCreatedBy(), actor, overrideReason, isAdmin, "PettyCashVoucher", voucher.getId()
        );

        voucher.setStatus(PettyCashVoucherStatus.APPROVED);
        voucher.setApprovedBy(actor);

        PettyCashVoucher saved = voucherRepository.save(voucher);
        String action = overrideUsed ? "OVERRIDE_APPROVED" : "APPROVED";
        publishWorkflowEvent(saved, action, null, actor);
        return saved;
    }

    @Override
    @Transactional
    public PettyCashVoucher disburseVoucher(UUID voucherId, String actor) {
        PettyCashVoucher voucher = getExistingVoucher(voucherId);
        ensureStatus(voucher, PettyCashVoucherStatus.APPROVED, "Only approved vouchers can be disbursed.");

        voucher.setStatus(PettyCashVoucherStatus.DISBURSED);
        voucher.setDisbursedBy(actor);
        voucher.setDisbursedAt(Instant.now());

        PettyCashVoucher saved = voucherRepository.save(voucher);
        publishWorkflowEvent(saved, "DISBURSED", null, actor);
        return saved;
    }

    @Override
    @Transactional
    public PettyCashVoucher settleVoucher(UUID voucherId, String receiptNumber, String notes, String actor) {
        PettyCashVoucher voucher = getExistingVoucher(voucherId);
        ensureStatus(voucher, PettyCashVoucherStatus.DISBURSED, "Only disbursed vouchers can be settled.");

        if (!Objects.equals(voucher.getReceiptNumber(), receiptNumber) && voucherRepository.existsByReceiptNumber(receiptNumber)) {
            throw new BusinessException("Receipt number already exists: " + receiptNumber);
        }

        voucher.setStatus(PettyCashVoucherStatus.SETTLED);
        voucher.setSettledBy(actor);
        voucher.setSettledAt(Instant.now());
        voucher.setReceiptNumber(receiptNumber);
        if (notes != null && !notes.isBlank()) {
            voucher.setNotes(notes);
        }

        PettyCashVoucher saved = voucherRepository.save(voucher);
        publishWorkflowEvent(saved, "SETTLED", receiptNumber, actor);
        return saved;
    }

    @Override
    @Transactional
    public PettyCashVoucher rejectVoucher(UUID voucherId, String reason, String actor) {
        PettyCashVoucher voucher = getExistingVoucher(voucherId);
        if (voucher.getStatus() != PettyCashVoucherStatus.SUBMITTED
                && voucher.getStatus() != PettyCashVoucherStatus.APPROVED) {
            throw new BusinessException("Only submitted or approved vouchers can be rejected.");
        }

        voucher.setStatus(PettyCashVoucherStatus.REJECTED);
        voucher.setRejectedBy(actor);
        voucher.setRejectedAt(Instant.now());
        voucher.setRejectionReason(reason);

        PettyCashVoucher saved = voucherRepository.save(voucher);
        publishWorkflowEvent(saved, "REJECTED", null, actor);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PettyCashVoucher getVoucherById(UUID voucherId) {
        return getExistingVoucher(voucherId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<PettyCashVoucher> getVouchers(PettyCashVoucherStatus status,
                                                    LocalDate startDate,
                                                    LocalDate endDate,
                                                    String cursor,
                                                    int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        UUID cursorId = (cursor == null || cursor.isBlank()) ? null : UUID.fromString(cursor);

        List<PettyCashVoucher> vouchers;
        if (status != null && startDate != null && endDate != null) {
            vouchers = cursorId == null
                    ? voucherRepository.findByStatusAndRequestDateBetween(status, startDate, endDate, pageRequest)
                    : voucherRepository.findByStatusAndRequestDateBetweenWithCursor(status, startDate, endDate, cursorId, pageRequest);
        } else if (status != null) {
            vouchers = cursorId == null
                    ? voucherRepository.findByStatus(status, pageRequest)
                    : voucherRepository.findByStatusWithCursor(status, cursorId, pageRequest);
        } else if (startDate != null && endDate != null) {
            vouchers = cursorId == null
                    ? voucherRepository.findByRequestDateBetween(startDate, endDate, pageRequest)
                    : voucherRepository.findByRequestDateBetweenWithCursor(startDate, endDate, cursorId, pageRequest);
        } else {
            vouchers = cursorId == null
                    ? voucherRepository.findAllPaged(pageRequest)
                    : voucherRepository.findAllWithCursor(cursorId, pageRequest);
        }

        return buildCursorPage(vouchers, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public PettyCashSummaryResponse getSummary(PettyCashVoucherStatus status,
                                               LocalDate startDate,
                                               LocalDate endDate) {
        List<PettyCashVoucher> vouchers = getVouchers(status, startDate, endDate, null, 5000).getItems();

        long submitted = vouchers.stream().filter(v -> v.getStatus() == PettyCashVoucherStatus.SUBMITTED).count();
        long approved = vouchers.stream().filter(v -> v.getStatus() == PettyCashVoucherStatus.APPROVED).count();
        long disbursed = vouchers.stream().filter(v -> v.getStatus() == PettyCashVoucherStatus.DISBURSED).count();
        long settled = vouchers.stream().filter(v -> v.getStatus() == PettyCashVoucherStatus.SETTLED).count();
        long rejected = vouchers.stream().filter(v -> v.getStatus() == PettyCashVoucherStatus.REJECTED).count();

        BigDecimal disbursedAmount = vouchers.stream()
                .filter(v -> v.getStatus() == PettyCashVoucherStatus.DISBURSED || v.getStatus() == PettyCashVoucherStatus.SETTLED)
                .map(PettyCashVoucher::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal settledAmount = vouchers.stream()
                .filter(v -> v.getStatus() == PettyCashVoucherStatus.SETTLED)
                .map(PettyCashVoucher::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PettyCashSummaryResponse(
                vouchers.size(),
                submitted,
                approved,
                disbursed,
                settled,
                rejected,
                disbursedAmount,
                settledAmount,
                disbursedAmount.subtract(settledAmount)
        );
    }

    private PettyCashVoucher getExistingVoucher(UUID voucherId) {
        return voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Petty cash voucher", voucherId));
    }

    private void ensureStatus(PettyCashVoucher voucher,
                              PettyCashVoucherStatus expected,
                              String errorMessage) {
        if (voucher.getStatus() != expected) {
            throw new BusinessException(errorMessage);
        }
    }

    private CursorPage<PettyCashVoucher> buildCursorPage(List<PettyCashVoucher> vouchers, int limit) {
        boolean hasMore = vouchers.size() > limit;
        List<PettyCashVoucher> items = hasMore ? vouchers.subList(0, limit) : vouchers;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;
        return CursorPage.of(items, nextCursor, hasMore);
    }

    private void publishWorkflowEvent(PettyCashVoucher voucher, String action, String receiptNumber, String actor) {
        outboxWriter.write(new PettyCashWorkflowEvent(
                voucher.getId(),
                action,
                voucher.getAmount(),
                voucher.getExpenseType().name(),
                voucher.getReferenceNumber(),
                receiptNumber,
                UUID.randomUUID(),
                actor
        ), "PettyCashVoucher", voucher.getId());
    }

    private String generateUniqueReferenceNumber() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = SecureIdGenerator.generate("PC");
            if (!voucherRepository.existsByReferenceNumber(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(
                "Unable to generate unique petty cash reference after " + MAX_GENERATION_ATTEMPTS + " attempts"
        );
    }
}
