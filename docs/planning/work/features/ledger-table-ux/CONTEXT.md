# Ledger Table UX — Discussion Summary

## Problem

The General Ledger page needs to handle millions of journal entries with column-level search. The current implementation uses a plain HTML table with client-side filtering and "Load More" pagination — this won't scale.

## Decisions

### 1. TanStack Table + TanStack Virtual

Add `@tanstack/react-table` and `@tanstack/react-virtual` as dependencies. These provide:
- Headless column filtering, sorting, server-side pagination
- Row virtualization (render only visible rows)
- Sticky header support

Scoped to the Ledger page initially. Other pages keep their plain HTML tables.

### 2. Full Server-Side Filtering (Backend Changes Required)

The `GET /api/v1/ledger/journal-entries` endpoint currently only supports `page`, `size`, `sort`. We need to add:

| Parameter | Type | Purpose |
|-----------|------|---------|
| `accountId` | UUID | Filter by account |
| `dateFrom` | LocalDate | Start of date range |
| `dateTo` | LocalDate | End of date range |
| `description` | String | ILIKE search on description |
| `entryNumber` | String | ILIKE search on entry number |
| `transactionType` | TransactionType | Filter by type |

Backend changes go in `sacco-ledger` module only.

### 3. Expandable Rows

One summary row per journal entry (ref, date, description, total debit, total credit). Click to expand and see the individual debit/credit journal lines underneath. Matches Xero/QuickBooks UX.

### 4. Column Header Inputs

Small text inputs and dropdowns embedded directly in each column header cell — compact, Excel-like. No separate filter bar. Filter inputs debounce server requests at 300-500ms.

### 5. Virtual Scrolling + Sticky Header

- Virtual scrolling renders only visible rows
- Sticky header keeps column labels and filter inputs visible while scrolling
- Server-side pagination fetches pages as user scrolls (infinite scroll pattern)

## Constraints

- No new Java modules — all backend changes in `sacco-ledger`
- Backend filtering must use indexed columns for query performance
- Preserve existing `PageResponse` API wrapper shape
- Column filter debounce: 300-500ms
- Must work within app shell layout (sidebar + scrollable main area)

## Open Questions

1. Database indexes for `description` LIKE search — add GIN/trigram index or rely on sequential scan?
2. Expanded row detail — show line-level description or just account/debit/credit?
3. Date range filter — MonthPicker reuse or from/to date pair?
4. CSV/PDF export for ledger (like Audit page has)?
5. Scroll container height — fixed pixels or `calc(100vh - offset)` fluid?

## Related Files

**Frontend:**
- `sacco-ui/src/pages/Ledger.tsx` — main page component (rewrite)
- `sacco-ui/src/pages/Ledger.css` — page styles (rewrite)
- `sacco-ui/src/types/ledger.ts` — TypeScript types (extend)

**Backend:**
- `sacco-ledger/.../controller/LedgerController.java` — add filter params
- `sacco-ledger/.../repository/JournalEntryRepository.java` — add filtered query
- `sacco-ledger/.../dto/JournalEntryResponse.java` — existing response shape
- `sacco-ledger/.../entity/JournalEntry.java` — entity reference
- `sacco-ledger/.../entity/JournalLine.java` — entity reference

## Research Sources

- [Enterprise Data Table UX Patterns](https://www.pencilandpaper.io/articles/ux-pattern-analysis-enterprise-data-tables)
- [Design Better Data Tables](https://coyleandrew.medium.com/design-better-data-tables-4ecc99d23356)
- [TanStack Table — Virtualized Infinite Scrolling](https://tanstack.com/table/latest/docs/framework/react/examples/virtualized-infinite-scrolling)
- [TanStack Table — Column Filtering Guide](https://tanstack.com/table/latest/docs/guide/column-filtering)
- [Best General Ledger Software 2026](https://www.dualentry.com/blog/best-general-ledger-software)
