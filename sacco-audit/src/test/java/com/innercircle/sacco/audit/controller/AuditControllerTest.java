package com.innercircle.sacco.audit.controller;

import com.innercircle.sacco.audit.dto.AuditEventResponse;
import com.innercircle.sacco.audit.entity.AuditAction;
import com.innercircle.sacco.audit.entity.AuditEvent;
import com.innercircle.sacco.audit.service.AuditService;
import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    // --- listAuditEvents tests ---

    @Test
    void listAuditEvents_withNoFilters_shouldReturnOkResponse() {
        List<AuditEvent> events = createAuditEventList(3);
        CursorPage<AuditEvent> cursorPage = CursorPage.of(events, null, false);

        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(cursorPage);

        ApiResponse<CursorPage<AuditEventResponse>> response = auditController.listAuditEvents(
                null, null, null, null, null, null, null, 50);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getItems()).hasSize(3);
        assertThat(response.getData().isHasMore()).isFalse();
    }

    @Test
    void listAuditEvents_withAllFilters_shouldPassFiltersToService() {
        UUID cursor = UUID.randomUUID();
        String entityType = "Member";
        UUID entityId = UUID.randomUUID();
        String actor = "admin";
        AuditAction action = AuditAction.CREATE;
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.queryEvents(eq(cursor), eq(entityType), eq(entityId), eq(actor),
                eq(action), eq(startDate), eq(endDate), eq(50)))
                .thenReturn(cursorPage);

        ApiResponse<CursorPage<AuditEventResponse>> response = auditController.listAuditEvents(
                cursor, entityType, entityId, actor, action, startDate, endDate, 50);

        assertThat(response.isSuccess()).isTrue();
        verify(auditService).queryEvents(eq(cursor), eq(entityType), eq(entityId), eq(actor),
                eq(action), eq(startDate), eq(endDate), eq(50));
    }

    @Test
    void listAuditEvents_withDefaultLimit_shouldUse50() {
        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), eq(50)))
                .thenReturn(cursorPage);

        auditController.listAuditEvents(null, null, null, null, null, null, null, 50);

        verify(auditService).queryEvents(any(), any(), any(), any(), any(), any(), any(), eq(50));
    }

    @Test
    void listAuditEvents_withPaginatedResults_shouldMapCorrectly() {
        UUID nextCursorId = UUID.randomUUID();
        List<AuditEvent> events = createAuditEventList(5);
        CursorPage<AuditEvent> cursorPage = CursorPage.of(events, nextCursorId.toString(), true);

        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(cursorPage);

        ApiResponse<CursorPage<AuditEventResponse>> response = auditController.listAuditEvents(
                null, null, null, null, null, null, null, 5);

        assertThat(response.getData().getItems()).hasSize(5);
        assertThat(response.getData().isHasMore()).isTrue();
        assertThat(response.getData().getNextCursor()).isEqualTo(nextCursorId.toString());
    }

    @Test
    void listAuditEvents_responseShouldMapEventFieldsCorrectly() {
        UUID eventId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        AuditEvent event = AuditEvent.builder()
                .id(eventId)
                .actor("admin@test.com")
                .actorName("Admin User")
                .action(AuditAction.APPROVE)
                .entityType("Loan")
                .entityId(entityId)
                .beforeSnapshot("{\"status\":\"PENDING\"}")
                .afterSnapshot("{\"status\":\"APPROVED\"}")
                .ipAddress("10.0.0.1")
                .timestamp(timestamp)
                .build();

        CursorPage<AuditEvent> cursorPage = CursorPage.of(List.of(event), null, false);
        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(cursorPage);

        ApiResponse<CursorPage<AuditEventResponse>> response = auditController.listAuditEvents(
                null, null, null, null, null, null, null, 50);

        AuditEventResponse dto = response.getData().getItems().get(0);
        assertThat(dto.getId()).isEqualTo(eventId);
        assertThat(dto.getActor()).isEqualTo("admin@test.com");
        assertThat(dto.getActorName()).isEqualTo("Admin User");
        assertThat(dto.getAction()).isEqualTo(AuditAction.APPROVE);
        assertThat(dto.getEntityType()).isEqualTo("Loan");
        assertThat(dto.getEntityId()).isEqualTo(entityId);
        assertThat(dto.getBeforeSnapshot()).isEqualTo("{\"status\":\"PENDING\"}");
        assertThat(dto.getAfterSnapshot()).isEqualTo("{\"status\":\"APPROVED\"}");
        assertThat(dto.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(dto.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void listAuditEvents_withLimitOverMax_shouldClampTo1000() {
        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        // The validate() method clamps limit to 1000
        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), eq(1000)))
                .thenReturn(cursorPage);

        auditController.listAuditEvents(null, null, null, null, null, null, null, 5000);

        verify(auditService).queryEvents(any(), any(), any(), any(), any(), any(), any(), eq(1000));
    }

    @Test
    void listAuditEvents_withNegativeLimit_shouldDefaultTo50() {
        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), eq(50)))
                .thenReturn(cursorPage);

        auditController.listAuditEvents(null, null, null, null, null, null, null, -1);

        verify(auditService).queryEvents(any(), any(), any(), any(), any(), any(), any(), eq(50));
    }

    // --- getEntityTrail tests ---

    @Test
    void getEntityTrail_shouldReturnTrailForEntity() {
        String entityType = "Member";
        UUID entityId = UUID.randomUUID();
        List<AuditEvent> events = createAuditEventList(2);
        CursorPage<AuditEvent> cursorPage = CursorPage.of(events, null, false);

        when(auditService.getEntityTrail(eq(entityType), eq(entityId), eq(null), eq(50)))
                .thenReturn(cursorPage);

        ApiResponse<CursorPage<AuditEventResponse>> response = auditController.getEntityTrail(
                entityType, entityId, null, 50);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getItems()).hasSize(2);
    }

    @Test
    void getEntityTrail_withCursor_shouldPassCursor() {
        UUID cursor = UUID.randomUUID();
        String entityType = "Loan";
        UUID entityId = UUID.randomUUID();

        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.getEntityTrail(eq(entityType), eq(entityId), eq(cursor), eq(50)))
                .thenReturn(cursorPage);

        auditController.getEntityTrail(entityType, entityId, cursor, 50);

        verify(auditService).getEntityTrail(eq(entityType), eq(entityId), eq(cursor), eq(50));
    }

    @Test
    void getEntityTrail_withNullLimit_shouldDefaultTo50() {
        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.getEntityTrail(any(), any(), any(), eq(50)))
                .thenReturn(cursorPage);

        auditController.getEntityTrail("Test", UUID.randomUUID(), null, null);

        verify(auditService).getEntityTrail(any(), any(), any(), eq(50));
    }

    @Test
    void getEntityTrail_withZeroLimit_shouldDefaultTo50() {
        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.getEntityTrail(any(), any(), any(), eq(50)))
                .thenReturn(cursorPage);

        auditController.getEntityTrail("Test", UUID.randomUUID(), null, 0);

        verify(auditService).getEntityTrail(any(), any(), any(), eq(50));
    }

    @Test
    void getEntityTrail_withLimitOver1000_shouldClampTo1000() {
        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.getEntityTrail(any(), any(), any(), eq(1000)))
                .thenReturn(cursorPage);

        auditController.getEntityTrail("Test", UUID.randomUUID(), null, 5000);

        verify(auditService).getEntityTrail(any(), any(), any(), eq(1000));
    }

    // --- exportAuditEvents tests ---

    @Test
    void exportAuditEvents_shouldReturnCsvBytes() {
        UUID eventId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2025-01-15T10:30:00Z");

        AuditEvent event = AuditEvent.builder()
                .id(eventId)
                .actor("admin")
                .actorName("Admin User")
                .action(AuditAction.CREATE)
                .entityType("Member")
                .entityId(entityId)
                .ipAddress("192.168.1.1")
                .timestamp(timestamp)
                .build();

        CursorPage<AuditEvent> cursorPage = CursorPage.of(List.of(event), null, false);
        when(auditService.queryEvents(eq(null), any(), any(), any(), any(), any(), any(), eq(10000)))
                .thenReturn(cursorPage);

        ResponseEntity<byte[]> response = auditController.exportAuditEvents(
                null, null, null, null, null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
        String csv = new String(response.getBody());
        assertThat(csv).contains("ID,Timestamp,Actor,Actor Name,Action,Entity Type,Entity ID,IP Address");
        assertThat(csv).contains(eventId.toString());
        assertThat(csv).contains("admin");
        assertThat(csv).contains("CREATE");
        assertThat(csv).contains("Member");
    }

    @Test
    void exportAuditEvents_withFilters_shouldPassToService() {
        String entityType = "Loan";
        UUID entityId = UUID.randomUUID();
        String actor = "admin";
        AuditAction action = AuditAction.APPROVE;
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.queryEvents(eq(null), eq(entityType), eq(entityId), eq(actor),
                eq(action), eq(startDate), eq(endDate), eq(10000)))
                .thenReturn(cursorPage);

        auditController.exportAuditEvents(entityType, entityId, actor, action, startDate, endDate);

        verify(auditService).queryEvents(eq(null), eq(entityType), eq(entityId), eq(actor),
                eq(action), eq(startDate), eq(endDate), eq(10000));
    }

    @Test
    void exportAuditEvents_withEmptyResults_shouldReturnCsvHeaderOnly() {
        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(cursorPage);

        ResponseEntity<byte[]> response = auditController.exportAuditEvents(
                null, null, null, null, null, null);

        String csv = new String(response.getBody());
        assertThat(csv).contains("ID,Timestamp,Actor,Actor Name,Action,Entity Type,Entity ID,IP Address");
        // Only header line
        String[] lines = csv.strip().split("\n");
        assertThat(lines).hasSize(1);
    }

    @Test
    void exportAuditEvents_withSpecialCharacters_shouldEscapeCsvValues() {
        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID())
                .actor("admin,user")
                .actorName("Admin \"Super\" User")
                .action(AuditAction.CREATE)
                .entityType("Member")
                .entityId(UUID.randomUUID())
                .ipAddress(null)
                .timestamp(Instant.now())
                .build();

        CursorPage<AuditEvent> cursorPage = CursorPage.of(List.of(event), null, false);
        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(cursorPage);

        ResponseEntity<byte[]> response = auditController.exportAuditEvents(
                null, null, null, null, null, null);

        String csv = new String(response.getBody());
        // Comma in value should be quoted
        assertThat(csv).contains("\"admin,user\"");
        // Quote in value should be escaped
        assertThat(csv).contains("\"Admin \"\"Super\"\" User\"");
    }

    @Test
    void exportAuditEvents_contentDispositionShouldContainFilename() {
        CursorPage<AuditEvent> cursorPage = CursorPage.of(Collections.emptyList(), null, false);
        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(cursorPage);

        ResponseEntity<byte[]> response = auditController.exportAuditEvents(
                null, null, null, null, null, null);

        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertThat(contentDisposition).contains("audit-events-");
        assertThat(contentDisposition).contains(".csv");
    }

    @Test
    void exportAuditEvents_withMultipleEvents_shouldHaveAllRows() {
        List<AuditEvent> events = createAuditEventList(5);
        CursorPage<AuditEvent> cursorPage = CursorPage.of(events, null, false);
        when(auditService.queryEvents(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(cursorPage);

        ResponseEntity<byte[]> response = auditController.exportAuditEvents(
                null, null, null, null, null, null);

        String csv = new String(response.getBody());
        String[] lines = csv.strip().split("\n");
        // 1 header + 5 data rows
        assertThat(lines).hasSize(6);
    }

    // --- Helpers ---

    private List<AuditEvent> createAuditEventList(int count) {
        List<AuditEvent> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(AuditEvent.builder()
                    .id(UUID.randomUUID())
                    .actor("actor" + i)
                    .actorName("Actor " + i)
                    .action(AuditAction.CREATE)
                    .entityType("TestEntity")
                    .entityId(UUID.randomUUID())
                    .ipAddress("10.0.0." + i)
                    .timestamp(Instant.now())
                    .build());
        }
        return events;
    }
}
