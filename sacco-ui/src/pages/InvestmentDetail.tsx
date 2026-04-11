import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft } from '@phosphor-icons/react'
import { Breadcrumb } from '../components/Breadcrumb'
import { StatCardGrid } from '../components/StatCard'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { Modal } from '../components/Modal'
import { Select } from '../components/Select'
import { DatePicker } from '../components/DatePicker'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ApiError } from '../services/apiClient'
import {
  activateInvestment,
  approveInvestment,
  disposeInvestment,
  getInvestment,
  getInvestmentIncome,
  getValuations,
  recordIncome,
  recordValuation,
  rejectInvestment,
  rollOverInvestment,
} from '../services/investmentService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { useToast } from '../hooks/useToast'
import type {
  InvestmentResponse,
  InvestmentIncomeResponse,
  InvestmentValuationResponse,
  IncomeType,
  RecordIncomeRequest,
  RecordValuationRequest,
  DisposeInvestmentRequest,
  RollOverRequest,
} from '../types/investments'
import { INVESTMENT_TYPE_LABELS, INCOME_TYPE_LABELS } from '../types/investments'
import './Investments.css'

/* ─── helpers ─── */

function fmt(n: number | string): string {
  const parsed = typeof n === 'number' ? n : Number(n)
  if (Number.isNaN(parsed)) return '0'
  return parsed.toLocaleString('en-KE')
}

function fmtDate(value: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-KE', { day: '2-digit', month: 'short', year: 'numeric' })
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

const UNIT_TYPES = ['UNIT_TRUST', 'EQUITY']

const statusClass: Record<string, string> = {
  PROPOSED: 'badge--pending',
  APPROVED: 'badge--pending',
  ACTIVE: 'badge--active',
  MATURED: 'badge--completed',
  PARTIALLY_DISPOSED: 'badge--pending',
  ROLLED_OVER: 'badge--active',
  CLOSED: 'badge--completed',
  REJECTED: 'badge--defaulted',
}

const INCOME_TYPE_OPTIONS = Object.entries(INCOME_TYPE_LABELS).map(([v, l]) => ({ value: v, label: l }))

/* ─── component ─── */

export function InvestmentDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { request } = useAuthenticatedApi()
  const { canAccess } = useAuthorization()
  const toast = useToast()
  const canManage = canAccess(['ADMIN', 'TREASURER'])

  /* state */
  const [investment, setInvestment] = useState<InvestmentResponse | null>(null)
  const [income, setIncome] = useState<InvestmentIncomeResponse[]>([])
  const [valuations, setValuations] = useState<InvestmentValuationResponse[]>([])
  const [loading, setLoading] = useState(true)

  /* modal states */
  const [showIncomeModal, setShowIncomeModal] = useState(false)
  const [showValuationModal, setShowValuationModal] = useState(false)
  const [showDisposeModal, setShowDisposeModal] = useState(false)
  const [showRolloverModal, setShowRolloverModal] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  /* income form */
  const [incType, setIncType] = useState('')
  const [incAmount, setIncAmount] = useState('')
  const [incDate, setIncDate] = useState('')
  const [incRef, setIncRef] = useState('')
  const [incNotes, setIncNotes] = useState('')

  /* valuation form */
  const [valMarketValue, setValMarketValue] = useState('')
  const [valNav, setValNav] = useState('')
  const [valDate, setValDate] = useState('')
  const [valSource, setValSource] = useState('')

  /* dispose form */
  const [dispType, setDispType] = useState<string>('FULL')
  const [dispProceeds, setDispProceeds] = useState('')
  const [dispFees, setDispFees] = useState('')
  const [dispDate, setDispDate] = useState('')
  const [dispUnits, setDispUnits] = useState('')
  const [dispNotes, setDispNotes] = useState('')

  /* rollover form */
  const [rollMaturity, setRollMaturity] = useState('')
  const [rollRate, setRollRate] = useState('')
  const [rollNotes, setRollNotes] = useState('')

  /* confirm */
  const [confirmAction, setConfirmAction] = useState<'approve' | 'reject' | 'activate' | null>(null)
  const [actionLoading, setActionLoading] = useState(false)

  const isUnitType = investment ? UNIT_TYPES.includes(investment.investmentType) : false

  /* ─── data loading ─── */

  const loadData = useCallback(async () => {
    if (!id) return
    setLoading(true)
    try {
      const [inv, inc, val] = await Promise.all([
        getInvestment(id, request),
        getInvestmentIncome(id, request),
        getValuations(id, request),
      ])
      setInvestment(inv)
      setIncome(inc)
      setValuations(val)
    } catch (err) {
      toast.error('Load failed', toErrorMessage(err, 'Unable to load investment details.'))
      setInvestment(null)
      setIncome([])
      setValuations([])
    } finally {
      setLoading(false)
    }
  }, [id, request, toast])

  useEffect(() => { void loadData() }, [loadData])

  /* ─── handlers ─── */

  async function handleRecordIncome(e: FormEvent) {
    e.preventDefault()
    if (!id) return
    setSubmitting(true)
    try {
      const payload: RecordIncomeRequest = {
        incomeType: incType as IncomeType,
        amount: Number(incAmount),
        incomeDate: incDate,
        ...(incRef.trim() ? { referenceNumber: incRef.trim() } : {}),
        ...(incNotes.trim() ? { notes: incNotes.trim() } : {}),
      }
      const created = await recordIncome(id, payload, request)
      setIncome(prev => [created, ...prev])
      setShowIncomeModal(false)
      setIncType(''); setIncAmount(''); setIncDate(''); setIncRef(''); setIncNotes('')
      toast.success('Income recorded', 'Investment income recorded successfully.')
    } catch (err) {
      toast.error('Record failed', toErrorMessage(err, 'Unable to record income.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRecordValuation(e: FormEvent) {
    e.preventDefault()
    if (!id) return
    setSubmitting(true)
    try {
      const payload: RecordValuationRequest = {
        ...(valMarketValue ? { marketValue: Number(valMarketValue) } : {}),
        ...(valNav ? { navPerUnit: Number(valNav) } : {}),
        valuationDate: valDate,
        source: valSource.trim(),
      }
      await recordValuation(id, payload, request)
      await loadData()
      setShowValuationModal(false)
      setValMarketValue(''); setValNav(''); setValDate(''); setValSource('')
      toast.success('Valuation recorded', 'Investment valuation updated successfully.')
    } catch (err) {
      toast.error('Record failed', toErrorMessage(err, 'Unable to record valuation.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDispose(e: FormEvent) {
    e.preventDefault()
    if (!id) return
    setSubmitting(true)
    try {
      const payload: DisposeInvestmentRequest = {
        disposalType: dispType as 'FULL' | 'PARTIAL' | 'MATURITY',
        proceedsAmount: Number(dispProceeds),
        fees: Number(dispFees) || 0,
        disposalDate: dispDate,
        ...(dispUnits ? { unitsRedeemed: Number(dispUnits) } : {}),
        ...(dispNotes.trim() ? { notes: dispNotes.trim() } : {}),
      }
      const updated = await disposeInvestment(id, payload, request)
      setInvestment(updated)
      setShowDisposeModal(false)
      setDispType('FULL'); setDispProceeds(''); setDispFees(''); setDispDate(''); setDispUnits(''); setDispNotes('')
      toast.success('Investment disposed', 'Investment disposal recorded successfully.')
    } catch (err) {
      toast.error('Dispose failed', toErrorMessage(err, 'Unable to dispose investment.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRollover(e: FormEvent) {
    e.preventDefault()
    if (!id) return
    setSubmitting(true)
    try {
      const payload: RollOverRequest = {
        newMaturityDate: rollMaturity,
        newInterestRate: Number(rollRate),
        ...(rollNotes.trim() ? { notes: rollNotes.trim() } : {}),
      }
      const updated = await rollOverInvestment(id, payload, request)
      setInvestment(updated)
      setShowRolloverModal(false)
      setRollMaturity(''); setRollRate(''); setRollNotes('')
      toast.success('Investment rolled over', 'Investment rolled over to new term.')
    } catch (err) {
      toast.error('Rollover failed', toErrorMessage(err, 'Unable to roll over investment.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleAction(action: 'approve' | 'reject' | 'activate') {
    if (!id) return
    setActionLoading(true)
    try {
      const updated = action === 'approve'
        ? await approveInvestment(id, request)
        : action === 'reject'
          ? await rejectInvestment(id, undefined, request)
          : await activateInvestment(id, request)
      setInvestment(updated)
      toast.success(
        `Investment ${action}d`,
        `Investment ${action === 'approve' ? 'approved' : action === 'reject' ? 'rejected' : 'activated'} successfully.`,
      )
    } catch (err) {
      toast.error(`Unable to ${action}`, toErrorMessage(err, `Unable to ${action} investment.`))
    } finally {
      setActionLoading(false)
    }
  }

  /* ─── columns ─── */

  const incomeColumns: ColumnDef<InvestmentIncomeResponse>[] = useMemo(() => [
    { key: 'date', header: 'Date', className: 'ledger-date', render: r => fmtDate(r.incomeDate) },
    { key: 'type', header: 'Type', className: 'data', render: r => INCOME_TYPE_LABELS[r.incomeType] },
    { key: 'amount', header: 'Amount (KES)', className: 'amount ledger-table-amount amount--positive', headerClassName: 'ledger-table-amount', render: r => fmt(r.amount) },
    { key: 'ref', header: 'Reference', className: 'data', render: r => r.referenceNumber ?? '—' },
    { key: 'notes', header: 'Notes', render: r => r.notes ?? '—' },
  ], [])

  const valuationColumns: ColumnDef<InvestmentValuationResponse>[] = useMemo(() => [
    { key: 'date', header: 'Date', className: 'ledger-date', render: r => fmtDate(r.valuationDate) },
    { key: 'value', header: 'Market Value (KES)', className: 'amount ledger-table-amount', headerClassName: 'ledger-table-amount', render: r => fmt(r.marketValue) },
    ...(isUnitType ? [{ key: 'nav', header: 'NAV / Unit', className: 'amount ledger-table-amount', headerClassName: 'ledger-table-amount', render: (r: InvestmentValuationResponse) => r.navPerUnit != null ? fmt(r.navPerUnit) : '—' }] : []),
    { key: 'source', header: 'Source', render: r => r.source },
  ], [isUnitType])

  const totalIncome = useMemo(() => income.reduce((sum, i) => sum + i.amount, 0), [income])

  /* ─── render ─── */

  if (loading) {
    return (
      <div className="ops-page">
        <Breadcrumb items={[{ label: 'Investments', to: '/investments' }, { label: 'Loading...' }]} />
        <p style={{ padding: 'var(--space-4)', color: 'var(--ink-muted)' }}>Loading investment details…</p>
      </div>
    )
  }

  if (!investment) {
    return (
      <div className="ops-page">
        <Breadcrumb items={[{ label: 'Investments', to: '/investments' }, { label: 'Not Found' }]} />
        <div className="empty-state empty-state--illustrated">
          <h3 className="empty-state-heading">Investment not found</h3>
          <p className="empty-state-text">The investment may have been removed or the link is invalid.</p>
          <button className="btn btn--secondary" onClick={() => navigate('/investments')}>
            <ArrowLeft size={14} /> Back to Investments
          </button>
        </div>
      </div>
    )
  }

  const gain = investment.currentValue - investment.purchasePrice
  const gainPct = investment.purchasePrice > 0 ? ((gain / investment.purchasePrice) * 100).toFixed(1) : '0.0'
  const isActive = ['ACTIVE', 'PARTIALLY_DISPOSED', 'ROLLED_OVER'].includes(investment.status)
  const isMatured = investment.status === 'MATURED'
  const isProposed = investment.status === 'PROPOSED'
  const isApproved = investment.status === 'APPROVED'

  return (
    <div className="ops-page">
      <Breadcrumb items={[{ label: 'Investments', to: '/investments' }, { label: investment.referenceNumber }]} />

      <div className="page-header">
        <div>
          <h1 className="page-title">{investment.name}</h1>
          <p className="page-subtitle">
            {investment.institution} · <span className={`badge ${statusClass[investment.status]}`}>{investment.status}</span>
          </p>
        </div>
        <button className="btn btn--secondary" onClick={() => navigate('/investments')}>
          <ArrowLeft size={14} /> Back
        </button>
      </div>

      <hr className="rule rule--strong" />

      {/* Detail info grid */}
      <section className="page-section">
        <span className="page-section-title">Investment Details</span>
        <hr className="rule" />
        <div className="inv-detail-grid">
          <div className="inv-detail-item"><span className="inv-detail-label">Reference</span><span className="inv-detail-value">{investment.referenceNumber}</span></div>
          <div className="inv-detail-item"><span className="inv-detail-label">Type</span><span className="inv-detail-value">{INVESTMENT_TYPE_LABELS[investment.investmentType]}</span></div>
          <div className="inv-detail-item"><span className="inv-detail-label">Institution</span><span className="inv-detail-value">{investment.institution}</span></div>
          <div className="inv-detail-item"><span className="inv-detail-label">Face Value</span><span className="inv-detail-value">KES {fmt(investment.faceValue)}</span></div>
          <div className="inv-detail-item"><span className="inv-detail-label">Purchase Price</span><span className="inv-detail-value">KES {fmt(investment.purchasePrice)}</span></div>
          <div className="inv-detail-item"><span className="inv-detail-label">Current Value</span><span className="inv-detail-value">KES {fmt(investment.currentValue)}</span></div>
          <div className="inv-detail-item"><span className="inv-detail-label">Return</span><span className={`inv-detail-value ${gain >= 0 ? 'amount--positive' : 'amount--negative'}`}>{gain >= 0 ? '+' : ''}{gainPct}% (KES {fmt(Math.abs(gain))})</span></div>
          {investment.interestRate > 0 && <div className="inv-detail-item"><span className="inv-detail-label">Interest Rate</span><span className="inv-detail-value">{investment.interestRate}%</span></div>}
          <div className="inv-detail-item"><span className="inv-detail-label">Purchase Date</span><span className="inv-detail-value">{fmtDate(investment.purchaseDate)}</span></div>
          <div className="inv-detail-item"><span className="inv-detail-label">Maturity Date</span><span className="inv-detail-value">{fmtDate(investment.maturityDate)}</span></div>
          {isUnitType && investment.units != null && <div className="inv-detail-item"><span className="inv-detail-label">Units Held</span><span className="inv-detail-value">{fmt(investment.units)}</span></div>}
          {isUnitType && investment.navPerUnit != null && <div className="inv-detail-item"><span className="inv-detail-label">NAV / Unit</span><span className="inv-detail-value">KES {fmt(investment.navPerUnit)}</span></div>}
          {investment.notes && <div className="inv-detail-item"><span className="inv-detail-label">Notes</span><span className="inv-detail-value">{investment.notes}</span></div>}
          {investment.approvedBy && <div className="inv-detail-item"><span className="inv-detail-label">Approved By</span><span className="inv-detail-value">{investment.approvedBy} on {fmtDate(investment.approvedAt)}</span></div>}
        </div>
      </section>

      <hr className="rule" />

      {/* Action buttons */}
      {canManage && (
        <div className="inv-actions-bar">
          {isProposed && (
            <>
              <button className="btn btn--primary" onClick={() => setConfirmAction('approve')} disabled={actionLoading}>Approve</button>
              <button className="btn btn--danger" onClick={() => setConfirmAction('reject')} disabled={actionLoading}>Reject</button>
            </>
          )}
          {isApproved && (
            <button className="btn btn--primary" onClick={() => setConfirmAction('activate')} disabled={actionLoading}>Activate</button>
          )}
          {isActive && (
            <>
              <button className="btn btn--primary" onClick={() => setShowIncomeModal(true)}>Record Income</button>
              <button className="btn btn--secondary" onClick={() => setShowValuationModal(true)}>Update Valuation</button>
              <button className="btn btn--secondary" onClick={() => setShowDisposeModal(true)}>Dispose / Mature</button>
            </>
          )}
          {(isActive || isMatured) && investment.maturityDate && (
            <button className="btn btn--secondary" onClick={() => setShowRolloverModal(true)}>Roll Over</button>
          )}
        </div>
      )}

      {/* Income summary + table */}
      <section className="page-section">
        <span className="page-section-title">Income History</span>
        <hr className="rule" />
        <StatCardGrid
          items={[
            { label: 'Total Income', value: `KES ${fmt(totalIncome)}`, valueClassName: 'amount--positive' },
            { label: 'Records', value: String(income.length) },
          ]}
          columns={2}
        />
        <DataTable<InvestmentIncomeResponse>
          columns={incomeColumns}
          data={income}
          getRowKey={r => r.id}
          emptyMessage="No income recorded yet."
        />
        <hr className="rule rule--strong" />
      </section>

      {/* Valuations table */}
      <section className="page-section">
        <span className="page-section-title">Valuation History</span>
        <hr className="rule" />
        <DataTable<InvestmentValuationResponse>
          columns={valuationColumns}
          data={valuations}
          getRowKey={r => r.id}
          emptyMessage="No valuations recorded yet."
        />
        <hr className="rule rule--strong" />
      </section>

      {/* Record Income Modal */}
      <Modal
        open={showIncomeModal}
        onClose={() => { if (!submitting) setShowIncomeModal(false) }}
        title="Record Income"
        subtitle={`Record income for ${investment.name}`}
        footer={
          <>
            <button className="btn btn--secondary" type="button" onClick={() => setShowIncomeModal(false)} disabled={submitting}>Cancel</button>
            <button className="btn btn--primary" type="submit" form="income-form" disabled={submitting}>
              {submitting ? 'Recording...' : 'Record Income'}
            </button>
          </>
        }
      >
        <form id="income-form" className="modal-form" onSubmit={e => void handleRecordIncome(e)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Income Type</label>
              <Select options={INCOME_TYPE_OPTIONS} value={incType} onChange={setIncType} placeholder="Select type..." required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Amount (KES)</label>
              <input className="field-input" type="number" min={0} required disabled={submitting} value={incAmount} onChange={e => setIncAmount(e.target.value)} placeholder="0" />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Income Date</label>
              <DatePicker value={incDate} onChange={setIncDate} required />
            </div>
            <div className="field">
              <label className="field-label">Reference Number</label>
              <input className="field-input" disabled={submitting} value={incRef} onChange={e => setIncRef(e.target.value)} placeholder="e.g. INT-Q1-2025" />
            </div>
          </div>
          <div className="field">
            <label className="field-label">Notes</label>
            <textarea className="field-input" disabled={submitting} value={incNotes} onChange={e => setIncNotes(e.target.value)} placeholder="Additional details..." />
          </div>
        </form>
      </Modal>

      {/* Record Valuation Modal */}
      <Modal
        open={showValuationModal}
        onClose={() => { if (!submitting) setShowValuationModal(false) }}
        title="Update Valuation"
        subtitle={`Record new valuation for ${investment.name}`}
        footer={
          <>
            <button className="btn btn--secondary" type="button" onClick={() => setShowValuationModal(false)} disabled={submitting}>Cancel</button>
            <button className="btn btn--primary" type="submit" form="valuation-form" disabled={submitting}>
              {submitting ? 'Recording...' : 'Record Valuation'}
            </button>
          </>
        }
      >
        <form id="valuation-form" className="modal-form" onSubmit={e => void handleRecordValuation(e)}>
          {isUnitType ? (
            <div className="field">
              <label className="field-label field-label--required">NAV per Unit (KES)</label>
              <input className="field-input" type="number" min={0} step="0.01" required disabled={submitting} value={valNav} onChange={e => setValNav(e.target.value)} placeholder="0.00" />
              <span className="field-hint">Total value will be calculated as units × NAV</span>
            </div>
          ) : (
            <div className="field">
              <label className="field-label field-label--required">Market Value (KES)</label>
              <input className="field-input" type="number" min={0} required disabled={submitting} value={valMarketValue} onChange={e => setValMarketValue(e.target.value)} placeholder="0" />
            </div>
          )}
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Valuation Date</label>
              <DatePicker value={valDate} onChange={setValDate} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Source / Valuer</label>
              <input className="field-input" required disabled={submitting} value={valSource} onChange={e => setValSource(e.target.value)} placeholder="e.g. Oak Capital portal" />
            </div>
          </div>
        </form>
      </Modal>


      {/* Dispose Modal */}
      <Modal
        open={showDisposeModal}
        onClose={() => { if (!submitting) setShowDisposeModal(false) }}
        title="Dispose / Mature Investment"
        subtitle={`Dispose or record maturity for ${investment.name}`}
        footer={
          <>
            <button className="btn btn--secondary" type="button" onClick={() => setShowDisposeModal(false)} disabled={submitting}>Cancel</button>
            <button className="btn btn--primary" type="submit" form="dispose-form" disabled={submitting}>
              {submitting ? 'Processing...' : 'Record Disposal'}
            </button>
          </>
        }
      >
        <form id="dispose-form" className="modal-form" onSubmit={e => void handleDispose(e)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Disposal Type</label>
              <Select
                options={[
                  { value: 'FULL', label: 'Full Disposal' },
                  { value: 'PARTIAL', label: 'Partial Disposal' },
                  { value: 'MATURITY', label: 'Maturity Redemption' },
                ]}
                value={dispType}
                onChange={setDispType}
                required
              />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Disposal Date</label>
              <DatePicker value={dispDate} onChange={setDispDate} required />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Proceeds Amount (KES)</label>
              <input className="field-input" type="number" min={0} required disabled={submitting} value={dispProceeds} onChange={e => setDispProceeds(e.target.value)} placeholder="0" />
            </div>
            <div className="field">
              <label className="field-label">Fees (KES)</label>
              <input className="field-input" type="number" min={0} disabled={submitting} value={dispFees} onChange={e => setDispFees(e.target.value)} placeholder="0" />
            </div>
          </div>
          {isUnitType && dispType === 'PARTIAL' && (
            <div className="field">
              <label className="field-label field-label--required">Units to Redeem</label>
              <input className="field-input" type="number" min={0} step="0.01" required disabled={submitting} value={dispUnits} onChange={e => setDispUnits(e.target.value)} placeholder="0" />
            </div>
          )}
          <div className="field">
            <label className="field-label">Notes</label>
            <textarea className="field-input" disabled={submitting} value={dispNotes} onChange={e => setDispNotes(e.target.value)} placeholder="Disposal details..." />
          </div>
        </form>
      </Modal>

      {/* Rollover Modal */}
      <Modal
        open={showRolloverModal}
        onClose={() => { if (!submitting) setShowRolloverModal(false) }}
        title="Roll Over Investment"
        subtitle={`Roll over ${investment.name} to a new term`}
        footer={
          <>
            <button className="btn btn--secondary" type="button" onClick={() => setShowRolloverModal(false)} disabled={submitting}>Cancel</button>
            <button className="btn btn--primary" type="submit" form="rollover-form" disabled={submitting}>
              {submitting ? 'Processing...' : 'Roll Over'}
            </button>
          </>
        }
      >
        <form id="rollover-form" className="modal-form" onSubmit={e => void handleRollover(e)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">New Maturity Date</label>
              <DatePicker value={rollMaturity} onChange={setRollMaturity} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">New Interest Rate (%)</label>
              <input className="field-input" type="number" min={0} step="0.01" required disabled={submitting} value={rollRate} onChange={e => setRollRate(e.target.value)} placeholder="0" />
            </div>
          </div>
          <div className="field">
            <label className="field-label">Notes</label>
            <textarea className="field-input" disabled={submitting} value={rollNotes} onChange={e => setRollNotes(e.target.value)} placeholder="Rollover details..." />
          </div>
        </form>
      </Modal>

      {/* Confirm action */}
      <ConfirmDialog
        open={confirmAction !== null}
        onClose={() => setConfirmAction(null)}
        onConfirm={() => {
          if (!confirmAction) return
          const action = confirmAction
          setConfirmAction(null)
          void handleAction(action)
        }}
        title={
          confirmAction === 'reject'
            ? 'Reject Investment'
            : confirmAction === 'activate'
              ? 'Activate Investment'
              : 'Approve Investment'
        }
        description={
          confirmAction === 'reject'
            ? 'Are you sure you want to reject this investment proposal?'
            : confirmAction === 'activate'
              ? 'Activate this investment? This will post it as an active holding.'
              : 'Approve this investment proposal? It will proceed to activation.'
        }
        confirmLabel={
          confirmAction === 'reject'
            ? 'Reject'
            : confirmAction === 'activate'
              ? 'Activate'
              : 'Approve'
        }
        variant={confirmAction === 'reject' ? 'danger' : 'info'}
        loading={actionLoading}
      />
    </div>
  )
}
