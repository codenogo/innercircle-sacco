package com.innercircle.sacco.loan.repository;

import com.innercircle.sacco.loan.entity.BatchProcessingLog;
import com.innercircle.sacco.loan.entity.BatchProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchProcessingLogRepository extends JpaRepository<BatchProcessingLog, UUID> {
    Optional<BatchProcessingLog> findByProcessingMonth(String processingMonth);
    boolean existsByProcessingMonth(String processingMonth);
    Optional<BatchProcessingLog> findTopByStatusOrderByProcessingMonthDesc(BatchProcessingStatus status);
}
