# Plan 01: Add server-side filtering to the journal entries API endpoint using JPA Specifications

## Goal
Add server-side filtering to the journal entries API endpoint using JPA Specifications

## Tasks

### Task 1: Add JpaSpecificationExecutor and filtered query to JournalEntryRepository
**CWD:** `sacco-ledger`
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/repository/JournalEntryRepository.java`
**Action:**
Extend JournalEntryRepository with JpaSpecificationExecutor<JournalEntry>. This follows the pattern already used by ContributionRepository. No new query methods needed — Specification will handle dynamic filtering.

**Verify:**
```bash
cd sacco-ledger && mvn -q compile
```

**Done when:** [Observable outcome]

### Task 2: Add filter parameters to LedgerController.getJournalEntries
**CWD:** `sacco-ledger`
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/controller/LedgerController.java`
**Action:**
Add optional @RequestParam filters to getJournalEntries: accountId (UUID), dateFrom (LocalDate), dateTo (LocalDate), description (String — ILIKE search), entryNumber (String — ILIKE search), transactionType (TransactionType). Build a Specification<JournalEntry> chain (same pattern as ContributionServiceImpl): start with posted=true, then conditionally .and() each filter. For description/entryNumber use cb.like(cb.lower(...), '%' + value.toLowerCase() + '%'). For account filtering, join through journalLines to account. Use repository.findAll(spec, pageable) and wrap with PageResponse.from().

**Verify:**
```bash
cd sacco-ledger && mvn -q compile
```

**Done when:** [Observable outcome]

### Task 3: Add database indexes for text search performance
**CWD:** `sacco-ledger`
**Files:** `sacco-ledger/src/main/resources/db/changelog/ledger/004-add-search-indexes.yaml`
**Action:**
Create Liquibase changeset 004-add-search-indexes.yaml: add btree index on journal_entries.description (for LIKE prefix queries) and a composite index on (posted, transaction_date, transaction_type) for the filtered query. Register in sacco-app db.changelog-master.yaml.

**Verify:**
```bash
cd sacco-app && mvn -q compile
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-ledger -q compile
mvn -pl sacco-ledger test -q
```

## Commit Message
```
feat(ledger): add server-side filtering to journal entries API
```
