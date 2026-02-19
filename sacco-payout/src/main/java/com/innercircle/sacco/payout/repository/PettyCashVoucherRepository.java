package com.innercircle.sacco.payout.repository;

import com.innercircle.sacco.payout.entity.PettyCashVoucher;
import com.innercircle.sacco.payout.entity.PettyCashVoucherStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PettyCashVoucherRepository extends JpaRepository<PettyCashVoucher, UUID> {

    @Query("SELECT v FROM PettyCashVoucher v ORDER BY v.id")
    List<PettyCashVoucher> findAllPaged(Pageable pageable);

    @Query("SELECT v FROM PettyCashVoucher v WHERE v.id > :cursor ORDER BY v.id")
    List<PettyCashVoucher> findAllWithCursor(@Param("cursor") UUID cursor, Pageable pageable);

    @Query("SELECT v FROM PettyCashVoucher v WHERE v.status = :status ORDER BY v.id")
    List<PettyCashVoucher> findByStatus(@Param("status") PettyCashVoucherStatus status, Pageable pageable);

    @Query("SELECT v FROM PettyCashVoucher v WHERE v.status = :status AND v.id > :cursor ORDER BY v.id")
    List<PettyCashVoucher> findByStatusWithCursor(
            @Param("status") PettyCashVoucherStatus status,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("""
            SELECT v
            FROM PettyCashVoucher v
            WHERE v.requestDate BETWEEN :startDate AND :endDate
            ORDER BY v.id
            """)
    List<PettyCashVoucher> findByRequestDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
            SELECT v
            FROM PettyCashVoucher v
            WHERE v.requestDate BETWEEN :startDate AND :endDate
              AND v.id > :cursor
            ORDER BY v.id
            """)
    List<PettyCashVoucher> findByRequestDateBetweenWithCursor(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("""
            SELECT v
            FROM PettyCashVoucher v
            WHERE v.status = :status
              AND v.requestDate BETWEEN :startDate AND :endDate
            ORDER BY v.id
            """)
    List<PettyCashVoucher> findByStatusAndRequestDateBetween(
            @Param("status") PettyCashVoucherStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
            SELECT v
            FROM PettyCashVoucher v
            WHERE v.status = :status
              AND v.requestDate BETWEEN :startDate AND :endDate
              AND v.id > :cursor
            ORDER BY v.id
            """)
    List<PettyCashVoucher> findByStatusAndRequestDateBetweenWithCursor(
            @Param("status") PettyCashVoucherStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    boolean existsByReferenceNumber(String referenceNumber);

    boolean existsByReceiptNumber(String receiptNumber);
}
