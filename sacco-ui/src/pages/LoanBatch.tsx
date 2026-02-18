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

export function LoanBatch() {
  const totalArrears = unpaidLoans.reduce((sum, row) => sum + row.arrears, 0)

  return (
    <div className="ops-page">
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

      <table className="ledger-table">
        <thead>
          <tr>
            <th className="label">Loan ID</th>
            <th className="label">Member</th>
            <th className="label">Due Date</th>
            <th className="label ledger-table-amount">Installment</th>
            <th className="label ledger-table-amount">Arrears</th>
          </tr>
        </thead>
        <tbody>
          {unpaidLoans.map((loan, i) => (
            <tr key={loan.loanId} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
              <td className="data">{loan.loanId}</td>
              <td>{loan.member}</td>
              <td className="data">{loan.dueDate}</td>
              <td className="amount ledger-table-amount">{fmt(loan.installment)}</td>
              <td className="amount ledger-table-amount amount--negative">{fmt(loan.arrears)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <p className="ops-note">
        API map: POST /api/v1/loans/batch/process, GET /api/v1/loans/batch/unpaid,
        POST /api/v1/loans/reversals/repayment/{'{repaymentId}'}, POST /api/v1/loans/reversals/penalty/{'{penaltyId}'}.
      </p>
    </div>
  )
}
