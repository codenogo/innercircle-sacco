package com.innercircle.sacco.payout.repository;

import com.innercircle.sacco.payout.entity.CashDisbursement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashDisbursementRepository extends JpaRepository<CashDisbursement, UUID> {

    @Query("SELECT c FROM CashDisbursement c WHERE c.memberId = :memberId AND c.id > :cursor ORDER BY c.id")
    List<CashDisbursement> findByMemberIdWithCursor(
            @Param("memberId") UUID memberId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT c FROM CashDisbursement c WHERE c.memberId = :memberId ORDER BY c.id")
    List<CashDisbursement> findByMemberId(@Param("memberId") UUID memberId, Pageable pageable);

    @Query("SELECT c FROM CashDisbursement c WHERE c.disbursementDate = :date AND c.id > :cursor ORDER BY c.id")
    List<CashDisbursement> findByDisbursementDateWithCursor(
            @Param("date") LocalDate date,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT c FROM CashDisbursement c WHERE c.disbursementDate = :date ORDER BY c.id")
    List<CashDisbursement> findByDisbursementDate(@Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT c FROM CashDisbursement c WHERE c.disbursementDate BETWEEN :startDate AND :endDate AND c.id > :cursor ORDER BY c.id")
    List<CashDisbursement> findByDisbursementDateBetweenWithCursor(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT c FROM CashDisbursement c WHERE c.disbursementDate BETWEEN :startDate AND :endDate ORDER BY c.id")
    List<CashDisbursement> findByDisbursementDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    Optional<CashDisbursement> findByReceiptNumber(String receiptNumber);

    @Query("SELECT c FROM CashDisbursement c WHERE c.id > :cursor ORDER BY c.id")
    List<CashDisbursement> findAllWithCursor(@Param("cursor") UUID cursor, Pageable pageable);

    @Query("SELECT c FROM CashDisbursement c ORDER BY c.id")
    List<CashDisbursement> findAllPaged(Pageable pageable);
}
