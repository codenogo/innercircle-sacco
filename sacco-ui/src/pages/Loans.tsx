import { useCallback, useEffect, useMemo, useState } from 'react'
import { Bank, MagnifyingGlass } from '@phosphor-icons/react'
import { Spinner } from '../components/Spinner'
import { DataTable } from '../components/DataTable'
import type { ColumnDef } from '../components/DataTable'
import { NewLoanModal } from '../components/NewLoanModal'
import { MakerCheckerWarning } from '../components/MakerCheckerWarning'
import { ApiError } from '../services/apiClient'
import { getAllMembers } from '../services/memberService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { isMakerCheckerViolation } from '../types/makerChecker'
import type { CursorPage } from '../types/users'
import type { LoanApplicationRequest, LoanResponse, LoanStatus, LoanSummaryResponse } from '../types/loans'
import type { MemberResponse } from '../types/members'
import './Loans.css'

const PAGE_SIZE = 50

const statusClass: Record<LoanStatus, string> = {
  DISBURSED: 'badge--active',
  REPAYING: 'badge--active',
  PENDING: 'badge--pending',
  APPROVED: 'badge--pending',
  CLOSED: 'badge--completed',
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

export function Loans() {
  const { request } = useAuthenticatedApi()
  const { canAccess, isMemberOnly } = useAuthorization()
  const { profile, loading: profileLoading } = useCurrentUser()
  const memberId = profile?.member?.id ?? null
  const canManageLoans = canAccess(['ADMIN', 'TREASURER'])

  const [loans, setLoans] = useState<LoanResponse[]>([])
  const [members, setMembers] = useState<MemberResponse[]>([])
  const [search, setSearch] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [submittingLoan, setSubmittingLoan] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [overrideTarget, setOverrideTarget] = useState<{ id: string; action: 'approve' | 'reject' } | null>(null)

  const loadLoans = useCallback(async (opts?: { append?: boolean; cursor?: string | null }) => {
    if (isMemberOnly) {
      if (!memberId) {
        setLoans([])
        setNextCursor(null)
        setHasMore(false)
        setError('Your account is not linked to a member profile.')
        setLoading(false)
        setLoadingMore(false)
        return
      }

      setLoading(true)
      setError(null)
      try {
        const summary = await request<LoanSummaryResponse>(`/api/v1/loans/member/${memberId}/summary`)
        setLoans(summary.loans)
        setNextCursor(null)
        setHasMore(false)
      } catch (err) {
        setError(toErrorMessage(err, 'Unable to load your loans.'))
      } finally {
        setLoading(false)
        setLoadingMore(false)
      }
      return
    }

    const append = Boolean(opts?.append)
    const cursor = opts?.cursor

    if (append) setLoadingMore(true)
    else setLoading(true)

    try {
      const path = cursor
        ? `/api/v1/loans?limit=${PAGE_SIZE}&cursor=${encodeURIComponent(cursor)}`
        : `/api/v1/loans?limit=${PAGE_SIZE}`
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
  }, [isMemberOnly, memberId, request])

  useEffect(() => {
    if (profileLoading) return
    void loadLoans({ append: false, cursor: null })
  }, [loadLoans, profileLoading])

  useEffect(() => {
    if (!canManageLoans) {
      setMembers([])
      return
    }

    let cancelled = false
    async function fetchMembers() {
      try {
        const allMembers = await getAllMembers(request)
        if (!cancelled) setMembers(allMembers)
      } catch {
        // members list is non-critical; modal can still show empty list
      }
    }
    void fetchMembers()
    return () => { cancelled = true }
  }, [canManageLoans, request])

  async function handleApplyLoan(payload: LoanApplicationRequest) {
    if (!canManageLoans) {
      throw new Error('You are not allowed to apply loans from this account.')
    }

    setSubmittingLoan(true)
    setFeedback(null)
    try {
      const created = await request<LoanResponse>('/api/v1/loans/apply', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setLoans(prev => [created, ...prev])
      setShowModal(false)
      setFeedback({ type: 'success', text: 'Loan application submitted successfully.' })
    } catch (err) {
      const message = toErrorMessage(err, 'Unable to submit loan application.')
      setFeedback({ type: 'error', text: message })
      throw err instanceof Error ? err : new Error(message)
    } finally {
      setSubmittingLoan(false)
    }
  }

  async function handleLoanAction(loanId: string, action: 'approve' | 'reject') {
    setActionLoading(loanId)
    setFeedback(null)
    try {
      const updated = await request<LoanResponse>(`/api/v1/loans/${loanId}/${action}`, {
        method: 'PATCH',
      })
      setLoans(prev => prev.map(l => l.id === loanId ? updated : l))
      setFeedback({ type: 'success', text: `Loan ${action === 'approve' ? 'approved' : 'rejected'} successfully.` })
    } catch (err) {
      if (isMakerCheckerViolation(err)) {
        setOverrideTarget({ id: loanId, action })
      } else {
        setFeedback({ type: 'error', text: toErrorMessage(err, `Unable to ${action} loan.`) })
      }
    } finally {
      setActionLoading(null)
    }
  }

  async function handleLoanOverride(reason: string) {
    if (!overrideTarget) return
    const { id, action } = overrideTarget
    setActionLoading(id)
    try {
      const updated = await request<LoanResponse>(`/api/v1/loans/${id}/${action}`, {
        method: 'PATCH',
        body: JSON.stringify({ overrideReason: reason }),
      })
      setLoans(prev => prev.map(l => l.id === id ? updated : l))
      setOverrideTarget(null)
      setFeedback({ type: 'success', text: `Loan ${action === 'approve' ? 'approved' : 'rejected'} with admin override.` })
    } catch (err) {
      setFeedback({ type: 'error', text: toErrorMessage(err, `Unable to ${action} loan with override.`) })
    } finally {
      setActionLoading(null)
    }
  }

  const memberList = members.map(m => ({ id: m.id, name: `${m.firstName} ${m.lastName}`.trim() }))

  const memberNumberById = useMemo(() => {
    const map = new Map<string, string>()
    members.forEach(member => map.set(member.id, member.memberNumber))
    if (profile?.member) {
      map.set(profile.member.id, profile.member.memberNumber)
    }
    return map
  }, [members, profile?.member])

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return loans

    return loans.filter(loan => {
      const text = [
        loan.loanNumber ?? '',
        loan.id,
        memberNumberById.get(loan.memberId) ?? '',
        loan.memberId,
        loan.status,
        loan.purpose,
        String(loan.principalAmount),
      ].join(' ').toLowerCase()

      return text.includes(query)
    })
  }, [loans, search, memberNumberById])

  const loanColumns: ColumnDef<LoanResponse>[] = useMemo(() => {
    const cols: ColumnDef<LoanResponse>[] = [
      {
        key: 'loanId',
        header: 'Loan Number',
        className: 'data loan-id',
        render: (loan) => loan.loanNumber ?? loan.id,
      },
      {
        key: 'memberId',
        header: 'Member Number',
        render: (loan) => (
          <span className="loan-member">{memberNumberById.get(loan.memberId) ?? loan.memberId}</span>
        ),
      },
      {
        key: 'rate',
        header: 'Rate',
        className: 'data',
        render: (loan) => `${loan.interestRate}%`,
      },
      {
        key: 'status',
        header: 'Status',
        render: (loan) => (
          <span className={`badge ${statusClass[loan.status]}`}>{loan.status}</span>
        ),
      },
      {
        key: 'disbursed',
        header: 'Disbursed',
        className: 'ledger-date',
        render: (loan) => fmtDate(loan.disbursedAt),
      },
      {
        key: 'principal',
        header: 'Principal',
        className: 'amount ledger-table-amount',
        headerClassName: 'ledger-table-amount',
        render: (loan) => fmt(loan.principalAmount),
      },
      {
        key: 'balance',
        header: 'Balance',
        className: 'amount ledger-table-amount',
        headerClassName: 'ledger-table-amount',
        render: (loan) => (
          <span className={loan.status === 'DEFAULTED' ? 'amount--negative' : ''}>
            {Number(loan.outstandingBalance) > 0 ? fmt(loan.outstandingBalance) : '\u2014'}
          </span>
        ),
      },
    ]

    if (canManageLoans) {
      cols.push({
        key: 'actions',
        header: 'Actions',
        render: (loan) => loan.status === 'PENDING' ? (
          <div className="table-actions">
            <button
              type="button"
              className="btn btn--ghost btn--small"
              disabled={actionLoading === loan.id}
              onClick={() => void handleLoanAction(loan.id, 'approve')}
            >
              Approve
            </button>
            <button
              type="button"
              className="btn btn--ghost btn--small btn--danger-text"
              disabled={actionLoading === loan.id}
              onClick={() => void handleLoanAction(loan.id, 'reject')}
            >
              Reject
            </button>
          </div>
        ) : null,
      })
    }

    return cols
  }, [canManageLoans, actionLoading, memberNumberById])

  const activeLoans = loans.filter(l => l.status === 'DISBURSED' || l.status === 'REPAYING')
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
        {canManageLoans && (
          <button className="btn btn--primary" onClick={() => setShowModal(true)}>
            <Bank size={14} weight="bold" />
            New Loan Application
          </button>
        )}
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

      {feedback && (
        <div className={`ops-feedback ops-feedback--${feedback.type}`} role="status">
          {feedback.text}
        </div>
      )}

      {error && (
        <div className="ops-feedback ops-feedback--error" role="status">
          {error}
        </div>
      )}

      {/* Search bar */}
      <div className="filter-bar">
        <div className="filter-search-wrap">
          <MagnifyingGlass size={16} className="filter-search-icon" />
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

        <DataTable<LoanResponse>
          columns={loanColumns}
          data={filtered}
          getRowKey={row => row.id}
          loading={loading}
          emptyMessage="No loans match your search."
        />

        {hasMore && (
          <div className="ops-pager">
            <button
              type="button"
              className="btn btn--secondary"
              disabled={loadingMore || !nextCursor}
              onClick={() => void loadLoans({ append: true, cursor: nextCursor })}
            >
              {loadingMore ? <><Spinner size="sm" /> Loading...</> : 'Load More'}
            </button>
          </div>
        )}

        <hr className="rule rule--strong" />
      </section>

      {canManageLoans && (
        <NewLoanModal
          open={showModal}
          onClose={() => setShowModal(false)}
          members={memberList}
          onSubmit={handleApplyLoan}
          isSubmitting={submittingLoan}
        />
      )}

      <MakerCheckerWarning
        open={overrideTarget !== null}
        onClose={() => setOverrideTarget(null)}
        isAdmin={canAccess(['ADMIN'])}
        onOverride={handleLoanOverride}
        submitting={actionLoading !== null}
        action={overrideTarget?.action ?? 'approve'}
      />
    </div>
  )
}
