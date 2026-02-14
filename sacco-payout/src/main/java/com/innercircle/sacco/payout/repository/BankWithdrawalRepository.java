package com.innercircle.sacco.payout.repository;

import com.innercircle.sacco.payout.entity.BankWithdrawal;
import com.innercircle.sacco.payout.entity.WithdrawalStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BankWithdrawalRepository extends JpaRepository<BankWithdrawal, UUID> {

    @Query("SELECT b FROM BankWithdrawal b WHERE b.memberId = :memberId AND b.id > :cursor ORDER BY b.id")
    List<BankWithdrawal> findByMemberIdWithCursor(
            @Param("memberId") UUID memberId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT b FROM BankWithdrawal b WHERE b.memberId = :memberId ORDER BY b.id")
    List<BankWithdrawal> findByMemberId(@Param("memberId") UUID memberId, Pageable pageable);

    @Query("SELECT b FROM BankWithdrawal b WHERE b.status = :status AND b.id > :cursor ORDER BY b.id")
    List<BankWithdrawal> findByStatusWithCursor(
            @Param("status") WithdrawalStatus status,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT b FROM BankWithdrawal b WHERE b.status = :status ORDER BY b.id")
    List<BankWithdrawal> findByStatus(@Param("status") WithdrawalStatus status, Pageable pageable);

    @Query("SELECT b FROM BankWithdrawal b WHERE b.reconciled = false AND b.id > :cursor ORDER BY b.id")
    List<BankWithdrawal> findUnreconciledWithCursor(@Param("cursor") UUID cursor, Pageable pageable);

    @Query("SELECT b FROM BankWithdrawal b WHERE b.reconciled = false ORDER BY b.id")
    List<BankWithdrawal> findUnreconciled(Pageable pageable);

    @Query("SELECT b FROM BankWithdrawal b WHERE b.transactionDate = :date AND b.id > :cursor ORDER BY b.id")
    List<BankWithdrawal> findByTransactionDateWithCursor(
            @Param("date") LocalDate date,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("SELECT b FROM BankWithdrawal b WHERE b.transactionDate = :date ORDER BY b.id")
    List<BankWithdrawal> findByTransactionDate(@Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT b FROM BankWithdrawal b WHERE b.id > :cursor ORDER BY b.id")
    List<BankWithdrawal> findAllWithCursor(@Param("cursor") UUID cursor, Pageable pageable);

    @Query("SELECT b FROM BankWithdrawal b ORDER BY b.id")
    List<BankWithdrawal> findAllPaged(Pageable pageable);
}
