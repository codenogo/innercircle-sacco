package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.entity.BankWithdrawal;
import com.innercircle.sacco.payout.entity.WithdrawalStatus;
import com.innercircle.sacco.payout.event.BankWithdrawalConfirmedEvent;
import com.innercircle.sacco.payout.event.BankWithdrawalInitiatedEvent;
import com.innercircle.sacco.payout.repository.BankWithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankWithdrawalServiceImpl implements BankWithdrawalService {

    private final BankWithdrawalRepository withdrawalRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public BankWithdrawal initiateWithdrawal(UUID memberId, BigDecimal amount, String bankName,
                                            String accountNumber, String actor) {
        BankWithdrawal withdrawal = new BankWithdrawal(memberId, amount, bankName, accountNumber);
        withdrawal.setCreatedBy(actor);

        BankWithdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        eventPublisher.publishEvent(new BankWithdrawalInitiatedEvent(
                savedWithdrawal.getId(),
                savedWithdrawal.getMemberId(),
                savedWithdrawal.getAmount(),
                savedWithdrawal.getBankName(),
                actor
        ));

        return savedWithdrawal;
    }

    @Override
    @Transactional
    public BankWithdrawal confirmWithdrawal(UUID withdrawalId, String referenceNumber, String actor) {
        BankWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found: " + withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Only pending withdrawals can be confirmed");
        }

        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        withdrawal.setReferenceNumber(referenceNumber);
        withdrawal.setTransactionDate(LocalDate.now());

        BankWithdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        eventPublisher.publishEvent(new BankWithdrawalConfirmedEvent(
                savedWithdrawal.getId(),
                savedWithdrawal.getMemberId(),
                referenceNumber,
                actor
        ));

        return savedWithdrawal;
    }

    @Override
    @Transactional
    public BankWithdrawal markReconciled(UUID withdrawalId, String actor) {
        BankWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found: " + withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.COMPLETED) {
            throw new IllegalStateException("Only completed withdrawals can be reconciled");
        }

        withdrawal.setReconciled(true);
        return withdrawalRepository.save(withdrawal);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<BankWithdrawal> getUnreconciled(String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<BankWithdrawal> withdrawals;

        if (cursor == null || cursor.isBlank()) {
            withdrawals = withdrawalRepository.findUnreconciled(pageRequest);
        } else {
            UUID cursorId = UUID.fromString(cursor);
            withdrawals = withdrawalRepository.findUnreconciledWithCursor(cursorId, pageRequest);
        }

        return buildCursorPage(withdrawals, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<BankWithdrawal> getWithdrawalsByMember(UUID memberId, String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<BankWithdrawal> withdrawals;

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
    public CursorPage<BankWithdrawal> getWithdrawalsByStatus(WithdrawalStatus status, String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<BankWithdrawal> withdrawals;

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
    public BankWithdrawal getWithdrawalById(UUID withdrawalId) {
        return withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found: " + withdrawalId));
    }

    private CursorPage<BankWithdrawal> buildCursorPage(List<BankWithdrawal> withdrawals, int limit) {
        boolean hasMore = withdrawals.size() > limit;
        List<BankWithdrawal> items = hasMore ? withdrawals.subList(0, limit) : withdrawals;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasMore);
    }
}
