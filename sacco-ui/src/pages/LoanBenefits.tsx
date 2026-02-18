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

      <table className="ledger-table">
        <thead>
          <tr>
            <th className="label">Member</th>
            <th className="label ledger-table-amount">Eligible (KES)</th>
            <th className="label ledger-table-amount">Utilized (KES)</th>
            <th className="label ledger-table-amount">Available (KES)</th>
            <th className="label">Active Loans</th>
          </tr>
        </thead>
        <tbody>
          {benefitRows.map((row, i) => {
            const available = row.eligibleAmount - row.utilizedAmount
            return (
              <tr key={row.member} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
                <td>{row.member}</td>
                <td className="amount ledger-table-amount">{fmt(row.eligibleAmount)}</td>
                <td className="amount ledger-table-amount">{fmt(row.utilizedAmount)}</td>
                <td className="amount ledger-table-amount">{fmt(available)}</td>
                <td className="data">{row.activeLoans}</td>
              </tr>
            )
          })}
        </tbody>
      </table>

      <p className="ops-note">
        Endpoints: GET /api/v1/loan-benefits/member/{'{memberId}'}, GET /api/v1/loan-benefits/loan/{'{loanId}'},
        GET /api/v1/loan-benefits, POST /api/v1/loan-benefits/refresh/{'{loanId}'}.
      </p>
    </div>
  )
}
