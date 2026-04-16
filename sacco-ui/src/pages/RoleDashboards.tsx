import { useState } from 'react'
import { DataTable, type ColumnDef } from '../components/DataTable'
import './Operations.css'

const roleCards = {
  member: [
    { label: 'Savings Balance', value: 'KES 135,000' },
    { label: 'Loan Balance', value: 'KES 43,200' },
    { label: 'Pending Contributions', value: 'KES 15,000' },
  ],
  treasurer: [
    { label: 'Group Fund', value: 'KES 2,450,000' },
    { label: 'Loans Outstanding', value: 'KES 890,000' },
    { label: 'Pending Payouts', value: 'KES 35,000' },
  ],
  admin: [
    { label: 'Active Users', value: '18' },
    { label: 'Locked Users', value: '2' },
    { label: 'Audit Events Today', value: '124' },
  ],
}

interface AnalyticsRow {
  metric: string
  value: string
}

const analyticsRows: AnalyticsRow[] = [
  { metric: 'Loan utilization rate', value: '63.8%' },
  { metric: 'Repayment success rate', value: '89.2%' },
  { metric: 'Interest realized (month)', value: 'KES 145,200' },
  { metric: 'Contribution completion', value: '80.6%' },
]

const analyticsColumns: ColumnDef<AnalyticsRow>[] = [
  { key: 'metric', header: 'Metric', render: row => row.metric },
  { key: 'value', header: 'Value', render: row => <span className="data">{row.value}</span> },
]

export function RoleDashboards() {
  const [role, setRole] = useState<'member' | 'treasurer' | 'admin'>('treasurer')

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Role Dashboards</h1>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <div className="filter-bar">
        <span className="page-section-title page-section-title--inline">Role</span>
        <select className="filter-select" value={role} onChange={e => setRole(e.target.value as 'member' | 'treasurer' | 'admin')}>
          <option value="member">Member</option>
          <option value="treasurer">Treasurer</option>
          <option value="admin">Admin</option>
        </select>
      </div>

      <div className="ops-kpi-grid">
        {roleCards[role].map(card => (
          <div key={card.label} className="ops-kpi">
            <span className="ops-kpi-label">{card.label}</span>
            <span className="ops-kpi-value">{card.value}</span>
          </div>
        ))}
      </div>

      <section className="page-section">
        <span className="page-section-title">Analytics</span>
        <hr className="rule" />
        <DataTable
          columns={analyticsColumns}
          data={analyticsRows}
          getRowKey={row => row.metric}
          emptyMessage="No analytics data."
          getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
        />
      </section>

    </div>
  )
}
