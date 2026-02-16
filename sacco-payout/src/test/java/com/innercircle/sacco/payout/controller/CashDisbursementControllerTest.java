package com.innercircle.sacco.payout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.payout.dto.CashDisbursementRequest;
import com.innercircle.sacco.payout.entity.CashDisbursement;
import com.innercircle.sacco.payout.service.CashDisbursementService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashDisbursementController.class)
@AutoConfigureMockMvc(addFilters = false)
class CashDisbursementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CashDisbursementService cashDisbursementService;

    private UUID memberId;
    private UUID disbursementId;
    private CashDisbursement testDisbursement;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        disbursementId = UUID.randomUUID();
        testDisbursement = new CashDisbursement(
                memberId, new BigDecimal("15000.00"), "John Doe", "Jane Smith",
                "RCP-001", LocalDate.of(2026, 2, 15)
        );
        testDisbursement.setId(disbursementId);
        testDisbursement.setCreatedAt(Instant.now());
        testDisbursement.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("POST /api/v1/cash-disbursements")
    class RecordDisbursementTests {

        @Test
        @DisplayName("should record disbursement and return 201")
        void shouldRecordDisbursementAndReturn201() throws Exception {
            when(cashDisbursementService.recordDisbursement(
                    any(UUID.class), any(BigDecimal.class), anyString(), anyString(),
                    anyString(), any(LocalDate.class), anyString()
            )).thenReturn(testDisbursement);

            CashDisbursementRequest request = new CashDisbursementRequest(
                    memberId, new BigDecimal("15000.00"), "John Doe", "Jane Smith",
                    "RCP-001", LocalDate.of(2026, 2, 15)
            );

            mockMvc.perform(post("/api/v1/cash-disbursements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .param("actor", "admin"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(disbursementId.toString()))
                    .andExpect(jsonPath("$.data.receivedBy").value("John Doe"))
                    .andExpect(jsonPath("$.data.disbursedBy").value("Jane Smith"))
                    .andExpect(jsonPath("$.data.receiptNumber").value("RCP-001"))
                    .andExpect(jsonPath("$.message").value("Cash disbursement recorded successfully"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/cash-disbursements/{disbursementId}/signoff")
    class SignoffTests {

        @Test
        @DisplayName("should sign off disbursement")
        void shouldSignOffDisbursement() throws Exception {
            testDisbursement.setSignoffBy("supervisor");
            when(cashDisbursementService.signoff(eq(disbursementId), eq("supervisor")))
                    .thenReturn(testDisbursement);

            mockMvc.perform(put("/api/v1/cash-disbursements/{disbursementId}/signoff", disbursementId)
                            .param("signoffBy", "supervisor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.signoffBy").value("supervisor"))
                    .andExpect(jsonPath("$.message").value("Cash disbursement signed off successfully"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cash-disbursements/{disbursementId}")
    class GetDisbursementByIdTests {

        @Test
        @DisplayName("should return disbursement by ID")
        void shouldReturnDisbursementById() throws Exception {
            when(cashDisbursementService.getDisbursementById(disbursementId)).thenReturn(testDisbursement);

            mockMvc.perform(get("/api/v1/cash-disbursements/{disbursementId}", disbursementId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(disbursementId.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cash-disbursements/receipt/{receiptNumber}")
    class GetDisbursementByReceiptTests {

        @Test
        @DisplayName("should return disbursement by receipt number")
        void shouldReturnDisbursementByReceiptNumber() throws Exception {
            when(cashDisbursementService.getDisbursementByReceipt("RCP-001")).thenReturn(testDisbursement);

            mockMvc.perform(get("/api/v1/cash-disbursements/receipt/{receiptNumber}", "RCP-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.receiptNumber").value("RCP-001"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cash-disbursements/member/{memberId}")
    class GetDisbursementHistoryTests {

        @Test
        @DisplayName("should return disbursement history for member")
        void shouldReturnDisbursementHistoryForMember() throws Exception {
            CursorPage<CashDisbursement> page = CursorPage.of(List.of(testDisbursement), null, false);
            when(cashDisbursementService.getDisbursementHistory(eq(memberId), isNull(), eq(20))).thenReturn(page);

            mockMvc.perform(get("/api/v1/cash-disbursements/member/{memberId}", memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].id").value(disbursementId.toString()));
        }

        @Test
        @DisplayName("should return disbursement history with cursor and limit")
        void shouldReturnDisbursementHistoryWithCursorAndLimit() throws Exception {
            String cursor = UUID.randomUUID().toString();
            CursorPage<CashDisbursement> page = CursorPage.of(List.of(testDisbursement), null, false);
            when(cashDisbursementService.getDisbursementHistory(eq(memberId), eq(cursor), eq(10))).thenReturn(page);

            mockMvc.perform(get("/api/v1/cash-disbursements/member/{memberId}", memberId)
                            .param("cursor", cursor)
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cash-disbursements/date-range")
    class GetDisbursementsByDateRangeTests {

        @Test
        @DisplayName("should return disbursements by date range")
        void shouldReturnDisbursementsByDateRange() throws Exception {
            CursorPage<CashDisbursement> page = CursorPage.of(List.of(testDisbursement), null, false);
            when(cashDisbursementService.getDisbursementsByDateRange(
                    eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)),
                    isNull(), eq(20)
            )).thenReturn(page);

            mockMvc.perform(get("/api/v1/cash-disbursements/date-range")
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("should return disbursements by date range with cursor")
        void shouldReturnDisbursementsByDateRangeWithCursor() throws Exception {
            String cursor = UUID.randomUUID().toString();
            CursorPage<CashDisbursement> page = CursorPage.of(List.of(testDisbursement), null, false);
            when(cashDisbursementService.getDisbursementsByDateRange(
                    eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)),
                    eq(cursor), eq(10)
            )).thenReturn(page);

            mockMvc.perform(get("/api/v1/cash-disbursements/date-range")
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31")
                            .param("cursor", cursor)
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
