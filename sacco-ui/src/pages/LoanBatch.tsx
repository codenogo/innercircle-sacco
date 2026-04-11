import { DataTable, type ColumnDef } from '../components/DataTable'
import { Breadcrumb } from '../components/Breadcrumb'
import './Operations.css'

interface UnpaidLoan {
  loanId: string
  member: string
  dueDate: string
  installment: number
  arrears: number
}

const unpaidLoans: UnpaidLoan[] = [
  { loanId: 'LN-004', member: 'James Kiprop', dueDate: '15 Feb 2026', installment: 9500, arrears: 19000 },
  { loanId: 'LN-007', member: 'Brian Otieno', dueDate: '15 Feb 2026', installment: 8200, arrears: 24600 },
]

function fmt(n: number) { return n.toLocaleString('en-KE') }

const columns: ColumnDef<UnpaidLoan>[] = [
  { key: 'loanId', header: 'Loan ID', render: row => <span className="data">{row.loanId}</span> },
  { key: 'member', header: 'Member', render: row => row.member },
  { key: 'dueDate', header: 'Due Date', render: row => <span className="data">{row.dueDate}</span> },
  { key: 'installment', header: 'Installment', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount', render: row => fmt(row.installment) },
  { key: 'arrears', header: 'Arrears', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount amount--negative', render: row => fmt(row.arrears) },
]

export function LoanBatch() {
  const totalArrears = unpaidLoans.reduce((sum, row) => sum + row.arrears, 0)

  return (
    <div className="ops-page">
      <Breadcrumb items={[
        { label: 'Operations', to: '/operations' },
        { label: 'Loan Batch Processing' },
      ]} />
      <div className="page-header">
        <div>
          <h1 className="page-title">Loan Batch Processing</h1>
          <p className="page-subtitle">Batch interest, unpaid queues, and reversal workflows</p>
        </div>
        <div className="ops-inline-actions">
          <button type="button" className="btn btn--primary" disabled>Run Monthly Batch</button>
          <button type="button" className="btn btn--secondary" disabled>Reverse Repayment</button>
          <button type="button" className="btn btn--secondary" disabled>Reverse Penalty</button>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <div className="ops-kpi-grid">
        <div className="ops-kpi">
          <span className="ops-kpi-label">Unpaid Loans</span>
          <span className="ops-kpi-value">{unpaidLoans.length}</span>
        </div>
        <div className="ops-kpi">
          <span className="ops-kpi-label">Arrears Total</span>
          <span className="ops-kpi-value">KES {fmt(totalArrears)}</span>
        </div>
        <div className="ops-kpi">
          <span className="ops-kpi-label">Next Batch Window</span>
          <span className="ops-kpi-value">01 Mar 2026</span>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={unpaidLoans}
        getRowKey={row => row.loanId}
        emptyMessage="No unpaid loans."
        getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
      />

      <p className="ops-note">
        API map: POST /api/v1/loans/batch/process, GET /api/v1/loans/batch/unpaid,
        POST /api/v1/loans/reversals/repayment/{'{repaymentId}'}, POST /api/v1/loans/reversals/penalty/{'{penaltyId}'}.
      </p>
    </div>
  )
}
