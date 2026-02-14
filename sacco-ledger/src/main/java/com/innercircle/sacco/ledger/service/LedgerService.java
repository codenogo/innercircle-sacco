package com.innercircle.sacco.ledger.service;

import com.innercircle.sacco.ledger.entity.JournalEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerService {

    /**
     * Create a journal entry with balance validation (debits must equal credits).
     *
     * @param journalEntry the journal entry to create
     * @return the created journal entry
     * @throws IllegalArgumentException if debits don't equal credits
     */
    JournalEntry createJournalEntry(JournalEntry journalEntry);

    /**
     * Post a journal entry and update account balances.
     *
     * @param journalEntryId the ID of the journal entry to post
     * @return the posted journal entry
     */
    JournalEntry postEntry(UUID journalEntryId);

    /**
     * Get the current balance of an account.
     *
     * @param accountId the ID of the account
     * @return the account balance
     */
    BigDecimal getAccountBalance(UUID accountId);

    /**
     * Get all journal entries for a given reference ID.
     *
     * @param referenceId the reference ID (e.g., contribution ID, loan ID)
     * @return list of journal entries
     */
    List<JournalEntry> getJournalEntriesByReference(UUID referenceId);

    /**
     * Generate a unique entry number.
     *
     * @return the generated entry number
     */
    String generateEntryNumber();
}
