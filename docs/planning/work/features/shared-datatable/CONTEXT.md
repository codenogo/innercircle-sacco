# Shared DataTable Component

## Summary

Extract the Ledger page's advanced table patterns into a reusable `<DataTable>` component and migrate all 16 tables across the frontend to use it. This standardizes virtualization, sticky headers, scroll containers, skeleton loading, and visual framing across every data table in the app.

## Current State

**Ledger (template):** Has all features — `@tanstack/react-virtual` virtualization, sticky `<thead>`, scroll container, `<colgroup>` with CSS custom property widths, expandable rows, skeleton loading, alternating row striping, sticky totals footer, CSV export, table frame wrapper.

**All other 15 tables:** Use bare `<table className="ledger-table">` with manual `<thead>`/`<tbody>`. None have sticky headers, scroll containers, virtualization, or `<colgroup>`. 9 of 15 use `<SkeletonTableRows>` for loading state.

## Key Decisions

### Component API

```tsx
interface ColumnDef<T> {
  key: string
  header: string | ReactNode
  width?: string                    // CSS value, maps to <col> and CSS var
  render: (row: T, index: number) => ReactNode
  className?: string                // e.g. 'amount' for right-aligned
  sortable?: boolean
  sortKey?: string                  // key for onSort callback
  headerClassName?: string
}

interface DataTableProps<T> {
  columns: ColumnDef<T>[]
  data: T[]
  getRowKey: (row: T) => string
  loading?: boolean
  skeletonRows?: number
  emptyMessage?: string | ReactNode
  maxHeight?: string                // default: 'calc(100vh - 260px)'
  getRowClassName?: (row: T, index: number) => string
  onRowClick?: (row: T) => void

  // Optional features
  sortColumn?: string
  sortDirection?: 'asc' | 'desc'
  onSort?: (column: string) => void

  expandedRows?: Set<string>
  renderExpanded?: (row: T) => ReactNode

  stickyTotals?: ReactNode          // rendered outside scroll, matched to column grid

  // Virtualization config
  estimateRowSize?: number          // default: 36
  overscan?: number                 // default: 14
}
```

### Architecture

- **Presentational only** — parent owns data fetching, filters, pagination
- **Virtualization always on** — `@tanstack/react-virtual` `useVirtualizer`
- **Column widths as CSS vars** — set on wrapper element, used by both `<colgroup>` and sticky totals grid
- **Sticky `<thead>`** — always enabled (`position: sticky; top: 0; z-index: 10`)
- **Table frame** — wrapper with border, radius, overflow hidden
- **Scroll container** — configurable max-height, overflows vertically

### Cell Patterns (handled via `render` functions)

| Pattern | Used by | Example |
|---------|---------|---------|
| Currency (right-aligned) | All financial pages | `fmtCurrency(row.amount)` |
| Status badge | Loans, Contributions, Payouts | `<span className="badge badge--active">` |
| Stacked text (name + metadata) | Members, Contributions | Name on top, ID/email below |
| Truncated ID | AuditTrail, Ledger | First 8 chars with title tooltip |
| Date (locale) | All pages | `new Date(row.date).toLocaleDateString()` |
| Action buttons | Loans, Payouts, PettyCash | Status-conditional edit/approve/delete |
| Expandable content | Ledger only | Chevron + nested sub-rows |

### Migration Order

1. Build `DataTable` component + CSS (extract from Ledger patterns)
2. Migrate high-traffic: Members, Contributions, Loans
3. Migrate operations: Payouts, PettyCash, ContributionOperations
4. Migrate admin: UsersAdmin, AuditTrail, ContributionCategories
5. Migrate secondary: LoanWorkflow, LoanBatch, LoanBenefits, LedgerStatements
6. Migrate dashboard tables: Dashboard, RoleDashboards
7. Convert Ledger to use DataTable (full circle)

## Constraints

- No data fetching or filter logic inside DataTable
- Must support `table-layout: fixed` with `<colgroup>`
- TypeScript generic `DataTable<T>` for row type safety
- Mobile responsive: horizontal scroll at 768px
- `aria` attributes for accessibility
- Cursor-based "Load More" pagination stays outside DataTable
