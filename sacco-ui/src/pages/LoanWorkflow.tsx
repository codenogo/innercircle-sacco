import { DataTable, type ColumnDef } from '../components/DataTable'
import './Operations.css'

type LoanStatus = 'PENDING' | 'APPROVED' | 'DISBURSED' | 'REJECTED'

interface LoanApplicationRow {
  id: string
  member: string
  amount: number
  tenure: string
  status: LoanStatus
}

const applications: LoanApplicationRow[] = [
  { id: 'LN-101', member: 'Sarah Wambui', amount: 40000, tenure: '12 months', status: 'PENDING' },
  { id: 'LN-102', member: 'John Ochieng', amount: 50000, tenure: '12 months', status: 'APPROVED' },
  { id: 'LN-103', member: 'Grace Njeri', amount: 30000, tenure: '9 months', status: 'DISBURSED' },
  { id: 'LN-104', member: 'Brian Otieno', amount: 45000, tenure: '12 months', status: 'REJECTED' },
]

const statusClass: Record<LoanStatus, string> = {
  PENDING: 'badge--pending',
  APPROVED: 'badge--approved',
  DISBURSED: 'badge--disbursed',
  REJECTED: 'badge--rejected',
}

function fmt(n: number) { return n.toLocaleString('en-KE') }

const columns: ColumnDef<LoanApplicationRow>[] = [
  { key: 'id', header: 'Loan ID', render: row => <span className="data">{row.id}</span> },
  { key: 'member', header: 'Member', render: row => row.member },
  { key: 'amount', header: 'Amount (KES)', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount', render: row => fmt(row.amount) },
  { key: 'tenure', header: 'Tenure', render: row => <span className="data">{row.tenure}</span> },
  { key: 'status', header: 'Status', render: row => <span className={`badge ${statusClass[row.status]}`}>{row.status}</span> },
  {
    key: 'actions', header: 'Actions', render: () => (
      <div className="ops-inline-actions">
        <button type="button" className="btn btn--secondary btn--small" disabled>Approve</button>
        <button type="button" className="btn btn--secondary btn--small" disabled>Reject</button>
        <button type="button" className="btn btn--secondary btn--small" disabled>Disburse</button>
        <button type="button" className="btn btn--secondary btn--small" disabled>Repay</button>
      </div>
    ),
  },
]

export function LoanWorkflow() {
  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Loan Workflow</h1>
          <p className="page-subtitle">Apply, approve, reject, disburse, repay, and schedule</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <DataTable
        columns={columns}
        data={applications}
        getRowKey={row => row.id}
        emptyMessage="No loan applications."
        getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
      />

      <p className="ops-note">
        Endpoints covered here: POST /api/v1/loans/apply, PATCH /api/v1/loans/{'{id}'}/approve, /reject, /disburse,
        POST /api/v1/loans/{'{id}'}/repay, and GET /api/v1/loans/{'{id}'}/schedule.
      </p>
    </div>
  )
}
