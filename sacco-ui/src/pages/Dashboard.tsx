import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { ArrowUpRight, ArrowDownRight } from '@phosphor-icons/react'
import { SkeletonRow, SkeletonStat } from '../components/Skeleton'
import { StatCardGrid } from '../components/StatCard'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { ApiError } from '../services/apiClient'
import { getInvestmentSummary } from '../services/investmentService'
import type { TreasurerDashboardResponse, SaccoStateResponse } from '../types/dashboard'
import type { InvestmentSummary } from '../types/investments'
import { INVESTMENT_TYPE_LABELS } from '../types/investments'
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
  const [investmentSummary, setInvestmentSummary] = useState<InvestmentSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [treasurer, state, investment] = await Promise.all([
        request<TreasurerDashboardResponse>('/api/v1/dashboard/treasurer'),
        request<SaccoStateResponse>('/api/v1/dashboard/state'),
        getInvestmentSummary(request).catch(() => null),
      ])
      setTreasurerData(treasurer)
      setSaccoState(state)
      setInvestmentSummary(investment)
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
  const collectedNet = treasurerData?.totalNetContributionsThisMonth ?? collected
  const welfareThisMonth = treasurerData?.totalWelfareThisMonth ?? 0
  const meetingFinesThisMonth = treasurerData?.totalMeetingFinesThisMonth ?? 0
  const welfareClaimsThisMonth = treasurerData?.totalWelfareClaimsThisMonth ?? 0
  const exitSettlementsThisMonth = treasurerData?.totalExitSettlementsThisMonth ?? 0
  const disbursed = treasurerData?.totalDisbursementsThisMonth ?? 0
  const collectionRate = saccoState?.loanRecoveryRate ?? 0

  const activeMembers = saccoState?.activeMembers ?? treasurerData?.activeMemberCount ?? 0
  const totalMembers = saccoState?.totalMembers ?? 0
  const pendingApprovals = treasurerData?.pendingApprovals ?? 0
  const overdueLoans = treasurerData?.overdueLoans ?? 0
  const investmentsValue = Math.max(investmentSummary?.currentValue ?? 0, 0)
  const marketReturnPct = investmentSummary && investmentSummary.totalInvested > 0
    ? (investmentSummary.unrealisedGain / investmentSummary.totalInvested) * 100
    : 0
  const totalReturnAmount = investmentSummary
    ? investmentSummary.unrealisedGain + investmentSummary.incomeYtd
    : 0
  const totalReturnPct = investmentSummary && investmentSummary.totalInvested > 0
    ? (totalReturnAmount / investmentSummary.totalInvested) * 100
    : 0

  const fundSummary = [
    { label: 'Total Savings', amount: totalSavings, currency: 'KES' },
    { label: 'Active Loans', amount: activeLoans, currency: 'KES' },
    { label: 'Investments', amount: investmentsValue, currency: 'KES' },
    { label: 'Available Balance', amount: availableBalance, currency: 'KES' },
  ]

  const savingsBase = Math.max(totalSavings, 0)
  const loansBase = Math.max(activeLoans, 0)
  const investmentsBase = investmentsValue
  const balanceBase = Math.abs(availableBalance)
  const allocationTotal = savingsBase + loansBase + investmentsBase + balanceBase
  const savingsPct = allocationTotal > 0 ? (savingsBase / allocationTotal) * 100 : 0
  const loansPct = allocationTotal > 0 ? (loansBase / allocationTotal) * 100 : 0
  const investmentsPct = allocationTotal > 0 ? (investmentsBase / allocationTotal) * 100 : 0
  const balancePct = allocationTotal > 0 ? (balanceBase / allocationTotal) * 100 : 0
  const isBalanceDeficit = availableBalance < 0

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
        <StatCardGrid
          items={fundSummary.map(({ label, amount, currency }) => ({
            label,
            value: `${currency} ${formatAmount(amount)}`,
            valueClassName: label === 'Available Balance' && amount < 0 ? 'amount--negative' : undefined,
          }))}
          columns={4}
        />
        {allocationTotal > 0 && (
          <>
            <div className="fund-allocation-bar">
              <div className="fund-allocation-segment fund-allocation-segment--savings" style={{ width: `${savingsPct}%` }} />
              <div className="fund-allocation-segment fund-allocation-segment--loans" style={{ width: `${loansPct}%` }} />
              <div className="fund-allocation-segment fund-allocation-segment--investments" style={{ width: `${investmentsPct}%` }} />
              <div
                className={`fund-allocation-segment ${isBalanceDeficit ? 'fund-allocation-segment--deficit' : 'fund-allocation-segment--balance'}`}
                style={{ width: `${balancePct}%` }}
              />
            </div>
            <div className="fund-allocation-legend">
              <span className="fund-allocation-legend-item"><span className="fund-allocation-dot fund-allocation-dot--savings" /> Savings ({savingsPct.toFixed(0)}%)</span>
              <span className="fund-allocation-legend-item"><span className="fund-allocation-dot fund-allocation-dot--loans" /> Loans ({loansPct.toFixed(0)}%)</span>
              <span className="fund-allocation-legend-item"><span className="fund-allocation-dot fund-allocation-dot--investments" /> Investments ({investmentsPct.toFixed(0)}%)</span>
              <span className="fund-allocation-legend-item">
                <span className={`fund-allocation-dot ${isBalanceDeficit ? 'fund-allocation-dot--deficit' : 'fund-allocation-dot--balance'}`} />
                {isBalanceDeficit ? 'Deficit' : 'Balance'} ({balancePct.toFixed(0)}%)
              </span>
            </div>
          </>
        )}
        <hr className="rule rule--strong" />
      </section>

      {/* Monthly Collection */}
      <section className="dashboard-section">
        <h2 className="label dashboard-section-title">This Month's Collections</h2>
        <hr className="rule" />
        <StatCardGrid
          items={[
            { label: 'Collected (Gross)', value: `KES ${formatAmount(collected)}`, valueClassName: 'amount--positive' },
            { label: 'Net Contributions', value: `KES ${formatAmount(collectedNet)}`, valueClassName: 'amount--positive' },
            { label: 'Welfare Portion', value: `KES ${formatAmount(welfareThisMonth)}` },
            { label: 'Meeting Fines', value: `KES ${formatAmount(meetingFinesThisMonth)}` },
            { label: 'Welfare Claims', value: `KES ${formatAmount(welfareClaimsThisMonth)}`, valueClassName: 'amount--negative' },
            { label: 'Exit Settlements', value: `KES ${formatAmount(exitSettlementsThisMonth)}`, valueClassName: 'amount--negative' },
            { label: 'Disbursed', value: `KES ${formatAmount(disbursed)}`, valueClassName: 'amount--negative' },
            { label: 'Overdue Loans', value: String(overdueLoans), valueClassName: overdueLoans > 0 ? 'amount--negative' : undefined },
            { label: 'Recovery Rate', value: `${collectionRate.toFixed(1)}%` },
          ]}
          columns={3}
        />
        {/* Color-coded recovery rate bar */}
        <div className="collection-bar" style={{ marginTop: 'var(--space-1)' }}>
          <div
            className={`collection-bar-fill ${collectionRate >= 80 ? 'collection-bar-fill--good' : collectionRate >= 50 ? 'collection-bar-fill--caution' : 'collection-bar-fill--poor'}`}
            style={{ width: `${Math.min(collectionRate, 100)}%` }}
          />
        </div>
        {/* Collected vs Disbursed comparison */}
        {(() => {
          const maxAmount = Math.max(collected, disbursed, 1)
          return (
            <div className="comparison-bars">
              <div className="comparison-bar-row">
                <span className="comparison-bar-label">Collected</span>
                <div className="comparison-bar-track">
                  <div className="comparison-bar-fill comparison-bar-fill--in" style={{ width: `${(collected / maxAmount) * 100}%` }} />
                </div>
                <span className="comparison-bar-amount">KES {formatAmount(collected)}</span>
              </div>
              <div className="comparison-bar-row">
                <span className="comparison-bar-label">Disbursed</span>
                <div className="comparison-bar-track">
                  <div className="comparison-bar-fill comparison-bar-fill--out" style={{ width: `${(disbursed / maxAmount) * 100}%` }} />
                </div>
                <span className="comparison-bar-amount">KES {formatAmount(disbursed)}</span>
              </div>
            </div>
          )
        })()}
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
                  Total Contributions (Gross)
                </span>
              ),
              value: <>KES {formatAmount(saccoState?.totalContributions ?? 0)}</>,
            },
            {
              key: 'net-contributions',
              label: (
                <span className="entry-type entry-type--in">
                  <ArrowDownRight size={12} weight="bold" />
                  Total Contributions (Net)
                </span>
              ),
              value: <>KES {formatAmount(saccoState?.totalNetContributions ?? 0)}</>,
            },
            {
              key: 'welfare-contributions',
              label: (
                <span className="entry-type entry-type--in">
                  <ArrowDownRight size={12} weight="bold" />
                  Welfare Contributions
                </span>
              ),
              value: <>KES {formatAmount(saccoState?.totalWelfareContributions ?? 0)}</>,
            },
            {
              key: 'meeting-fines',
              label: (
                <span className="entry-type entry-type--in">
                  <ArrowDownRight size={12} weight="bold" />
                  Meeting Fines (Total)
                </span>
              ),
              value: <>KES {formatAmount(saccoState?.totalMeetingFines ?? 0)}</>,
            },
            {
              key: 'welfare-benefits',
              label: (
                <span className="entry-type entry-type--out">
                  <ArrowUpRight size={12} weight="bold" />
                  Welfare Benefits Paid
                </span>
              ),
              value: <>KES {formatAmount(saccoState?.totalWelfareBenefitsPaid ?? 0)}</>,
            },
            {
              key: 'exit-settlements',
              label: (
                <span className="entry-type entry-type--out">
                  <ArrowUpRight size={12} weight="bold" />
                  Exit Settlements Paid
                </span>
              ),
              value: <>KES {formatAmount(saccoState?.totalExitSettlements ?? 0)}</>,
            },
            {
              key: 'exit-fees',
              label: (
                <span className="entry-type entry-type--in">
                  <ArrowDownRight size={12} weight="bold" />
                  Exit Fees Collected
                </span>
              ),
              value: <>KES {formatAmount(saccoState?.totalExitFees ?? 0)}</>,
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

      {/* Investment Portfolio */}
      {investmentSummary && (
        <section className="dashboard-section">
          <h2 className="label dashboard-section-title">Investment Portfolio</h2>
          <hr className="rule" />
          <StatCardGrid
            items={[
              { label: 'Total Invested', value: `KES ${formatAmount(investmentSummary.totalInvested)}` },
              { label: 'Current Value', value: `KES ${formatAmount(investmentSummary.currentValue)}` },
              {
                label: 'Unrealised Gain',
                value: `KES ${formatAmount(investmentSummary.unrealisedGain)}`,
                valueClassName: investmentSummary.unrealisedGain >= 0 ? 'amount--positive' : 'amount--negative',
              },
              { label: 'Income YTD', value: `KES ${formatAmount(investmentSummary.incomeYtd)}`, valueClassName: 'amount--positive' },
              {
                label: 'Market Return',
                value: `${marketReturnPct >= 0 ? '+' : ''}${marketReturnPct.toFixed(2)}%`,
                valueClassName: marketReturnPct >= 0 ? 'amount--positive' : 'amount--negative',
              },
              {
                label: 'Total Return',
                value: `${totalReturnPct >= 0 ? '+' : ''}${totalReturnPct.toFixed(2)}%`,
                valueClassName: totalReturnPct >= 0 ? 'amount--positive' : 'amount--negative',
              },
            ]}
            columns={3}
          />
          <StatCardGrid
            items={[
              {
                label: 'Total Return Amount',
                value: `KES ${formatAmount(totalReturnAmount)}`,
                valueClassName: totalReturnAmount >= 0 ? 'amount--positive' : 'amount--negative',
              },
            ]}
            columns={1}
          />
          <StatCardGrid
            items={[
              { label: 'Active', value: String(investmentSummary.activeCount) },
              { label: 'Matured', value: String(investmentSummary.maturedCount) },
              { label: 'Proposed', value: String(investmentSummary.proposedCount) },
              { label: 'Closed', value: String(investmentSummary.closedCount) },
            ]}
            columns={4}
          />
          {investmentSummary.byType.length > 0 && (
            <>
              <div className="dashboard-investment-alloc-bar">
                {investmentSummary.byType.map(segment => (
                  <div
                    key={segment.type}
                    className={`dashboard-investment-alloc-segment dashboard-investment-alloc-segment--${segment.type}`}
                    style={{ width: `${segment.percentage}%` }}
                    title={`${INVESTMENT_TYPE_LABELS[segment.type]}: ${segment.percentage}%`}
                  />
                ))}
              </div>
              <div className="dashboard-investment-alloc-legend">
                {investmentSummary.byType.map(segment => (
                  <span key={segment.type} className="dashboard-investment-alloc-legend-item">
                    <span className={`dashboard-investment-alloc-dot dashboard-investment-alloc-dot--${segment.type}`} />
                    {INVESTMENT_TYPE_LABELS[segment.type]} {segment.percentage}%
                  </span>
                ))}
              </div>
            </>
          )}
          <hr className="rule rule--strong" />
        </section>
      )}

      {/* Members */}
      <section className="dashboard-section">
        <h2 className="label dashboard-section-title">Members</h2>
        <hr className="rule" />
        <StatCardGrid
          items={[
            { label: 'Active Members', value: `${activeMembers} of ${totalMembers}` },
            { label: 'Pending Approvals', value: String(pendingApprovals) },
          ]}
          columns={2}
        />
        <hr className="rule" />
      </section>
    </div>
  )
}
