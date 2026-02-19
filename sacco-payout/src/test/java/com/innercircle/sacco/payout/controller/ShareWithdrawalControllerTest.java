package com.innercircle.sacco.payout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.security.MemberAccessHelper;
import com.innercircle.sacco.payout.dto.ShareWithdrawalRequest;
import com.innercircle.sacco.payout.entity.ShareWithdrawal;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalStatus;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalType;
import com.innercircle.sacco.payout.service.ShareWithdrawalService;
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

@WebMvcTest(ShareWithdrawalController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShareWithdrawalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShareWithdrawalService shareWithdrawalService;

    @MockitoBean
    private MemberAccessHelper memberAccessHelper;

    private UUID memberId;
    private UUID withdrawalId;
    private ShareWithdrawal testWithdrawal;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        withdrawalId = UUID.randomUUID();
        testWithdrawal = new ShareWithdrawal(
                memberId, new BigDecimal("5000.00"), ShareWithdrawalType.PARTIAL, new BigDecimal("10000.00")
        );
        testWithdrawal.setId(withdrawalId);
        testWithdrawal.setStatus(ShareWithdrawalStatus.PENDING);
        testWithdrawal.setCreatedAt(Instant.now());
        testWithdrawal.setUpdatedAt(Instant.now());

        when(memberAccessHelper.currentActor(any())).thenReturn("admin");
    }

    @Nested
    @DisplayName("POST /api/v1/share-withdrawals")
    class RequestWithdrawalTests {

        @Test
        @DisplayName("should request withdrawal and return 201")
        void shouldRequestWithdrawalAndReturn201() throws Exception {
            when(shareWithdrawalService.requestWithdrawal(
                    any(UUID.class), any(BigDecimal.class), any(ShareWithdrawalType.class),
                    any(BigDecimal.class), anyString()
            )).thenReturn(testWithdrawal);

            ShareWithdrawalRequest request = new ShareWithdrawalRequest(
                    memberId, new BigDecimal("5000.00"), ShareWithdrawalType.PARTIAL, new BigDecimal("10000.00")
            );

            mockMvc.perform(post("/api/v1/share-withdrawals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(withdrawalId.toString()))
                    .andExpect(jsonPath("$.data.withdrawalType").value("PARTIAL"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.message").value("Share withdrawal requested successfully"));
        }

        @Test
        @DisplayName("should use default actor when not provided")
        void shouldUseDefaultActor() throws Exception {
            when(shareWithdrawalService.requestWithdrawal(
                    any(UUID.class), any(BigDecimal.class), any(ShareWithdrawalType.class),
                    any(BigDecimal.class), eq("admin")
            )).thenReturn(testWithdrawal);

            ShareWithdrawalRequest request = new ShareWithdrawalRequest(
                    memberId, new BigDecimal("5000.00"), ShareWithdrawalType.PARTIAL, new BigDecimal("10000.00")
            );

            mockMvc.perform(post("/api/v1/share-withdrawals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/share-withdrawals/{withdrawalId}/approve")
    class ApproveWithdrawalTests {

        @Test
        @DisplayName("should approve withdrawal and return 200")
        void shouldApproveWithdrawalAndReturn200() throws Exception {
            testWithdrawal.setStatus(ShareWithdrawalStatus.APPROVED);
            testWithdrawal.setApprovedBy("admin");
            when(shareWithdrawalService.approveWithdrawal(eq(withdrawalId), eq("admin"), isNull(), eq(false)))
                    .thenReturn(testWithdrawal);

            mockMvc.perform(put("/api/v1/share-withdrawals/{withdrawalId}/approve", withdrawalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.message").value("Share withdrawal approved successfully"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/share-withdrawals/{withdrawalId}/process")
    class ProcessWithdrawalTests {

        @Test
        @DisplayName("should process withdrawal and return 200")
        void shouldProcessWithdrawalAndReturn200() throws Exception {
            testWithdrawal.setStatus(ShareWithdrawalStatus.PROCESSED);
            testWithdrawal.setNewShareBalance(new BigDecimal("5000.00"));
            when(shareWithdrawalService.processWithdrawal(eq(withdrawalId), eq("admin")))
                    .thenReturn(testWithdrawal);

            mockMvc.perform(put("/api/v1/share-withdrawals/{withdrawalId}/process", withdrawalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("PROCESSED"))
                    .andExpect(jsonPath("$.data.newShareBalance").value(5000.00))
                    .andExpect(jsonPath("$.message").value("Share withdrawal processed successfully"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/share-withdrawals/{withdrawalId}")
    class GetWithdrawalByIdTests {

        @Test
        @DisplayName("should return withdrawal by ID")
        void shouldReturnWithdrawalById() throws Exception {
            when(shareWithdrawalService.getWithdrawalById(withdrawalId)).thenReturn(testWithdrawal);

            mockMvc.perform(get("/api/v1/share-withdrawals/{withdrawalId}", withdrawalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(withdrawalId.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/share-withdrawals/member/{memberId}")
    class GetWithdrawalsByMemberTests {

        @Test
        @DisplayName("should return withdrawals by member")
        void shouldReturnWithdrawalsByMember() throws Exception {
            CursorPage<ShareWithdrawal> page = CursorPage.of(List.of(testWithdrawal), null, false);
            when(shareWithdrawalService.getWithdrawalsByMember(eq(memberId), isNull(), eq(20))).thenReturn(page);

            mockMvc.perform(get("/api/v1/share-withdrawals/member/{memberId}", memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].id").value(withdrawalId.toString()));
        }

        @Test
        @DisplayName("should return withdrawals by member with cursor and limit")
        void shouldReturnWithdrawalsByMemberWithCursorAndLimit() throws Exception {
            String cursor = UUID.randomUUID().toString();
            CursorPage<ShareWithdrawal> page = CursorPage.of(List.of(testWithdrawal), null, false);
            when(shareWithdrawalService.getWithdrawalsByMember(eq(memberId), eq(cursor), eq(10))).thenReturn(page);

            mockMvc.perform(get("/api/v1/share-withdrawals/member/{memberId}", memberId)
                            .param("cursor", cursor)
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/share-withdrawals/status/{status}")
    class GetWithdrawalsByStatusTests {

        @Test
        @DisplayName("should return withdrawals by status")
        void shouldReturnWithdrawalsByStatus() throws Exception {
            CursorPage<ShareWithdrawal> page = CursorPage.of(List.of(testWithdrawal), null, false);
            when(shareWithdrawalService.getWithdrawalsByStatus(eq(ShareWithdrawalStatus.PENDING), isNull(), eq(20)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/share-withdrawals/status/{status}", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }
}
