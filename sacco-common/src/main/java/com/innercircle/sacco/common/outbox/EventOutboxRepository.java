package com.innercircle.sacco.common.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventOutboxRepository extends JpaRepository<EventOutbox, UUID> {

    List<EventOutbox> findByStatusOrderByCreatedAtAsc(EventOutboxStatus status, Pageable pageable);

    @Query("select eo.id from EventOutbox eo where eo.status = :status order by eo.createdAt asc")
    List<UUID> findIdsByStatusOrderByCreatedAtAsc(@Param("status") EventOutboxStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select eo from EventOutbox eo where eo.id = :id")
    Optional<EventOutbox> findByIdForUpdate(@Param("id") UUID id);
}
