package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.PayoutProcessedEvent;
import com.innercircle.sacco.payout.entity.Payout;
import com.innercircle.sacco.payout.entity.PayoutStatus;
import com.innercircle.sacco.payout.entity.PayoutType;
import com.innercircle.sacco.payout.repository.PayoutRepository;
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
class PayoutServiceImplTest {

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PayoutServiceImpl payoutService;

    @Captor
    private ArgumentCaptor<Payout> payoutCaptor;

    @Captor
    private ArgumentCaptor<PayoutProcessedEvent> eventCaptor;

    private UUID memberId;
    private UUID payoutId;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        payoutId = UUID.randomUUID();
        amount = new BigDecimal("5000.00");
    }

    // -- Helper methods --

    private Payout createTestPayout(PayoutStatus status) {
        Payout payout = new Payout(memberId, amount, PayoutType.MERRY_GO_ROUND);
        payout.setId(payoutId);
        payout.setStatus(status);
        payout.setCreatedAt(Instant.now());
        payout.setUpdatedAt(Instant.now());
        return payout;
    }

    // ===================================================================
    // createPayout
    // ===================================================================

    @Nested
    @DisplayName("createPayout")
    class CreatePayoutTests {

        @Test
        @DisplayName("should create a payout with PENDING status")
        void shouldCreatePayoutWithPendingStatus() {
            Payout expectedPayout = createTestPayout(PayoutStatus.PENDING);
            when(payoutRepository.save(any(Payout.class))).thenReturn(expectedPayout);

            Payout result = payoutService.createPayout(memberId, amount, PayoutType.MERRY_GO_ROUND, "admin");

            verify(payoutRepository).save(payoutCaptor.capture());
            Payout capturedPayout = payoutCaptor.getValue();
            assertThat(capturedPayout.getMemberId()).isEqualTo(memberId);
            assertThat(capturedPayout.getAmount()).isEqualByComparingTo(amount);
            assertThat(capturedPayout.getType()).isEqualTo(PayoutType.MERRY_GO_ROUND);
            assertThat(capturedPayout.getStatus()).isEqualTo(PayoutStatus.PENDING);
            assertThat(capturedPayout.getCreatedBy()).isEqualTo("admin");
            assertThat(result).isEqualTo(expectedPayout);
        }

        @Test
        @DisplayName("should create payout with AD_HOC type")
        void shouldCreatePayoutWithAdHocType() {
            Payout expectedPayout = createTestPayout(PayoutStatus.PENDING);
            expectedPayout.setType(PayoutType.AD_HOC);
            when(payoutRepository.save(any(Payout.class))).thenReturn(expectedPayout);

            Payout result = payoutService.createPayout(memberId, amount, PayoutType.AD_HOC, "admin");

            verify(payoutRepository).save(payoutCaptor.capture());
            assertThat(payoutCaptor.getValue().getType()).isEqualTo(PayoutType.AD_HOC);
            assertThat(result.getType()).isEqualTo(PayoutType.AD_HOC);
        }

        @Test
        @DisplayName("should create payout with DIVIDEND type")
        void shouldCreatePayoutWithDividendType() {
            Payout expectedPayout = createTestPayout(PayoutStatus.PENDING);
            expectedPayout.setType(PayoutType.DIVIDEND);
            when(payoutRepository.save(any(Payout.class))).thenReturn(expectedPayout);

            Payout result = payoutService.createPayout(memberId, amount, PayoutType.DIVIDEND, "admin");

            verify(payoutRepository).save(payoutCaptor.capture());
            assertThat(payoutCaptor.getValue().getType()).isEqualTo(PayoutType.DIVIDEND);
            assertThat(result.getType()).isEqualTo(PayoutType.DIVIDEND);
        }

        @Test
        @DisplayName("should set createdBy to the actor parameter")
        void shouldSetCreatedByToActor() {
            when(payoutRepository.save(any(Payout.class))).thenReturn(createTestPayout(PayoutStatus.PENDING));

            payoutService.createPayout(memberId, amount, PayoutType.MERRY_GO_ROUND, "treasurer");

            verify(payoutRepository).save(payoutCaptor.capture());
            assertThat(payoutCaptor.getValue().getCreatedBy()).isEqualTo("treasurer");
        }
    }

    // ===================================================================
    // approvePayout
    // ===================================================================

    @Nested
    @DisplayName("approvePayout")
    class ApprovePayoutTests {

        @Test
        @DisplayName("should approve a PENDING payout")
        void shouldApprovePendingPayout() {
            Payout pendingPayout = createTestPayout(PayoutStatus.PENDING);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(pendingPayout));

            Payout approvedPayout = createTestPayout(PayoutStatus.APPROVED);
            approvedPayout.setApprovedBy("admin");
            when(payoutRepository.save(any(Payout.class))).thenReturn(approvedPayout);

            Payout result = payoutService.approvePayout(payoutId, "admin");

            verify(payoutRepository).save(payoutCaptor.capture());
            Payout capturedPayout = payoutCaptor.getValue();
            assertThat(capturedPayout.getStatus()).isEqualTo(PayoutStatus.APPROVED);
            assertThat(capturedPayout.getApprovedBy()).isEqualTo("admin");
            assertThat(result.getStatus()).isEqualTo(PayoutStatus.APPROVED);
        }

        @Test
        @DisplayName("should throw when payout not found")
        void shouldThrowWhenPayoutNotFound() {
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> payoutService.approvePayout(payoutId, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Payout not found");
        }

        @Test
        @DisplayName("should throw when payout is already approved")
        void shouldThrowWhenPayoutAlreadyApproved() {
            Payout approvedPayout = createTestPayout(PayoutStatus.APPROVED);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(approvedPayout));

            assertThatThrownBy(() -> payoutService.approvePayout(payoutId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only pending payouts can be approved");

            verify(payoutRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when payout is already processed")
        void shouldThrowWhenPayoutAlreadyProcessed() {
            Payout processedPayout = createTestPayout(PayoutStatus.PROCESSED);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(processedPayout));

            assertThatThrownBy(() -> payoutService.approvePayout(payoutId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only pending payouts can be approved");

            verify(payoutRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when payout is FAILED")
        void shouldThrowWhenPayoutFailed() {
            Payout failedPayout = createTestPayout(PayoutStatus.FAILED);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(failedPayout));

            assertThatThrownBy(() -> payoutService.approvePayout(payoutId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only pending payouts can be approved");
        }
    }

    // ===================================================================
    // processPayout
    // ===================================================================

    @Nested
    @DisplayName("processPayout")
    class ProcessPayoutTests {

        @Test
        @DisplayName("should process an APPROVED payout")
        void shouldProcessApprovedPayout() {
            Payout approvedPayout = createTestPayout(PayoutStatus.APPROVED);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(approvedPayout));

            Payout processedPayout = createTestPayout(PayoutStatus.PROCESSED);
            processedPayout.setProcessedAt(Instant.now());
            processedPayout.setReferenceNumber("PAY-12345678");
            when(payoutRepository.save(any(Payout.class))).thenReturn(processedPayout);

            Payout result = payoutService.processPayout(payoutId, "admin");

            verify(payoutRepository).save(payoutCaptor.capture());
            Payout capturedPayout = payoutCaptor.getValue();
            assertThat(capturedPayout.getStatus()).isEqualTo(PayoutStatus.PROCESSED);
            assertThat(capturedPayout.getProcessedAt()).isNotNull();
            assertThat(capturedPayout.getReferenceNumber()).startsWith("PAY-");
            assertThat(result.getStatus()).isEqualTo(PayoutStatus.PROCESSED);
        }

        @Test
        @DisplayName("should publish PayoutProcessedEvent")
        void shouldPublishPayoutProcessedEvent() {
            Payout approvedPayout = createTestPayout(PayoutStatus.APPROVED);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(approvedPayout));

            Payout processedPayout = createTestPayout(PayoutStatus.PROCESSED);
            processedPayout.setProcessedAt(Instant.now());
            processedPayout.setReferenceNumber("PAY-12345678");
            when(payoutRepository.save(any(Payout.class))).thenReturn(processedPayout);

            payoutService.processPayout(payoutId, "admin");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            PayoutProcessedEvent event = eventCaptor.getValue();
            assertThat(event.payoutId()).isEqualTo(processedPayout.getId());
            assertThat(event.memberId()).isEqualTo(processedPayout.getMemberId());
            assertThat(event.amount()).isEqualByComparingTo(processedPayout.getAmount());
            assertThat(event.payoutType()).isEqualTo(processedPayout.getType().name());
            assertThat(event.actor()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should throw when payout not found")
        void shouldThrowWhenPayoutNotFound() {
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> payoutService.processPayout(payoutId, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Payout not found");
        }

        @Test
        @DisplayName("should throw when payout is PENDING (not approved)")
        void shouldThrowWhenPayoutIsPending() {
            Payout pendingPayout = createTestPayout(PayoutStatus.PENDING);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(pendingPayout));

            assertThatThrownBy(() -> payoutService.processPayout(payoutId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only approved payouts can be processed");

            verify(payoutRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should throw when payout is already processed")
        void shouldThrowWhenPayoutAlreadyProcessed() {
            Payout processedPayout = createTestPayout(PayoutStatus.PROCESSED);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(processedPayout));

            assertThatThrownBy(() -> payoutService.processPayout(payoutId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only approved payouts can be processed");
        }

        @Test
        @DisplayName("should throw when payout is FAILED")
        void shouldThrowWhenPayoutFailed() {
            Payout failedPayout = createTestPayout(PayoutStatus.FAILED);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(failedPayout));

            assertThatThrownBy(() -> payoutService.processPayout(payoutId, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only approved payouts can be processed");
        }

        @Test
        @DisplayName("should generate reference number starting with PAY-")
        void shouldGenerateReferenceNumberWithPrefix() {
            Payout approvedPayout = createTestPayout(PayoutStatus.APPROVED);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(approvedPayout));
            when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

            Payout result = payoutService.processPayout(payoutId, "admin");

            assertThat(result.getReferenceNumber()).startsWith("PAY-");
            assertThat(result.getReferenceNumber()).hasSize(12); // "PAY-" + 8 chars
        }
    }

    // ===================================================================
    // getPayoutHistory
    // ===================================================================

    @Nested
    @DisplayName("getPayoutHistory")
    class GetPayoutHistoryTests {

        @Test
        @DisplayName("should return payout history without cursor")
        void shouldReturnPayoutHistoryWithoutCursor() {
            List<Payout> payouts = List.of(createTestPayout(PayoutStatus.PROCESSED));
            when(payoutRepository.findByMemberId(any(UUID.class), any(PageRequest.class))).thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getPayoutHistory(memberId, null, 20);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasMore()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should return payout history with blank cursor")
        void shouldReturnPayoutHistoryWithBlankCursor() {
            List<Payout> payouts = List.of(createTestPayout(PayoutStatus.PROCESSED));
            when(payoutRepository.findByMemberId(any(UUID.class), any(PageRequest.class))).thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getPayoutHistory(memberId, "  ", 20);

            assertThat(result.getItems()).hasSize(1);
            verify(payoutRepository).findByMemberId(any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should return payout history with cursor")
        void shouldReturnPayoutHistoryWithCursor() {
            UUID cursorId = UUID.randomUUID();
            List<Payout> payouts = List.of(createTestPayout(PayoutStatus.PROCESSED));
            when(payoutRepository.findByMemberIdWithCursor(any(UUID.class), any(UUID.class), any(PageRequest.class)))
                    .thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getPayoutHistory(memberId, cursorId.toString(), 20);

            assertThat(result.getItems()).hasSize(1);
            verify(payoutRepository).findByMemberIdWithCursor(any(UUID.class), any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should indicate hasMore when results exceed limit")
        void shouldIndicateHasMore() {
            List<Payout> payouts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Payout p = new Payout(memberId, amount, PayoutType.MERRY_GO_ROUND);
                p.setId(UUID.randomUUID());
                payouts.add(p);
            }
            when(payoutRepository.findByMemberId(any(UUID.class), any(PageRequest.class))).thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getPayoutHistory(memberId, null, 2);

            assertThat(result.getItems()).hasSize(2);
            assertThat(result.isHasMore()).isTrue();
            assertThat(result.getNextCursor()).isNotNull();
        }

        @Test
        @DisplayName("should return empty page when no results")
        void shouldReturnEmptyPage() {
            when(payoutRepository.findByMemberId(any(UUID.class), any(PageRequest.class))).thenReturn(List.of());

            CursorPage<Payout> result = payoutService.getPayoutHistory(memberId, null, 20);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasMore()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }
    }

    // ===================================================================
    // getPayoutsByStatus
    // ===================================================================

    @Nested
    @DisplayName("getPayoutsByStatus")
    class GetPayoutsByStatusTests {

        @Test
        @DisplayName("should return payouts by status without cursor")
        void shouldReturnPayoutsByStatusWithoutCursor() {
            List<Payout> payouts = List.of(createTestPayout(PayoutStatus.PENDING));
            when(payoutRepository.findByStatus(any(PayoutStatus.class), any(PageRequest.class))).thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getPayoutsByStatus(PayoutStatus.PENDING, null, 20);

            assertThat(result.getItems()).hasSize(1);
            verify(payoutRepository).findByStatus(any(PayoutStatus.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should return payouts by status with cursor")
        void shouldReturnPayoutsByStatusWithCursor() {
            UUID cursorId = UUID.randomUUID();
            List<Payout> payouts = List.of(createTestPayout(PayoutStatus.PENDING));
            when(payoutRepository.findByStatusWithCursor(any(PayoutStatus.class), any(UUID.class), any(PageRequest.class)))
                    .thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getPayoutsByStatus(PayoutStatus.PENDING, cursorId.toString(), 20);

            assertThat(result.getItems()).hasSize(1);
            verify(payoutRepository).findByStatusWithCursor(any(PayoutStatus.class), any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should indicate hasMore for status query")
        void shouldIndicateHasMoreForStatusQuery() {
            List<Payout> payouts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Payout p = new Payout(memberId, amount, PayoutType.MERRY_GO_ROUND);
                p.setId(UUID.randomUUID());
                payouts.add(p);
            }
            when(payoutRepository.findByStatus(any(PayoutStatus.class), any(PageRequest.class))).thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getPayoutsByStatus(PayoutStatus.PENDING, null, 2);

            assertThat(result.isHasMore()).isTrue();
            assertThat(result.getItems()).hasSize(2);
        }
    }

    // ===================================================================
    // getAllPayouts
    // ===================================================================

    @Nested
    @DisplayName("getAllPayouts")
    class GetAllPayoutsTests {

        @Test
        @DisplayName("should return all payouts without cursor")
        void shouldReturnAllPayoutsWithoutCursor() {
            List<Payout> payouts = List.of(createTestPayout(PayoutStatus.PENDING));
            when(payoutRepository.findAllPaged(any(PageRequest.class))).thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getAllPayouts(null, 20);

            assertThat(result.getItems()).hasSize(1);
            verify(payoutRepository).findAllPaged(any(PageRequest.class));
        }

        @Test
        @DisplayName("should return all payouts with cursor")
        void shouldReturnAllPayoutsWithCursor() {
            UUID cursorId = UUID.randomUUID();
            List<Payout> payouts = List.of(createTestPayout(PayoutStatus.PENDING));
            when(payoutRepository.findAllWithCursor(any(UUID.class), any(PageRequest.class))).thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getAllPayouts(cursorId.toString(), 20);

            assertThat(result.getItems()).hasSize(1);
            verify(payoutRepository).findAllWithCursor(any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should return all payouts with blank cursor treated as no cursor")
        void shouldReturnAllPayoutsWithBlankCursor() {
            List<Payout> payouts = List.of(createTestPayout(PayoutStatus.PENDING));
            when(payoutRepository.findAllPaged(any(PageRequest.class))).thenReturn(payouts);

            CursorPage<Payout> result = payoutService.getAllPayouts("", 20);

            verify(payoutRepository).findAllPaged(any(PageRequest.class));
            assertThat(result.getItems()).hasSize(1);
        }
    }

    // ===================================================================
    // getPayoutById
    // ===================================================================

    @Nested
    @DisplayName("getPayoutById")
    class GetPayoutByIdTests {

        @Test
        @DisplayName("should return payout when found")
        void shouldReturnPayoutWhenFound() {
            Payout payout = createTestPayout(PayoutStatus.PROCESSED);
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.of(payout));

            Payout result = payoutService.getPayoutById(payoutId);

            assertThat(result).isEqualTo(payout);
            assertThat(result.getId()).isEqualTo(payoutId);
        }

        @Test
        @DisplayName("should throw when payout not found")
        void shouldThrowWhenPayoutNotFound() {
            when(payoutRepository.findById(payoutId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> payoutService.getPayoutById(payoutId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Payout not found")
                    .hasMessageContaining(payoutId.toString());
        }
    }
}
