package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.entity.BankWithdrawal;
import com.innercircle.sacco.payout.entity.WithdrawalStatus;
import com.innercircle.sacco.payout.event.BankWithdrawalConfirmedEvent;
import com.innercircle.sacco.payout.event.BankWithdrawalInitiatedEvent;
import com.innercircle.sacco.payout.repository.BankWithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankWithdrawalServiceImplTest {

    @Mock
    private BankWithdrawalRepository withdrawalRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BankWithdrawalServiceImpl bankWithdrawalService;

    @Captor
    private ArgumentCaptor<BankWithdrawal> withdrawalCaptor;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private UUID memberId;
    private UUID withdrawalId;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        withdrawalId = UUID.randomUUID();
        amount = new BigDecimal("10000.00");
        bankName = "KCB Bank";
        accountNumber = "1234567890";
    }

    private BankWithdrawal createTestWithdrawal(WithdrawalStatus status) {
        BankWithdrawal withdrawal = new BankWithdrawal(memberId, amount, bankName, accountNumber);
        withdrawal.setId(withdrawalId);
        withdrawal.setStatus(status);
        withdrawal.setCreatedAt(Instant.now());
        withdrawal.setUpdatedAt(Instant.now());
        return withdrawal;
    }

    // ===================================================================
    // initiateWithdrawal
    // ===================================================================

    @Nested
    @DisplayName("initiateWithdrawal")
    class InitiateWithdrawalTests {

        @Test
        @DisplayName("should initiate withdrawal with PENDING status")
        void shouldInitiateWithdrawalWithPendingStatus() {
            BankWithdrawal expected = createTestWithdrawal(WithdrawalStatus.PENDING);
            when(withdrawalRepository.save(any(BankWithdrawal.class))).thenReturn(expected);

            BankWithdrawal result = bankWithdrawalService.initiateWithdrawal(
                    memberId, amount, bankName, accountNumber, "admin"
            );

            verify(withdrawalRepository).save(withdrawalCaptor.capture());
            BankWithdrawal captured = withdrawalCaptor.getValue();
            assertThat(captured.getMemberId()).isEqualTo(memberId);
            assertThat(captured.getAmount()).isEqualByComparingTo(amount);
            assertThat(captured.getBankName()).isEqualTo(bankName);
            assertThat(captured.getAccountNumber()).isEqualTo(accountNumber);
            assertThat(captured.getStatus()).isEqualTo(WithdrawalStatus.PENDING);
            assertThat(captured.isReconciled()).isFalse();
            assertThat(captured.getCreatedBy()).isEqualTo("admin");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should publish BankWithdrawalInitiatedEvent")
        void shouldPublishBankWithdrawalInitiatedEvent() {
            BankWithdrawal saved = createTestWithdrawal(WithdrawalStatus.PENDING);
            when(withdrawalRepository.save(any(BankWithdrawal.class))).thenReturn(saved);

            bankWithdrawalService.initiateWithdrawal(memberId, amount, bankName, accountNumber, "admin");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            Object event = eventCaptor.getValue();
            assertThat(event).isInstanceOf(BankWithdrawalInitiatedEvent.class);
            BankWithdrawalInitiatedEvent initiatedEvent = (BankWithdrawalInitiatedEvent) event;
            assertThat(initiatedEvent.withdrawalId()).isEqualTo(saved.getId());
            assertThat(initiatedEvent.memberId()).isEqualTo(saved.getMemberId());
            assertThat(initiatedEvent.amount()).isEqualByComparingTo(saved.getAmount());
            assertThat(initiatedEvent.bankName()).isEqualTo(bankName);
            assertThat(initiatedEvent.actor()).isEqualTo("admin");
        }
    }

    // ===================================================================
    // confirmWithdrawal
    // ===================================================================

    @Nested
    @DisplayName("confirmWithdrawal")
    class ConfirmWithdrawalTests {

        @Test
        @DisplayName("should confirm a PENDING withdrawal")
        void shouldConfirmPendingWithdrawal() {
            BankWithdrawal pendingWithdrawal = createTestWithdrawal(WithdrawalStatus.PENDING);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(pendingWithdrawal));

            BankWithdrawal confirmedWithdrawal = createTestWithdrawal(WithdrawalStatus.COMPLETED);
            confirmedWithdrawal.setReferenceNumber("REF-001");
            confirmedWithdrawal.setTransactionDate(LocalDate.now());
            when(withdrawalRepository.save(any(BankWithdrawal.class))).thenReturn(confirmedWithdrawal);

            BankWithdrawal result = bankWithdrawalService.confirmWithdrawal(withdrawalId, "REF-001", "admin");

            verify(withdrawalRepository).save(withdrawalCaptor.capture());
            BankWithdrawal captured = withdrawalCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(WithdrawalStatus.COMPLETED);
            assertThat(captured.getReferenceNumber()).isEqualTo("REF-001");
            assertThat(captured.getTransactionDate()).isEqualTo(LocalDate.now());
            assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.COMPLETED);
        }

        @Test
        @DisplayName("should publish BankWithdrawalConfirmedEvent")
        void shouldPublishBankWithdrawalConfirmedEvent() {
            BankWithdrawal pendingWithdrawal = createTestWithdrawal(WithdrawalStatus.PENDING);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(pendingWithdrawal));

            BankWithdrawal confirmedWithdrawal = createTestWithdrawal(WithdrawalStatus.COMPLETED);
            confirmedWithdrawal.setReferenceNumber("REF-001");
            when(withdrawalRepository.save(any(BankWithdrawal.class))).thenReturn(confirmedWithdrawal);

            bankWithdrawalService.confirmWithdrawal(withdrawalId, "REF-001", "admin");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            Object event = eventCaptor.getValue();
            assertThat(event).isInstanceOf(BankWithdrawalConfirmedEvent.class);
            BankWithdrawalConfirmedEvent confirmedEvent = (BankWithdrawalConfirmedEvent) event;
            assertThat(confirmedEvent.withdrawalId()).isEqualTo(confirmedWithdrawal.getId());
            assertThat(confirmedEvent.memberId()).isEqualTo(confirmedWithdrawal.getMemberId());
            assertThat(confirmedEvent.referenceNumber()).isEqualTo("REF-001");
            assertThat(confirmedEvent.actor()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should throw when withdrawal not found")
        void shouldThrowWhenWithdrawalNotFound() {
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bankWithdrawalService.confirmWithdrawal(withdrawalId, "REF-001", "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Withdrawal not found");
        }

        @Test
        @DisplayName("should throw when withdrawal is not PENDING")
        void shouldThrowWhenWithdrawalNotPending() {
            BankWithdrawal completedWithdrawal = createTestWithdrawal(WithdrawalStatus.COMPLETED);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(completedWithdrawal));

            assertThatThrownBy(() -> bankWithdrawalService.confirmWithdrawal(withdrawalId, "REF-001", "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only pending withdrawals can be confirmed");

            verify(withdrawalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when withdrawal has PROCESSING status")
        void shouldThrowWhenWithdrawalIsProcessing() {
            BankWithdrawal processingWithdrawal = createTestWithdrawal(WithdrawalStatus.PROCESSING);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(processingWithdrawal));

            assertThatThrownBy(() -> bankWithdrawalService.confirmWithdrawal(withdrawalId, "REF-001", "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only pending withdrawals can be confirmed");
        }

        @Test
        @DisplayName("should throw when withdrawal has FAILED status")
        void shouldThrowWhenWithdrawalFailed() {
            BankWithdrawal failedWithdrawal = createTestWithdrawal(WithdrawalStatus.FAILED);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(failedWithdrawal));

            assertThatThrownBy(() -> bankWithdrawalService.confirmWithdrawal(withdrawalId, "REF-001", "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only pending withdrawals can be confirmed");
        }
    }

    // ===================================================================
    // markReconciled
    // ===================================================================

    @Nested
    @DisplayName("markReconciled")
    class MarkReconciledTests {

        @Test
        @DisplayName("should mark COMPLETED withdrawal as reconciled")
        void shouldMarkCompletedWithdrawalAsReconciled() {
            BankWithdrawal completedWithdrawal = createTestWithdrawal(WithdrawalStatus.COMPLETED);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(completedWithdrawal));

            BankWithdrawal reconciledWithdrawal = createTestWithdrawal(WithdrawalStatus.COMPLETED);
            reconciledWithdrawal.setReconciled(true);
            when(withdrawalRepository.save(any(BankWithdrawal.class))).thenReturn(reconciledWithdrawal);

            BankWithdrawal result = bankWithdrawalService.markReconciled(withdrawalId, "admin");

            verify(withdrawalRepository).save(withdrawalCaptor.capture());
            assertThat(withdrawalCaptor.getValue().isReconciled()).isTrue();
            assertThat(result.isReconciled()).isTrue();
        }

        @Test
        @DisplayName("should throw when withdrawal not found")
        void shouldThrowWhenWithdrawalNotFound() {
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bankWithdrawalService.markReconciled(withdrawalId, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Withdrawal not found");
        }

        @Test
        @DisplayName("should throw when withdrawal is not COMPLETED")
        void shouldThrowWhenWithdrawalNotCompleted() {
            BankWithdrawal pendingWithdrawal = createTestWithdrawal(WithdrawalStatus.PENDING);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(pendingWithdrawal));

            assertThatThrownBy(() -> bankWithdrawalService.markReconciled(withdrawalId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only completed withdrawals can be reconciled");

            verify(withdrawalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when withdrawal has FAILED status")
        void shouldThrowWhenWithdrawalFailed() {
            BankWithdrawal failedWithdrawal = createTestWithdrawal(WithdrawalStatus.FAILED);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(failedWithdrawal));

            assertThatThrownBy(() -> bankWithdrawalService.markReconciled(withdrawalId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only completed withdrawals can be reconciled");
        }
    }

    // ===================================================================
    // getUnreconciled
    // ===================================================================

    @Nested
    @DisplayName("getUnreconciled")
    class GetUnreconciledTests {

        @Test
        @DisplayName("should return unreconciled withdrawals without cursor")
        void shouldReturnUnreconciledWithoutCursor() {
            List<BankWithdrawal> withdrawals = List.of(createTestWithdrawal(WithdrawalStatus.COMPLETED));
            when(withdrawalRepository.findUnreconciled(any(PageRequest.class))).thenReturn(withdrawals);

            CursorPage<BankWithdrawal> result = bankWithdrawalService.getUnreconciled(null, 20);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasMore()).isFalse();
            verify(withdrawalRepository).findUnreconciled(any(PageRequest.class));
        }

        @Test
        @DisplayName("should return unreconciled withdrawals with cursor")
        void shouldReturnUnreconciledWithCursor() {
            UUID cursorId = UUID.randomUUID();
            List<BankWithdrawal> withdrawals = List.of(createTestWithdrawal(WithdrawalStatus.COMPLETED));
            when(withdrawalRepository.findUnreconciledWithCursor(any(UUID.class), any(PageRequest.class)))
                    .thenReturn(withdrawals);

            CursorPage<BankWithdrawal> result = bankWithdrawalService.getUnreconciled(cursorId.toString(), 20);

            assertThat(result.getItems()).hasSize(1);
            verify(withdrawalRepository).findUnreconciledWithCursor(any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should indicate hasMore for unreconciled query")
        void shouldIndicateHasMore() {
            List<BankWithdrawal> withdrawals = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                BankWithdrawal w = new BankWithdrawal(memberId, amount, bankName, accountNumber);
                w.setId(UUID.randomUUID());
                withdrawals.add(w);
            }
            when(withdrawalRepository.findUnreconciled(any(PageRequest.class))).thenReturn(withdrawals);

            CursorPage<BankWithdrawal> result = bankWithdrawalService.getUnreconciled(null, 2);

            assertThat(result.isHasMore()).isTrue();
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getNextCursor()).isNotNull();
        }

        @Test
        @DisplayName("should treat blank cursor as no cursor")
        void shouldTreatBlankCursorAsNoCursor() {
            when(withdrawalRepository.findUnreconciled(any(PageRequest.class))).thenReturn(List.of());

            bankWithdrawalService.getUnreconciled("  ", 20);

            verify(withdrawalRepository).findUnreconciled(any(PageRequest.class));
        }
    }

    // ===================================================================
    // getWithdrawalsByMember
    // ===================================================================

    @Nested
    @DisplayName("getWithdrawalsByMember")
    class GetWithdrawalsByMemberTests {

        @Test
        @DisplayName("should return withdrawals by member without cursor")
        void shouldReturnWithdrawalsByMemberWithoutCursor() {
            List<BankWithdrawal> withdrawals = List.of(createTestWithdrawal(WithdrawalStatus.COMPLETED));
            when(withdrawalRepository.findByMemberId(any(UUID.class), any(PageRequest.class))).thenReturn(withdrawals);

            CursorPage<BankWithdrawal> result = bankWithdrawalService.getWithdrawalsByMember(memberId, null, 20);

            assertThat(result.getItems()).hasSize(1);
            verify(withdrawalRepository).findByMemberId(any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should return withdrawals by member with cursor")
        void shouldReturnWithdrawalsByMemberWithCursor() {
            UUID cursorId = UUID.randomUUID();
            List<BankWithdrawal> withdrawals = List.of(createTestWithdrawal(WithdrawalStatus.COMPLETED));
            when(withdrawalRepository.findByMemberIdWithCursor(any(UUID.class), any(UUID.class), any(PageRequest.class)))
                    .thenReturn(withdrawals);

            CursorPage<BankWithdrawal> result = bankWithdrawalService.getWithdrawalsByMember(memberId, cursorId.toString(), 20);

            assertThat(result.getItems()).hasSize(1);
            verify(withdrawalRepository).findByMemberIdWithCursor(any(UUID.class), any(UUID.class), any(PageRequest.class));
        }
    }

    // ===================================================================
    // getWithdrawalsByStatus
    // ===================================================================

    @Nested
    @DisplayName("getWithdrawalsByStatus")
    class GetWithdrawalsByStatusTests {

        @Test
        @DisplayName("should return withdrawals by status without cursor")
        void shouldReturnWithdrawalsByStatusWithoutCursor() {
            List<BankWithdrawal> withdrawals = List.of(createTestWithdrawal(WithdrawalStatus.PENDING));
            when(withdrawalRepository.findByStatus(any(WithdrawalStatus.class), any(PageRequest.class)))
                    .thenReturn(withdrawals);

            CursorPage<BankWithdrawal> result = bankWithdrawalService.getWithdrawalsByStatus(
                    WithdrawalStatus.PENDING, null, 20
            );

            assertThat(result.getItems()).hasSize(1);
            verify(withdrawalRepository).findByStatus(any(WithdrawalStatus.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should return withdrawals by status with cursor")
        void shouldReturnWithdrawalsByStatusWithCursor() {
            UUID cursorId = UUID.randomUUID();
            List<BankWithdrawal> withdrawals = List.of(createTestWithdrawal(WithdrawalStatus.PENDING));
            when(withdrawalRepository.findByStatusWithCursor(any(WithdrawalStatus.class), any(UUID.class), any(PageRequest.class)))
                    .thenReturn(withdrawals);

            CursorPage<BankWithdrawal> result = bankWithdrawalService.getWithdrawalsByStatus(
                    WithdrawalStatus.PENDING, cursorId.toString(), 20
            );

            assertThat(result.getItems()).hasSize(1);
            verify(withdrawalRepository).findByStatusWithCursor(any(WithdrawalStatus.class), any(UUID.class), any(PageRequest.class));
        }
    }

    // ===================================================================
    // getWithdrawalById
    // ===================================================================

    @Nested
    @DisplayName("getWithdrawalById")
    class GetWithdrawalByIdTests {

        @Test
        @DisplayName("should return withdrawal when found")
        void shouldReturnWithdrawalWhenFound() {
            BankWithdrawal withdrawal = createTestWithdrawal(WithdrawalStatus.COMPLETED);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(withdrawal));

            BankWithdrawal result = bankWithdrawalService.getWithdrawalById(withdrawalId);

            assertThat(result).isEqualTo(withdrawal);
        }

        @Test
        @DisplayName("should throw when withdrawal not found")
        void shouldThrowWhenWithdrawalNotFound() {
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bankWithdrawalService.getWithdrawalById(withdrawalId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Withdrawal not found")
                    .hasMessageContaining(withdrawalId.toString());
        }
    }
}
