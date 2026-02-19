package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.AuditableEvent;
import com.innercircle.sacco.common.exception.MakerCheckerViolationException;
import com.innercircle.sacco.payout.entity.ShareWithdrawal;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalStatus;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalType;
import com.innercircle.sacco.payout.event.ShareWithdrawalProcessedEvent;
import com.innercircle.sacco.payout.event.ShareWithdrawalRequestedEvent;
import com.innercircle.sacco.payout.repository.ShareWithdrawalRepository;
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
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareWithdrawalServiceImplTest {

    @Mock
    private ShareWithdrawalRepository withdrawalRepository;

    @Mock
    private EventOutboxWriter outboxWriter;

    @InjectMocks
    private ShareWithdrawalServiceImpl shareWithdrawalService;

    @Captor
    private ArgumentCaptor<ShareWithdrawal> withdrawalCaptor;

    @Captor
    private ArgumentCaptor<AuditableEvent> eventCaptor;

    private UUID memberId;
    private UUID withdrawalId;
    private BigDecimal amount;
    private BigDecimal currentShareBalance;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        withdrawalId = UUID.randomUUID();
        amount = new BigDecimal("5000.00");
        currentShareBalance = new BigDecimal("10000.00");
    }

    private ShareWithdrawal createTestWithdrawal(ShareWithdrawalStatus status, ShareWithdrawalType type) {
        ShareWithdrawal withdrawal = new ShareWithdrawal(memberId, amount, type, currentShareBalance);
        withdrawal.setId(withdrawalId);
        withdrawal.setStatus(status);
        withdrawal.setCreatedAt(Instant.now());
        withdrawal.setUpdatedAt(Instant.now());
        return withdrawal;
    }

    // ===================================================================
    // requestWithdrawal
    // ===================================================================

    @Nested
    @DisplayName("requestWithdrawal")
    class RequestWithdrawalTests {

        @Test
        @DisplayName("should create a PARTIAL withdrawal request")
        void shouldCreatePartialWithdrawalRequest() {
            ShareWithdrawal expected = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenReturn(expected);

            ShareWithdrawal result = shareWithdrawalService.requestWithdrawal(
                    memberId, amount, ShareWithdrawalType.PARTIAL, currentShareBalance, "admin"
            );

            verify(withdrawalRepository).save(withdrawalCaptor.capture());
            ShareWithdrawal captured = withdrawalCaptor.getValue();
            assertThat(captured.getMemberId()).isEqualTo(memberId);
            assertThat(captured.getAmount()).isEqualByComparingTo(amount);
            assertThat(captured.getWithdrawalType()).isEqualTo(ShareWithdrawalType.PARTIAL);
            assertThat(captured.getCurrentShareBalance()).isEqualByComparingTo(currentShareBalance);
            assertThat(captured.getStatus()).isEqualTo(ShareWithdrawalStatus.PENDING);
            assertThat(captured.getCreatedBy()).isEqualTo("admin");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should create a FULL withdrawal request when amount equals balance")
        void shouldCreateFullWithdrawalRequest() {
            BigDecimal fullAmount = new BigDecimal("10000.00");
            ShareWithdrawal expected = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.FULL);
            expected.setAmount(fullAmount);
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenReturn(expected);

            ShareWithdrawal result = shareWithdrawalService.requestWithdrawal(
                    memberId, fullAmount, ShareWithdrawalType.FULL, currentShareBalance, "admin"
            );

            verify(withdrawalRepository).save(any(ShareWithdrawal.class));
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should publish ShareWithdrawalRequestedEvent")
        void shouldPublishShareWithdrawalRequestedEvent() {
            ShareWithdrawal saved = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenReturn(saved);

            shareWithdrawalService.requestWithdrawal(
                    memberId, amount, ShareWithdrawalType.PARTIAL, currentShareBalance, "admin"
            );

            verify(outboxWriter).write(eventCaptor.capture(), eq("ShareWithdrawal"), any(UUID.class));
            AuditableEvent event = eventCaptor.getValue();
            assertThat(event).isInstanceOf(ShareWithdrawalRequestedEvent.class);
            ShareWithdrawalRequestedEvent requestedEvent = (ShareWithdrawalRequestedEvent) event;
            assertThat(requestedEvent.withdrawalId()).isEqualTo(saved.getId());
            assertThat(requestedEvent.memberId()).isEqualTo(saved.getMemberId());
            assertThat(requestedEvent.amount()).isEqualByComparingTo(saved.getAmount());
            assertThat(requestedEvent.withdrawalType()).isEqualTo("PARTIAL");
            assertThat(requestedEvent.actor()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should throw when withdrawal amount exceeds current share balance")
        void shouldThrowWhenAmountExceedsBalance() {
            BigDecimal excessiveAmount = new BigDecimal("15000.00");

            assertThatThrownBy(() -> shareWithdrawalService.requestWithdrawal(
                    memberId, excessiveAmount, ShareWithdrawalType.PARTIAL, currentShareBalance, "admin"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Withdrawal amount exceeds current share balance");

            verify(withdrawalRepository, never()).save(any());
            verify(outboxWriter, never()).write(any(), any(), any());
        }

        @Test
        @DisplayName("should throw when full withdrawal amount does not equal current balance")
        void shouldThrowWhenFullWithdrawalNotEqualToBalance() {
            BigDecimal partialAmount = new BigDecimal("5000.00");

            assertThatThrownBy(() -> shareWithdrawalService.requestWithdrawal(
                    memberId, partialAmount, ShareWithdrawalType.FULL, currentShareBalance, "admin"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Full withdrawal must equal current share balance");

            verify(withdrawalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow partial withdrawal exactly equal to balance")
        void shouldAllowPartialWithdrawalEqualToBalance() {
            BigDecimal fullAmount = currentShareBalance;
            ShareWithdrawal expected = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL);
            expected.setAmount(fullAmount);
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenReturn(expected);

            ShareWithdrawal result = shareWithdrawalService.requestWithdrawal(
                    memberId, fullAmount, ShareWithdrawalType.PARTIAL, currentShareBalance, "admin"
            );

            assertThat(result).isNotNull();
            verify(withdrawalRepository).save(any(ShareWithdrawal.class));
        }
    }

    // ===================================================================
    // approveWithdrawal
    // ===================================================================

    @Nested
    @DisplayName("approveWithdrawal")
    class ApproveWithdrawalTests {

        @Test
        @DisplayName("should approve a PENDING withdrawal by a different user")
        void shouldApprovePendingWithdrawal() {
            ShareWithdrawal pendingWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL);
            pendingWithdrawal.setCreatedBy("treasurer");
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(pendingWithdrawal));

            ShareWithdrawal approvedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.APPROVED, ShareWithdrawalType.PARTIAL);
            approvedWithdrawal.setApprovedBy("admin");
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenReturn(approvedWithdrawal);

            ShareWithdrawal result = shareWithdrawalService.approveWithdrawal(withdrawalId, "admin", null, false);

            verify(withdrawalRepository).save(withdrawalCaptor.capture());
            ShareWithdrawal captured = withdrawalCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(ShareWithdrawalStatus.APPROVED);
            assertThat(captured.getApprovedBy()).isEqualTo("admin");
            assertThat(result.getStatus()).isEqualTo(ShareWithdrawalStatus.APPROVED);
        }

        @Test
        @DisplayName("should throw MakerCheckerViolationException when creator tries to approve")
        void shouldThrowWhenCreatorApprovesOwnWithdrawal() {
            ShareWithdrawal pendingWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL);
            pendingWithdrawal.setCreatedBy("treasurer");
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(pendingWithdrawal));

            assertThatThrownBy(() -> shareWithdrawalService.approveWithdrawal(withdrawalId, "treasurer", null, false))
                    .isInstanceOf(MakerCheckerViolationException.class);

            verify(withdrawalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow ADMIN to override and approve their own withdrawal with reason")
        void shouldAllowAdminOverrideWithReason() {
            ShareWithdrawal pendingWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL);
            pendingWithdrawal.setCreatedBy("admin");
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(pendingWithdrawal));

            ShareWithdrawal approvedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.APPROVED, ShareWithdrawalType.PARTIAL);
            approvedWithdrawal.setApprovedBy("admin");
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenReturn(approvedWithdrawal);

            ShareWithdrawal result = shareWithdrawalService.approveWithdrawal(withdrawalId, "admin", "Emergency approval", true);

            assertThat(result.getStatus()).isEqualTo(ShareWithdrawalStatus.APPROVED);
        }

        @Test
        @DisplayName("should throw when ADMIN override attempted without reason")
        void shouldThrowWhenAdminOverrideWithoutReason() {
            ShareWithdrawal pendingWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL);
            pendingWithdrawal.setCreatedBy("admin");
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(pendingWithdrawal));

            assertThatThrownBy(() -> shareWithdrawalService.approveWithdrawal(withdrawalId, "admin", null, true))
                    .isInstanceOf(MakerCheckerViolationException.class);

            verify(withdrawalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when withdrawal not found")
        void shouldThrowWhenWithdrawalNotFound() {
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shareWithdrawalService.approveWithdrawal(withdrawalId, "admin", null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Withdrawal not found");
        }

        @Test
        @DisplayName("should throw when withdrawal is not PENDING")
        void shouldThrowWhenWithdrawalNotPending() {
            ShareWithdrawal approvedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.APPROVED, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(approvedWithdrawal));

            assertThatThrownBy(() -> shareWithdrawalService.approveWithdrawal(withdrawalId, "admin", null, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only pending withdrawals can be approved");

            verify(withdrawalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when withdrawal is PROCESSED")
        void shouldThrowWhenWithdrawalProcessed() {
            ShareWithdrawal processedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.PROCESSED, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(processedWithdrawal));

            assertThatThrownBy(() -> shareWithdrawalService.approveWithdrawal(withdrawalId, "admin", null, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only pending withdrawals can be approved");
        }
    }

    // ===================================================================
    // processWithdrawal
    // ===================================================================

    @Nested
    @DisplayName("processWithdrawal")
    class ProcessWithdrawalTests {

        @Test
        @DisplayName("should process an APPROVED withdrawal")
        void shouldProcessApprovedWithdrawal() {
            ShareWithdrawal approvedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.APPROVED, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(approvedWithdrawal));

            ShareWithdrawal processedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.PROCESSED, ShareWithdrawalType.PARTIAL);
            processedWithdrawal.setNewShareBalance(new BigDecimal("5000.00"));
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenReturn(processedWithdrawal);

            ShareWithdrawal result = shareWithdrawalService.processWithdrawal(withdrawalId, "admin");

            verify(withdrawalRepository).save(withdrawalCaptor.capture());
            ShareWithdrawal captured = withdrawalCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(ShareWithdrawalStatus.PROCESSED);
            assertThat(captured.getNewShareBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(result.getStatus()).isEqualTo(ShareWithdrawalStatus.PROCESSED);
        }

        @Test
        @DisplayName("should calculate new share balance correctly")
        void shouldCalculateNewShareBalanceCorrectly() {
            ShareWithdrawal approvedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.APPROVED, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(approvedWithdrawal));
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenAnswer(inv -> inv.getArgument(0));

            shareWithdrawalService.processWithdrawal(withdrawalId, "admin");

            verify(withdrawalRepository).save(withdrawalCaptor.capture());
            ShareWithdrawal captured = withdrawalCaptor.getValue();
            BigDecimal expectedNewBalance = currentShareBalance.subtract(amount);
            assertThat(captured.getNewShareBalance()).isEqualByComparingTo(expectedNewBalance);
        }

        @Test
        @DisplayName("should publish ShareWithdrawalProcessedEvent")
        void shouldPublishShareWithdrawalProcessedEvent() {
            ShareWithdrawal approvedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.APPROVED, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(approvedWithdrawal));

            ShareWithdrawal processedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.PROCESSED, ShareWithdrawalType.PARTIAL);
            processedWithdrawal.setNewShareBalance(new BigDecimal("5000.00"));
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenReturn(processedWithdrawal);

            shareWithdrawalService.processWithdrawal(withdrawalId, "admin");

            verify(outboxWriter).write(eventCaptor.capture(), eq("ShareWithdrawal"), any(UUID.class));
            AuditableEvent event = eventCaptor.getValue();
            assertThat(event).isInstanceOf(ShareWithdrawalProcessedEvent.class);
            ShareWithdrawalProcessedEvent processedEvent = (ShareWithdrawalProcessedEvent) event;
            assertThat(processedEvent.withdrawalId()).isEqualTo(processedWithdrawal.getId());
            assertThat(processedEvent.memberId()).isEqualTo(processedWithdrawal.getMemberId());
            assertThat(processedEvent.amount()).isEqualByComparingTo(processedWithdrawal.getAmount());
            assertThat(processedEvent.actor()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should throw when withdrawal not found")
        void shouldThrowWhenWithdrawalNotFound() {
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shareWithdrawalService.processWithdrawal(withdrawalId, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Withdrawal not found");
        }

        @Test
        @DisplayName("should throw when withdrawal is PENDING (not approved)")
        void shouldThrowWhenWithdrawalIsPending() {
            ShareWithdrawal pendingWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(pendingWithdrawal));

            assertThatThrownBy(() -> shareWithdrawalService.processWithdrawal(withdrawalId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only approved withdrawals can be processed");

            verify(withdrawalRepository, never()).save(any());
            verify(outboxWriter, never()).write(any(), any(), any());
        }

        @Test
        @DisplayName("should throw when withdrawal is already PROCESSED")
        void shouldThrowWhenAlreadyProcessed() {
            ShareWithdrawal processedWithdrawal = createTestWithdrawal(ShareWithdrawalStatus.PROCESSED, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(processedWithdrawal));

            assertThatThrownBy(() -> shareWithdrawalService.processWithdrawal(withdrawalId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only approved withdrawals can be processed");
        }

        @Test
        @DisplayName("should result in zero balance for FULL withdrawal")
        void shouldResultInZeroBalanceForFullWithdrawal() {
            BigDecimal fullAmount = currentShareBalance;
            ShareWithdrawal fullWithdrawal = new ShareWithdrawal(memberId, fullAmount, ShareWithdrawalType.FULL, currentShareBalance);
            fullWithdrawal.setId(withdrawalId);
            fullWithdrawal.setStatus(ShareWithdrawalStatus.APPROVED);
            fullWithdrawal.setCreatedAt(Instant.now());
            fullWithdrawal.setUpdatedAt(Instant.now());
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(fullWithdrawal));
            when(withdrawalRepository.save(any(ShareWithdrawal.class))).thenAnswer(inv -> inv.getArgument(0));

            shareWithdrawalService.processWithdrawal(withdrawalId, "admin");

            verify(withdrawalRepository).save(withdrawalCaptor.capture());
            assertThat(withdrawalCaptor.getValue().getNewShareBalance()).isEqualByComparingTo(BigDecimal.ZERO);
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
            List<ShareWithdrawal> withdrawals = List.of(
                    createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL)
            );
            when(withdrawalRepository.findByMemberId(any(UUID.class), any(PageRequest.class)))
                    .thenReturn(withdrawals);

            CursorPage<ShareWithdrawal> result = shareWithdrawalService.getWithdrawalsByMember(memberId, null, 20);

            assertThat(result.getItems()).hasSize(1);
            verify(withdrawalRepository).findByMemberId(any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should return withdrawals by member with cursor")
        void shouldReturnWithdrawalsByMemberWithCursor() {
            UUID cursorId = UUID.randomUUID();
            List<ShareWithdrawal> withdrawals = List.of(
                    createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL)
            );
            when(withdrawalRepository.findByMemberIdWithCursor(any(UUID.class), any(UUID.class), any(PageRequest.class)))
                    .thenReturn(withdrawals);

            CursorPage<ShareWithdrawal> result = shareWithdrawalService.getWithdrawalsByMember(
                    memberId, cursorId.toString(), 20
            );

            assertThat(result.getItems()).hasSize(1);
            verify(withdrawalRepository).findByMemberIdWithCursor(any(UUID.class), any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should indicate hasMore when results exceed limit")
        void shouldIndicateHasMore() {
            List<ShareWithdrawal> withdrawals = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                ShareWithdrawal w = new ShareWithdrawal(memberId, amount, ShareWithdrawalType.PARTIAL, currentShareBalance);
                w.setId(UUID.randomUUID());
                withdrawals.add(w);
            }
            when(withdrawalRepository.findByMemberId(any(UUID.class), any(PageRequest.class)))
                    .thenReturn(withdrawals);

            CursorPage<ShareWithdrawal> result = shareWithdrawalService.getWithdrawalsByMember(memberId, null, 2);

            assertThat(result.isHasMore()).isTrue();
            assertThat(result.getItems()).hasSize(2);
        }

        @Test
        @DisplayName("should treat blank cursor as no cursor")
        void shouldTreatBlankCursorAsNoCursor() {
            when(withdrawalRepository.findByMemberId(any(UUID.class), any(PageRequest.class)))
                    .thenReturn(List.of());

            shareWithdrawalService.getWithdrawalsByMember(memberId, "", 20);

            verify(withdrawalRepository).findByMemberId(any(UUID.class), any(PageRequest.class));
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
            List<ShareWithdrawal> withdrawals = List.of(
                    createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL)
            );
            when(withdrawalRepository.findByStatus(any(ShareWithdrawalStatus.class), any(PageRequest.class)))
                    .thenReturn(withdrawals);

            CursorPage<ShareWithdrawal> result = shareWithdrawalService.getWithdrawalsByStatus(
                    ShareWithdrawalStatus.PENDING, null, 20
            );

            assertThat(result.getItems()).hasSize(1);
            verify(withdrawalRepository).findByStatus(any(ShareWithdrawalStatus.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should return withdrawals by status with cursor")
        void shouldReturnWithdrawalsByStatusWithCursor() {
            UUID cursorId = UUID.randomUUID();
            List<ShareWithdrawal> withdrawals = List.of(
                    createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL)
            );
            when(withdrawalRepository.findByStatusWithCursor(any(ShareWithdrawalStatus.class), any(UUID.class), any(PageRequest.class)))
                    .thenReturn(withdrawals);

            CursorPage<ShareWithdrawal> result = shareWithdrawalService.getWithdrawalsByStatus(
                    ShareWithdrawalStatus.PENDING, cursorId.toString(), 20
            );

            assertThat(result.getItems()).hasSize(1);
            verify(withdrawalRepository).findByStatusWithCursor(any(ShareWithdrawalStatus.class), any(UUID.class), any(PageRequest.class));
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
            ShareWithdrawal withdrawal = createTestWithdrawal(ShareWithdrawalStatus.PENDING, ShareWithdrawalType.PARTIAL);
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(withdrawal));

            ShareWithdrawal result = shareWithdrawalService.getWithdrawalById(withdrawalId);

            assertThat(result).isEqualTo(withdrawal);
        }

        @Test
        @DisplayName("should throw when withdrawal not found")
        void shouldThrowWhenWithdrawalNotFound() {
            when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shareWithdrawalService.getWithdrawalById(withdrawalId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Withdrawal not found")
                    .hasMessageContaining(withdrawalId.toString());
        }
    }
}
