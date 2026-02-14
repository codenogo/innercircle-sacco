package com.innercircle.sacco.ledger.service;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.JournalEntry;
import com.innercircle.sacco.ledger.entity.JournalLine;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import com.innercircle.sacco.ledger.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceImpl implements LedgerService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        // Validate that debits equal credits
        validateBalancedEntry(journalEntry);

        // Generate entry number if not provided
        if (journalEntry.getEntryNumber() == null || journalEntry.getEntryNumber().isEmpty()) {
            journalEntry.setEntryNumber(generateEntryNumber());
        }

        // Ensure all journal lines are linked to this entry
        journalEntry.getJournalLines().forEach(line -> line.setJournalEntry(journalEntry));

        JournalEntry saved = journalEntryRepository.save(journalEntry);
        log.info("Created journal entry: {}", saved.getEntryNumber());
        return saved;
    }

    @Override
    @Transactional
    public JournalEntry postEntry(UUID journalEntryId) {
        JournalEntry entry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry", journalEntryId));

        if (entry.isPosted()) {
            throw new BusinessException("Journal entry already posted: " + entry.getEntryNumber());
        }

        // Validate balance again before posting
        validateBalancedEntry(entry);

        // Update account balances
        for (JournalLine line : entry.getJournalLines()) {
            Account account = line.getAccount();
            BigDecimal debitAmount = line.getDebitAmount();
            BigDecimal creditAmount = line.getCreditAmount();

            // Update account balance based on account type
            BigDecimal balanceChange = debitAmount.subtract(creditAmount);
            account.setBalance(account.getBalance().add(balanceChange));
            accountRepository.save(account);

            log.debug("Updated account {} balance by {}", account.getAccountCode(), balanceChange);
        }

        // Mark as posted
        entry.setPosted(true);
        entry.setPostedAt(Instant.now());
        JournalEntry posted = journalEntryRepository.save(entry);

        log.info("Posted journal entry: {}", posted.getEntryNumber());
        return posted;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        return account.getBalance();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntriesByReference(UUID referenceId) {
        return journalEntryRepository.findByReferenceId(referenceId);
    }

    @Override
    public String generateEntryNumber() {
        int maxNumber = journalEntryRepository.findMaxEntryNumber();
        return String.format("JE%06d", maxNumber + 1);
    }

    private void validateBalancedEntry(JournalEntry entry) {
        if (entry.getJournalLines().isEmpty()) {
            throw new BusinessException("Journal entry must have at least one line");
        }

        BigDecimal totalDebits = entry.getJournalLines().stream()
                .map(JournalLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entry.getJournalLines().stream()
                .map(JournalLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessException(
                    String.format("Journal entry is not balanced. Debits: %s, Credits: %s",
                            totalDebits, totalCredits)
            );
        }
    }
}
