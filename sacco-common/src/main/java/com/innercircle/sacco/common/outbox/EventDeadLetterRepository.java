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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventDeadLetterRepository extends JpaRepository<EventDeadLetter, UUID> {

    List<EventDeadLetter> findByStatusAndNextRetryAtBeforeAndRetriesLessThan(
            EventDeadLetterStatus status, Instant now, int maxRetries, Pageable pageable);

    @Query("select dl.id from EventDeadLetter dl " +
            "where dl.status = :status and dl.nextRetryAt <= :now and dl.retries < :maxRetries " +
            "order by dl.createdAt asc")
    List<UUID> findIdsByStatusAndNextRetryAtBeforeAndRetriesLessThan(
            @Param("status") EventDeadLetterStatus status,
            @Param("now") Instant now,
            @Param("maxRetries") int maxRetries,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select dl from EventDeadLetter dl where dl.id = :id")
    Optional<EventDeadLetter> findByIdForUpdate(@Param("id") UUID id);

    Optional<EventDeadLetter> findByOutboxId(UUID outboxId);
}
