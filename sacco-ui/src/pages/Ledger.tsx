import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { CaretRight, CaretDown, MagnifyingGlass, DownloadSimple } from '@phosphor-icons/react'
import { Select } from '../components/Select'
import { DatePicker } from '../components/DatePicker'
import { Spinner } from '../components/Spinner'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useDebounce } from '../hooks/useDebounce'
import { localISODate } from '../utils/date'
import type {
  AccountResponse,
  JournalEntryFilters,
  JournalEntryResponse,
  Page,
  TransactionType,
} from '../types/ledger'
import './Ledger.css'

const PAGE_SIZE = 50
const DEBOUNCE_MS = 400
const ENTRY_ROW_ESTIMATE = 36
const LINE_ROW_ESTIMATE = 32

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

type SortDir = 'asc' | 'desc'

type LedgerDisplayRow =
  | {
    key: string
    kind: 'entry'
    entry: JournalEntryResponse
    isExpanded: boolean
    isAlt: boolean
    entryDebit: number
    entryCredit: number
    imbalance: number
    isBalanced: boolean
  }
  | {
    key: string
    kind: 'line'
    line: JournalEntryResponse['journalLines'][number]
  }

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
  return value.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())
}

function splitLedgerSearch(query: string): Pick<JournalEntryFilters, 'entryNumber' | 'description'> {
  const trimmed = query.trim()
  if (!trimmed) return { entryNumber: undefined, description: undefined }

  const normalized = trimmed.replace(/\s+/g, '')
  const looksLikeEntryNumber = /^[a-z]{2,4}-?\d*$/i.test(normalized)

  return looksLikeEntryNumber
    ? { entryNumber: trimmed, description: undefined }
    : { entryNumber: undefined, description: trimmed }
}

export function Ledger() {
  const { request, requestBlob } = useAuthenticatedApi()

  const [entries, setEntries] = useState<JournalEntryResponse[]>([])
  const [accounts, setAccounts] = useState<AccountResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [hasMore, setHasMore] = useState(false)
  const [totalElements, setTotalElements] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<Record<string, boolean>>({})

  // Sort state
  const [sortDir, setSortDir] = useState<SortDir>('desc')

  // Filter state
  const [filterQuery, setFilterQuery] = useState('')
  const [filterDateFrom, setFilterDateFrom] = useState('')
  const [filterDateTo, setFilterDateTo] = useState('')
  const [filterType, setFilterType] = useState('')
  const [filterAccountId, setFilterAccountId] = useState('')

  // Debounce text input
  const debouncedQuery = useDebounce(filterQuery, DEBOUNCE_MS)

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

  const filters: JournalEntryFilters = useMemo(() => {
    const searchFilters = splitLedgerSearch(debouncedQuery)
    return {
      ...searchFilters,
      dateFrom: filterDateFrom || undefined,
      dateTo: filterDateTo || undefined,
      transactionType: (filterType as TransactionType) || undefined,
      accountId: filterAccountId || undefined,
    }
  }, [debouncedQuery, filterDateFrom, filterDateTo, filterType, filterAccountId])

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
        sort: `transactionDate,${sortDir}`,
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
  }, [request, filters, sortDir])

  // Initial load + load accounts
  useEffect(() => {
    void loadAccounts()
  }, [loadAccounts])

  // Re-fetch when debounced filters or sort direction change
  useEffect(() => {
    void loadEntries({ currentFilters: filters })
  }, [filters, sortDir]) // eslint-disable-line react-hooks/exhaustive-deps

  const toggleExpanded = useCallback((entryId: string) => {
    setExpanded(prev => ({ ...prev, [entryId]: !prev[entryId] }))
  }, [])

  const toggleSort = useCallback(() => {
    setSortDir(prev => prev === 'desc' ? 'asc' : 'desc')
  }, [])

  const getRowClassName = useCallback((row: LedgerDisplayRow) => {
    if (row.kind === 'line') return 'ledger-subrow'
    const classes = ['ledger-row--expandable']
    if (!row.isAlt) classes.push('ledger-row--alt')
    if (row.isExpanded) classes.push('ledger-row--expanded')
    return classes.join(' ')
  }, [])

  const handleExportCsv = useCallback(async () => {
    if (totalElements === 0 || exporting) return

    setExporting(true)
    setError(null)
    try {
      const params = new URLSearchParams({
        sort: `transactionDate,${sortDir}`,
      })
      if (filters.entryNumber) params.set('entryNumber', filters.entryNumber)
      if (filters.description) params.set('description', filters.description)
      if (filters.dateFrom) params.set('dateFrom', filters.dateFrom)
      if (filters.dateTo) params.set('dateTo', filters.dateTo)
      if (filters.transactionType) params.set('transactionType', filters.transactionType)
      if (filters.accountId) params.set('accountId', filters.accountId)

      const blob = await requestBlob(`/api/v1/ledger/journal-entries/export?${params}`)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `journal-entries-${localISODate()}.csv`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(toErrorMessage(err, 'Unable to export journal entries.'))
    } finally {
      setExporting(false)
    }
  }, [exporting, filters, requestBlob, sortDir, totalElements])

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

  const displayRows = useMemo<LedgerDisplayRow[]>(() => {
    const rows: LedgerDisplayRow[] = []
    let alt = false
    for (const entry of entries) {
      alt = !alt
      const isExpanded = Boolean(expanded[entry.id])
      const entryDebit = entry.journalLines.reduce((sum, line) => sum + (Number(line.debitAmount) || 0), 0)
      const entryCredit = entry.journalLines.reduce((sum, line) => sum + (Number(line.creditAmount) || 0), 0)
      const imbalance = Math.abs(entryDebit - entryCredit)
      rows.push({
        key: `entry-${entry.id}`,
        kind: 'entry',
        entry,
        isExpanded,
        isAlt: alt,
        entryDebit,
        entryCredit,
        imbalance,
        isBalanced: imbalance < 0.005,
      })

      if (isExpanded) {
        for (const line of entry.journalLines) {
          rows.push({
            key: `line-${line.id}`,
            kind: 'line',
            line,
          })
        }
      }
    }
    return rows
  }, [entries, expanded])

  const estimateRowSize = useCallback(
    (index: number) => displayRows[index]?.kind === 'line' ? LINE_ROW_ESTIMATE : ENTRY_ROW_ESTIMATE,
    [displayRows],
  )

  const columns = useMemo<ColumnDef<LedgerDisplayRow>[]>(() => [
    {
      key: 'ref',
      header: 'Ref',
      width: 'var(--col-ref)',
      className: 'data journal-ref',
      render: (row) => {
        if (row.kind !== 'entry') return null
        return (
          <>
            <button
              type="button"
              className="ledger-expand-btn"
              aria-expanded={row.isExpanded}
              aria-label={row.isExpanded ? `Collapse ${row.entry.entryNumber}` : `Expand ${row.entry.entryNumber}`}
              onClick={() => toggleExpanded(row.entry.id)}
            >
              {row.isExpanded ? <CaretDown size={12} /> : <CaretRight size={12} />}
            </button>
            {row.entry.entryNumber}
          </>
        )
      },
    },
    {
      key: 'date',
      header: 'Date',
      width: 'var(--col-date)',
      sortable: true,
      sortKey: 'date',
      className: 'data ledger-date',
      render: (row) => row.kind === 'entry' ? fmtDate(row.entry.transactionDate) : null,
    },
    {
      key: 'type',
      header: 'Type',
      width: 'var(--col-type)',
      className: 'data ledger-type',
      render: (row) => {
        if (row.kind !== 'entry') return null
        const label = fmtType(row.entry.transactionType)
        return <span title={label}>{label}</span>
      },
    },
    {
      key: 'desc',
      header: 'Description',
      className: 'journal-desc',
      render: (row) => {
        if (row.kind === 'line') {
          return <span className="journal-account">{`${row.line.accountCode} — ${row.line.accountName}`}</span>
        }
        return (
          <div className="ledger-entry-desc-wrap">
            <span className="ledger-entry-description" title={row.entry.description}>
              {row.entry.description}
            </span>
            {!row.isBalanced && (
              <span className="entry-status entry-status--imbalance">
                {`Imbalance KES ${fmtCurrency(row.imbalance)}`}
              </span>
            )}
          </div>
        )
      },
    },
    {
      key: 'debit',
      header: 'Debit (Dr)',
      width: 'var(--col-debit)',
      headerClassName: 'ledger-th-amount',
      className: 'amount ledger-amount--debit',
      render: (row) => {
        if (row.kind === 'line') {
          const debit = Number(row.line.debitAmount) || 0
          return debit > 0 ? fmtCurrency(debit) : ''
        }
        return <span className="ledger-entry-amount">{row.isExpanded ? '' : fmtCurrency(row.entryDebit)}</span>
      },
    },
    {
      key: 'credit',
      header: 'Credit (Cr)',
      width: 'var(--col-credit)',
      headerClassName: 'ledger-th-amount',
      className: 'amount ledger-amount--credit',
      render: (row) => {
        if (row.kind === 'line') {
          const credit = Number(row.line.creditAmount) || 0
          return credit > 0 ? fmtCurrency(credit) : ''
        }
        return <span className="ledger-entry-amount">{row.isExpanded ? '' : fmtCurrency(row.entryCredit)}</span>
      },
    },
  ], [toggleExpanded])

  // Account options for Select
  const accountOptions = useMemo(() => [
    { value: '', label: 'All Accounts' },
    ...accounts.map(a => ({ value: a.id, label: `${a.accountCode} — ${a.accountName}` })),
  ], [accounts])

  // Type options for Select
  const typeOptions = useMemo(() => [
    { value: '', label: 'All Types' },
    ...TRANSACTION_TYPES.map(t => ({ value: t, label: fmtType(t) })),
  ], [])

  const clearFilters = useCallback(() => {
    setFilterQuery('')
    setFilterDateFrom('')
    setFilterDateTo('')
    setFilterType('')
    setFilterAccountId('')
  }, [])

  const hasActiveFilters = Boolean(
    filterQuery.trim()
    || filterDateFrom
    || filterDateTo
    || filterType
    || filterAccountId,
  )

  const loadedTotals = useMemo(() => {
    let debit = 0
    let credit = 0
    for (const entry of entries) {
      for (const line of entry.journalLines) {
        debit += Number(line.debitAmount) || 0
        credit += Number(line.creditAmount) || 0
      }
    }
    return { debit, credit }
  }, [entries])

  return (
    <div className="ledger-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">General Ledger</h1>
          <p className="page-subtitle">Double-entry journal</p>
        </div>
        <button
          type="button"
          className="btn btn--secondary"
          disabled={totalElements === 0 || exporting}
          onClick={() => void handleExportCsv()}
        >
          <DownloadSimple size={14} weight="bold" />
          {exporting ? 'Exporting…' : 'Export CSV'}
        </button>
      </div>

      <hr className="rule rule--strong" />

      <div className="page-summary">
        Showing <strong>{entries.length.toLocaleString()}</strong> of{' '}
        <strong>{totalElements.toLocaleString()}</strong> journal entries
      </div>

      {error && (
        <div className="ops-feedback ops-feedback--error" role="status">
          {error}
        </div>
      )}

      {/* Filter bar */}
      <div className="filter-bar ledger-filter-bar">
        <div className="filter-search-wrap">
          <MagnifyingGlass size={16} className="filter-search-icon" />
          <input
            type="text"
            className="filter-search"
            placeholder="Search ref or description..."
            value={filterQuery}
            onChange={e => setFilterQuery(e.target.value)}
          />
        </div>
        <div className="filter-select-wrap">
          <Select
            value={filterAccountId}
            onChange={setFilterAccountId}
            options={accountOptions}
            searchable
          />
        </div>
        <div className="filter-select-wrap">
          <Select
            value={filterType}
            onChange={setFilterType}
            options={typeOptions}
            searchable
          />
        </div>
        <div className="ledger-filter-date">
          <span className="page-section-title page-section-title--inline ledger-filter-date-label">From</span>
          <div className="ledger-filter-date-wrap">
            <DatePicker value={filterDateFrom} onChange={setFilterDateFrom} />
          </div>
        </div>
        <div className="ledger-filter-date">
          <span className="page-section-title page-section-title--inline ledger-filter-date-label">To</span>
          <div className="ledger-filter-date-wrap">
            <DatePicker value={filterDateTo} onChange={setFilterDateTo} />
          </div>
        </div>
        <button
          type="button"
          className="btn btn--secondary btn--small ledger-clear-filters"
          onClick={clearFilters}
          disabled={!hasActiveFilters}
        >
          Clear
        </button>
      </div>

      <DataTable<LedgerDisplayRow>
        columns={columns}
        data={displayRows}
        getRowKey={(row) => row.key}
        loading={loading}
        emptyMessage="No journal entries found."
        getRowClassName={getRowClassName}
        sortColumn="date"
        sortDirection={sortDir}
        onSort={toggleSort}
        estimateRowSize={estimateRowSize}
        scrollRef={scrollContainerRef}
        afterScrollContent={loadingMore ? (
          <div className="ledger-loading-more">
            <Spinner size="sm" /> Loading more entries…
          </div>
        ) : undefined}
        stickyTotals={entries.length > 0 ? (
          <div className="ledger-sticky-totals" role="status" aria-live="polite">
            <div className="ledger-sticky-totals__ghost"></div>
            <div className="ledger-sticky-totals__ghost"></div>
            <div className="ledger-sticky-totals__ghost"></div>
            <div className="ledger-sticky-totals__label">Loaded Totals</div>
            <div className="ledger-sticky-totals__debit">{fmtCurrency(loadedTotals.debit)}</div>
            <div className="ledger-sticky-totals__credit">{fmtCurrency(loadedTotals.credit)}</div>
          </div>
        ) : undefined}
      />
    </div>
  )
}
