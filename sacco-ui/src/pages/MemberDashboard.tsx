import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLineDown, Bank, SignOut, Wallet } from '@phosphor-icons/react'
import { StatCardGrid } from '../components/StatCard'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { SkeletonRow, SkeletonStat } from '../components/Skeleton'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { ApiError } from '../services/apiClient'
import { getMemberContributions, getMemberContributionSummary } from '../services/contributionService'
import { getMemberExitRequests } from '../services/policyWorkflowService'
import type { ContributionResponse, ContributionSummaryResponse } from '../types/contributions'
import type { LoanSummaryResponse } from '../types/loans'
import type { CursorPage } from '../types/users'
import type { PayoutResponse } from '../types/payouts'
import type { MemberExitRequestResponse } from '../types/policyWorkflows'
import './Dashboard.css'

function fmt(n: number | string | null | undefined): string {
  if (n == null) return '0'
  const parsed = typeof n === 'number' ? n : Number(n)
  if (Number.isNaN(parsed)) return '0'
  return parsed.toLocaleString('en-KE')
}

function fmtDate(value: string | null | undefined): string {
  if (!value) return '—'
  const parsed = new Date(value.length === 10 ? `${value}T00:00:00` : value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return parsed.toLocaleDateString('en-KE', { day: '2-digit', month: 'short', year: 'numeric' })
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

const OPEN_EXIT_STATUSES = new Set(['REQUESTED', 'UNDER_REVIEW', 'APPROVED', 'IN_PROGRESS'])

export function MemberDashboard() {
  const { request } = useAuthenticatedApi()
  const { profile, loading: profileLoading } = useCurrentUser()
  const memberId = profile?.member?.id ?? null

  const [contributionSummary, setContributionSummary] = useState<ContributionSummaryResponse | null>(null)
  const [recentContributions, setRecentContributions] = useState<ContributionResponse[]>([])
  const [loanSummary, setLoanSummary] = useState<LoanSummaryResponse | null>(null)
  const [pendingPayouts, setPendingPayouts] = useState<PayoutResponse[]>([])
  const [exitRequests, setExitRequests] = useState<MemberExitRequestResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    if (!memberId) return
    setLoading(true)
    setError(null)
    try {
      const [summary, contribPage, loans, payouts, exits] = await Promise.all([
        getMemberContributionSummary(memberId, request).catch(() => null),
        getMemberContributions(memberId, undefined, 5, request).catch(() => null),
        request<LoanSummaryResponse>(`/api/v1/loans/member/${memberId}/summary`).catch(() => null),
        request<CursorPage<PayoutResponse>>(`/api/v1/payouts/member/${memberId}?limit=20`).catch(() => null),
        getMemberExitRequests(memberId, request).catch(() => []),
      ])
      setContributionSummary(summary)
      setRecentContributions(contribPage?.items ?? [])
      setLoanSummary(loans)
      setPendingPayouts((payouts?.items ?? []).filter(p => p.status === 'PENDING' || p.status === 'APPROVED'))
      setExitRequests(exits ?? [])
    } catch (err) {
      setError(toErrorMessage(err, 'Unable to load your dashboard.'))
    } finally {
      setLoading(false)
    }
  }, [memberId, request])

  useEffect(() => {
    if (profileLoading) return
    void loadData()
  }, [loadData, profileLoading])

  const displayName = profile?.member
    ? `${profile.member.firstName} ${profile.member.lastName}`.trim()
    : profile?.username ?? 'Member'

  const openExitRequest = useMemo(
    () => exitRequests.find(r => OPEN_EXIT_STATUSES.has(r.status)) ?? null,
    [exitRequests],
  )

  const activeLoan = useMemo(
    () => loanSummary?.loans.find(l => l.status === 'DISBURSED' || l.status === 'REPAYING') ?? null,
    [loanSummary],
  )

  const pendingPayoutTotal = useMemo(
    () => pendingPayouts.reduce((sum, p) => sum + Number(p.amount ?? 0), 0),
    [pendingPayouts],
  )

  const contributionColumns: ColumnDef<ContributionResponse>[] = useMemo(() => [
    { key: 'date', header: 'Date', className: 'ledger-date', render: row => fmtDate(row.contributionDate) },
    { key: 'category', header: 'Category', render: row => row.category.name },
    { key: 'status', header: 'Status', render: row => row.status },
    { key: 'amount', header: 'Amount (KES)', className: 'amount ledger-table-amount', headerClassName: 'ledger-table-amount', render: row => fmt(row.amount) },
  ], [])

  if (profileLoading || loading) {
    return (
      <div className="dashboard">
        <header className="dashboard-header">
          <h1 className="heading dashboard-title">Welcome, {displayName}</h1>
        </header>
        <hr className="rule rule--strong" />
        <div className="dashboard-skeleton"><SkeletonStat /><SkeletonStat /><SkeletonRow cells={4} /><SkeletonRow cells={4} /></div>
      </div>
    )
  }

  if (!memberId) {
    return (
      <div className="dashboard">
        <header className="dashboard-header"><h1 className="heading dashboard-title">Welcome</h1></header>
        <hr className="rule rule--strong" />
        <div className="ops-feedback ops-feedback--error" role="alert">Your account is not linked to a member profile. Contact an administrator.</div>
      </div>
    )
  }

  const nextInstallment = loanSummary?.loans
    .flatMap(l => (l.outstandingBalance > 0 ? [l] : []))
    .sort((a, b) => Number(b.outstandingBalance) - Number(a.outstandingBalance))[0] ?? null

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1 className="heading dashboard-title">Welcome, {displayName}</h1>
        <div className="dashboard-meta">
          <span className="dashboard-period">Member {profile?.member?.memberNumber ?? ''}</span>
        </div>
      </header>
      <hr className="rule rule--strong" />

      {error && <div className="ops-feedback ops-feedback--error" role="alert">{error}</div>}

      {openExitRequest && (
        <div className="ops-feedback ops-feedback--info" role="status">
          You have an active exit request ({openExitRequest.status}). <Link to="/member-exit">Open request</Link>
        </div>
      )}

      <section className="dashboard-section">
        <h2 className="label dashboard-section-title">My Position</h2>
        <hr className="rule" />
        <StatCardGrid
          columns={4}
          items={[
            { label: 'Share Balance', value: `KES ${fmt(contributionSummary?.totalContributed ?? 0)}`, valueClassName: 'amount--positive' },
            { label: 'Active Loan Balance', value: `KES ${fmt(activeLoan?.outstandingBalance ?? 0)}`, valueClassName: activeLoan ? 'amount--negative' : undefined },
            { label: 'Pending Contributions', value: `KES ${fmt(contributionSummary?.totalPending ?? 0)}` },
            { label: 'Pending Payouts', value: `KES ${fmt(pendingPayoutTotal)}` },
          ]}
        />
      </section>

      {nextInstallment && (
        <section className="dashboard-section">
          <h2 className="label dashboard-section-title">Next Loan Action</h2>
          <hr className="rule" />
          <div className="settings-row">
            <div>
              <span className="settings-row-label">Loan {nextInstallment.loanNumber ?? nextInstallment.id.slice(0, 8)}</span>
              <span className="settings-row-desc">{nextInstallment.status} · {nextInstallment.interestRate}% · {nextInstallment.termMonths} months</span>
            </div>
            <span className="settings-row-value amount">KES {fmt(nextInstallment.outstandingBalance)}</span>
          </div>
          <hr className="rule" />
        </section>
      )}

      <section className="dashboard-section">
        <h2 className="label dashboard-section-title">Recent Contributions</h2>
        <hr className="rule" />
        <DataTable<ContributionResponse>
          columns={contributionColumns}
          data={recentContributions}
          getRowKey={row => row.id}
          emptyMessage="No contributions recorded yet."
        />
      </section>

      <section className="dashboard-section">
        <h2 className="label dashboard-section-title">Quick Actions</h2>
        <hr className="rule" />
        <div className="ops-inline-actions">
          <Link to="/contributions" className="btn btn--secondary"><Wallet size={14} /> My Contributions</Link>
          <Link to="/loans" className="btn btn--secondary"><Bank size={14} /> My Loans</Link>
          <Link to="/payouts" className="btn btn--secondary"><ArrowLineDown size={14} /> My Payouts</Link>
          <Link to="/member-exit" className="btn btn--ghost"><SignOut size={14} /> Exit Workflow</Link>
        </div>
      </section>
    </div>
  )
}
