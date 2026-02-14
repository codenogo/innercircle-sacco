package com.innercircle.sacco.audit.service;

import com.innercircle.sacco.audit.entity.AuditAction;
import com.innercircle.sacco.audit.entity.AuditEvent;
import com.innercircle.sacco.audit.repository.AuditEventRepository;
import com.innercircle.sacco.common.dto.CursorPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of audit service.
 * Provides functionality for creating and querying audit events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditEventRepository auditEventRepository;

    @Override
    @Transactional
    public AuditEvent logEvent(
            String actor,
            String actorName,
            AuditAction action,
            String entityType,
            UUID entityId,
            String beforeSnapshot,
            String afterSnapshot,
            String ipAddress
    ) {
        AuditEvent auditEvent = AuditEvent.builder()
                .actor(actor)
                .actorName(actorName)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeSnapshot(beforeSnapshot)
                .afterSnapshot(afterSnapshot)
                .ipAddress(ipAddress)
                .build();

        AuditEvent saved = auditEventRepository.save(auditEvent);

        log.debug("Audit event logged: actor={}, action={}, entityType={}, entityId={}",
                actor, action, entityType, entityId);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<AuditEvent> queryEvents(
            UUID cursor,
            String entityType,
            UUID entityId,
            String actor,
            AuditAction action,
            Instant startDate,
            Instant endDate,
            int limit
    ) {
        // Fetch one extra to determine if there are more results
        int fetchSize = limit + 1;
        PageRequest pageRequest = PageRequest.of(0, fetchSize);

        List<AuditEvent> events = auditEventRepository.findWithFilters(
                cursor,
                entityType,
                entityId,
                actor,
                action,
                startDate,
                endDate,
                pageRequest
        );

        boolean hasMore = events.size() > limit;
        List<AuditEvent> items = hasMore ? events.subList(0, limit) : events;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<AuditEvent> getEntityTrail(
            String entityType,
            UUID entityId,
            UUID cursor,
            int limit
    ) {
        // Fetch one extra to determine if there are more results
        int fetchSize = limit + 1;
        PageRequest pageRequest = PageRequest.of(0, fetchSize);

        List<AuditEvent> events = auditEventRepository.findByEntityTypeAndEntityId(
                entityType,
                entityId,
                cursor,
                pageRequest
        );

        boolean hasMore = events.size() > limit;
        List<AuditEvent> items = hasMore ? events.subList(0, limit) : events;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasMore);
    }
}
