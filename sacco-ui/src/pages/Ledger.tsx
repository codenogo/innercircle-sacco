import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from '@tanstack/react-table'
import { useVirtualizer } from '@tanstack/react-virtual'
import { ChevronRight, ChevronDown } from 'lucide-react'
import { Spinner } from '../components/Spinner'
import { SkeletonTableRows } from '../components/Skeleton'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useDebounce } from '../hooks/useDebounce'
import type {
  AccountResponse,
  JournalEntryFilters,
  JournalEntryResponse,
  JournalLineDto,
  Page,
  TransactionType,
} from '../types/ledger'
import './Ledger.css'

const PAGE_SIZE = 50
const ROW_HEIGHT = 40
const SUB_ROW_HEIGHT = 36
const DEBOUNCE_MS = 400

const TRANSACTION_TYPES: TransactionType[] = [
  'CONTRIBUTION',
  'LOAN_DISBURSEMENT',
  'LOAN_REPAYMENT',
  'PAYOUT',
  'PETTY_CASH_DISBURSEMENT',
  'PETTY_CASH_SETTLEMENT',
  'PENALTY',
  'INTEREST_ACCRUAL',
  'MANUAL_ADJUSTMENT',
  'LOAN_REVERSAL',
  'CONTRIBUTION_REVERSAL',
  'PENALTY_WAIVER',
  'BENEFIT_DISTRIBUTION',
]

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function fmtCurrency(n: number): string {
  return n > 0 ? n.toLocaleString('en-KE') : ''
}

function fmtDate(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('en-KE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function fmtType(value: string): string {
  return value.replace(/_/g, ' ')
}

type DisplayRow =
  | { type: 'entry'; entry: JournalEntryResponse }
  | { type: 'line'; line: JournalLineDto; entryNumber: string }

const columnHelper = createColumnHelper<JournalEntryResponse>()

const columns = [
  columnHelper.display({
    id: 'expand',
    header: '',
    cell: () => null,
    size: 32,
  }),
  columnHelper.accessor('entryNumber', {
    header: 'Ref',
    size: 100,
  }),
  columnHelper.accessor('transactionDate', {
    header: 'Date',
    size: 180,
  }),
  columnHelper.accessor('transactionType', {
    header: 'Type',
    size: 140,
  }),
  columnHelper.accessor('description', {
    header: 'Description',
    size: 220,
  }),
  columnHelper.display({
    id: 'totalDebit',
    header: 'Debit (KES)',
    size: 110,
  }),
  columnHelper.display({
    id: 'totalCredit',
    header: 'Credit (KES)',
    size: 110,
  }),
]

export function Ledger() {
  const { request } = useAuthenticatedApi()

  const [entries, setEntries] = useState<JournalEntryResponse[]>([])
  const [accounts, setAccounts] = useState<AccountResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [hasMore, setHasMore] = useState(false)
  const [totalElements, setTotalElements] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<Record<string, boolean>>({})

  // Column filter state
  const [filterRef, setFilterRef] = useState('')
  const [filterDateFrom, setFilterDateFrom] = useState('')
  const [filterDateTo, setFilterDateTo] = useState('')
  const [filterType, setFilterType] = useState('')
  const [filterDesc, setFilterDesc] = useState('')
  const [filterAccountId, setFilterAccountId] = useState('')

  // Debounce text inputs
  const debouncedRef = useDebounce(filterRef, DEBOUNCE_MS)
  const debouncedDesc = useDebounce(filterDesc, DEBOUNCE_MS)
  const debouncedDateFrom = useDebounce(filterDateFrom, DEBOUNCE_MS)
  const debouncedDateTo = useDebounce(filterDateTo, DEBOUNCE_MS)

  const pageRef = useRef(0)
  const scrollContainerRef = useRef<HTMLDivElement>(null)

  const loadAccounts = useCallback(async () => {
    try {
      const data = await request<AccountResponse[]>('/api/v1/ledger/accounts')
      setAccounts(data)
    } catch {
      // non-critical — filter dropdown will be empty
    }
  }, [request])

  const filters: JournalEntryFilters = useMemo(() => ({
    entryNumber: debouncedRef || undefined,
    description: debouncedDesc || undefined,
    dateFrom: debouncedDateFrom || undefined,
    dateTo: debouncedDateTo || undefined,
    transactionType: (filterType as TransactionType) || undefined,
    accountId: filterAccountId || undefined,
  }), [debouncedRef, debouncedDesc, debouncedDateFrom, debouncedDateTo, filterType, filterAccountId])

  const loadEntries = useCallback(async (opts?: { append?: boolean; currentFilters?: JournalEntryFilters }) => {
    const append = Boolean(opts?.append)
    const f = opts?.currentFilters ?? filters

    if (append) {
      pageRef.current += 1
      setLoadingMore(true)
    } else {
      pageRef.current = 0
      setLoading(true)
    }
    setError(null)

    try {
      const params = new URLSearchParams({
        page: String(pageRef.current),
        size: String(PAGE_SIZE),
        sort: 'transactionDate,desc',
      })

      if (f.entryNumber) params.set('entryNumber', f.entryNumber)
      if (f.description) params.set('description', f.description)
      if (f.dateFrom) params.set('dateFrom', f.dateFrom)
      if (f.dateTo) params.set('dateTo', f.dateTo)
      if (f.transactionType) params.set('transactionType', f.transactionType)
      if (f.accountId) params.set('accountId', f.accountId)

      const data = await request<Page<JournalEntryResponse>>(
        `/api/v1/ledger/journal-entries?${params}`,
      )

      if (append) {
        setEntries(prev => [...prev, ...data.content])
      } else {
        setEntries(data.content)
        setTotalElements(data.totalElements)
        setExpanded({})
      }
      setHasMore(!data.last)
    } catch (err) {
      setError(toErrorMessage(err, 'Unable to load journal entries.'))
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [request, filters])

  // Initial load + load accounts
  useEffect(() => {
    void loadAccounts()
  }, [loadAccounts])

  // Re-fetch when debounced filters change
  useEffect(() => {
    void loadEntries({ currentFilters: filters })
  }, [filters]) // eslint-disable-line react-hooks/exhaustive-deps

  const table = useReactTable({
    data: entries,
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    manualFiltering: true,
  })

  const toggleExpanded = useCallback((entryId: string) => {
    setExpanded(prev => ({ ...prev, [entryId]: !prev[entryId] }))
  }, [])

  // Build flat display rows for virtualization
  const displayRows = useMemo((): DisplayRow[] => {
    const rows: DisplayRow[] = []
    for (const entry of entries) {
      rows.push({ type: 'entry', entry })
      if (expanded[entry.id]) {
        for (const line of entry.journalLines) {
          rows.push({ type: 'line', line, entryNumber: entry.entryNumber })
        }
      }
    }
    return rows
  }, [entries, expanded])

  const virtualizer = useVirtualizer({
    count: displayRows.length,
    getScrollElement: () => scrollContainerRef.current,
    estimateSize: (index) =>
      displayRows[index]?.type === 'line' ? SUB_ROW_HEIGHT : ROW_HEIGHT,
    overscan: 10,
  })

  // Infinite scroll: load more when near bottom
  const loadingMoreRef = useRef(false)
  loadingMoreRef.current = loadingMore

  useEffect(() => {
    const container = scrollContainerRef.current
    if (!container) return

    const handleScroll = () => {
      if (loadingMoreRef.current || !hasMore) return
      const { scrollTop, scrollHeight, clientHeight } = container
      if (scrollHeight - scrollTop - clientHeight < 200) {
        void loadEntries({ append: true })
      }
    }

    container.addEventListener('scroll', handleScroll, { passive: true })
    return () => container.removeEventListener('scroll', handleScroll)
  }, [hasMore, loadEntries])

  // Entry-boundary alternation
  const entryAltMap = useMemo(() => {
    const map = new Map<string, boolean>()
    let alt = false
    for (const entry of entries) {
      alt = !alt
      map.set(entry.id, alt)
    }
    return map
  }, [entries])

  const headerGroups = table.getHeaderGroups()

  return (
    <div className="ledger-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">General Ledger</h1>
          <p className="page-subtitle">
            {totalElements > 0
              ? `${totalElements.toLocaleString()} journal entries`
              : 'Double-entry journal'}
          </p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      {error && (
        <div className="ops-feedback ops-feedback--error" role="status">
          {error}
        </div>
      )}

      {loading && entries.length === 0 ? (
        <table className="ledger-table ledger-journal">
          <thead>
            <tr>
              {headerGroups[0]?.headers.map(header => (
                <th key={header.id} className="label">
                  {flexRender(header.column.columnDef.header, header.getContext())}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            <SkeletonTableRows cols={7} />
          </tbody>
        </table>
      ) : (
        <div className="ledger-scroll-container" ref={scrollContainerRef}>
          <table className="ledger-table ledger-journal">
            <thead>
              <tr>
                {headerGroups[0]?.headers.map(header => (
                  <th
                    key={header.id}
                    className="label"
                    style={{ width: header.getSize() }}
                  >
                    {flexRender(header.column.columnDef.header, header.getContext())}
                  </th>
                ))}
              </tr>
              {/* Filter row */}
              <tr className="ledger-th-filter">
                {/* Expand column — account filter */}
                <th>
                  <select
                    className="ledger-filter-select"
                    value={filterAccountId}
                    onChange={e => setFilterAccountId(e.target.value)}
                    title="Filter by account"
                  >
                    <option value="">Acct</option>
                    {accounts.map(a => (
                      <option key={a.id} value={a.id}>{a.accountCode}</option>
                    ))}
                  </select>
                </th>
                {/* Ref */}
                <th>
                  <input
                    className="ledger-filter-input"
                    type="text"
                    placeholder="Ref…"
                    value={filterRef}
                    onChange={e => setFilterRef(e.target.value)}
                  />
                </th>
                {/* Date (from – to) */}
                <th>
                  <div className="ledger-filter-date">
                    <input
                      type="date"
                      className="ledger-filter-input"
                      value={filterDateFrom}
                      onChange={e => setFilterDateFrom(e.target.value)}
                      title="From date"
                    />
                    <span className="ledger-filter-date-sep">–</span>
                    <input
                      type="date"
                      className="ledger-filter-input"
                      value={filterDateTo}
                      onChange={e => setFilterDateTo(e.target.value)}
                      title="To date"
                    />
                  </div>
                </th>
                {/* Type */}
                <th>
                  <select
                    className="ledger-filter-select"
                    value={filterType}
                    onChange={e => setFilterType(e.target.value)}
                  >
                    <option value="">All types</option>
                    {TRANSACTION_TYPES.map(t => (
                      <option key={t} value={t}>{fmtType(t)}</option>
                    ))}
                  </select>
                </th>
                {/* Description */}
                <th>
                  <input
                    className="ledger-filter-input"
                    type="text"
                    placeholder="Search…"
                    value={filterDesc}
                    onChange={e => setFilterDesc(e.target.value)}
                  />
                </th>
                {/* Debit — no filter */}
                <th></th>
                {/* Credit — no filter */}
                <th></th>
              </tr>
            </thead>
            <tbody
              style={{
                height: `${virtualizer.getTotalSize()}px`,
                position: 'relative',
              }}
            >
              {displayRows.length === 0 ? (
                <tr>
                  <td colSpan={7} className="table-empty">
                    No journal entries found.
                  </td>
                </tr>
              ) : (
                virtualizer.getVirtualItems().map(virtualRow => {
                  const displayRow = displayRows[virtualRow.index]
                  if (!displayRow) return null

                  if (displayRow.type === 'entry') {
                    const entry = displayRow.entry
                    const isExpanded = expanded[entry.id]
                    const isAlt = entryAltMap.get(entry.id)
                    const totalDebit = entry.journalLines.reduce(
                      (sum, l) => sum + (Number(l.debitAmount) || 0), 0,
                    )
                    const totalCredit = entry.journalLines.reduce(
                      (sum, l) => sum + (Number(l.creditAmount) || 0), 0,
                    )

                    return (
                      <tr
                        key={`entry-${entry.id}`}
                        className={`ledger-row--expandable${isAlt ? '' : ' ledger-row--alt'}`}
                        style={{
                          position: 'absolute',
                          top: 0,
                          left: 0,
                          width: '100%',
                          height: `${virtualRow.size}px`,
                          transform: `translateY(${virtualRow.start}px)`,
                        }}
                        onClick={() => toggleExpanded(entry.id)}
                      >
                        <td className="data ledger-expand-cell">
                          {isExpanded
                            ? <ChevronDown size={14} />
                            : <ChevronRight size={14} />}
                        </td>
                        <td className="data journal-ref">{entry.entryNumber}</td>
                        <td className="data ledger-date">{fmtDate(entry.transactionDate)}</td>
                        <td className="data ledger-type">
                          {fmtType(entry.transactionType)}
                        </td>
                        <td className="journal-desc" title={entry.description}>
                          {entry.description}
                        </td>
                        <td className="amount ledger-table-amount">{fmtCurrency(totalDebit)}</td>
                        <td className="amount ledger-table-amount">{fmtCurrency(totalCredit)}</td>
                      </tr>
                    )
                  }

                  // Sub-row (journal line)
                  const line = displayRow.line
                  return (
                    <tr
                      key={`line-${displayRow.entryNumber}-${line.id}`}
                      className="ledger-subrow"
                      style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        width: '100%',
                        height: `${virtualRow.size}px`,
                        transform: `translateY(${virtualRow.start}px)`,
                      }}
                    >
                      <td></td>
                      <td></td>
                      <td></td>
                      <td></td>
                      <td className="journal-account">{line.accountName}</td>
                      <td className="amount ledger-table-amount">
                        {fmtCurrency(Number(line.debitAmount) || 0)}
                      </td>
                      <td className="amount ledger-table-amount">
                        {fmtCurrency(Number(line.creditAmount) || 0)}
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>

          {loadingMore && (
            <div className="ledger-loading-more">
              <Spinner size="sm" /> Loading more entries…
            </div>
          )}
        </div>
      )}
    </div>
  )
}
