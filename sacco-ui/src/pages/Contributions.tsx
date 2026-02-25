import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CurrencyCircleDollar, HandCoins, Plus } from '@phosphor-icons/react'
import { Spinner } from '../components/Spinner'
import { StatCardGrid } from '../components/StatCard'
import { DataTable } from '../components/DataTable'
import type { ColumnDef } from '../components/DataTable'
import { RecordContributionModal } from '../components/RecordContributionModal'
import { MonthPicker } from '../components/MonthPicker'
import { ApiError } from '../services/apiClient'
import {
  getContributions,
  getMemberContributions,
  getMemberContributionSummary,
  recordContribution,
} from '../services/contributionService'
import { getAllMembers } from '../services/memberService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useToast } from '../hooks/useToast'
import type { MemberResponse } from '../types/members'
import type {
  ContributionResponse,
  ContributionStatus,
  ContributionSummaryResponse,
  RecordContributionRequest,
} from '../types/contributions'
import { filterByMonth } from '../utils/contributions'
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
  const { canAccess, isMemberOnly } = useAuthorization()
  const { profile, loading: profileLoading } = useCurrentUser()
  const memberId = profile?.member?.id ?? null
  const canRecordContribution = canAccess(['ADMIN', 'TREASURER'])
  const canUseContributionOps = canAccess(['ADMIN', 'TREASURER'])
  const canManageCategories = canAccess(['ADMIN', 'TREASURER'])
  const toast = useToast()

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
  const [memberSummary, setMemberSummary] = useState<ContributionSummaryResponse | null>(null)

  const loadContributions = useCallback(async (opts?: { append?: boolean; cursor?: string | null }) => {
    const append = Boolean(opts?.append)
    const cursor = opts?.cursor

    if (append) setLoadingMore(true)
    else setLoading(true)

    try {
      if (isMemberOnly && !memberId) {
        setContributions([])
        setNextCursor(null)
        setHasMore(false)
        setMemberSummary(null)
        setFeedback({ type: 'error', text: 'Your account is not linked to a member profile.' })
        return
      }

      const page = isMemberOnly
        ? await getMemberContributions(memberId!, cursor ?? undefined, PAGE_SIZE, request, month)
        : await getContributions(cursor ?? undefined, PAGE_SIZE, request, month)

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
  }, [isMemberOnly, memberId, month, request])

  const loadMemberSummary = useCallback(async () => {
    if (!isMemberOnly || !memberId) {
      setMemberSummary(null)
      return
    }

    try {
      const summary = await getMemberContributionSummary(memberId, request)
      setMemberSummary(summary)
    } catch {
      setMemberSummary(null)
    }
  }, [isMemberOnly, memberId, request])

  useEffect(() => {
    if (profileLoading) return
    void loadContributions({ append: false, cursor: null })
  }, [loadContributions, profileLoading])

  useEffect(() => {
    if (profileLoading) return
    void loadMemberSummary()
  }, [loadMemberSummary, profileLoading])

  const monthContributions = useMemo(
    () => filterByMonth(contributions, month),
    [contributions, month],
  )

  const summary = useMemo(() => {
    const activeContributions = monthContributions.filter(c => c.status !== 'REVERSED')
    const grossCollected = activeContributions.reduce((sum, c) => sum + c.amount, 0)
    const netCollected = activeContributions.reduce((sum, c) => sum + (c.contributionAmount ?? c.amount), 0)
    const welfareCollected = activeContributions.reduce((sum, c) => sum + (c.welfareAmount ?? 0), 0)
    const confirmed = monthContributions.filter(c => c.status === 'CONFIRMED').length
    const pending = monthContributions.filter(c => c.status === 'PENDING').length
    const reversed = monthContributions.filter(c => c.status === 'REVERSED').length
    return { grossCollected, netCollected, welfareCollected, confirmed, pending, reversed, total: monthContributions.length }
  }, [monthContributions])

  const loadMembers = useCallback(async () => {
    if (isMemberOnly) {
      setMembers([])
      return
    }

    try {
      const allMembers = await getAllMembers(request)
      setMembers(allMembers)
    } catch {
      // Members will remain empty; modal member dropdown will be empty
    }
  }, [isMemberOnly, request])

  useEffect(() => {
    void loadMembers()
  }, [loadMembers])

  const memberMap = useMemo(() => {
    const map = new Map<string, string>()
    members.forEach(m => map.set(m.id, `${m.firstName} ${m.lastName}`.trim()))
    if (profile?.member) {
      map.set(profile.member.id, `${profile.member.firstName} ${profile.member.lastName}`.trim())
    }
    return map
  }, [members, profile?.member])

  const memberList = useMemo(
    () => members.map(m => ({ id: m.id, name: `${m.firstName} ${m.lastName}`.trim() })),
    [members],
  )

  const contributionColumns: ColumnDef<ContributionResponse>[] = useMemo(() => [
    {
      key: 'member',
      header: 'Member',
      render: (c) => memberMap.get(c.memberId) ?? c.memberId,
    },
    {
      key: 'category',
      header: 'Category',
      className: 'ledger-date',
      render: (c) => c.category.name,
    },
    {
      key: 'date',
      header: 'Date',
      className: 'data ledger-date',
      render: (c) => c.contributionDate ? fmtDate(c.contributionDate) : '\u2014',
    },
    {
      key: 'status',
      header: 'Status',
      render: (c) => (
        <span className={`badge ${statusClass[c.status]}`}>{c.status}</span>
      ),
    },
    {
      key: 'amount',
      header: 'Gross (KES)',
      className: 'amount ledger-table-amount',
      headerClassName: 'ledger-table-amount',
      render: (c) => c.amount > 0 ? fmt(c.amount) : '\u2014',
    },
    {
      key: 'contribution-amount',
      header: 'Net (KES)',
      className: 'amount ledger-table-amount',
      headerClassName: 'ledger-table-amount',
      render: (c) => (c.contributionAmount ?? c.amount) > 0 ? fmt(c.contributionAmount ?? c.amount) : '\u2014',
    },
    {
      key: 'welfare-amount',
      header: 'Welfare (KES)',
      className: 'amount ledger-table-amount',
      headerClassName: 'ledger-table-amount',
      render: (c) => (c.welfareAmount ?? 0) > 0 ? fmt(c.welfareAmount ?? 0) : '\u2014',
    },
  ], [memberMap])

  async function handleRecordContribution(payload: RecordContributionRequest) {
    if (!canRecordContribution) {
      throw new Error('You are not allowed to record contributions.')
    }

    setRecordingContribution(true)
    try {
      const created = await recordContribution(payload, request)
      setContributions(prev => [created, ...prev])
      setShowModal(false)
      toast.success('Contribution recorded', 'Contribution recorded successfully.')
    } catch (error) {
      const message = toErrorMessage(error, 'Unable to record contribution.')
      toast.error('Unable to record contribution', message)
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
        <div className="contrib-actions">
          {canUseContributionOps && (
            <Link to="/contribution-ops" className="btn btn--secondary">
              <CurrencyCircleDollar size={14} />
              Contribution Ops
            </Link>
          )}
          {canManageCategories && (
            <Link to="/contribution-categories" className="btn btn--secondary">
              <HandCoins size={14} />
              Categories
            </Link>
          )}
          {canRecordContribution && (
            <button className="btn btn--primary" onClick={() => setShowModal(true)}>
              <Plus size={14} weight="bold" />
              Record Contribution
            </button>
          )}
        </div>
      </div>

      <hr className="rule rule--strong" />

      <StatCardGrid
        items={[
          { label: 'Total', value: String(summary.total) },
          { label: 'Confirmed', value: String(summary.confirmed) },
          { label: 'Pending', value: String(summary.pending) },
          { label: 'Reversed', value: String(summary.reversed) },
          { label: 'Collected (Gross)', value: `KES ${fmt(summary.grossCollected)}` },
          { label: 'Collected (Net)', value: `KES ${fmt(summary.netCollected)}` },
          { label: 'Welfare Portion', value: `KES ${fmt(summary.welfareCollected)}` },
          ...(isMemberOnly && memberSummary ? [
            { label: 'Lifetime', value: `KES ${fmt(memberSummary.totalContributed)}` },
            { label: 'Pending Amount', value: `KES ${fmt(memberSummary.totalPending)}` },
            { label: 'Penalties', value: `KES ${fmt(memberSummary.totalPenalties)}`, valueClassName: 'amount--negative' },
          ] : []),
        ]}
      />

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

      <DataTable<ContributionResponse>
        columns={contributionColumns}
        data={monthContributions}
        getRowKey={row => row.id}
        loading={loading}
        emptyMessage={
          contributions.length === 0
            ? <div className="empty-state empty-state--illustrated">
                <h3 className="empty-state-heading">No contributions recorded</h3>
                <p className="empty-state-text">Record your first contribution to begin tracking member payments.</p>
              </div>
            : 'No contributions for the selected month.'
        }
      />

      {hasMore && (
        <div className="ops-pager">
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loadingMore || !nextCursor}
            onClick={() => void loadContributions({ append: true, cursor: nextCursor })}
          >
            {loadingMore ? <><Spinner size="sm" /> Loading...</> : 'Load More'}
          </button>
        </div>
      )}

      <hr className="rule rule--strong" />

      {canRecordContribution && (
        <RecordContributionModal
          open={showModal}
          onClose={() => setShowModal(false)}
          members={memberList}
          onSubmit={handleRecordContribution}
          isSubmitting={recordingContribution}
        />
      )}
    </div>
  )
}
