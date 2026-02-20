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
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final int ENTRY_NUMBER_COLLISION_RETRIES = 3;

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        // Validate that debits equal credits
        validateBalancedEntry(journalEntry);

        if (journalEntry.getReferenceId() != null && journalEntry.getTransactionType() != null) {
            JournalEntry existing = journalEntryRepository
                    .findByReferenceIdAndTransactionType(journalEntry.getReferenceId(), journalEntry.getTransactionType())
                    .orElse(null);
            if (existing != null) {
                log.info("Journal entry already exists for reference {} and type {}: {}",
                        journalEntry.getReferenceId(), journalEntry.getTransactionType(), existing.getEntryNumber());
                return existing;
            }
        }

        // Ensure all journal lines are linked to this entry
        journalEntry.getJournalLines().forEach(line -> line.setJournalEntry(journalEntry));

        return saveWithEntryNumberRecovery(journalEntry);
    }

    @Override
    @Transactional
    public JournalEntry postEntry(UUID journalEntryId) {
        JournalEntry entry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry", journalEntryId));

        if (entry.isPosted()) {
            log.info("Journal entry already posted, skipping repost: {}", entry.getEntryNumber());
            return entry;
        }

        // Validate balance again before posting
        validateBalancedEntry(entry);

        // Update account balances
        for (JournalLine line : entry.getJournalLines()) {
            Account account = line.getAccount();
            BigDecimal debitAmount = line.getDebitAmount();
            BigDecimal creditAmount = line.getCreditAmount();

            // Update account balance based on account type
            BigDecimal balanceChange;
            if (account.getAccountType().isNormalDebit()) {
                balanceChange = debitAmount.subtract(creditAmount);
            } else {
                balanceChange = creditAmount.subtract(debitAmount);
            }
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
    @Transactional
    public String generateEntryNumber() {
        Long nextVal = journalEntryRepository.getNextEntryNumber();
        return String.format("JE%06d", nextVal);
    }

    private JournalEntry saveWithEntryNumberRecovery(JournalEntry entry) {
        for (int attempt = 0; attempt < ENTRY_NUMBER_COLLISION_RETRIES; attempt++) {
            if (entry.getEntryNumber() == null || entry.getEntryNumber().isBlank()) {
                entry.setEntryNumber(generateEntryNumber());
            }

            try {
                JournalEntry saved = journalEntryRepository.saveAndFlush(entry);
                log.info("Created journal entry: {}", saved.getEntryNumber());
                return saved;
            } catch (DataIntegrityViolationException e) {
                if (!isEntryNumberCollision(e)) {
                    throw e;
                }

                log.warn("Entry number collision for {} on attempt {}/{}. Resyncing sequence.",
                        entry.getEntryNumber(), attempt + 1, ENTRY_NUMBER_COLLISION_RETRIES);
                journalEntryRepository.syncEntryNumberSequenceToMax();
                entry.setEntryNumber(null);
            }
        }

        throw new IllegalStateException("Unable to generate unique journal entry number after retries");
    }

    private boolean isEntryNumberCollision(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("entry_number") && lower.contains("unique")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
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
