package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.guard.MakerCheckerGuard;
import com.innercircle.sacco.payout.entity.ShareWithdrawal;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalStatus;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalType;
import com.innercircle.sacco.payout.event.ShareWithdrawalProcessedEvent;
import com.innercircle.sacco.payout.event.ShareWithdrawalRequestedEvent;
import com.innercircle.sacco.payout.repository.ShareWithdrawalRepository;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareWithdrawalServiceImpl implements ShareWithdrawalService {

    private final ShareWithdrawalRepository withdrawalRepository;
    private final EventOutboxWriter outboxWriter;

    @Override
    @Transactional
    public ShareWithdrawal requestWithdrawal(UUID memberId, BigDecimal amount, ShareWithdrawalType withdrawalType,
                                            BigDecimal currentShareBalance, String actor) {
        // Validate withdrawal amount
        if (amount.compareTo(currentShareBalance) > 0) {
            throw new IllegalArgumentException("Withdrawal amount exceeds current share balance");
        }

        if (withdrawalType == ShareWithdrawalType.FULL && amount.compareTo(currentShareBalance) != 0) {
            throw new IllegalArgumentException("Full withdrawal must equal current share balance");
        }

        ShareWithdrawal withdrawal = new ShareWithdrawal(memberId, amount, withdrawalType, currentShareBalance);
        withdrawal.setCreatedBy(actor);

        ShareWithdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        outboxWriter.write(new ShareWithdrawalRequestedEvent(
                savedWithdrawal.getId(),
                savedWithdrawal.getMemberId(),
                savedWithdrawal.getAmount(),
                withdrawalType.name(),
                UUID.randomUUID(),
                actor
        ), "ShareWithdrawal", savedWithdrawal.getId());

        return savedWithdrawal;
    }

    @Override
    @Transactional
    public ShareWithdrawal approveWithdrawal(UUID withdrawalId, String actor, String overrideReason, boolean isAdmin) {
        ShareWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found: " + withdrawalId));

        if (withdrawal.getStatus() != ShareWithdrawalStatus.PENDING) {
            throw new IllegalStateException("Only pending withdrawals can be approved");
        }

        MakerCheckerGuard.assertOrOverride(
                withdrawal.getCreatedBy(), actor, overrideReason, isAdmin, "ShareWithdrawal", withdrawal.getId()
        );

        withdrawal.setStatus(ShareWithdrawalStatus.APPROVED);
        withdrawal.setApprovedBy(actor);

        return withdrawalRepository.save(withdrawal);
    }

    @Override
    @Transactional
    public ShareWithdrawal processWithdrawal(UUID withdrawalId, String actor) {
        ShareWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found: " + withdrawalId));

        if (withdrawal.getStatus() != ShareWithdrawalStatus.APPROVED) {
            throw new IllegalStateException("Only approved withdrawals can be processed");
        }

        // Calculate new share balance
        BigDecimal newShareBalance = withdrawal.getCurrentShareBalance().subtract(withdrawal.getAmount());
        withdrawal.setNewShareBalance(newShareBalance);
        withdrawal.setStatus(ShareWithdrawalStatus.PROCESSED);

        ShareWithdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        outboxWriter.write(new ShareWithdrawalProcessedEvent(
                savedWithdrawal.getId(),
                savedWithdrawal.getMemberId(),
                savedWithdrawal.getAmount(),
                newShareBalance,
                UUID.randomUUID(),
                actor
        ), "ShareWithdrawal", savedWithdrawal.getId());

        return savedWithdrawal;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ShareWithdrawal> getWithdrawalsByMember(UUID memberId, String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<ShareWithdrawal> withdrawals;

        if (cursor == null || cursor.isBlank()) {
            withdrawals = withdrawalRepository.findByMemberId(memberId, pageRequest);
        } else {
            UUID cursorId = UUID.fromString(cursor);
            withdrawals = withdrawalRepository.findByMemberIdWithCursor(memberId, cursorId, pageRequest);
        }

        return buildCursorPage(withdrawals, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ShareWithdrawal> getWithdrawalsByStatus(ShareWithdrawalStatus status, String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<ShareWithdrawal> withdrawals;

        if (cursor == null || cursor.isBlank()) {
            withdrawals = withdrawalRepository.findByStatus(status, pageRequest);
        } else {
            UUID cursorId = UUID.fromString(cursor);
            withdrawals = withdrawalRepository.findByStatusWithCursor(status, cursorId, pageRequest);
        }

        return buildCursorPage(withdrawals, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ShareWithdrawal getWithdrawalById(UUID withdrawalId) {
        return withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found: " + withdrawalId));
    }

    private CursorPage<ShareWithdrawal> buildCursorPage(List<ShareWithdrawal> withdrawals, int limit) {
        boolean hasMore = withdrawals.size() > limit;
        List<ShareWithdrawal> items = hasMore ? withdrawals.subList(0, limit) : withdrawals;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasMore);
    }
}
