import { DataTable, type ColumnDef } from '../components/DataTable'
import './Operations.css'

interface LoanBenefitRow {
  member: string
  eligibleAmount: number
  utilizedAmount: number
  activeLoans: number
}

const benefitRows: LoanBenefitRow[] = [
  { member: 'John Ochieng', eligibleAmount: 108000, utilizedAmount: 50000, activeLoans: 1 },
  { member: 'Grace Njeri', eligibleAmount: 99000, utilizedAmount: 30000, activeLoans: 1 },
  { member: 'Sarah Wambui', eligibleAmount: 81000, utilizedAmount: 0, activeLoans: 0 },
]

function fmt(n: number) { return n.toLocaleString('en-KE') }

const columns: ColumnDef<LoanBenefitRow>[] = [
  { key: 'member', header: 'Member', render: row => row.member },
  { key: 'eligible', header: 'Eligible (KES)', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount', render: row => fmt(row.eligibleAmount) },
  { key: 'utilized', header: 'Utilized (KES)', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount', render: row => fmt(row.utilizedAmount) },
  { key: 'available', header: 'Available (KES)', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount', render: row => fmt(row.eligibleAmount - row.utilizedAmount) },
  { key: 'activeLoans', header: 'Active Loans', render: row => <span className="data">{row.activeLoans}</span> },
]

export function LoanBenefits() {
  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Loan Benefits</h1>
          <p className="page-subtitle">Eligibility and benefit snapshots</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <DataTable
        columns={columns}
        data={benefitRows}
        getRowKey={row => row.member}
        emptyMessage="No benefit data."
        getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
      />

      <p className="ops-note">
        Endpoints: GET /api/v1/loan-benefits/member/{'{memberId}'}, GET /api/v1/loan-benefits/loan/{'{loanId}'},
        GET /api/v1/loan-benefits, POST /api/v1/loan-benefits/refresh/{'{loanId}'}.
      </p>
    </div>
  )
}
