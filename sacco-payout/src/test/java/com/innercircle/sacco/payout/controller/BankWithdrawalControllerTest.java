package com.innercircle.sacco.payout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.dto.BankWithdrawalRequest;
import com.innercircle.sacco.payout.entity.BankWithdrawal;
import com.innercircle.sacco.payout.entity.WithdrawalStatus;
import com.innercircle.sacco.payout.service.BankWithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankWithdrawalController.class)
@AutoConfigureMockMvc(addFilters = false)
class BankWithdrawalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BankWithdrawalService bankWithdrawalService;

    private UUID memberId;
    private UUID withdrawalId;
    private BankWithdrawal testWithdrawal;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        withdrawalId = UUID.randomUUID();
        testWithdrawal = new BankWithdrawal(memberId, new BigDecimal("10000.00"), "KCB Bank", "1234567890");
        testWithdrawal.setId(withdrawalId);
        testWithdrawal.setStatus(WithdrawalStatus.PENDING);
        testWithdrawal.setCreatedAt(Instant.now());
        testWithdrawal.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("POST /api/v1/bank-withdrawals")
    class InitiateWithdrawalTests {

        @Test
        @DisplayName("should initiate withdrawal and return 201")
        void shouldInitiateWithdrawalAndReturn201() throws Exception {
            when(bankWithdrawalService.initiateWithdrawal(
                    any(UUID.class), any(BigDecimal.class), anyString(), anyString(), anyString()
            )).thenReturn(testWithdrawal);

            BankWithdrawalRequest request = new BankWithdrawalRequest(
                    memberId, new BigDecimal("10000.00"), "KCB Bank", "1234567890"
            );

            mockMvc.perform(post("/api/v1/bank-withdrawals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .param("actor", "admin"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(withdrawalId.toString()))
                    .andExpect(jsonPath("$.data.bankName").value("KCB Bank"))
                    .andExpect(jsonPath("$.data.accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.message").value("Bank withdrawal initiated successfully"));
        }

        @Test
        @DisplayName("should use default actor when not provided")
        void shouldUseDefaultActor() throws Exception {
            when(bankWithdrawalService.initiateWithdrawal(
                    any(UUID.class), any(BigDecimal.class), anyString(), anyString(), eq("system")
            )).thenReturn(testWithdrawal);

            BankWithdrawalRequest request = new BankWithdrawalRequest(
                    memberId, new BigDecimal("10000.00"), "KCB Bank", "1234567890"
            );

            mockMvc.perform(post("/api/v1/bank-withdrawals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/bank-withdrawals/{withdrawalId}/confirm")
    class ConfirmWithdrawalTests {

        @Test
        @DisplayName("should confirm withdrawal and return 200")
        void shouldConfirmWithdrawalAndReturn200() throws Exception {
            testWithdrawal.setStatus(WithdrawalStatus.COMPLETED);
            testWithdrawal.setReferenceNumber("REF-001");
            testWithdrawal.setTransactionDate(LocalDate.now());
            when(bankWithdrawalService.confirmWithdrawal(eq(withdrawalId), eq("REF-001"), eq("admin")))
                    .thenReturn(testWithdrawal);

            mockMvc.perform(put("/api/v1/bank-withdrawals/{withdrawalId}/confirm", withdrawalId)
                            .param("referenceNumber", "REF-001")
                            .param("actor", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.referenceNumber").value("REF-001"))
                    .andExpect(jsonPath("$.message").value("Bank withdrawal confirmed successfully"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/bank-withdrawals/{withdrawalId}/reconcile")
    class MarkReconciledTests {

        @Test
        @DisplayName("should mark withdrawal as reconciled")
        void shouldMarkWithdrawalAsReconciled() throws Exception {
            testWithdrawal.setReconciled(true);
            when(bankWithdrawalService.markReconciled(eq(withdrawalId), eq("admin"))).thenReturn(testWithdrawal);

            mockMvc.perform(put("/api/v1/bank-withdrawals/{withdrawalId}/reconcile", withdrawalId)
                            .param("actor", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reconciled").value(true))
                    .andExpect(jsonPath("$.message").value("Bank withdrawal marked as reconciled"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bank-withdrawals/{withdrawalId}")
    class GetWithdrawalByIdTests {

        @Test
        @DisplayName("should return withdrawal by ID")
        void shouldReturnWithdrawalById() throws Exception {
            when(bankWithdrawalService.getWithdrawalById(withdrawalId)).thenReturn(testWithdrawal);

            mockMvc.perform(get("/api/v1/bank-withdrawals/{withdrawalId}", withdrawalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(withdrawalId.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bank-withdrawals/unreconciled")
    class GetUnreconciledTests {

        @Test
        @DisplayName("should return unreconciled withdrawals")
        void shouldReturnUnreconciledWithdrawals() throws Exception {
            CursorPage<BankWithdrawal> page = CursorPage.of(List.of(testWithdrawal), null, false);
            when(bankWithdrawalService.getUnreconciled(isNull(), eq(20))).thenReturn(page);

            mockMvc.perform(get("/api/v1/bank-withdrawals/unreconciled"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.hasMore").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bank-withdrawals/member/{memberId}")
    class GetWithdrawalsByMemberTests {

        @Test
        @DisplayName("should return withdrawals by member")
        void shouldReturnWithdrawalsByMember() throws Exception {
            CursorPage<BankWithdrawal> page = CursorPage.of(List.of(testWithdrawal), null, false);
            when(bankWithdrawalService.getWithdrawalsByMember(eq(memberId), isNull(), eq(20))).thenReturn(page);

            mockMvc.perform(get("/api/v1/bank-withdrawals/member/{memberId}", memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bank-withdrawals/status/{status}")
    class GetWithdrawalsByStatusTests {

        @Test
        @DisplayName("should return withdrawals by status")
        void shouldReturnWithdrawalsByStatus() throws Exception {
            CursorPage<BankWithdrawal> page = CursorPage.of(List.of(testWithdrawal), null, false);
            when(bankWithdrawalService.getWithdrawalsByStatus(eq(WithdrawalStatus.PENDING), isNull(), eq(20)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/bank-withdrawals/status/{status}", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }
}
