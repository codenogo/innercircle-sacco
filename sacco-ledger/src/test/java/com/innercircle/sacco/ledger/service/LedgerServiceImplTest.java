package com.innercircle.sacco.ledger.service;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.AccountType;
import com.innercircle.sacco.ledger.entity.JournalEntry;
import com.innercircle.sacco.ledger.entity.JournalLine;
import com.innercircle.sacco.ledger.entity.TransactionType;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import com.innercircle.sacco.ledger.repository.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceImplTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @Captor
    private ArgumentCaptor<JournalEntry> journalEntryCaptor;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private Account cashAccount;
    private Account memberSharesAccount;
    private Account loanReceivableAccount;
    private Account interestIncomeAccount;
    private Account expenseAccount;

    @BeforeEach
    void setUp() {
        cashAccount = new Account("1001", "Cash", AccountType.ASSET, BigDecimal.ZERO, "Cash on hand", true, null, null);
        cashAccount.setId(UUID.randomUUID());

        memberSharesAccount = new Account("2001", "Member Shares", AccountType.LIABILITY, BigDecimal.ZERO, "Member shares", true, null, null);
        memberSharesAccount.setId(UUID.randomUUID());

        loanReceivableAccount = new Account("1002", "Loan Receivable", AccountType.ASSET, BigDecimal.ZERO, "Loans receivable", true, null, null);
        loanReceivableAccount.setId(UUID.randomUUID());

        interestIncomeAccount = new Account("4001", "Interest Income", AccountType.REVENUE, BigDecimal.ZERO, "Interest income", true, null, null);
        interestIncomeAccount.setId(UUID.randomUUID());

        expenseAccount = new Account("5001", "Operating Expenses", AccountType.EXPENSE, BigDecimal.ZERO, "Operating expenses", true, null, null);
        expenseAccount.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("createJournalEntry")
    class CreateJournalEntryTests {

        @Test
        @DisplayName("should create balanced journal entry successfully")
        void shouldCreateBalancedJournalEntry() {
            JournalEntry entry = buildBalancedEntry(new BigDecimal("5000.00"));
            when(journalEntryRepository.getNextEntryNumber()).thenReturn(1L);
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
                JournalEntry saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            JournalEntry result = ledgerService.createJournalEntry(entry);

            assertNotNull(result);
            assertEquals("JE000001", result.getEntryNumber());
            verify(journalEntryRepository).save(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();
            assertEquals(2, captured.getJournalLines().size());
            // Verify all lines are linked to the entry
            captured.getJournalLines().forEach(line ->
                    assertEquals(captured, line.getJournalEntry())
            );
        }

        @Test
        @DisplayName("should keep existing entry number if already provided")
        void shouldKeepExistingEntryNumber() {
            JournalEntry entry = buildBalancedEntry(new BigDecimal("3000.00"));
            entry.setEntryNumber("JE999999");
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
                JournalEntry saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            JournalEntry result = ledgerService.createJournalEntry(entry);

            assertEquals("JE999999", result.getEntryNumber());
            verify(journalEntryRepository, never()).getNextEntryNumber();
        }

        @Test
        @DisplayName("should generate entry number when null")
        void shouldGenerateEntryNumberWhenNull() {
            JournalEntry entry = buildBalancedEntry(new BigDecimal("1000.00"));
            entry.setEntryNumber(null);
            when(journalEntryRepository.getNextEntryNumber()).thenReturn(42L);
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            JournalEntry result = ledgerService.createJournalEntry(entry);

            assertEquals("JE000042", result.getEntryNumber());
        }

        @Test
        @DisplayName("should generate entry number when empty string")
        void shouldGenerateEntryNumberWhenEmpty() {
            JournalEntry entry = buildBalancedEntry(new BigDecimal("1000.00"));
            entry.setEntryNumber("");
            when(journalEntryRepository.getNextEntryNumber()).thenReturn(100L);
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            JournalEntry result = ledgerService.createJournalEntry(entry);

            assertEquals("JE000100", result.getEntryNumber());
        }

        @Test
        @DisplayName("should throw BusinessException when debits do not equal credits")
        void shouldThrowWhenDebitsNotEqualCredits() {
            JournalEntry entry = buildUnbalancedEntry();

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> ledgerService.createJournalEntry(entry));

            assertTrue(exception.getMessage().contains("not balanced"));
            verify(journalEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when journal entry has no lines")
        void shouldThrowWhenNoJournalLines() {
            JournalEntry entry = new JournalEntry();
            entry.setTransactionDate(LocalDate.now());
            entry.setDescription("Empty entry");
            entry.setTransactionType(TransactionType.CONTRIBUTION);
            entry.setReferenceId(UUID.randomUUID());
            entry.setJournalLines(new ArrayList<>());

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> ledgerService.createJournalEntry(entry));

            assertEquals("Journal entry must have at least one line", exception.getMessage());
            verify(journalEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should link all journal lines to the entry before saving")
        void shouldLinkJournalLinesToEntry() {
            JournalEntry entry = buildBalancedEntry(new BigDecimal("2500.00"));
            // Deliberately clear the back-references
            entry.getJournalLines().forEach(line -> line.setJournalEntry(null));

            when(journalEntryRepository.getNextEntryNumber()).thenReturn(5L);
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ledgerService.createJournalEntry(entry);

            verify(journalEntryRepository).save(journalEntryCaptor.capture());
            JournalEntry captured = journalEntryCaptor.getValue();
            captured.getJournalLines().forEach(line ->
                    assertEquals(captured, line.getJournalEntry())
            );
        }

        @Test
        @DisplayName("should handle multi-line balanced entry with three lines")
        void shouldHandleMultiLineBalancedEntry() {
            JournalEntry entry = new JournalEntry();
            entry.setTransactionDate(LocalDate.now());
            entry.setDescription("Loan repayment with interest");
            entry.setTransactionType(TransactionType.LOAN_REPAYMENT);
            entry.setReferenceId(UUID.randomUUID());

            JournalLine debitCash = new JournalLine();
            debitCash.setAccount(cashAccount);
            debitCash.setDebitAmount(new BigDecimal("1500.00"));
            debitCash.setCreditAmount(BigDecimal.ZERO);
            debitCash.setDescription("Cash received");
            entry.addJournalLine(debitCash);

            JournalLine creditLoan = new JournalLine();
            creditLoan.setAccount(loanReceivableAccount);
            creditLoan.setDebitAmount(BigDecimal.ZERO);
            creditLoan.setCreditAmount(new BigDecimal("1200.00"));
            creditLoan.setDescription("Principal repayment");
            entry.addJournalLine(creditLoan);

            JournalLine creditInterest = new JournalLine();
            creditInterest.setAccount(interestIncomeAccount);
            creditInterest.setDebitAmount(BigDecimal.ZERO);
            creditInterest.setCreditAmount(new BigDecimal("300.00"));
            creditInterest.setDescription("Interest income");
            entry.addJournalLine(creditInterest);

            when(journalEntryRepository.getNextEntryNumber()).thenReturn(10L);
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            JournalEntry result = ledgerService.createJournalEntry(entry);

            assertNotNull(result);
            assertEquals(3, result.getJournalLines().size());
        }
    }

    @Nested
    @DisplayName("postEntry")
    class PostEntryTests {

        @Test
        @DisplayName("should post entry and update account balances for debit-normal accounts")
        void shouldPostEntryAndUpdateDebitNormalAccountBalances() {
            UUID entryId = UUID.randomUUID();
            JournalEntry entry = buildBalancedEntry(new BigDecimal("5000.00"));
            entry.setId(entryId);
            entry.setEntryNumber("JE000001");
            entry.setPosted(false);

            when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            JournalEntry result = ledgerService.postEntry(entryId);

            assertTrue(result.isPosted());
            assertNotNull(result.getPostedAt());
            // Cash account (ASSET - normal debit): debit 5000 - credit 0 = +5000
            assertEquals(new BigDecimal("5000.00"), cashAccount.getBalance());
            // Member Shares (LIABILITY - normal credit): credit 5000 - debit 0 = +5000
            assertEquals(new BigDecimal("5000.00"), memberSharesAccount.getBalance());
            verify(accountRepository, times(2)).save(any(Account.class));
            verify(journalEntryRepository).save(any(JournalEntry.class));
        }

        @Test
        @DisplayName("should correctly calculate balance change for normal debit accounts (ASSET)")
        void shouldCalculateBalanceForNormalDebitAccounts() {
            UUID entryId = UUID.randomUUID();
            JournalEntry entry = new JournalEntry();
            entry.setId(entryId);
            entry.setEntryNumber("JE000002");
            entry.setTransactionDate(LocalDate.now());
            entry.setDescription("Test debit normal");
            entry.setTransactionType(TransactionType.LOAN_DISBURSEMENT);
            entry.setReferenceId(UUID.randomUUID());
            entry.setPosted(false);

            // Set initial balance on accounts
            loanReceivableAccount.setBalance(new BigDecimal("10000.00"));
            cashAccount.setBalance(new BigDecimal("20000.00"));

            JournalLine debitLine = new JournalLine();
            debitLine.setAccount(loanReceivableAccount);
            debitLine.setDebitAmount(new BigDecimal("5000.00"));
            debitLine.setCreditAmount(BigDecimal.ZERO);
            entry.addJournalLine(debitLine);

            JournalLine creditLine = new JournalLine();
            creditLine.setAccount(cashAccount);
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(new BigDecimal("5000.00"));
            entry.addJournalLine(creditLine);

            when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ledgerService.postEntry(entryId);

            // Loan Receivable (ASSET - normal debit): 10000 + (5000 - 0) = 15000
            assertEquals(new BigDecimal("15000.00"), loanReceivableAccount.getBalance());
            // Cash (ASSET - normal debit): 20000 + (0 - 5000) = 15000
            assertEquals(new BigDecimal("15000.00"), cashAccount.getBalance());
        }

        @Test
        @DisplayName("should correctly calculate balance change for normal credit accounts (REVENUE)")
        void shouldCalculateBalanceForNormalCreditAccounts() {
            UUID entryId = UUID.randomUUID();
            JournalEntry entry = new JournalEntry();
            entry.setId(entryId);
            entry.setEntryNumber("JE000003");
            entry.setTransactionDate(LocalDate.now());
            entry.setDescription("Test credit normal");
            entry.setTransactionType(TransactionType.LOAN_REPAYMENT);
            entry.setReferenceId(UUID.randomUUID());
            entry.setPosted(false);

            cashAccount.setBalance(new BigDecimal("10000.00"));
            interestIncomeAccount.setBalance(new BigDecimal("2000.00"));

            JournalLine debitLine = new JournalLine();
            debitLine.setAccount(cashAccount);
            debitLine.setDebitAmount(new BigDecimal("1000.00"));
            debitLine.setCreditAmount(BigDecimal.ZERO);
            entry.addJournalLine(debitLine);

            JournalLine creditLine = new JournalLine();
            creditLine.setAccount(interestIncomeAccount);
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(new BigDecimal("1000.00"));
            entry.addJournalLine(creditLine);

            when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ledgerService.postEntry(entryId);

            // Cash (ASSET - normal debit): 10000 + (1000 - 0) = 11000
            assertEquals(new BigDecimal("11000.00"), cashAccount.getBalance());
            // Interest Income (REVENUE - normal credit): 2000 + (1000 - 0) = 3000
            assertEquals(new BigDecimal("3000.00"), interestIncomeAccount.getBalance());
        }

        @Test
        @DisplayName("should correctly calculate balance change for EXPENSE accounts")
        void shouldCalculateBalanceForExpenseAccounts() {
            UUID entryId = UUID.randomUUID();
            JournalEntry entry = new JournalEntry();
            entry.setId(entryId);
            entry.setEntryNumber("JE000004");
            entry.setTransactionDate(LocalDate.now());
            entry.setDescription("Expense transaction");
            entry.setTransactionType(TransactionType.MANUAL_ADJUSTMENT);
            entry.setReferenceId(UUID.randomUUID());
            entry.setPosted(false);

            cashAccount.setBalance(new BigDecimal("20000.00"));
            expenseAccount.setBalance(new BigDecimal("1000.00"));

            JournalLine debitLine = new JournalLine();
            debitLine.setAccount(expenseAccount);
            debitLine.setDebitAmount(new BigDecimal("500.00"));
            debitLine.setCreditAmount(BigDecimal.ZERO);
            entry.addJournalLine(debitLine);

            JournalLine creditLine = new JournalLine();
            creditLine.setAccount(cashAccount);
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(new BigDecimal("500.00"));
            entry.addJournalLine(creditLine);

            when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ledgerService.postEntry(entryId);

            // Expense (EXPENSE - normal debit): 1000 + (500 - 0) = 1500
            assertEquals(new BigDecimal("1500.00"), expenseAccount.getBalance());
            // Cash (ASSET - normal debit): 20000 + (0 - 500) = 19500
            assertEquals(new BigDecimal("19500.00"), cashAccount.getBalance());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when entry not found")
        void shouldThrowWhenEntryNotFound() {
            UUID entryId = UUID.randomUUID();
            when(journalEntryRepository.findById(entryId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> ledgerService.postEntry(entryId));

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when entry is already posted")
        void shouldThrowWhenEntryAlreadyPosted() {
            UUID entryId = UUID.randomUUID();
            JournalEntry entry = buildBalancedEntry(new BigDecimal("1000.00"));
            entry.setId(entryId);
            entry.setEntryNumber("JE000005");
            entry.setPosted(true);
            entry.setPostedAt(Instant.now());

            when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> ledgerService.postEntry(entryId));

            assertTrue(exception.getMessage().contains("already posted"));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should re-validate balance before posting")
        void shouldRevalidateBalanceBeforePosting() {
            UUID entryId = UUID.randomUUID();
            JournalEntry entry = new JournalEntry();
            entry.setId(entryId);
            entry.setEntryNumber("JE000006");
            entry.setTransactionDate(LocalDate.now());
            entry.setDescription("Unbalanced entry");
            entry.setTransactionType(TransactionType.CONTRIBUTION);
            entry.setReferenceId(UUID.randomUUID());
            entry.setPosted(false);

            // Create an unbalanced entry (debits != credits)
            JournalLine debitLine = new JournalLine();
            debitLine.setAccount(cashAccount);
            debitLine.setDebitAmount(new BigDecimal("5000.00"));
            debitLine.setCreditAmount(BigDecimal.ZERO);
            entry.addJournalLine(debitLine);

            JournalLine creditLine = new JournalLine();
            creditLine.setAccount(memberSharesAccount);
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(new BigDecimal("4000.00"));
            entry.addJournalLine(creditLine);

            when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> ledgerService.postEntry(entryId));

            assertTrue(exception.getMessage().contains("not balanced"));
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAccountBalance")
    class GetAccountBalanceTests {

        @Test
        @DisplayName("should return account balance")
        void shouldReturnAccountBalance() {
            UUID accountId = cashAccount.getId();
            cashAccount.setBalance(new BigDecimal("15000.00"));
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(cashAccount));

            BigDecimal balance = ledgerService.getAccountBalance(accountId);

            assertEquals(new BigDecimal("15000.00"), balance);
        }

        @Test
        @DisplayName("should return zero balance for new account")
        void shouldReturnZeroBalanceForNewAccount() {
            UUID accountId = cashAccount.getId();
            cashAccount.setBalance(BigDecimal.ZERO);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(cashAccount));

            BigDecimal balance = ledgerService.getAccountBalance(accountId);

            assertEquals(BigDecimal.ZERO, balance);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when account not found")
        void shouldThrowWhenAccountNotFound() {
            UUID accountId = UUID.randomUUID();
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> ledgerService.getAccountBalance(accountId));
        }
    }

    @Nested
    @DisplayName("getJournalEntriesByReference")
    class GetJournalEntriesByReferenceTests {

        @Test
        @DisplayName("should return entries by reference ID")
        void shouldReturnEntriesByReferenceId() {
            UUID referenceId = UUID.randomUUID();
            JournalEntry entry1 = buildBalancedEntry(new BigDecimal("1000.00"));
            entry1.setId(UUID.randomUUID());
            entry1.setReferenceId(referenceId);

            JournalEntry entry2 = buildBalancedEntry(new BigDecimal("2000.00"));
            entry2.setId(UUID.randomUUID());
            entry2.setReferenceId(referenceId);

            when(journalEntryRepository.findByReferenceId(referenceId)).thenReturn(List.of(entry1, entry2));

            List<JournalEntry> result = ledgerService.getJournalEntriesByReference(referenceId);

            assertEquals(2, result.size());
            verify(journalEntryRepository).findByReferenceId(referenceId);
        }

        @Test
        @DisplayName("should return empty list when no entries for reference")
        void shouldReturnEmptyListWhenNoEntries() {
            UUID referenceId = UUID.randomUUID();
            when(journalEntryRepository.findByReferenceId(referenceId)).thenReturn(List.of());

            List<JournalEntry> result = ledgerService.getJournalEntriesByReference(referenceId);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("generateEntryNumber")
    class GenerateEntryNumberTests {

        @Test
        @DisplayName("should generate formatted entry number")
        void shouldGenerateFormattedEntryNumber() {
            when(journalEntryRepository.getNextEntryNumber()).thenReturn(1L);

            String entryNumber = ledgerService.generateEntryNumber();

            assertEquals("JE000001", entryNumber);
        }

        @Test
        @DisplayName("should generate entry number with large sequence")
        void shouldGenerateEntryNumberWithLargeSequence() {
            when(journalEntryRepository.getNextEntryNumber()).thenReturn(999999L);

            String entryNumber = ledgerService.generateEntryNumber();

            assertEquals("JE999999", entryNumber);
        }

        @Test
        @DisplayName("should zero-pad entry numbers")
        void shouldZeroPadEntryNumbers() {
            when(journalEntryRepository.getNextEntryNumber()).thenReturn(42L);

            String entryNumber = ledgerService.generateEntryNumber();

            assertEquals("JE000042", entryNumber);
        }
    }

    // --- Helper methods ---

    private JournalEntry buildBalancedEntry(BigDecimal amount) {
        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Test journal entry");
        entry.setTransactionType(TransactionType.CONTRIBUTION);
        entry.setReferenceId(UUID.randomUUID());

        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(cashAccount);
        debitLine.setDebitAmount(amount);
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Debit cash");
        entry.addJournalLine(debitLine);

        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(memberSharesAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(amount);
        creditLine.setDescription("Credit member shares");
        entry.addJournalLine(creditLine);

        return entry;
    }

    private JournalEntry buildUnbalancedEntry() {
        JournalEntry entry = new JournalEntry();
        entry.setTransactionDate(LocalDate.now());
        entry.setDescription("Unbalanced entry");
        entry.setTransactionType(TransactionType.CONTRIBUTION);
        entry.setReferenceId(UUID.randomUUID());

        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(cashAccount);
        debitLine.setDebitAmount(new BigDecimal("5000.00"));
        debitLine.setCreditAmount(BigDecimal.ZERO);
        entry.addJournalLine(debitLine);

        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(memberSharesAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(new BigDecimal("3000.00"));
        entry.addJournalLine(creditLine);

        return entry;
    }
}
