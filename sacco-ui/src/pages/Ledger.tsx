import { useCallback, useEffect, useState } from 'react'
import { PenLine } from 'lucide-react'
import { SkeletonRow } from '../components/Skeleton'
import { NewJournalEntryModal } from '../components/NewJournalEntryModal'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import type { AccountResponse, JournalEntryResponse, Page } from '../types/ledger'
import './Ledger.css'

const PAGE_SIZE = 20

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

interface FlatLine {
  key: string
  entryNumber: string
  transactionDate: string
  description: string
  accountName: string
  debit: number
  credit: number
}

function flattenEntries(entries: JournalEntryResponse[]): FlatLine[] {
  const lines: FlatLine[] = []
  for (const entry of entries) {
    for (let i = 0; i < entry.journalLines.length; i++) {
      const line = entry.journalLines[i]
      lines.push({
        key: `${entry.id}-${i}`,
        entryNumber: entry.entryNumber,
        transactionDate: entry.transactionDate,
        description: entry.description,
        accountName: line.accountName,
        debit: Number(line.debitAmount) || 0,
        credit: Number(line.creditAmount) || 0,
      })
    }
  }
  return lines
}

export function Ledger() {
  const { request } = useAuthenticatedApi()

  const [entries, setEntries] = useState<JournalEntryResponse[]>([])
  const [accounts, setAccounts] = useState<AccountResponse[]>([])
  const [accountFilter, setAccountFilter] = useState('All Accounts')
  const [showModal, setShowModal] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const loadAccounts = useCallback(async () => {
    try {
      const data = await request<AccountResponse[]>('/api/v1/ledger/accounts')
      setAccounts(data)
    } catch {
      // non-critical — filter dropdown will be empty
    }
  }, [request])

  const loadEntries = useCallback(async (page: number) => {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(PAGE_SIZE),
        sort: 'transactionDate,desc',
      })
      const data = await request<Page<JournalEntryResponse>>(
        `/api/v1/ledger/journal-entries?${params}`,
      )
      setEntries(data.content)
      setTotalPages(data.totalPages)
      setTotalElements(data.totalElements)
      setCurrentPage(data.number)
    } catch (err) {
      setError(toErrorMessage(err, 'Unable to load journal entries.'))
    } finally {
      setLoading(false)
    }
  }, [request])

  useEffect(() => {
    void loadAccounts()
    void loadEntries(0)
  }, [loadAccounts, loadEntries])

  const flatLines = flattenEntries(entries)

  const filtered = accountFilter === 'All Accounts'
    ? flatLines
    : flatLines.filter(line => line.accountName === accountFilter)

  const totalDebit = filtered.reduce((sum, line) => sum + line.debit, 0)
  const totalCredit = filtered.reduce((sum, line) => sum + line.credit, 0)

  const accountOptions = ['All Accounts', ...accounts.map(a => a.accountName)]

  return (
    <div className="ledger-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">General Ledger</h1>
          <p className="page-subtitle">
            {totalElements > 0
              ? `${totalElements} journal entries`
              : 'Double-entry journal'}
          </p>
        </div>
        <button className="btn btn--primary" onClick={() => setShowModal(true)}>
          <PenLine size={14} strokeWidth={2} />
          New Entry
        </button>
      </div>

      <hr className="rule rule--strong" />

      <div className="filter-bar">
        <span className="page-section-title page-section-title--inline">Account</span>
        <select
          className="filter-select"
          value={accountFilter}
          onChange={e => setAccountFilter(e.target.value)}
        >
          {accountOptions.map(a => <option key={a} value={a}>{a}</option>)}
        </select>
      </div>

      {error && (
        <div className="ops-feedback ops-feedback--error" role="status">
          {error}
        </div>
      )}

      <table className="ledger-table ledger-journal">
        <thead>
          <tr>
            <th className="label">Ref</th>
            <th className="label">Date</th>
            <th className="label">Description</th>
            <th className="label">Account</th>
            <th className="label ledger-table-amount">Debit (KES)</th>
            <th className="label ledger-table-amount">Credit (KES)</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr><td colSpan={6}><SkeletonRow cells={4} /><SkeletonRow cells={4} /><SkeletonRow cells={4} /></td></tr>
          ) : filtered.length === 0 ? (
            <tr><td colSpan={6} className="table-empty">No entries for this account.</td></tr>
          ) : filtered.map((line, i) => (
            <tr key={line.key} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
              <td className="data journal-ref">{line.entryNumber}</td>
              <td className="data ledger-date">{fmtDate(line.transactionDate)}</td>
              <td className="journal-desc">{line.description}</td>
              <td className="journal-account">{line.accountName}</td>
              <td className="amount ledger-table-amount">{fmtCurrency(line.debit)}</td>
              <td className="amount ledger-table-amount">{fmtCurrency(line.credit)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className="journal-totals">
            <td colSpan={4} className="label ledger-table-amount">Totals</td>
            <td className="amount ledger-table-amount">{totalDebit.toLocaleString('en-KE')}</td>
            <td className="amount ledger-table-amount">{totalCredit.toLocaleString('en-KE')}</td>
          </tr>
        </tfoot>
      </table>

      {totalPages > 1 && (
        <div className="ops-pager">
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loading || currentPage === 0}
            onClick={() => void loadEntries(currentPage - 1)}
          >
            Previous
          </button>
          <span className="ops-pager-info">
            Page {currentPage + 1} of {totalPages}
          </span>
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loading || currentPage >= totalPages - 1}
            onClick={() => void loadEntries(currentPage + 1)}
          >
            Next
          </button>
        </div>
      )}

      <hr className="rule rule--strong" />

      <NewJournalEntryModal
        open={showModal}
        onClose={() => setShowModal(false)}
        accounts={accounts.map(a => a.accountName)}
      />
    </div>
  )
}
