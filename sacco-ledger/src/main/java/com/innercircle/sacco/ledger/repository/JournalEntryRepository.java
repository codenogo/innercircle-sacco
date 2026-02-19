package com.innercircle.sacco.ledger.repository;

import com.innercircle.sacco.ledger.entity.JournalEntry;
import com.innercircle.sacco.ledger.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID>, JpaSpecificationExecutor<JournalEntry> {

    Optional<JournalEntry> findByEntryNumber(String entryNumber);

    List<JournalEntry> findByReferenceId(UUID referenceId);

    Page<JournalEntry> findByPostedTrue(Pageable pageable);

    Page<JournalEntry> findByTransactionType(TransactionType transactionType, Pageable pageable);

    @Query("SELECT je FROM JournalEntry je WHERE je.posted = true AND je.transactionDate BETWEEN :startDate AND :endDate")
    List<JournalEntry> findPostedEntriesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(je.entryNumber, 3) AS int)), 0) FROM JournalEntry je WHERE je.entryNumber LIKE 'JE%'")
    int findMaxEntryNumber();

    @Query(value = "SELECT nextval('journal_entry_number_seq')", nativeQuery = true)
    Long getNextEntryNumber();
}
