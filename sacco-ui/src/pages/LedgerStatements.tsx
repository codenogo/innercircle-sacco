import { DataTable, type ColumnDef } from '../components/DataTable'
import './Operations.css'

function fmt(n: number) {
  return n.toLocaleString('en-KE')
}

interface TrialBalanceRow {
  account: string
  debit: number
  credit: number
}

const trialBalance: TrialBalanceRow[] = [
  { account: 'Cash at Bank', debit: 2476500, credit: 0 },
  { account: 'Loan Receivable', debit: 0, credit: 890000 },
  { account: 'Member Savings', debit: 0, credit: 2450000 },
  { account: 'Interest Revenue', debit: 0, credit: 145200 },
]

const trialColumns: ColumnDef<TrialBalanceRow>[] = [
  { key: 'account', header: 'Account', render: row => row.account },
  { key: 'debit', header: 'Debit (KES)', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount', render: row => row.debit > 0 ? fmt(row.debit) : '-' },
  { key: 'credit', header: 'Credit (KES)', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount', render: row => row.credit > 0 ? fmt(row.credit) : '-' },
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
        <DataTable
          columns={trialColumns}
          data={trialBalance}
          getRowKey={row => row.account}
          emptyMessage="No trial balance data."
          getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
          stickyTotals={
            <table className="datatable">
              <tfoot>
                <tr className="journal-totals">
                  <td className="label">Totals</td>
                  <td className="amount ledger-table-amount">{fmt(totalDebit)}</td>
                  <td className="amount ledger-table-amount">{fmt(totalCredit)}</td>
                </tr>
              </tfoot>
            </table>
          }
        />
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
