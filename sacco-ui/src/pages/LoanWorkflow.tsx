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

      <table className="ledger-table">
        <thead>
          <tr>
            <th className="label">Loan ID</th>
            <th className="label">Member</th>
            <th className="label ledger-table-amount">Amount (KES)</th>
            <th className="label">Tenure</th>
            <th className="label">Status</th>
            <th className="label">Actions</th>
          </tr>
        </thead>
        <tbody>
          {applications.map((item, i) => (
            <tr key={item.id} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
              <td className="data">{item.id}</td>
              <td>{item.member}</td>
              <td className="amount ledger-table-amount">{fmt(item.amount)}</td>
              <td className="data">{item.tenure}</td>
              <td><span className={`badge ${statusClass[item.status]}`}>{item.status}</span></td>
              <td>
                <div className="ops-inline-actions">
                  <button type="button" className="btn btn--secondary btn--small" disabled>Approve</button>
                  <button type="button" className="btn btn--secondary btn--small" disabled>Reject</button>
                  <button type="button" className="btn btn--secondary btn--small" disabled>Disburse</button>
                  <button type="button" className="btn btn--secondary btn--small" disabled>Repay</button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <p className="ops-note">
        Endpoints covered here: POST /api/v1/loans/apply, PATCH /api/v1/loans/{'{id}'}/approve, /reject, /disburse,
        POST /api/v1/loans/{'{id}'}/repay, and GET /api/v1/loans/{'{id}'}/schedule.
      </p>
    </div>
  )
}
