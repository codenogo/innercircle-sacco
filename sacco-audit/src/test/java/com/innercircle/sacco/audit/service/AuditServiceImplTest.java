package com.innercircle.sacco.audit.service;

import com.innercircle.sacco.audit.entity.AuditAction;
import com.innercircle.sacco.audit.entity.AuditEvent;
import com.innercircle.sacco.audit.repository.AuditEventRepository;
import com.innercircle.sacco.common.dto.CursorPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    // --- logEvent tests ---

    @Test
    void logEvent_shouldCreateAndSaveAuditEvent() {
        String actor = "user@example.com";
        String actorName = "John Doe";
        AuditAction action = AuditAction.CREATE;
        String entityType = "Member";
        UUID entityId = UUID.randomUUID();
        String beforeSnapshot = null;
        String afterSnapshot = "{\"name\":\"John\"}";
        String ipAddress = "192.168.1.1";

        AuditEvent savedEvent = AuditEvent.builder()
                .id(UUID.randomUUID())
                .actor(actor)
                .actorName(actorName)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeSnapshot(beforeSnapshot)
                .afterSnapshot(afterSnapshot)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(savedEvent);

        AuditEvent result = auditService.logEvent(actor, actorName, action, entityType, entityId,
                beforeSnapshot, afterSnapshot, ipAddress);

        assertThat(result).isNotNull();
        assertThat(result.getActor()).isEqualTo(actor);
        assertThat(result.getActorName()).isEqualTo(actorName);
        assertThat(result.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(result.getEntityType()).isEqualTo(entityType);
        assertThat(result.getEntityId()).isEqualTo(entityId);
        assertThat(result.getAfterSnapshot()).isEqualTo(afterSnapshot);
        assertThat(result.getIpAddress()).isEqualTo(ipAddress);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent captured = captor.getValue();
        assertThat(captured.getActor()).isEqualTo(actor);
        assertThat(captured.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(captured.getEntityType()).isEqualTo(entityType);
        assertThat(captured.getEntityId()).isEqualTo(entityId);
    }

    @Test
    void logEvent_withNullOptionalFields_shouldStillSave() {
        String actor = "system";
        AuditAction action = AuditAction.UPDATE;
        String entityType = "Loan";
        UUID entityId = UUID.randomUUID();

        AuditEvent savedEvent = AuditEvent.builder()
                .id(UUID.randomUUID())
                .actor(actor)
                .actorName(null)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .timestamp(Instant.now())
                .build();

        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(savedEvent);

        AuditEvent result = auditService.logEvent(actor, null, action, entityType, entityId,
                null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getActorName()).isNull();
        assertThat(result.getBeforeSnapshot()).isNull();
        assertThat(result.getAfterSnapshot()).isNull();
        assertThat(result.getIpAddress()).isNull();
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    void logEvent_withAllAuditActions_shouldCreateCorrectEvents() {
        for (AuditAction action : AuditAction.values()) {
            UUID entityId = UUID.randomUUID();
            AuditEvent savedEvent = AuditEvent.builder()
                    .id(UUID.randomUUID())
                    .actor("admin")
                    .action(action)
                    .entityType("TestEntity")
                    .entityId(entityId)
                    .timestamp(Instant.now())
                    .build();

            when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(savedEvent);

            AuditEvent result = auditService.logEvent("admin", "Admin", action, "TestEntity",
                    entityId, null, null, null);

            assertThat(result.getAction()).isEqualTo(action);
        }
    }

    @Test
    void logEvent_withBeforeAndAfterSnapshots_shouldPreserveBoth() {
        String beforeSnapshot = "{\"status\":\"PENDING\"}";
        String afterSnapshot = "{\"status\":\"APPROVED\"}";
        UUID entityId = UUID.randomUUID();

        AuditEvent savedEvent = AuditEvent.builder()
                .id(UUID.randomUUID())
                .actor("admin")
                .actorName("Admin User")
                .action(AuditAction.APPROVE)
                .entityType("Loan")
                .entityId(entityId)
                .beforeSnapshot(beforeSnapshot)
                .afterSnapshot(afterSnapshot)
                .timestamp(Instant.now())
                .build();

        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(savedEvent);

        AuditEvent result = auditService.logEvent("admin", "Admin User", AuditAction.APPROVE,
                "Loan", entityId, beforeSnapshot, afterSnapshot, null);

        assertThat(result.getBeforeSnapshot()).isEqualTo(beforeSnapshot);
        assertThat(result.getAfterSnapshot()).isEqualTo(afterSnapshot);
    }

    // --- queryEvents tests ---

    @Test
    void queryEvents_withNoFilters_shouldReturnPagedResults() {
        int limit = 10;
        UUID eventId = UUID.randomUUID();
        List<AuditEvent> events = createAuditEventList(limit);

        when(auditEventRepository.findWithFilters(
                eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(events);

        CursorPage<AuditEvent> result = auditService.queryEvents(
                null, null, null, null, null, null, null, limit);

        assertThat(result.getItems()).hasSize(limit);
        assertThat(result.isHasMore()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

    @Test
    void queryEvents_withMoreResults_shouldReturnHasMoreTrue() {
        int limit = 5;
        // Return limit + 1 results to indicate there are more
        List<AuditEvent> events = createAuditEventList(limit + 1);

        when(auditEventRepository.findWithFilters(
                any(), any(), any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(events);

        CursorPage<AuditEvent> result = auditService.queryEvents(
                null, null, null, null, null, null, null, limit);

        assertThat(result.getItems()).hasSize(limit);
        assertThat(result.isHasMore()).isTrue();
        assertThat(result.getNextCursor()).isNotNull();
    }

    @Test
    void queryEvents_withEntityTypeFilter_shouldPassFilterToRepository() {
        String entityType = "Member";
        int limit = 20;

        when(auditEventRepository.findWithFilters(
                eq(null), eq(entityType), eq(null), eq(null), eq(null),
                eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        CursorPage<AuditEvent> result = auditService.queryEvents(
                null, entityType, null, null, null, null, null, limit);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.isHasMore()).isFalse();
        verify(auditEventRepository).findWithFilters(
                eq(null), eq(entityType), eq(null), eq(null), eq(null),
                eq(null), eq(null), any(PageRequest.class));
    }

    @Test
    void queryEvents_withAllFilters_shouldPassAllToRepository() {
        UUID cursor = UUID.randomUUID();
        String entityType = "Loan";
        UUID entityId = UUID.randomUUID();
        String actor = "admin";
        AuditAction action = AuditAction.APPROVE;
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();
        int limit = 25;

        when(auditEventRepository.findWithFilters(
                eq(cursor), eq(entityType), eq(entityId), eq(actor), eq(action),
                eq(startDate), eq(endDate), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        CursorPage<AuditEvent> result = auditService.queryEvents(
                cursor, entityType, entityId, actor, action, startDate, endDate, limit);

        assertThat(result.getItems()).isEmpty();
        verify(auditEventRepository).findWithFilters(
                eq(cursor), eq(entityType), eq(entityId), eq(actor), eq(action),
                eq(startDate), eq(endDate), any(PageRequest.class));
    }

    @Test
    void queryEvents_shouldFetchOnMoreThanLimit() {
        int limit = 10;

        when(auditEventRepository.findWithFilters(
                any(), any(), any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        auditService.queryEvents(null, null, null, null, null, null, null, limit);

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(auditEventRepository).findWithFilters(
                any(), any(), any(), any(), any(), any(), any(), pageRequestCaptor.capture());

        PageRequest capturedPageRequest = pageRequestCaptor.getValue();
        assertThat(capturedPageRequest.getPageSize()).isEqualTo(limit + 1);
        assertThat(capturedPageRequest.getPageNumber()).isEqualTo(0);
    }

    @Test
    void queryEvents_withExactLimitResults_shouldNotHaveMore() {
        int limit = 3;
        List<AuditEvent> events = createAuditEventList(3);

        when(auditEventRepository.findWithFilters(
                any(), any(), any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(events);

        CursorPage<AuditEvent> result = auditService.queryEvents(
                null, null, null, null, null, null, null, limit);

        assertThat(result.getItems()).hasSize(3);
        assertThat(result.isHasMore()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

    @Test
    void queryEvents_withCursor_shouldPassCursorToRepository() {
        UUID cursor = UUID.randomUUID();

        when(auditEventRepository.findWithFilters(
                eq(cursor), any(), any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        auditService.queryEvents(cursor, null, null, null, null, null, null, 10);

        verify(auditEventRepository).findWithFilters(
                eq(cursor), any(), any(), any(), any(), any(), any(), any(PageRequest.class));
    }

    // --- getEntityTrail tests ---

    @Test
    void getEntityTrail_shouldReturnTrailForEntity() {
        String entityType = "Member";
        UUID entityId = UUID.randomUUID();
        int limit = 10;
        List<AuditEvent> events = createAuditEventList(3);

        when(auditEventRepository.findByEntityTypeAndEntityId(
                eq(entityType), eq(entityId), eq(null), any(PageRequest.class)))
                .thenReturn(events);

        CursorPage<AuditEvent> result = auditService.getEntityTrail(entityType, entityId, null, limit);

        assertThat(result.getItems()).hasSize(3);
        assertThat(result.isHasMore()).isFalse();
    }

    @Test
    void getEntityTrail_withCursor_shouldPassCursor() {
        String entityType = "Loan";
        UUID entityId = UUID.randomUUID();
        UUID cursor = UUID.randomUUID();
        int limit = 5;

        when(auditEventRepository.findByEntityTypeAndEntityId(
                eq(entityType), eq(entityId), eq(cursor), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        CursorPage<AuditEvent> result = auditService.getEntityTrail(entityType, entityId, cursor, limit);

        assertThat(result.getItems()).isEmpty();
        verify(auditEventRepository).findByEntityTypeAndEntityId(
                eq(entityType), eq(entityId), eq(cursor), any(PageRequest.class));
    }

    @Test
    void getEntityTrail_withMoreResults_shouldReturnHasMore() {
        String entityType = "Contribution";
        UUID entityId = UUID.randomUUID();
        int limit = 2;
        List<AuditEvent> events = createAuditEventList(limit + 1);

        when(auditEventRepository.findByEntityTypeAndEntityId(
                eq(entityType), eq(entityId), eq(null), any(PageRequest.class)))
                .thenReturn(events);

        CursorPage<AuditEvent> result = auditService.getEntityTrail(entityType, entityId, null, limit);

        assertThat(result.getItems()).hasSize(limit);
        assertThat(result.isHasMore()).isTrue();
        assertThat(result.getNextCursor()).isNotNull();
    }

    @Test
    void getEntityTrail_shouldFetchLimitPlusOne() {
        int limit = 15;

        when(auditEventRepository.findByEntityTypeAndEntityId(
                any(), any(), any(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        auditService.getEntityTrail("Member", UUID.randomUUID(), null, limit);

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(auditEventRepository).findByEntityTypeAndEntityId(
                any(), any(), any(), pageRequestCaptor.capture());

        assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(limit + 1);
    }

    @Test
    void getEntityTrail_nextCursorShouldBeLastItemId() {
        int limit = 2;
        UUID lastId = UUID.randomUUID();
        List<AuditEvent> events = new ArrayList<>();
        events.add(createAuditEvent(UUID.randomUUID()));
        events.add(createAuditEvent(lastId));
        events.add(createAuditEvent(UUID.randomUUID())); // extra to trigger hasMore

        when(auditEventRepository.findByEntityTypeAndEntityId(
                any(), any(), any(), any(PageRequest.class)))
                .thenReturn(events);

        CursorPage<AuditEvent> result = auditService.getEntityTrail("Test", UUID.randomUUID(), null, limit);

        assertThat(result.getNextCursor()).isEqualTo(lastId.toString());
    }

    // --- Helpers ---

    private List<AuditEvent> createAuditEventList(int count) {
        List<AuditEvent> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(createAuditEvent(UUID.randomUUID()));
        }
        return events;
    }

    private AuditEvent createAuditEvent(UUID id) {
        return AuditEvent.builder()
                .id(id)
                .actor("admin")
                .actorName("Admin")
                .action(AuditAction.CREATE)
                .entityType("Test")
                .entityId(UUID.randomUUID())
                .timestamp(Instant.now())
                .build();
    }
}
