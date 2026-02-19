package com.innercircle.sacco.payout.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.AuditableEvent;
import com.innercircle.sacco.common.event.PettyCashWorkflowEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.MakerCheckerViolationException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.payout.dto.PettyCashSummaryResponse;
import com.innercircle.sacco.payout.entity.PettyCashExpenseType;
import com.innercircle.sacco.payout.entity.PettyCashVoucher;
import com.innercircle.sacco.payout.entity.PettyCashVoucherStatus;
import com.innercircle.sacco.payout.repository.PettyCashVoucherRepository;
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
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class PettyCashServiceImplTest {

    @Mock
    private PettyCashVoucherRepository voucherRepository;

    @Mock
    private EventOutboxWriter outboxWriter;

    @InjectMocks
    private PettyCashServiceImpl pettyCashService;

    @Captor
    private ArgumentCaptor<PettyCashVoucher> voucherCaptor;

    @Captor
    private ArgumentCaptor<AuditableEvent> eventCaptor;

    private UUID voucherId;

    @BeforeEach
    void setUp() {
        voucherId = UUID.randomUUID();
    }

    private PettyCashVoucher createTestVoucher(PettyCashVoucherStatus status) {
        PettyCashVoucher voucher = new PettyCashVoucher(
                "PC-TESTREF1",
                new BigDecimal("1500.00"),
                "Office supplies",
                PettyCashExpenseType.OPERATIONS,
                LocalDate.now(),
                null
        );
        voucher.setId(voucherId);
        voucher.setStatus(status);
        return voucher;
    }

    // ===================================================================
    // createVoucher
    // ===================================================================

    @Nested
    @DisplayName("createVoucher")
    class CreateVoucherTests {

        @Test
        @DisplayName("should create voucher with SUBMITTED status")
        void shouldCreateVoucherWithSubmittedStatus() {
            PettyCashVoucher saved = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            when(voucherRepository.existsByReferenceNumber(any())).thenReturn(false);
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenReturn(saved);

            PettyCashVoucher result = pettyCashService.createVoucher(
                    new BigDecimal("1500.00"),
                    "Office supplies",
                    PettyCashExpenseType.OPERATIONS,
                    LocalDate.now(),
                    null,
                    "treasurer"
            );

            verify(voucherRepository).save(voucherCaptor.capture());
            PettyCashVoucher captured = voucherCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(PettyCashVoucherStatus.SUBMITTED);
            assertThat(captured.getCreatedBy()).isEqualTo("treasurer");
            assertThat(result).isEqualTo(saved);
        }

        @Test
        @DisplayName("should publish CREATED workflow event on create")
        void shouldPublishCreatedEvent() {
            PettyCashVoucher saved = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            when(voucherRepository.existsByReferenceNumber(any())).thenReturn(false);
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenReturn(saved);

            pettyCashService.createVoucher(
                    new BigDecimal("1500.00"),
                    "Office supplies",
                    PettyCashExpenseType.OPERATIONS,
                    LocalDate.now(),
                    null,
                    "treasurer"
            );

            verify(outboxWriter).write(eventCaptor.capture(), eq("PettyCashVoucher"), any(UUID.class));
            AuditableEvent event = eventCaptor.getValue();
            assertThat(event).isInstanceOf(PettyCashWorkflowEvent.class);
            PettyCashWorkflowEvent workflowEvent = (PettyCashWorkflowEvent) event;
            assertThat(workflowEvent.action()).isEqualTo("CREATED");
            assertThat(workflowEvent.actor()).isEqualTo("treasurer");
        }

        @Test
        @DisplayName("should use today as requestDate when not provided")
        void shouldDefaultRequestDateToToday() {
            PettyCashVoucher saved = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            when(voucherRepository.existsByReferenceNumber(any())).thenReturn(false);
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenReturn(saved);

            pettyCashService.createVoucher(
                    new BigDecimal("1500.00"),
                    "Office supplies",
                    PettyCashExpenseType.OPERATIONS,
                    null,
                    null,
                    "treasurer"
            );

            verify(voucherRepository).save(voucherCaptor.capture());
            assertThat(voucherCaptor.getValue().getRequestDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("should throw BusinessException when reference number cannot be generated uniquely")
        void shouldThrowWhenReferenceCannotBeGenerated() {
            when(voucherRepository.existsByReferenceNumber(any())).thenReturn(true);

            assertThatThrownBy(() -> pettyCashService.createVoucher(
                    new BigDecimal("1500.00"),
                    "Office supplies",
                    PettyCashExpenseType.OPERATIONS,
                    LocalDate.now(),
                    null,
                    "treasurer"
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Unable to generate unique petty cash reference");

            verify(voucherRepository, never()).save(any());
        }
    }

    // ===================================================================
    // approveVoucher
    // ===================================================================

    @Nested
    @DisplayName("approveVoucher")
    class ApproveVoucherTests {

        @Test
        @DisplayName("should approve SUBMITTED voucher by a different user")
        void shouldApproveSubmittedVoucherByDifferentUser() {
            PettyCashVoucher submitted = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            submitted.setCreatedBy("treasurer");
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(submitted));

            PettyCashVoucher approved = createTestVoucher(PettyCashVoucherStatus.APPROVED);
            approved.setApprovedBy("admin");
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenReturn(approved);

            PettyCashVoucher result = pettyCashService.approveVoucher(voucherId, "admin", null, false);

            verify(voucherRepository).save(voucherCaptor.capture());
            PettyCashVoucher captured = voucherCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(PettyCashVoucherStatus.APPROVED);
            assertThat(captured.getApprovedBy()).isEqualTo("admin");
            assertThat(result.getStatus()).isEqualTo(PettyCashVoucherStatus.APPROVED);
        }

        @Test
        @DisplayName("should publish APPROVED event when approved by different user")
        void shouldPublishApprovedEvent() {
            PettyCashVoucher submitted = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            submitted.setCreatedBy("treasurer");
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(submitted));
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenAnswer(inv -> inv.getArgument(0));

            pettyCashService.approveVoucher(voucherId, "admin", null, false);

            verify(outboxWriter).write(eventCaptor.capture(), eq("PettyCashVoucher"), any(UUID.class));
            PettyCashWorkflowEvent event = (PettyCashWorkflowEvent) eventCaptor.getValue();
            assertThat(event.action()).isEqualTo("APPROVED");
            assertThat(event.actor()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should throw MakerCheckerViolationException when creator tries to approve")
        void shouldThrowWhenCreatorApprovesOwnVoucher() {
            PettyCashVoucher submitted = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            submitted.setCreatedBy("treasurer");
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(submitted));

            assertThatThrownBy(() -> pettyCashService.approveVoucher(voucherId, "treasurer", null, false))
                    .isInstanceOf(MakerCheckerViolationException.class);

            verify(voucherRepository, never()).save(any());
            verify(outboxWriter, never()).write(any(), any(), any());
        }

        @Test
        @DisplayName("should allow ADMIN to override-approve their own voucher with reason")
        void shouldAllowAdminOverrideWithReason() {
            PettyCashVoucher submitted = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            submitted.setCreatedBy("admin");
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(submitted));
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenAnswer(inv -> inv.getArgument(0));

            PettyCashVoucher result = pettyCashService.approveVoucher(voucherId, "admin", "Emergency approval", true);

            assertThat(result.getStatus()).isEqualTo(PettyCashVoucherStatus.APPROVED);
            verify(outboxWriter).write(eventCaptor.capture(), eq("PettyCashVoucher"), any(UUID.class));
            PettyCashWorkflowEvent event = (PettyCashWorkflowEvent) eventCaptor.getValue();
            assertThat(event.action()).isEqualTo("OVERRIDE_APPROVED");
        }

        @Test
        @DisplayName("should throw MakerCheckerViolationException when ADMIN override attempted without reason")
        void shouldThrowWhenAdminOverrideWithoutReason() {
            PettyCashVoucher submitted = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            submitted.setCreatedBy("admin");
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(submitted));

            assertThatThrownBy(() -> pettyCashService.approveVoucher(voucherId, "admin", null, true))
                    .isInstanceOf(MakerCheckerViolationException.class);

            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when voucher not found")
        void shouldThrowWhenVoucherNotFound() {
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pettyCashService.approveVoucher(voucherId, "admin", null, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BusinessException when voucher is not SUBMITTED")
        void shouldThrowWhenVoucherNotSubmitted() {
            PettyCashVoucher approved = createTestVoucher(PettyCashVoucherStatus.APPROVED);
            approved.setCreatedBy("treasurer");
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(approved));

            assertThatThrownBy(() -> pettyCashService.approveVoucher(voucherId, "admin", null, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only submitted vouchers can be approved");

            verify(voucherRepository, never()).save(any());
        }
    }

    // ===================================================================
    // disburseVoucher
    // ===================================================================

    @Nested
    @DisplayName("disburseVoucher")
    class DisburseVoucherTests {

        @Test
        @DisplayName("should disburse an APPROVED voucher")
        void shouldDisbursePApprovedVoucher() {
            PettyCashVoucher approved = createTestVoucher(PettyCashVoucherStatus.APPROVED);
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(approved));
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenAnswer(inv -> inv.getArgument(0));

            PettyCashVoucher result = pettyCashService.disburseVoucher(voucherId, "admin");

            verify(voucherRepository).save(voucherCaptor.capture());
            PettyCashVoucher captured = voucherCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(PettyCashVoucherStatus.DISBURSED);
            assertThat(captured.getDisbursedBy()).isEqualTo("admin");
            assertThat(captured.getDisbursedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw when voucher is not APPROVED")
        void shouldThrowWhenVoucherNotApproved() {
            PettyCashVoucher submitted = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(submitted));

            assertThatThrownBy(() -> pettyCashService.disburseVoucher(voucherId, "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only approved vouchers can be disbursed");

            verify(voucherRepository, never()).save(any());
        }
    }

    // ===================================================================
    // settleVoucher
    // ===================================================================

    @Nested
    @DisplayName("settleVoucher")
    class SettleVoucherTests {

        @Test
        @DisplayName("should settle a DISBURSED voucher")
        void shouldSettleDisbursedVoucher() {
            PettyCashVoucher disbursed = createTestVoucher(PettyCashVoucherStatus.DISBURSED);
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(disbursed));
            when(voucherRepository.existsByReceiptNumber("RCT-001")).thenReturn(false);
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenAnswer(inv -> inv.getArgument(0));

            PettyCashVoucher result = pettyCashService.settleVoucher(voucherId, "RCT-001", null, "admin");

            verify(voucherRepository).save(voucherCaptor.capture());
            PettyCashVoucher captured = voucherCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(PettyCashVoucherStatus.SETTLED);
            assertThat(captured.getReceiptNumber()).isEqualTo("RCT-001");
            assertThat(captured.getSettledBy()).isEqualTo("admin");
            assertThat(captured.getSettledAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw when receipt number already exists")
        void shouldThrowWhenReceiptNumberDuplicated() {
            PettyCashVoucher disbursed = createTestVoucher(PettyCashVoucherStatus.DISBURSED);
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(disbursed));
            when(voucherRepository.existsByReceiptNumber("RCT-DUP")).thenReturn(true);

            assertThatThrownBy(() -> pettyCashService.settleVoucher(voucherId, "RCT-DUP", null, "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Receipt number already exists");

            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when voucher is not DISBURSED")
        void shouldThrowWhenVoucherNotDisbursed() {
            PettyCashVoucher approved = createTestVoucher(PettyCashVoucherStatus.APPROVED);
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(approved));

            assertThatThrownBy(() -> pettyCashService.settleVoucher(voucherId, "RCT-001", null, "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only disbursed vouchers can be settled");
        }
    }

    // ===================================================================
    // rejectVoucher
    // ===================================================================

    @Nested
    @DisplayName("rejectVoucher")
    class RejectVoucherTests {

        @Test
        @DisplayName("should reject a SUBMITTED voucher")
        void shouldRejectSubmittedVoucher() {
            PettyCashVoucher submitted = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(submitted));
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenAnswer(inv -> inv.getArgument(0));

            PettyCashVoucher result = pettyCashService.rejectVoucher(voucherId, "Invalid claim", "admin");

            verify(voucherRepository).save(voucherCaptor.capture());
            PettyCashVoucher captured = voucherCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(PettyCashVoucherStatus.REJECTED);
            assertThat(captured.getRejectedBy()).isEqualTo("admin");
            assertThat(captured.getRejectionReason()).isEqualTo("Invalid claim");
        }

        @Test
        @DisplayName("should reject an APPROVED voucher")
        void shouldRejectApprovedVoucher() {
            PettyCashVoucher approved = createTestVoucher(PettyCashVoucherStatus.APPROVED);
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(approved));
            when(voucherRepository.save(any(PettyCashVoucher.class))).thenAnswer(inv -> inv.getArgument(0));

            PettyCashVoucher result = pettyCashService.rejectVoucher(voucherId, "Cancelled", "admin");

            assertThat(result.getStatus()).isEqualTo(PettyCashVoucherStatus.REJECTED);
        }

        @Test
        @DisplayName("should throw when voucher is DISBURSED (cannot reject)")
        void shouldThrowWhenVoucherDisbursed() {
            PettyCashVoucher disbursed = createTestVoucher(PettyCashVoucherStatus.DISBURSED);
            when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(disbursed));

            assertThatThrownBy(() -> pettyCashService.rejectVoucher(voucherId, "reason", "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only submitted or approved vouchers can be rejected");
        }
    }

    // ===================================================================
    // getVouchers
    // ===================================================================

    @Nested
    @DisplayName("getVouchers")
    class GetVouchersTests {

        @Test
        @DisplayName("should return all vouchers without filters")
        void shouldReturnAllVouchersWithoutFilters() {
            List<PettyCashVoucher> vouchers = List.of(createTestVoucher(PettyCashVoucherStatus.SUBMITTED));
            when(voucherRepository.findAllPaged(any(PageRequest.class))).thenReturn(vouchers);

            CursorPage<PettyCashVoucher> result = pettyCashService.getVouchers(null, null, null, null, 20);

            assertThat(result.getItems()).hasSize(1);
            verify(voucherRepository).findAllPaged(any(PageRequest.class));
        }

        @Test
        @DisplayName("should filter by status only")
        void shouldFilterByStatus() {
            List<PettyCashVoucher> vouchers = List.of(createTestVoucher(PettyCashVoucherStatus.SUBMITTED));
            when(voucherRepository.findByStatus(eq(PettyCashVoucherStatus.SUBMITTED), any(PageRequest.class)))
                    .thenReturn(vouchers);

            CursorPage<PettyCashVoucher> result = pettyCashService.getVouchers(
                    PettyCashVoucherStatus.SUBMITTED, null, null, null, 20
            );

            assertThat(result.getItems()).hasSize(1);
            verify(voucherRepository).findByStatus(eq(PettyCashVoucherStatus.SUBMITTED), any(PageRequest.class));
        }

        @Test
        @DisplayName("should indicate hasMore when results exceed limit")
        void shouldIndicateHasMore() {
            PettyCashVoucher v1 = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            v1.setId(UUID.randomUUID());
            PettyCashVoucher v2 = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            v2.setId(UUID.randomUUID());
            PettyCashVoucher v3 = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            v3.setId(UUID.randomUUID());

            when(voucherRepository.findAllPaged(any(PageRequest.class))).thenReturn(List.of(v1, v2, v3));

            CursorPage<PettyCashVoucher> result = pettyCashService.getVouchers(null, null, null, null, 2);

            assertThat(result.isHasMore()).isTrue();
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getNextCursor()).isNotNull();
        }
    }

    // ===================================================================
    // getSummary
    // ===================================================================

    @Nested
    @DisplayName("getSummary")
    class GetSummaryTests {

        @Test
        @DisplayName("should return summary counts for all statuses")
        void shouldReturnSummaryCounts() {
            PettyCashVoucher submitted = createTestVoucher(PettyCashVoucherStatus.SUBMITTED);
            PettyCashVoucher settled = createTestVoucher(PettyCashVoucherStatus.SETTLED);
            settled.setId(UUID.randomUUID());

            when(voucherRepository.findAllPaged(any(PageRequest.class)))
                    .thenReturn(List.of(submitted, settled));

            PettyCashSummaryResponse summary = pettyCashService.getSummary(null, null, null);

            assertThat(summary.totalCount()).isEqualTo(2);
            assertThat(summary.submittedCount()).isEqualTo(1);
            assertThat(summary.settledCount()).isEqualTo(1);
        }
    }
}
