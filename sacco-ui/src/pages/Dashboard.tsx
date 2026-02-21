import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { ArrowUpRight, ArrowDownRight, TrendUp } from '@phosphor-icons/react'
import { SkeletonRow, SkeletonStat } from '../components/Skeleton'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { ApiError } from '../services/apiClient'
import type { TreasurerDashboardResponse, SaccoStateResponse } from '../types/dashboard'
import './Dashboard.css'

function formatAmount(amount: number): string {
  return amount.toLocaleString('en-KE')
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

const keyMetricColumns: ColumnDef<{ key: string; label: ReactNode; value: ReactNode }>[] = [
  { key: 'metric', header: 'Metric', render: row => row.label },
  { key: 'value', header: 'Value', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount', render: row => row.value },
]

export function Dashboard() {
  const { request } = useAuthenticatedApi()

  const [treasurerData, setTreasurerData] = useState<TreasurerDashboardResponse | null>(null)
  const [saccoState, setSaccoState] = useState<SaccoStateResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [treasurer, state] = await Promise.all([
        request<TreasurerDashboardResponse>('/api/v1/dashboard/treasurer'),
        request<SaccoStateResponse>('/api/v1/dashboard/state'),
      ])
      setTreasurerData(treasurer)
      setSaccoState(state)
    } catch (err) {
      setError(toErrorMessage(err, 'Unable to load dashboard data.'))
    } finally {
      setLoading(false)
    }
  }, [request])

  useEffect(() => {
    void loadDashboard()
  }, [loadDashboard])

  const today = new Date()
  const monthYear = today.toLocaleDateString('en-KE', { month: 'long', year: 'numeric' })
  const dateStr = today.toLocaleDateString('en-KE', { day: 'numeric', month: 'short', year: 'numeric' })

  if (loading) {
    return (
      <div className="dashboard">
        <header className="dashboard-header">
          <h1 className="heading dashboard-title">Group Fund Overview</h1>
          <div className="dashboard-meta">
            <span className="dashboard-period">Period: {monthYear}</span>
            <span className="dashboard-date">As at {dateStr}</span>
          </div>
        </header>
        <hr className="rule rule--strong" />
        <div className="dashboard-skeleton">
          <SkeletonStat />
          <SkeletonStat />
          <SkeletonRow cells={4} />
          <SkeletonRow cells={4} />
          <SkeletonRow cells={4} />
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="dashboard">
        <header className="dashboard-header">
          <h1 className="heading dashboard-title">Group Fund Overview</h1>
          <div className="dashboard-meta">
            <span className="dashboard-period">Period: {monthYear}</span>
            <span className="dashboard-date">As at {dateStr}</span>
          </div>
        </header>
        <hr className="rule rule--strong" />
        <div className="ops-feedback ops-feedback--error" role="alert">{error}</div>
      </div>
    )
  }

  const totalSavings = saccoState?.totalShareCapital ?? treasurerData?.totalShareCapital ?? 0
  const activeLoans = saccoState?.totalOutstandingLoans ?? 0
  const availableBalance = treasurerData?.cashPosition ?? 0

  const collected = treasurerData?.totalCollectionsThisMonth ?? 0
  const disbursed = treasurerData?.totalDisbursementsThisMonth ?? 0
  const collectionRate = saccoState?.loanRecoveryRate ?? 0

  const activeMembers = saccoState?.activeMembers ?? treasurerData?.activeMemberCount ?? 0
  const totalMembers = saccoState?.totalMembers ?? 0
  const pendingApprovals = treasurerData?.pendingApprovals ?? 0
  const overdueLoans = treasurerData?.overdueLoans ?? 0

  const fundSummary = [
    { label: 'Total Savings', amount: totalSavings, currency: 'KES' },
    { label: 'Active Loans', amount: activeLoans, currency: 'KES' },
    { label: 'Available Balance', amount: availableBalance, currency: 'KES' },
  ]

  return (
    <div className="dashboard">
      {/* Page heading */}
      <header className="dashboard-header">
        <h1 className="heading dashboard-title">Group Fund Overview</h1>
        <div className="dashboard-meta">
          <span className="dashboard-period">Period: {monthYear}</span>
          <span className="dashboard-date">As at {dateStr}</span>
        </div>
      </header>

      <hr className="rule rule--strong" />

      {/* Fund Summary */}
      <section className="dashboard-section">
        <h2 className="label dashboard-section-title">Fund Summary</h2>
        <hr className="rule" />
        <div className="fund-summary">
          {fundSummary.map(({ label, amount, currency }, i) => (
            <div key={label} className={`dot-leader ${i === fundSummary.length - 1 ? 'fund-summary-total' : ''}`}>
              <span className="fund-summary-label">{label}</span>
              <span className="dot-leader-value">
                <span className="fund-summary-currency">{currency}</span>{' '}
                {formatAmount(amount)}
              </span>
            </div>
          ))}
        </div>
        <hr className="rule rule--strong" />
      </section>

      {/* Monthly Collection */}
      <section className="dashboard-section">
        <h2 className="label dashboard-section-title">This Month's Collections</h2>
        <hr className="rule" />
        <div className="collection-grid">
          <div className="collection-block">
            <div className="dot-leader">
              <span>Collected</span>
              <span className="dot-leader-value amount--positive">KES {formatAmount(collected)}</span>
            </div>
            <div className="dot-leader">
              <span>Disbursed</span>
              <span className="dot-leader-value amount--negative">KES {formatAmount(disbursed)}</span>
            </div>
            <div className="dot-leader">
              <span>Overdue Loans</span>
              <span className="dot-leader-value amount--negative">{overdueLoans}</span>
            </div>
          </div>
          <div className="collection-rate">
            <div className="collection-rate-number">
              <TrendUp size={14} />
              <span className="data">{collectionRate.toFixed(1)}%</span>
            </div>
            <span className="collection-rate-label">Recovery Rate</span>
            <div className="collection-bar">
              <div
                className="collection-bar-fill"
                style={{ width: `${Math.min(collectionRate, 100)}%` }}
              />
            </div>
          </div>
        </div>
        <hr className="rule" />
      </section>

      {/* Key Metrics */}
      <section className="dashboard-section">
        <h2 className="label dashboard-section-title">Key Metrics</h2>
        <hr className="rule" />
        <DataTable<{ key: string; label: ReactNode; value: ReactNode }>
          columns={keyMetricColumns}
          data={[
            {
              key: 'contributions',
              label: (
                <span className="entry-type entry-type--in">
                  <ArrowDownRight size={12} weight="bold" />
                  Total Contributions
                </span>
              ),
              value: <>KES {formatAmount(saccoState?.totalContributions ?? 0)}</>,
            },
            {
              key: 'payouts',
              label: (
                <span className="entry-type entry-type--out">
                  <ArrowUpRight size={12} weight="bold" />
                  Total Payouts
                </span>
              ),
              value: <>KES {formatAmount(saccoState?.totalPayouts ?? 0)}</>,
            },
            { key: 'approvals', label: 'Pending Approvals', value: pendingApprovals },
            { key: 'growth', label: 'Member Growth Rate', value: `${(saccoState?.memberGrowthRate ?? 0).toFixed(1)}%` },
          ]}
          getRowKey={row => row.key}
          getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
        />
        <hr className="rule rule--strong" />
      </section>

      {/* Members */}
      <section className="dashboard-section">
        <h2 className="label dashboard-section-title">Members</h2>
        <hr className="rule" />
        <div className="member-summary">
          <span>Active: <strong className="data">{activeMembers}</strong> of {totalMembers}</span>
          <span className="member-summary-divider">|</span>
          <span>Pending Approvals: <strong className="data">{pendingApprovals}</strong></span>
        </div>
        <hr className="rule" />
      </section>
    </div>
  )
}
