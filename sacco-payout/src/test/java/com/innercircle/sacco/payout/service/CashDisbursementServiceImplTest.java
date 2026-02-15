package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.entity.CashDisbursement;
import com.innercircle.sacco.payout.event.CashDisbursementRecordedEvent;
import com.innercircle.sacco.payout.repository.CashDisbursementRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashDisbursementServiceImplTest {

    @Mock
    private CashDisbursementRepository disbursementRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CashDisbursementServiceImpl cashDisbursementService;

    @Captor
    private ArgumentCaptor<CashDisbursement> disbursementCaptor;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private UUID memberId;
    private UUID disbursementId;
    private BigDecimal amount;
    private String receivedBy;
    private String disbursedBy;
    private String receiptNumber;
    private LocalDate disbursementDate;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        disbursementId = UUID.randomUUID();
        amount = new BigDecimal("15000.00");
        receivedBy = "John Doe";
        disbursedBy = "Jane Smith";
        receiptNumber = "RCP-001";
        disbursementDate = LocalDate.now();
    }

    private CashDisbursement createTestDisbursement() {
        CashDisbursement disbursement = new CashDisbursement(
                memberId, amount, receivedBy, disbursedBy, receiptNumber, disbursementDate
        );
        disbursement.setId(disbursementId);
        disbursement.setCreatedAt(Instant.now());
        disbursement.setUpdatedAt(Instant.now());
        return disbursement;
    }

    // ===================================================================
    // recordDisbursement
    // ===================================================================

    @Nested
    @DisplayName("recordDisbursement")
    class RecordDisbursementTests {

        @Test
        @DisplayName("should record a new cash disbursement")
        void shouldRecordNewCashDisbursement() {
            when(disbursementRepository.findByReceiptNumber(receiptNumber)).thenReturn(Optional.empty());
            CashDisbursement expected = createTestDisbursement();
            when(disbursementRepository.save(any(CashDisbursement.class))).thenReturn(expected);

            CashDisbursement result = cashDisbursementService.recordDisbursement(
                    memberId, amount, receivedBy, disbursedBy, receiptNumber, disbursementDate, "admin"
            );

            verify(disbursementRepository).save(disbursementCaptor.capture());
            CashDisbursement captured = disbursementCaptor.getValue();
            assertThat(captured.getMemberId()).isEqualTo(memberId);
            assertThat(captured.getAmount()).isEqualByComparingTo(amount);
            assertThat(captured.getReceivedBy()).isEqualTo(receivedBy);
            assertThat(captured.getDisbursedBy()).isEqualTo(disbursedBy);
            assertThat(captured.getReceiptNumber()).isEqualTo(receiptNumber);
            assertThat(captured.getDisbursementDate()).isEqualTo(disbursementDate);
            assertThat(captured.getCreatedBy()).isEqualTo("admin");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should publish CashDisbursementRecordedEvent")
        void shouldPublishCashDisbursementRecordedEvent() {
            when(disbursementRepository.findByReceiptNumber(receiptNumber)).thenReturn(Optional.empty());
            CashDisbursement saved = createTestDisbursement();
            when(disbursementRepository.save(any(CashDisbursement.class))).thenReturn(saved);

            cashDisbursementService.recordDisbursement(
                    memberId, amount, receivedBy, disbursedBy, receiptNumber, disbursementDate, "admin"
            );

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            Object event = eventCaptor.getValue();
            assertThat(event).isInstanceOf(CashDisbursementRecordedEvent.class);
            CashDisbursementRecordedEvent recordedEvent = (CashDisbursementRecordedEvent) event;
            assertThat(recordedEvent.disbursementId()).isEqualTo(saved.getId());
            assertThat(recordedEvent.memberId()).isEqualTo(saved.getMemberId());
            assertThat(recordedEvent.amount()).isEqualByComparingTo(saved.getAmount());
            assertThat(recordedEvent.receiptNumber()).isEqualTo(receiptNumber);
            assertThat(recordedEvent.actor()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should throw when receipt number already exists")
        void shouldThrowWhenReceiptNumberAlreadyExists() {
            CashDisbursement existingDisbursement = createTestDisbursement();
            when(disbursementRepository.findByReceiptNumber(receiptNumber))
                    .thenReturn(Optional.of(existingDisbursement));

            assertThatThrownBy(() -> cashDisbursementService.recordDisbursement(
                    memberId, amount, receivedBy, disbursedBy, receiptNumber, disbursementDate, "admin"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Receipt number already exists")
                    .hasMessageContaining(receiptNumber);

            verify(disbursementRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    // ===================================================================
    // signoff
    // ===================================================================

    @Nested
    @DisplayName("signoff")
    class SignoffTests {

        @Test
        @DisplayName("should sign off a disbursement")
        void shouldSignOffDisbursement() {
            CashDisbursement disbursement = createTestDisbursement();
            disbursement.setSignoffBy(null);
            when(disbursementRepository.findById(disbursementId)).thenReturn(Optional.of(disbursement));

            CashDisbursement signedOff = createTestDisbursement();
            signedOff.setSignoffBy("supervisor");
            when(disbursementRepository.save(any(CashDisbursement.class))).thenReturn(signedOff);

            CashDisbursement result = cashDisbursementService.signoff(disbursementId, "supervisor");

            verify(disbursementRepository).save(disbursementCaptor.capture());
            assertThat(disbursementCaptor.getValue().getSignoffBy()).isEqualTo("supervisor");
            assertThat(result.getSignoffBy()).isEqualTo("supervisor");
        }

        @Test
        @DisplayName("should throw when disbursement not found")
        void shouldThrowWhenDisbursementNotFound() {
            when(disbursementRepository.findById(disbursementId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cashDisbursementService.signoff(disbursementId, "supervisor"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Disbursement not found");
        }

        @Test
        @DisplayName("should throw when disbursement already signed off")
        void shouldThrowWhenAlreadySignedOff() {
            CashDisbursement disbursement = createTestDisbursement();
            disbursement.setSignoffBy("another-supervisor");
            when(disbursementRepository.findById(disbursementId)).thenReturn(Optional.of(disbursement));

            assertThatThrownBy(() -> cashDisbursementService.signoff(disbursementId, "supervisor"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Disbursement already signed off");

            verify(disbursementRepository, never()).save(any());
        }
    }

    // ===================================================================
    // getDisbursementHistory
    // ===================================================================

    @Nested
    @DisplayName("getDisbursementHistory")
    class GetDisbursementHistoryTests {

        @Test
        @DisplayName("should return disbursement history without cursor")
        void shouldReturnDisbursementHistoryWithoutCursor() {
            List<CashDisbursement> disbursements = List.of(createTestDisbursement());
            when(disbursementRepository.findByMemberId(any(UUID.class), any(PageRequest.class)))
                    .thenReturn(disbursements);

            CursorPage<CashDisbursement> result = cashDisbursementService.getDisbursementHistory(
                    memberId, null, 20
            );

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasMore()).isFalse();
            verify(disbursementRepository).findByMemberId(any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should return disbursement history with cursor")
        void shouldReturnDisbursementHistoryWithCursor() {
            UUID cursorId = UUID.randomUUID();
            List<CashDisbursement> disbursements = List.of(createTestDisbursement());
            when(disbursementRepository.findByMemberIdWithCursor(any(UUID.class), any(UUID.class), any(PageRequest.class)))
                    .thenReturn(disbursements);

            CursorPage<CashDisbursement> result = cashDisbursementService.getDisbursementHistory(
                    memberId, cursorId.toString(), 20
            );

            assertThat(result.getItems()).hasSize(1);
            verify(disbursementRepository).findByMemberIdWithCursor(any(UUID.class), any(UUID.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("should indicate hasMore when results exceed limit")
        void shouldIndicateHasMore() {
            List<CashDisbursement> disbursements = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                CashDisbursement d = new CashDisbursement(
                        memberId, amount, receivedBy, disbursedBy, "RCP-" + i, disbursementDate
                );
                d.setId(UUID.randomUUID());
                disbursements.add(d);
            }
            when(disbursementRepository.findByMemberId(any(UUID.class), any(PageRequest.class)))
                    .thenReturn(disbursements);

            CursorPage<CashDisbursement> result = cashDisbursementService.getDisbursementHistory(
                    memberId, null, 2
            );

            assertThat(result.isHasMore()).isTrue();
            assertThat(result.getItems()).hasSize(2);
        }

        @Test
        @DisplayName("should return empty page when no results")
        void shouldReturnEmptyPage() {
            when(disbursementRepository.findByMemberId(any(UUID.class), any(PageRequest.class)))
                    .thenReturn(List.of());

            CursorPage<CashDisbursement> result = cashDisbursementService.getDisbursementHistory(
                    memberId, null, 20
            );

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasMore()).isFalse();
        }

        @Test
        @DisplayName("should treat blank cursor as no cursor")
        void shouldTreatBlankCursorAsNoCursor() {
            when(disbursementRepository.findByMemberId(any(UUID.class), any(PageRequest.class)))
                    .thenReturn(List.of());

            cashDisbursementService.getDisbursementHistory(memberId, "", 20);

            verify(disbursementRepository).findByMemberId(any(UUID.class), any(PageRequest.class));
        }
    }

    // ===================================================================
    // getDisbursementsByDateRange
    // ===================================================================

    @Nested
    @DisplayName("getDisbursementsByDateRange")
    class GetDisbursementsByDateRangeTests {

        @Test
        @DisplayName("should return disbursements by date range without cursor")
        void shouldReturnDisbursementsByDateRangeWithoutCursor() {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 31);
            List<CashDisbursement> disbursements = List.of(createTestDisbursement());
            when(disbursementRepository.findByDisbursementDateBetween(
                    any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)
            )).thenReturn(disbursements);

            CursorPage<CashDisbursement> result = cashDisbursementService.getDisbursementsByDateRange(
                    startDate, endDate, null, 20
            );

            assertThat(result.getItems()).hasSize(1);
            verify(disbursementRepository).findByDisbursementDateBetween(
                    any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)
            );
        }

        @Test
        @DisplayName("should return disbursements by date range with cursor")
        void shouldReturnDisbursementsByDateRangeWithCursor() {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 31);
            UUID cursorId = UUID.randomUUID();
            List<CashDisbursement> disbursements = List.of(createTestDisbursement());
            when(disbursementRepository.findByDisbursementDateBetweenWithCursor(
                    any(LocalDate.class), any(LocalDate.class), any(UUID.class), any(PageRequest.class)
            )).thenReturn(disbursements);

            CursorPage<CashDisbursement> result = cashDisbursementService.getDisbursementsByDateRange(
                    startDate, endDate, cursorId.toString(), 20
            );

            assertThat(result.getItems()).hasSize(1);
            verify(disbursementRepository).findByDisbursementDateBetweenWithCursor(
                    any(LocalDate.class), any(LocalDate.class), any(UUID.class), any(PageRequest.class)
            );
        }

        @Test
        @DisplayName("should treat blank cursor as no cursor for date range")
        void shouldTreatBlankCursorAsNoCursorForDateRange() {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 31);
            when(disbursementRepository.findByDisbursementDateBetween(
                    any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)
            )).thenReturn(List.of());

            cashDisbursementService.getDisbursementsByDateRange(startDate, endDate, "  ", 20);

            verify(disbursementRepository).findByDisbursementDateBetween(
                    any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)
            );
        }
    }

    // ===================================================================
    // getDisbursementById
    // ===================================================================

    @Nested
    @DisplayName("getDisbursementById")
    class GetDisbursementByIdTests {

        @Test
        @DisplayName("should return disbursement when found")
        void shouldReturnDisbursementWhenFound() {
            CashDisbursement disbursement = createTestDisbursement();
            when(disbursementRepository.findById(disbursementId)).thenReturn(Optional.of(disbursement));

            CashDisbursement result = cashDisbursementService.getDisbursementById(disbursementId);

            assertThat(result).isEqualTo(disbursement);
        }

        @Test
        @DisplayName("should throw when disbursement not found")
        void shouldThrowWhenDisbursementNotFound() {
            when(disbursementRepository.findById(disbursementId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cashDisbursementService.getDisbursementById(disbursementId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Disbursement not found");
        }
    }

    // ===================================================================
    // getDisbursementByReceipt
    // ===================================================================

    @Nested
    @DisplayName("getDisbursementByReceipt")
    class GetDisbursementByReceiptTests {

        @Test
        @DisplayName("should return disbursement when receipt found")
        void shouldReturnDisbursementWhenReceiptFound() {
            CashDisbursement disbursement = createTestDisbursement();
            when(disbursementRepository.findByReceiptNumber(receiptNumber))
                    .thenReturn(Optional.of(disbursement));

            CashDisbursement result = cashDisbursementService.getDisbursementByReceipt(receiptNumber);

            assertThat(result).isEqualTo(disbursement);
        }

        @Test
        @DisplayName("should throw when receipt not found")
        void shouldThrowWhenReceiptNotFound() {
            when(disbursementRepository.findByReceiptNumber("INVALID-RECEIPT"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> cashDisbursementService.getDisbursementByReceipt("INVALID-RECEIPT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Disbursement not found for receipt")
                    .hasMessageContaining("INVALID-RECEIPT");
        }
    }
}
