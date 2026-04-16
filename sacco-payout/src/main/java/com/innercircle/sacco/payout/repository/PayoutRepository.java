package com.innercircle.sacco.payout.repository;

import com.innercircle.sacco.payout.entity.Payout;
import com.innercircle.sacco.payout.entity.PayoutStatus;
import com.innercircle.sacco.payout.entity.PayoutType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    @Query("SELECT p FROM Payout p WHERE p.memberId = :memberId AND p.id > :cursor ORDER BY p.id")
    List<Payout> findByMemberIdWithCursor(
            @Param("memberId") UUID memberId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT p FROM Payout p WHERE p.memberId = :memberId ORDER BY p.id")
    List<Payout> findByMemberId(@Param("memberId") UUID memberId, Pageable pageable);

    @Query("SELECT p FROM Payout p WHERE p.status = :status AND p.id > :cursor ORDER BY p.id")
    List<Payout> findByStatusWithCursor(
            @Param("status") PayoutStatus status,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT p FROM Payout p WHERE p.status = :status ORDER BY p.id")
    List<Payout> findByStatus(@Param("status") PayoutStatus status, Pageable pageable);

    @Query("SELECT p FROM Payout p WHERE p.type = :type AND p.id > :cursor ORDER BY p.id")
    List<Payout> findByTypeWithCursor(
            @Param("type") PayoutType type,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT p FROM Payout p WHERE p.type = :type ORDER BY p.id")
    List<Payout> findByType(@Param("type") PayoutType type, Pageable pageable);

    @Query("SELECT p FROM Payout p WHERE p.memberId = :memberId AND p.status = :status AND p.id > :cursor ORDER BY p.id")
    List<Payout> findByMemberIdAndStatusWithCursor(
            @Param("memberId") UUID memberId,
            @Param("status") PayoutStatus status,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT p FROM Payout p WHERE p.memberId = :memberId AND p.status = :status ORDER BY p.id")
    List<Payout> findByMemberIdAndStatus(
            @Param("memberId") UUID memberId,
            @Param("status") PayoutStatus status,
            Pageable pageable
    );

    @Query("SELECT p FROM Payout p WHERE p.id > :cursor ORDER BY p.id")
    List<Payout> findAllWithCursor(@Param("cursor") UUID cursor, Pageable pageable);

    @Query("SELECT p FROM Payout p ORDER BY p.id")
    List<Payout> findAllPaged(Pageable pageable);

    boolean existsByReferenceNumber(String referenceNumber);
}
