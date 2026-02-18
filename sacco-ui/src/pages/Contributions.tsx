import { useCallback, useEffect, useMemo, useState } from 'react'
import { Plus } from 'lucide-react'
import { RecordContributionModal } from '../components/RecordContributionModal'
import { MonthPicker } from '../components/MonthPicker'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import type { CursorPage } from '../types/users'
import type { MemberResponse } from '../types/members'
import type { ContributionResponse, ContributionStatus, RecordContributionRequest } from '../types/contributions'
import './Contributions.css'

const PAGE_SIZE = 50

const statusClass: Record<ContributionStatus, string> = {
  CONFIRMED: 'badge--paid',
  PENDING: 'badge--pending',
  REVERSED: 'badge--overdue',
}

function fmt(n: number) { return n.toLocaleString('en-KE') }

function fmtDate(value: string): string {
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return '-'
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

function currentMonth(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

type FeedbackType = 'success' | 'error'

interface Feedback {
  type: FeedbackType
  text: string
}

export function Contributions() {
  const { request } = useAuthenticatedApi()

  const [contributions, setContributions] = useState<ContributionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [month, setMonth] = useState(currentMonth)
  const [showModal, setShowModal] = useState(false)
  const [members, setMembers] = useState<MemberResponse[]>([])
  const [recordingContribution, setRecordingContribution] = useState(false)

  const loadContributions = useCallback(async (opts?: { append?: boolean; cursor?: string | null }) => {
    const append = Boolean(opts?.append)
    const cursor = opts?.cursor

    if (append) setLoadingMore(true)
    else setLoading(true)

    try {
      const path = cursor
        ? `/api/v1/contributions?size=${PAGE_SIZE}&cursor=${encodeURIComponent(cursor)}`
        : `/api/v1/contributions?size=${PAGE_SIZE}`
      const page = await request<CursorPage<ContributionResponse>>(path)

      setContributions(prev => {
        if (!append) return page.items

        const merged = new Map<string, ContributionResponse>()
        prev.forEach(c => merged.set(c.id, c))
        page.items.forEach(c => merged.set(c.id, c))
        return Array.from(merged.values())
      })
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
      setFeedback(null)
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to load contributions.') })
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [request])

  useEffect(() => {
    void loadContributions({ append: false, cursor: null })
  }, [loadContributions])

  const monthContributions = useMemo(
    () => contributions.filter(c => c.contributionMonth === month),
    [contributions, month],
  )

  const summary = useMemo(() => {
    const collected = monthContributions.reduce((sum, c) => sum + c.amount, 0)
    const confirmed = monthContributions.filter(c => c.status === 'CONFIRMED').length
    const pending = monthContributions.filter(c => c.status === 'PENDING').length
    const reversed = monthContributions.filter(c => c.status === 'REVERSED').length
    return { collected, confirmed, pending, reversed, total: monthContributions.length }
  }, [monthContributions])

  const loadMembers = useCallback(async () => {
    try {
      const page = await request<CursorPage<MemberResponse>>('/api/v1/members?size=200')
      setMembers(page.items)
    } catch {
      // Members will remain empty; modal member dropdown will be empty
    }
  }, [request])

  useEffect(() => {
    void loadMembers()
  }, [loadMembers])

  const memberMap = useMemo(() => {
    const map = new Map<string, string>()
    members.forEach(m => map.set(m.id, `${m.firstName} ${m.lastName}`.trim()))
    return map
  }, [members])

  const memberList = useMemo(
    () => members.map(m => ({ id: m.id, name: `${m.firstName} ${m.lastName}`.trim() })),
    [members],
  )

  async function handleRecordContribution(payload: RecordContributionRequest) {
    setRecordingContribution(true)
    setFeedback(null)
    try {
      const created = await request<ContributionResponse>('/api/v1/contributions', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setContributions(prev => [created, ...prev])
      setShowModal(false)
      setFeedback({ type: 'success', text: 'Contribution recorded successfully.' })
    } catch (error) {
      const message = toErrorMessage(error, 'Unable to record contribution.')
      setFeedback({ type: 'error', text: message })
      throw error instanceof Error ? error : new Error(message)
    } finally {
      setRecordingContribution(false)
    }
  }

  return (
    <div className="contributions-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Contributions</h1>
          <p className="page-subtitle">Monthly contribution tracking</p>
        </div>
        <button className="btn btn--primary" onClick={() => setShowModal(true)}>
          <Plus size={14} strokeWidth={2} />
          Record Contribution
        </button>
      </div>

      <hr className="rule rule--strong" />

      <div className="page-summary">
        <span>Total: <strong>{summary.total}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Confirmed: <strong>{summary.confirmed}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Pending: <strong>{summary.pending}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Reversed: <strong>{summary.reversed}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Collected: <strong>KES {fmt(summary.collected)}</strong></span>
      </div>

      <hr className="rule" />

      {feedback && (
        <div className={`ops-feedback ops-feedback--${feedback.type}`} role="status">
          {feedback.text}
        </div>
      )}

      <section className="page-section">
        <div className="filter-bar">
          <span className="page-section-title page-section-title--inline">Month</span>
          <MonthPicker value={month} onChange={setMonth} />
        </div>

        <hr className="rule" />
      </section>

      <table className="ledger-table">
        <thead>
          <tr>
            <th className="label">Member</th>
            <th className="label">Category</th>
            <th className="label">Date</th>
            <th className="label">Status</th>
            <th className="label ledger-table-amount">Amount (KES)</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr><td colSpan={5} className="table-empty">Loading contributions...</td></tr>
          ) : monthContributions.length === 0 ? (
            <tr><td colSpan={5} className="table-empty">No contributions for the selected month.</td></tr>
          ) : monthContributions.map((c, i) => (
            <tr key={c.id} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
              <td>{memberMap.get(c.memberId) ?? c.memberId}</td>
              <td className="ledger-date">{c.category}</td>
              <td className="data ledger-date">{c.contributionDate ? fmtDate(c.contributionDate) : '\u2014'}</td>
              <td><span className={`badge ${statusClass[c.status]}`}>{c.status}</span></td>
              <td className="amount ledger-table-amount">
                {c.amount > 0 ? fmt(c.amount) : '\u2014'}
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
            onClick={() => void loadContributions({ append: true, cursor: nextCursor })}
          >
            {loadingMore ? 'Loading...' : 'Load More'}
          </button>
        </div>
      )}

      <hr className="rule rule--strong" />

      <RecordContributionModal
        open={showModal}
        onClose={() => setShowModal(false)}
        members={memberList}
        onSubmit={handleRecordContribution}
        isSubmitting={recordingContribution}
      />
    </div>
  )
}
