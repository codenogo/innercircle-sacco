package com.innercircle.sacco.audit.controller;

import com.innercircle.sacco.audit.dto.AuditEventResponse;
import com.innercircle.sacco.audit.dto.AuditQueryRequest;
import com.innercircle.sacco.audit.entity.AuditAction;
import com.innercircle.sacco.audit.entity.AuditEvent;
import com.innercircle.sacco.audit.service.AuditService;
import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for audit trail queries.
 * Provides endpoints for viewing and exporting audit events.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    /**
     * List audit events with cursor pagination and optional filters.
     * Requires ADMIN or TREASURER role.
     *
     * @param cursor Cursor for pagination
     * @param entityType Filter by entity type
     * @param entityId Filter by entity ID
     * @param actor Filter by actor
     * @param action Filter by action
     * @param startDate Filter by start date
     * @param endDate Filter by end date
     * @param limit Maximum number of results (default: 50, max: 1000)
     * @return Cursor-paginated list of audit events
     */
    @GetMapping
    public ApiResponse<CursorPage<AuditEventResponse>> listAuditEvents(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        // Build query request
        AuditQueryRequest queryRequest = AuditQueryRequest.builder()
                .cursor(cursor)
                .entityType(entityType)
                .entityId(entityId)
                .actor(actor)
                .action(action)
                .startDate(startDate)
                .endDate(endDate)
                .limit(limit)
                .build();

        queryRequest.validate();

        // Query events
        CursorPage<AuditEvent> events = auditService.queryEvents(
                queryRequest.getCursor(),
                queryRequest.getEntityType(),
                queryRequest.getEntityId(),
                queryRequest.getActor(),
                queryRequest.getAction(),
                queryRequest.getStartDate(),
                queryRequest.getEndDate(),
                queryRequest.getLimit()
        );

        // Convert to response DTOs
        List<AuditEventResponse> responses = events.getItems().stream()
                .map(AuditEventResponse::from)
                .collect(Collectors.toList());

        CursorPage<AuditEventResponse> responsePage = CursorPage.of(
                responses,
                events.getNextCursor(),
                events.isHasMore()
        );

        return ApiResponse.ok(responsePage);
    }

    /**
     * Get the complete audit trail for a specific entity.
     * Requires ADMIN or TREASURER role.
     *
     * @param entityType Type of the entity
     * @param entityId ID of the entity
     * @param cursor Cursor for pagination
     * @param limit Maximum number of results (default: 50, max: 1000)
     * @return Cursor-paginated audit trail for the entity
     */
    @GetMapping("/{entityType}/{entityId}")
    public ApiResponse<CursorPage<AuditEventResponse>> getEntityTrail(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        if (limit == null || limit <= 0) {
            limit = 50;
        }
        if (limit > 1000) {
            limit = 1000;
        }

        CursorPage<AuditEvent> events = auditService.getEntityTrail(
                entityType,
                entityId,
                cursor,
                limit
        );

        List<AuditEventResponse> responses = events.getItems().stream()
                .map(AuditEventResponse::from)
                .collect(Collectors.toList());

        CursorPage<AuditEventResponse> responsePage = CursorPage.of(
                responses,
                events.getNextCursor(),
                events.isHasMore()
        );

        return ApiResponse.ok(responsePage);
    }

    /**
     * Export audit events as CSV.
     * Requires ADMIN role.
     *
     * @param entityType Filter by entity type
     * @param entityId Filter by entity ID
     * @param actor Filter by actor
     * @param action Filter by action
     * @param startDate Filter by start date
     * @param endDate Filter by end date
     * @return CSV file with filtered audit events
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAuditEvents(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate
    ) {
        // Fetch events with a reasonable limit for export (e.g., 10,000)
        CursorPage<AuditEvent> events = auditService.queryEvents(
                null,
                entityType,
                entityId,
                actor,
                action,
                startDate,
                endDate,
                10000
        );

        // Generate CSV
        byte[] csv = generateCsv(events.getItems());

        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment",
                "audit-events-" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    /**
     * Generate CSV from audit events.
     */
    private byte[] generateCsv(List<AuditEvent> events) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // Write CSV header
        writer.println("ID,Timestamp,Actor,Actor Name,Action,Entity Type,Entity ID,IP Address");

        // Write data rows
        for (AuditEvent event : events) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    escapeCsv(event.getId().toString()),
                    escapeCsv(event.getTimestamp().toString()),
                    escapeCsv(event.getActor()),
                    escapeCsv(event.getActorName()),
                    escapeCsv(event.getAction().toString()),
                    escapeCsv(event.getEntityType()),
                    escapeCsv(event.getEntityId().toString()),
                    escapeCsv(event.getIpAddress())
            );
        }

        writer.flush();
        return baos.toByteArray();
    }

    /**
     * Escape CSV value to handle commas and quotes.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
