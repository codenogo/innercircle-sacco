# Plan 04: Wire Ledger page/modal and Audit Trail to real backend API endpoints

## Goal
Wire Ledger page/modal and Audit Trail to real backend API endpoints

## Tasks

### Task 1: Create ledger and audit services with types
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/types/ledger.ts`, `sacco-ui/src/services/ledgerService.ts`, `sacco-ui/src/types/audit.ts`, `sacco-ui/src/services/auditService.ts`
**Action:**
Create ledger types: AccountResponse (matching Account entity fields: id, code, name, type, balance), JournalEntryResponse (id, entryNumber, transactionDate, description, transactionType, referenceId, posted, postedAt, journalLines, createdAt), JournalLineDto (accountCode, accountName, debit, credit), TrialBalanceResponse, IncomeStatementResponse, BalanceSheetResponse. Create ledgerService.ts: getAccounts(), getJournalEntries(page?, size?, sort?), getTrialBalance(asOfDate?), getIncomeStatement(startDate, endDate), getBalanceSheet(asOfDate?). Note: ledger uses offset pagination (Page<T>), not cursor. Create audit types: AuditEventResponse (id, timestamp, actor, actorName, action, entityType, entityId, ipAddress), AuditAction and AuditEntityType union types. Create auditService.ts: getAuditEvents(params: {cursor?, entityType?, entityId?, actor?, action?, startDate?, endDate?, limit?}), getEntityHistory(entityType, entityId, cursor?, limit?), exportAuditCsv(params).

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 2: Wire Ledger page and modal to real API
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/Ledger.tsx`, `sacco-ui/src/components/NewJournalEntryModal.tsx`
**Action:**
In Ledger.tsx: Replace mock entries with useAuthenticatedApi. Fetch accounts from /api/v1/ledger/accounts to populate filter dropdown dynamically. Fetch journal entries from /api/v1/ledger/journal-entries with pagination params. Map JournalEntryResponse + journalLines to table rows. Each journal entry may have multiple lines (debit/credit pairs) — flatten for display or group by entry. Compute totals from real data. Add offset-based pagination (Next/Previous page). Add loading/error states. Remove hardcoded entries/accounts arrays. In NewJournalEntryModal: This is cosmetic only for now — backend doesn't have a POST journal entry endpoint (entries are created automatically by domain events). Keep modal as-is with console.log or disable submit with a note.

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

### Task 3: Wire Audit Trail page to real API
**CWD:** `sacco-ui`
**Files:** `sacco-ui/src/pages/AuditTrail.tsx`
**Action:**
Replace mock auditEvents with useAuthenticatedApi. Fetch from /api/v1/audit with cursor pagination. Map AuditEventResponse fields to table: id, timestamp, actorName, entityType:entityId, action. Wire entity type filter to ?entityType= query param (re-fetch on filter change, not client-side filter). Add cursor-based Load More. Add date range filter (startDate/endDate params). Wire Export CSV button to /api/v1/audit/export — trigger browser download of returned CSV bytes. Add loading/error states. Remove hardcoded auditEvents array.

**Verify:**
```bash
npx tsc --noEmit
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
npx tsc --noEmit
npx vite build
```

## Commit Message
```
feat(frontend-backend-wiring): wire ledger and audit trail pages to real API
```
