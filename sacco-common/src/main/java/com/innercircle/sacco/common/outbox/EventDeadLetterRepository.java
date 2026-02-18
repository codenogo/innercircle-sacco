package com.innercircle.sacco.common.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventDeadLetterRepository extends JpaRepository<EventDeadLetter, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    List<EventDeadLetter> findByStatusAndNextRetryAtBeforeAndRetriesLessThan(
            EventDeadLetterStatus status, Instant now, int maxRetries, Pageable pageable);

    Optional<EventDeadLetter> findByOutboxId(UUID outboxId);
}
