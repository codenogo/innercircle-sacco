package com.innercircle.sacco.payout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.dto.PayoutRequest;
import com.innercircle.sacco.payout.entity.Payout;
import com.innercircle.sacco.payout.entity.PayoutStatus;
import com.innercircle.sacco.payout.entity.PayoutType;
import com.innercircle.sacco.payout.service.PayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
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

@WebMvcTest(PayoutController.class)
class PayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PayoutService payoutService;

    private UUID memberId;
    private UUID payoutId;
    private Payout testPayout;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        payoutId = UUID.randomUUID();
        testPayout = new Payout(memberId, new BigDecimal("5000.00"), PayoutType.MERRY_GO_ROUND);
        testPayout.setId(payoutId);
        testPayout.setStatus(PayoutStatus.PENDING);
        testPayout.setCreatedAt(Instant.now());
        testPayout.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("POST /api/v1/payouts")
    class CreatePayoutTests {

        @Test
        @DisplayName("should create payout and return 201")
        void shouldCreatePayoutAndReturn201() throws Exception {
            when(payoutService.createPayout(any(UUID.class), any(BigDecimal.class), any(PayoutType.class), anyString()))
                    .thenReturn(testPayout);

            PayoutRequest request = new PayoutRequest(memberId, new BigDecimal("5000.00"), PayoutType.MERRY_GO_ROUND);

            mockMvc.perform(post("/api/v1/payouts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .param("actor", "admin"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(payoutId.toString()))
                    .andExpect(jsonPath("$.data.memberId").value(memberId.toString()))
                    .andExpect(jsonPath("$.data.amount").value(5000.00))
                    .andExpect(jsonPath("$.data.type").value("MERRY_GO_ROUND"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.message").value("Payout created successfully"));
        }

        @Test
        @DisplayName("should use default actor when not provided")
        void shouldUseDefaultActorWhenNotProvided() throws Exception {
            when(payoutService.createPayout(any(UUID.class), any(BigDecimal.class), any(PayoutType.class), eq("system")))
                    .thenReturn(testPayout);

            PayoutRequest request = new PayoutRequest(memberId, new BigDecimal("5000.00"), PayoutType.MERRY_GO_ROUND);

            mockMvc.perform(post("/api/v1/payouts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/payouts/{payoutId}/approve")
    class ApprovePayoutTests {

        @Test
        @DisplayName("should approve payout and return 200")
        void shouldApprovePayoutAndReturn200() throws Exception {
            testPayout.setStatus(PayoutStatus.APPROVED);
            testPayout.setApprovedBy("admin");
            when(payoutService.approvePayout(eq(payoutId), eq("admin"))).thenReturn(testPayout);

            mockMvc.perform(put("/api/v1/payouts/{payoutId}/approve", payoutId)
                            .param("actor", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.message").value("Payout approved successfully"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/payouts/{payoutId}/process")
    class ProcessPayoutTests {

        @Test
        @DisplayName("should process payout and return 200")
        void shouldProcessPayoutAndReturn200() throws Exception {
            testPayout.setStatus(PayoutStatus.PROCESSED);
            testPayout.setProcessedAt(Instant.now());
            testPayout.setReferenceNumber("PAY-12345678");
            when(payoutService.processPayout(eq(payoutId), eq("admin"))).thenReturn(testPayout);

            mockMvc.perform(put("/api/v1/payouts/{payoutId}/process", payoutId)
                            .param("actor", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("PROCESSED"))
                    .andExpect(jsonPath("$.data.referenceNumber").value("PAY-12345678"))
                    .andExpect(jsonPath("$.message").value("Payout processed successfully"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payouts/{payoutId}")
    class GetPayoutByIdTests {

        @Test
        @DisplayName("should return payout by ID")
        void shouldReturnPayoutById() throws Exception {
            when(payoutService.getPayoutById(payoutId)).thenReturn(testPayout);

            mockMvc.perform(get("/api/v1/payouts/{payoutId}", payoutId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(payoutId.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payouts/member/{memberId}")
    class GetPayoutHistoryTests {

        @Test
        @DisplayName("should return payout history for member")
        void shouldReturnPayoutHistoryForMember() throws Exception {
            CursorPage<Payout> page = CursorPage.of(List.of(testPayout), null, false);
            when(payoutService.getPayoutHistory(eq(memberId), isNull(), eq(20))).thenReturn(page);

            mockMvc.perform(get("/api/v1/payouts/member/{memberId}", memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].id").value(payoutId.toString()))
                    .andExpect(jsonPath("$.data.hasMore").value(false));
        }

        @Test
        @DisplayName("should return payout history with cursor and limit")
        void shouldReturnPayoutHistoryWithCursorAndLimit() throws Exception {
            String cursor = UUID.randomUUID().toString();
            CursorPage<Payout> page = CursorPage.of(List.of(testPayout), null, false);
            when(payoutService.getPayoutHistory(eq(memberId), eq(cursor), eq(10))).thenReturn(page);

            mockMvc.perform(get("/api/v1/payouts/member/{memberId}", memberId)
                            .param("cursor", cursor)
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payouts/status/{status}")
    class GetPayoutsByStatusTests {

        @Test
        @DisplayName("should return payouts by status")
        void shouldReturnPayoutsByStatus() throws Exception {
            CursorPage<Payout> page = CursorPage.of(List.of(testPayout), null, false);
            when(payoutService.getPayoutsByStatus(eq(PayoutStatus.PENDING), isNull(), eq(20))).thenReturn(page);

            mockMvc.perform(get("/api/v1/payouts/status/{status}", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payouts")
    class GetAllPayoutsTests {

        @Test
        @DisplayName("should return all payouts")
        void shouldReturnAllPayouts() throws Exception {
            CursorPage<Payout> page = CursorPage.of(List.of(testPayout), null, false);
            when(payoutService.getAllPayouts(isNull(), eq(20))).thenReturn(page);

            mockMvc.perform(get("/api/v1/payouts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("should return all payouts with pagination")
        void shouldReturnAllPayoutsWithPagination() throws Exception {
            String cursor = UUID.randomUUID().toString();
            CursorPage<Payout> page = CursorPage.of(List.of(testPayout), cursor, true);
            when(payoutService.getAllPayouts(isNull(), eq(5))).thenReturn(page);

            mockMvc.perform(get("/api/v1/payouts")
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hasMore").value(true))
                    .andExpect(jsonPath("$.data.nextCursor").value(cursor));
        }
    }
}
