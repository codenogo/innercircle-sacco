package com.innercircle.sacco.ledger.repository;

import com.innercircle.sacco.ledger.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {

    List<JournalLine> findByJournalEntryId(UUID journalEntryId);

    List<JournalLine> findByAccountId(UUID accountId);

    @Query("SELECT jl FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.journalEntry.posted = true AND jl.journalEntry.transactionDate BETWEEN :startDate AND :endDate")
    List<JournalLine> findPostedLinesByAccountAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(jl.debitAmount), 0) - COALESCE(SUM(jl.creditAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.journalEntry.posted = true")
    BigDecimal calculateAccountBalance(@Param("accountId") UUID accountId);
}
