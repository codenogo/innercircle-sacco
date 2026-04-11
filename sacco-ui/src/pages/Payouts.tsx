import { useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowLineDown, MagnifyingGlass } from '@phosphor-icons/react'
import { Spinner } from '../components/Spinner'
import { ActionMenu } from '../components/ActionMenu'
import { StatCardGrid } from '../components/StatCard'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { NewPayoutModal } from '../components/NewPayoutModal'
import { MakerCheckerWarning } from '../components/MakerCheckerWarning'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Select } from '../components/Select'
import { ApiError } from '../services/apiClient'
import { getAllMembers } from '../services/memberService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useToast } from '../hooks/useToast'
import { isMakerCheckerViolation } from '../types/makerChecker'
import type { CursorPage } from '../types/users'
import type { PayoutResponse, PayoutRequest, PayoutStatus, PayoutType } from '../types/payouts'
import './Payouts.css'

type StatusFilter = 'all' | PayoutStatus
type FeedbackType = 'success' | 'error'

interface Feedback {
  type: FeedbackType
  text: string
}

const PAGE_SIZE = 50

const statusClass: Record<PayoutStatus, string> = {
  PENDING: 'badge--pending',
  APPROVED: 'badge--active',
  PROCESSED: 'badge--completed',
  FAILED: 'badge--rejected',
}

const payoutTypeLabel: Record<PayoutType, string> = {
  MERRY_GO_ROUND: 'Merry-Go-Round',
  AD_HOC: 'Ad Hoc',
  DIVIDEND: 'Dividend',
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function fmtCurrency(value: number | string): string {
  const parsed = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(parsed)) return '0'
  return parsed.toLocaleString('en-KE')
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

export function Payouts() {
  const { request } = useAuthenticatedApi()
  const { canAccess, isMemberOnly } = useAuthorization()
  const { profile, loading: profileLoading } = useCurrentUser()
  const memberId = profile?.member?.id ?? null
  const canCreatePayout = canAccess(['ADMIN', 'TREASURER'])
  const toast = useToast()

  const [payouts, setPayouts] = useState<PayoutResponse[]>([])
  const [memberMap, setMemberMap] = useState<Map<string, string>>(new Map())
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<StatusFilter>('all')
  const [showModal, setShowModal] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [creatingPayout, setCreatingPayout] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [overrideTarget, setOverrideTarget] = useState<{ id: string; action: 'approve' } | null>(null)
  const [confirmPayoutId, setConfirmPayoutId] = useState<string | null>(null)

  const loadPayouts = useCallback(async (opts?: { append?: boolean; cursor?: string | null }) => {
    if (isMemberOnly) {
      if (!memberId) {
        setPayouts([])
        setNextCursor(null)
        setHasMore(false)
        setFeedback({ type: 'error', text: 'Your account is not linked to a member profile.' })
        setLoading(false)
        setLoadingMore(false)
        return
      }

      setLoading(true)
      setFeedback(null)
      try {
        const path = opts?.cursor
          ? `/api/v1/payouts/member/${memberId}?limit=${PAGE_SIZE}&cursor=${encodeURIComponent(opts.cursor)}`
          : `/api/v1/payouts/member/${memberId}?limit=${PAGE_SIZE}`
        const page = await request<CursorPage<PayoutResponse>>(path)
        setPayouts(prev => {
          if (!opts?.append) return page.items

          const merged = new Map<string, PayoutResponse>()
          prev.forEach(p => merged.set(p.id, p))
          page.items.forEach(p => merged.set(p.id, p))
          return Array.from(merged.values())
        })
        setNextCursor(page.nextCursor)
        setHasMore(page.hasMore)
      } catch (error) {
        setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to load your payouts.') })
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
        ? `/api/v1/payouts?limit=${PAGE_SIZE}&cursor=${encodeURIComponent(cursor)}`
        : `/api/v1/payouts?limit=${PAGE_SIZE}`
      const page = await request<CursorPage<PayoutResponse>>(path)

      setPayouts(prev => {
        if (!append) return page.items

        const merged = new Map<string, PayoutResponse>()
        prev.forEach(p => merged.set(p.id, p))
        page.items.forEach(p => merged.set(p.id, p))
        return Array.from(merged.values())
      })
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
      setFeedback(null)
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to load payouts.') })
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [isMemberOnly, memberId, request])

  const loadMembers = useCallback(async () => {
    if (!canCreatePayout) {
      setMemberMap(new Map())
      return
    }

    try {
      const page = await getAllMembers(request)
      const map = new Map<string, string>()
      page.forEach(m => map.set(m.id, `${m.firstName} ${m.lastName}`.trim()))
      setMemberMap(map)
    } catch {
      // Members list is non-critical; silently fall back to showing IDs
    }
  }, [canCreatePayout, request])

  useEffect(() => {
    if (profileLoading) return
    void loadPayouts({ append: false, cursor: null })
    void loadMembers()
  }, [loadPayouts, loadMembers, profileLoading])

  async function handleCreatePayout(payload: PayoutRequest) {
    if (!canCreatePayout) {
      throw new Error('You are not allowed to create payouts.')
    }

    setCreatingPayout(true)
    try {
      const created = await request<PayoutResponse>('/api/v1/payouts', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setPayouts(prev => [created, ...prev])
      setShowModal(false)
      toast.success('Payout submitted', 'Payout request submitted successfully.')
    } catch (error) {
      const message = toErrorMessage(error, 'Unable to create payout.')
      toast.error('Unable to create payout', message)
      throw error instanceof Error ? error : new Error(message)
    } finally {
      setCreatingPayout(false)
    }
  }

  async function handleApprovePayout(payoutId: string) {
    setActionLoading(payoutId)
    try {
      const updated = await request<PayoutResponse>(`/api/v1/payouts/${payoutId}/approve`, {
        method: 'PUT',
      })
      setPayouts(prev => prev.map(p => p.id === payoutId ? updated : p))
      toast.success('Payout approved', 'Payout approved successfully.')
    } catch (err) {
      if (isMakerCheckerViolation(err)) {
        setOverrideTarget({ id: payoutId, action: 'approve' })
      } else {
        toast.error('Unable to approve payout', toErrorMessage(err, 'Unable to approve payout.'))
      }
    } finally {
      setActionLoading(null)
    }
  }

  async function handlePayoutOverride(reason: string) {
    if (!overrideTarget) return
    const { id } = overrideTarget
    setActionLoading(id)
    try {
      const updated = await request<PayoutResponse>(`/api/v1/payouts/${id}/approve`, {
        method: 'PUT',
        body: JSON.stringify({ overrideReason: reason }),
      })
      setPayouts(prev => prev.map(p => p.id === id ? updated : p))
      setOverrideTarget(null)
      toast.success('Payout approved with admin override.')
    } catch (err) {
      toast.error('Unable to approve payout with override', toErrorMessage(err, ''))
    } finally {
      setActionLoading(null)
    }
  }

  const getMemberName = useCallback((memberId: string): string => {
    if (memberId === profile?.member?.id && profile.member) {
      return `${profile.member.firstName} ${profile.member.lastName}`.trim()
    }
    return memberMap.get(memberId) ?? memberId
  }, [memberMap, profile?.member])

  const memberList = useMemo(() => {
    return Array.from(memberMap.entries()).map(([id, name]) => ({ id, name }))
  }, [memberMap])

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase()

    return payouts.filter(p => {
      if (filter !== 'all' && p.status !== filter) return false
      if (!query) return true

      const text = [
        getMemberName(p.memberId),
        payoutTypeLabel[p.type],
        p.status,
        p.referenceNumber ?? '',
      ].join(' ').toLowerCase()

      return text.includes(query)
    })
  }, [filter, payouts, search, getMemberName])

  const payoutColumns = useMemo((): ColumnDef<PayoutResponse>[] => {
    const cols: ColumnDef<PayoutResponse>[] = [
      {
        key: 'member',
        header: 'Member',
        render: p => (
          <>
            <span className="payout-member">{getMemberName(p.memberId)}</span>
            <span className="payout-date">{fmtDate(p.createdAt)}</span>
          </>
        ),
      },
      {
        key: 'type',
        header: 'Type',
        className: 'payout-type',
        render: p => payoutTypeLabel[p.type],
      },
      {
        key: 'status',
        header: 'Status',
        render: p => <span className={`badge ${statusClass[p.status]}`}>{p.status}</span>,
      },
      {
        key: 'reference',
        header: 'Reference',
        className: 'data payout-ref',
        render: p => p.referenceNumber ?? '\u2014',
      },
      {
        key: 'amount',
        header: 'Amount (KES)',
        className: 'amount ledger-table-amount',
        headerClassName: 'ledger-table-amount',
        render: p => fmtCurrency(p.amount),
      },
    ]

    if (canCreatePayout) {
      cols.push({
        key: 'actions',
        header: '',
        width: '52px',
        headerClassName: 'datatable-col-actions',
        className: 'datatable-col-actions',
        render: p =>
          p.status === 'PENDING' ? (
            <ActionMenu
              actions={[
                { label: 'Approve', onClick: () => setConfirmPayoutId(p.id), disabled: actionLoading === p.id },
              ]}
            />
          ) : null,
      })
    }

    return cols
  }, [canCreatePayout, getMemberName, actionLoading])

  const completed = payouts.filter(p => p.status === 'PROCESSED')
  const pending = payouts.filter(p => p.status === 'PENDING' || p.status === 'APPROVED')
  const totalPaid = completed.reduce((s, p) => s + Number(p.amount), 0)
  const totalPending = pending.reduce((s, p) => s + Number(p.amount), 0)

  return (
    <div className="payouts-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Payouts</h1>
          <p className="page-subtitle">Disbursements and withdrawals</p>
        </div>
        {canCreatePayout && (
          <button className="btn btn--primary" onClick={() => setShowModal(true)}>
            <ArrowLineDown size={14} weight="bold" />
            New Payout
          </button>
        )}
      </div>

      <hr className="rule rule--strong" />

      <section className="page-section">
        <span className="page-section-title">Summary</span>
        <hr className="rule" />
        <StatCardGrid
          items={[
            { label: `Total Paid Out (${completed.length})`, value: `KES ${fmtCurrency(totalPaid)}` },
            { label: `Pending / Processing (${pending.length})`, value: `KES ${fmtCurrency(totalPending)}`, valueClassName: 'amount--negative' },
          ]}
          columns={2}
        />
        <hr className="rule" />
      </section>

      {feedback && (
        <div className={`ops-feedback ops-feedback--${feedback.type}`} role="status">
          {feedback.text}
        </div>
      )}

      <section className="page-section">
        <span className="page-section-title">All Payouts</span>
        <hr className="rule" />

        <div className="filter-bar">
          <div className="filter-search-wrap">
            <MagnifyingGlass size={16} className="filter-search-icon" />
            <input
              type="text"
              className="filter-search"
              placeholder="Search payouts..."
              aria-label="Search payouts"
              value={search}
              onChange={event => setSearch(event.target.value)}
            />
          </div>
          <div className="filter-select-wrap">
            <Select
              value={filter}
              onChange={value => setFilter(value as StatusFilter)}
              options={[
                { value: 'all', label: 'All Status' },
                { value: 'PENDING', label: 'Pending' },
                { value: 'APPROVED', label: 'Approved' },
                { value: 'PROCESSED', label: 'Processed' },
                { value: 'FAILED', label: 'Failed' },
              ]}
            />
          </div>
        </div>

        <DataTable
          columns={payoutColumns}
          data={filtered}
          getRowKey={p => p.id}
          loading={loading}
          emptyMessage={
            payouts.length === 0
              ? <div className="empty-state empty-state--illustrated">
                  <h3 className="empty-state-heading">No payouts yet</h3>
                  <p className="empty-state-text">Create a new payout when members are ready to receive disbursements.</p>
                </div>
              : 'No payouts match your search.'
          }
          getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
        />

        {hasMore && (
          <div className="ops-pager">
            <button
              type="button"
              className="btn btn--secondary"
              disabled={loadingMore || !nextCursor}
              onClick={() => void loadPayouts({ append: true, cursor: nextCursor })}
            >
              {loadingMore ? <><Spinner size="sm" /> Loading...</> : 'Load More'}
            </button>
          </div>
        )}

        <hr className="rule rule--strong" />
      </section>

      {canCreatePayout && (
        <NewPayoutModal
          open={showModal}
          onClose={() => setShowModal(false)}
          onSubmit={handleCreatePayout}
          isSubmitting={creatingPayout}
          members={memberList}
        />
      )}

      <MakerCheckerWarning
        open={overrideTarget !== null}
        onClose={() => setOverrideTarget(null)}
        isAdmin={canAccess(['ADMIN'])}
        onOverride={handlePayoutOverride}
        submitting={actionLoading !== null}
        action="approve"
      />

      <ConfirmDialog
        open={confirmPayoutId !== null}
        onClose={() => setConfirmPayoutId(null)}
        onConfirm={() => {
          if (!confirmPayoutId) return
          const id = confirmPayoutId
          setConfirmPayoutId(null)
          void handleApprovePayout(id)
        }}
        title="Approve Payout"
        description="Approve this payout for disbursement? Funds will be released to the member."
        confirmLabel="Approve Payout"
        variant="info"
        loading={actionLoading !== null}
      />
    </div>
  )
}
