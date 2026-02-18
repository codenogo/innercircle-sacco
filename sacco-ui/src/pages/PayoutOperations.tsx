import './Operations.css'

type OpsStatus = 'PENDING' | 'APPROVED' | 'PROCESSING' | 'RECONCILE' | 'SIGNED_OFF'

interface PayoutOpsRow {
  ref: string
  member: string
  channel: string
  amount: number
  status: OpsStatus
}

const payoutOpsRows: PayoutOpsRow[] = [
  { ref: 'PO-009', member: 'Jane Wanjiku', channel: 'Payout', amount: 15000, status: 'PENDING' },
  { ref: 'BW-021', member: 'Peter Kamau', channel: 'Bank Withdrawal', amount: 60000, status: 'RECONCILE' },
  { ref: 'CD-014', member: 'David Mwangi', channel: 'Cash Disbursement', amount: 10000, status: 'SIGNED_OFF' },
  { ref: 'SW-007', member: 'Grace Njeri', channel: 'Share Withdrawal', amount: 20000, status: 'APPROVED' },
]

const statusClass: Record<OpsStatus, string> = {
  PENDING: 'badge--pending',
  APPROVED: 'badge--approved',
  PROCESSING: 'badge--processing',
  RECONCILE: 'badge--defaulted',
  SIGNED_OFF: 'badge--completed',
}

function fmt(n: number) { return n.toLocaleString('en-KE') }

export function PayoutOperations() {
  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Payout Operations</h1>
          <p className="page-subtitle">Approval and processing queues for payout channels</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <div className="ops-tags">
        <span className="ops-tag data">/api/v1/payouts</span>
        <span className="ops-tag data">/api/v1/bank-withdrawals</span>
        <span className="ops-tag data">/api/v1/cash-disbursements</span>
        <span className="ops-tag data">/api/v1/share-withdrawals</span>
      </div>

      <table className="ledger-table">
        <thead>
          <tr>
            <th className="label">Reference</th>
            <th className="label">Member</th>
            <th className="label">Channel</th>
            <th className="label ledger-table-amount">Amount (KES)</th>
            <th className="label">Status</th>
            <th className="label">Actions</th>
          </tr>
        </thead>
        <tbody>
          {payoutOpsRows.map((row, i) => (
            <tr key={row.ref} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
              <td className="data">{row.ref}</td>
              <td>{row.member}</td>
              <td>{row.channel}</td>
              <td className="amount ledger-table-amount">{fmt(row.amount)}</td>
              <td><span className={`badge ${statusClass[row.status]}`}>{row.status}</span></td>
              <td>
                <div className="ops-inline-actions">
                  <button type="button" className="btn btn--secondary btn--small" disabled>Approve</button>
                  <button type="button" className="btn btn--secondary btn--small" disabled>Process</button>
                  <button type="button" className="btn btn--secondary btn--small" disabled>Reconcile</button>
                  <button type="button" className="btn btn--secondary btn--small" disabled>Signoff</button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
