package com.innercircle.sacco.common.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventOutboxRepository extends JpaRepository<EventOutbox, UUID> {

    List<EventOutbox> findByStatusOrderByCreatedAtAsc(EventOutboxStatus status, Pageable pageable);
}
