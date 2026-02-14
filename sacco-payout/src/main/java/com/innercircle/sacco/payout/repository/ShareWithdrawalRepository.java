package com.innercircle.sacco.payout.repository;

import com.innercircle.sacco.payout.entity.ShareWithdrawal;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalStatus;
import com.innercircle.sacco.payout.entity.ShareWithdrawal.ShareWithdrawalType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShareWithdrawalRepository extends JpaRepository<ShareWithdrawal, UUID> {

    @Query("SELECT s FROM ShareWithdrawal s WHERE s.memberId = :memberId AND s.id > :cursor ORDER BY s.id")
    List<ShareWithdrawal> findByMemberIdWithCursor(
            @Param("memberId") UUID memberId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT s FROM ShareWithdrawal s WHERE s.memberId = :memberId ORDER BY s.id")
    List<ShareWithdrawal> findByMemberId(@Param("memberId") UUID memberId, Pageable pageable);

    @Query("SELECT s FROM ShareWithdrawal s WHERE s.status = :status AND s.id > :cursor ORDER BY s.id")
    List<ShareWithdrawal> findByStatusWithCursor(
            @Param("status") ShareWithdrawalStatus status,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT s FROM ShareWithdrawal s WHERE s.status = :status ORDER BY s.id")
    List<ShareWithdrawal> findByStatus(@Param("status") ShareWithdrawalStatus status, Pageable pageable);

    @Query("SELECT s FROM ShareWithdrawal s WHERE s.withdrawalType = :type AND s.id > :cursor ORDER BY s.id")
    List<ShareWithdrawal> findByWithdrawalTypeWithCursor(
            @Param("type") ShareWithdrawalType type,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT s FROM ShareWithdrawal s WHERE s.withdrawalType = :type ORDER BY s.id")
    List<ShareWithdrawal> findByWithdrawalType(@Param("type") ShareWithdrawalType type, Pageable pageable);

    @Query("SELECT s FROM ShareWithdrawal s WHERE s.memberId = :memberId AND s.status = :status AND s.id > :cursor ORDER BY s.id")
    List<ShareWithdrawal> findByMemberIdAndStatusWithCursor(
            @Param("memberId") UUID memberId,
            @Param("status") ShareWithdrawalStatus status,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT s FROM ShareWithdrawal s WHERE s.memberId = :memberId AND s.status = :status ORDER BY s.id")
    List<ShareWithdrawal> findByMemberIdAndStatus(
            @Param("memberId") UUID memberId,
            @Param("status") ShareWithdrawalStatus status,
            Pageable pageable
    );

    @Query("SELECT s FROM ShareWithdrawal s WHERE s.id > :cursor ORDER BY s.id")
    List<ShareWithdrawal> findAllWithCursor(@Param("cursor") UUID cursor, Pageable pageable);

    @Query("SELECT s FROM ShareWithdrawal s ORDER BY s.id")
    List<ShareWithdrawal> findAllPaged(Pageable pageable);
}
