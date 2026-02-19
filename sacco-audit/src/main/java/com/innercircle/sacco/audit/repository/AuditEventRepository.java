package com.innercircle.sacco.audit.repository;

import com.innercircle.sacco.audit.entity.AuditAction;
import com.innercircle.sacco.audit.entity.AuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for querying immutable audit events.
 * Supports cursor-based pagination and filtering by various criteria.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    /**
     * Find audit events for a specific entity with cursor pagination.
     */
    @Query("SELECT ae FROM AuditEvent ae " +
           "WHERE ae.entityType = :entityType " +
           "AND ae.entityId = :entityId " +
           "AND (:cursor IS NULL OR ae.id > :cursor) " +
           "ORDER BY ae.id ASC")
    List<AuditEvent> findByEntityTypeAndEntityId(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    /**
     * Find all audit events with optional filters and cursor pagination.
     */
    @Query("SELECT ae FROM AuditEvent ae " +
           "WHERE (:cursor IS NULL OR ae.id > :cursor) " +
           "AND (:entityType IS NULL OR ae.entityType = :entityType) " +
           "AND (:entityId IS NULL OR ae.entityId = :entityId) " +
           "AND (:actor IS NULL OR ae.actor = :actor) " +
           "AND (:action IS NULL OR ae.action = :action) " +
           "AND ae.timestamp >= COALESCE(:startDate, ae.timestamp) " +
           "AND ae.timestamp <= COALESCE(:endDate, ae.timestamp) " +
           "ORDER BY ae.id ASC")
    List<AuditEvent> findWithFilters(
            @Param("cursor") UUID cursor,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("actor") String actor,
            @Param("action") AuditAction action,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    /**
     * Find audit events by actor with cursor pagination.
     */
    @Query("SELECT ae FROM AuditEvent ae " +
           "WHERE ae.actor = :actor " +
           "AND (:cursor IS NULL OR ae.id > :cursor) " +
           "ORDER BY ae.id ASC")
    List<AuditEvent> findByActor(
            @Param("actor") String actor,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    /**
     * Find audit events by action with cursor pagination.
     */
    @Query("SELECT ae FROM AuditEvent ae " +
           "WHERE ae.action = :action " +
           "AND (:cursor IS NULL OR ae.id > :cursor) " +
           "ORDER BY ae.id ASC")
    List<AuditEvent> findByAction(
            @Param("action") AuditAction action,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    /**
     * Find audit events within a date range with cursor pagination.
     */
    @Query("SELECT ae FROM AuditEvent ae " +
           "WHERE ae.timestamp BETWEEN :startDate AND :endDate " +
           "AND (:cursor IS NULL OR ae.id > :cursor) " +
           "ORDER BY ae.id ASC")
    List<AuditEvent> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );
}
