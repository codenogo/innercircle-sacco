import { useCallback, useEffect, useMemo, useState } from 'react'
import { Landmark, Search } from 'lucide-react'
import { NewLoanModal } from '../components/NewLoanModal'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import type { CursorPage } from '../types/users'
import type { LoanApplicationRequest, LoanResponse, LoanStatus } from '../types/loans'
import type { MemberResponse } from '../types/members'
import './Loans.css'

const PAGE_SIZE = 50

const statusClass: Record<LoanStatus, string> = {
  ACTIVE: 'badge--active',
  PENDING: 'badge--pending',
  APPROVED: 'badge--pending',
  COMPLETED: 'badge--completed',
  DEFAULTED: 'badge--defaulted',
  REJECTED: 'badge--defaulted',
}

function fmt(n: number | string): string {
  const parsed = typeof n === 'number' ? n : Number(n)
  if (Number.isNaN(parsed)) return '0'
  return parsed.toLocaleString('en-KE')
}

function fmtDate(value: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-KE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function shortId(id: string): string {
  return id.length > 8 ? id.slice(0, 8) : id
}

export function Loans() {
  const { request } = useAuthenticatedApi()

  const [loans, setLoans] = useState<LoanResponse[]>([])
  const [search, setSearch] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadLoans = useCallback(async (opts?: { append?: boolean; cursor?: string | null }) => {
    const append = Boolean(opts?.append)
    const cursor = opts?.cursor

    if (append) setLoadingMore(true)
    else setLoading(true)

    try {
      const path = cursor
        ? `/api/v1/loans?size=${PAGE_SIZE}&cursor=${encodeURIComponent(cursor)}`
        : `/api/v1/loans?size=${PAGE_SIZE}`
      const page = await request<CursorPage<LoanResponse>>(path)

      setLoans(prev => {
        if (!append) return page.items

        const merged = new Map<string, LoanResponse>()
        prev.forEach(loan => merged.set(loan.id, loan))
        page.items.forEach(loan => merged.set(loan.id, loan))
        return Array.from(merged.values())
      })
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
      setError(null)
    } catch (err) {
      setError(toErrorMessage(err, 'Unable to load loans.'))
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [request])

  useEffect(() => {
    void loadLoans({ append: false, cursor: null })
  }, [loadLoans])

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return loans

    return loans.filter(loan => {
      const text = [
        loan.id,
        loan.memberId,
        loan.status,
        loan.purpose,
        String(loan.principalAmount),
      ].join(' ').toLowerCase()

      return text.includes(query)
    })
  }, [loans, search])

  const activeLoans = loans.filter(l => l.status === 'ACTIVE')
  const totalDisbursed = activeLoans.reduce((s, l) => s + Number(l.principalAmount), 0)
  const totalOutstanding = activeLoans.reduce((s, l) => s + Number(l.outstandingBalance), 0)
  const totalRepaid = activeLoans.reduce((s, l) => s + Number(l.totalRepaid), 0)

  return (
    <div className="loans-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Loans</h1>
          <p className="page-subtitle">{activeLoans.length} active loans</p>
        </div>
        <button className="btn btn--primary" onClick={() => setShowModal(true)}>
          <Landmark size={14} strokeWidth={2} />
          New Loan Application
        </button>
      </div>

      <hr className="rule rule--strong" />

      {/* Loan portfolio summary */}
      <section className="page-section">
        <span className="page-section-title">Loan Portfolio</span>
        <hr className="rule" />
        <div className="loan-summary">
          <div className="dot-leader">
            <span>Total Disbursed (active)</span>
            <span className="dot-leader-value">KES {fmt(totalDisbursed)}</span>
          </div>
          <div className="dot-leader">
            <span>Total Repaid</span>
            <span className="dot-leader-value amount--positive">KES {fmt(totalRepaid)}</span>
          </div>
          <div className="dot-leader loan-summary-total">
            <span>Outstanding Balance</span>
            <span className="dot-leader-value">KES {fmt(totalOutstanding)}</span>
          </div>
        </div>
        <hr className="rule rule--strong" />
      </section>

      {error && (
        <div className="ops-feedback ops-feedback--error" role="status">
          {error}
        </div>
      )}

      {/* Search bar */}
      <div className="filter-bar">
        <div className="filter-search-wrap">
          <Search size={14} strokeWidth={1.75} className="filter-search-icon" />
          <input
            type="text"
            className="filter-search"
            placeholder="Search loans..."
            value={search}
            onChange={event => setSearch(event.target.value)}
          />
        </div>
      </div>

      {/* Loans table */}
      <section className="page-section">
        <span className="page-section-title">All Loans</span>
        <hr className="rule" />

        <table className="ledger-table">
          <thead>
            <tr>
              <th className="label">Loan ID</th>
              <th className="label">Member ID</th>
              <th className="label">Rate</th>
              <th className="label">Status</th>
              <th className="label">Disbursed</th>
              <th className="label ledger-table-amount">Principal</th>
              <th className="label ledger-table-amount">Balance</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="table-empty">Loading loans...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={7} className="table-empty">No loans match your search.</td></tr>
            ) : filtered.map((loan, i) => (
              <tr key={loan.id} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
                <td className="data loan-id">{shortId(loan.id)}</td>
                <td>
                  <span className="loan-member">{shortId(loan.memberId)}</span>
                </td>
                <td className="data">{loan.interestRate}%</td>
                <td><span className={`badge ${statusClass[loan.status]}`}>{loan.status}</span></td>
                <td className="ledger-date">{fmtDate(loan.disbursedAt)}</td>
                <td className="amount ledger-table-amount">{fmt(loan.principalAmount)}</td>
                <td className={`amount ledger-table-amount ${loan.status === 'DEFAULTED' ? 'amount--negative' : ''}`}>
                  {Number(loan.outstandingBalance) > 0 ? fmt(loan.outstandingBalance) : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {hasMore && (
          <div className="ops-pager">
            <button
              type="button"
              className="btn btn--secondary"
              disabled={loadingMore || !nextCursor}
              onClick={() => void loadLoans({ append: true, cursor: nextCursor })}
            >
              {loadingMore ? 'Loading...' : 'Load More'}
            </button>
          </div>
        )}

        <hr className="rule rule--strong" />
      </section>

      <NewLoanModal open={showModal} onClose={() => setShowModal(false)} members={[]} />
    </div>
  )
}
