import './Operations.css'

function fmt(n: number) {
  return n.toLocaleString('en-KE')
}

const trialBalance = [
  { account: 'Cash at Bank', debit: 2476500, credit: 0 },
  { account: 'Loan Receivable', debit: 0, credit: 890000 },
  { account: 'Member Savings', debit: 0, credit: 2450000 },
  { account: 'Interest Revenue', debit: 0, credit: 145200 },
]

export function LedgerStatements() {
  const totalDebit = trialBalance.reduce((sum, row) => sum + row.debit, 0)
  const totalCredit = trialBalance.reduce((sum, row) => sum + row.credit, 0)

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Ledger Statements</h1>
          <p className="page-subtitle">Accounts, trial balance, income statement, and balance sheet</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <section className="page-section">
        <span className="page-section-title">Trial Balance</span>
        <hr className="rule" />
        <table className="ledger-table">
          <thead>
            <tr>
              <th className="label">Account</th>
              <th className="label ledger-table-amount">Debit (KES)</th>
              <th className="label ledger-table-amount">Credit (KES)</th>
            </tr>
          </thead>
          <tbody>
            {trialBalance.map((row, i) => (
              <tr key={row.account} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
                <td>{row.account}</td>
                <td className="amount ledger-table-amount">{row.debit > 0 ? fmt(row.debit) : '-'}</td>
                <td className="amount ledger-table-amount">{row.credit > 0 ? fmt(row.credit) : '-'}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="journal-totals">
              <td className="label">Totals</td>
              <td className="amount ledger-table-amount">{fmt(totalDebit)}</td>
              <td className="amount ledger-table-amount">{fmt(totalCredit)}</td>
            </tr>
          </tfoot>
        </table>
      </section>

      <section className="page-section">
        <span className="page-section-title">Income Statement</span>
        <hr className="rule" />
        <div className="dot-leader">
          <span>Interest income</span>
          <span className="dot-leader-value">KES {fmt(145200)}</span>
        </div>
        <div className="dot-leader">
          <span>Operating expenses</span>
          <span className="dot-leader-value">KES {fmt(24200)}</span>
        </div>
        <div className="dot-leader">
          <span>Net surplus</span>
          <span className="dot-leader-value amount--positive">KES {fmt(121000)}</span>
        </div>
      </section>

      <section className="page-section">
        <span className="page-section-title">Balance Sheet</span>
        <hr className="rule" />
        <div className="dot-leader">
          <span>Total assets</span>
          <span className="dot-leader-value">KES {fmt(3366500)}</span>
        </div>
        <div className="dot-leader">
          <span>Total liabilities</span>
          <span className="dot-leader-value">KES {fmt(2450000)}</span>
        </div>
        <div className="dot-leader">
          <span>Equity</span>
          <span className="dot-leader-value">KES {fmt(916500)}</span>
        </div>
      </section>

      <p className="ops-note">
        Backend endpoints: GET /api/v1/ledger/accounts, /journal-entries, /trial-balance, /income-statement, /balance-sheet.
      </p>
    </div>
  )
}
