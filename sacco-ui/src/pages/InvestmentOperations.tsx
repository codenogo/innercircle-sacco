import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { MagnifyingGlass } from '@phosphor-icons/react'
import { Breadcrumb } from '../components/Breadcrumb'
import { ActionMenu } from '../components/ActionMenu'
import { StatCardGrid } from '../components/StatCard'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { Modal } from '../components/Modal'
import { Select } from '../components/Select'
import { DatePicker } from '../components/DatePicker'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ApiError } from '../services/apiClient'
import {
  approveInvestment,
  getInvestments,
  recordIncome,
  recordValuation,
  rejectInvestment,
} from '../services/investmentService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useToast } from '../hooks/useToast'
import type {
  InvestmentResponse,
  IncomeType,
  RecordIncomeRequest,
  RecordValuationRequest,
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

const INCOME_TYPE_OPTIONS = Object.entries(INCOME_TYPE_LABELS).map(([v, l]) => ({ value: v, label: l }))

function normalizeSearchText(value: string): string {
  return value
    .toLowerCase()
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

/* ─── component ─── */

export function InvestmentOperations() {
  const { request } = useAuthenticatedApi()
  const navigate = useNavigate()
  const toast = useToast()

  /* state */
  const [investments, setInvestments] = useState<InvestmentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  /* modal states */
  const [confirmTarget, setConfirmTarget] = useState<{ id: string; action: 'approve' | 'reject' } | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [showIncomeModal, setShowIncomeModal] = useState(false)
  const [showValuationModal, setShowValuationModal] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  /* income form */
  const [incInvestmentId, setIncInvestmentId] = useState('')
  const [incType, setIncType] = useState('')
  const [incAmount, setIncAmount] = useState('')
  const [incDate, setIncDate] = useState('')
  const [incRef, setIncRef] = useState('')
  const [incNotes, setIncNotes] = useState('')

  /* valuation form */
  const [valInvestmentId, setValInvestmentId] = useState('')
  const [valMarketValue, setValMarketValue] = useState('')
  const [valNav, setValNav] = useState('')
  const [valDate, setValDate] = useState('')
  const [valSource, setValSource] = useState('')

  /* ─── data loading ─── */

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const data = await getInvestments(request)
      setInvestments(data)
    } catch (err) {
      toast.error('Load failed', toErrorMessage(err, 'Unable to load investments.'))
      setInvestments([])
    } finally {
      setLoading(false)
    }
  }, [request, toast])

  useEffect(() => { void loadData() }, [loadData])

  /* ─── derived data ─── */

  const pendingApprovals = useMemo(() =>
    investments.filter(inv => inv.status === 'PROPOSED'),
  [investments])

  const activeInvestments = useMemo(() =>
    investments.filter(inv => ['ACTIVE', 'PARTIALLY_DISPOSED', 'ROLLED_OVER'].includes(inv.status)),
  [investments])

  const investmentSelectOptions = useMemo(() =>
    activeInvestments.map(inv => ({
      value: inv.id,
      label: `${inv.referenceNumber} — ${inv.name}`,
    })),
  [activeInvestments])

  const filteredPending = useMemo(() => {
    const terms = normalizeSearchText(search).split(' ').filter(Boolean)
    if (terms.length === 0) return pendingApprovals

    return pendingApprovals.filter(inv => {
      const searchable = normalizeSearchText([
        inv.referenceNumber ?? '',
        inv.name ?? '',
        inv.institution ?? '',
        inv.investmentType ?? '',
        INVESTMENT_TYPE_LABELS[inv.investmentType],
        inv.status ?? '',
      ].join(' '))

      return terms.every(term => searchable.includes(term))
    })
  }, [pendingApprovals, search])


  const selectedForVal = useMemo(() =>
    activeInvestments.find(inv => inv.id === valInvestmentId),
  [activeInvestments, valInvestmentId])

  const isUnitTypeVal = selectedForVal
    ? ['UNIT_TRUST', 'EQUITY'].includes(selectedForVal.investmentType) : false

  /* ─── handlers ─── */

  async function handleApproveReject() {
    if (!confirmTarget) return
    setActionLoading(confirmTarget.id)
    try {
      if (confirmTarget.action === 'approve') {
        await approveInvestment(confirmTarget.id, request)
      } else {
        await rejectInvestment(confirmTarget.id, undefined, request)
      }
      await loadData()
      toast.success(
        `Investment ${confirmTarget.action}d`,
        `Investment ${confirmTarget.action === 'approve' ? 'approved' : 'rejected'} successfully.`,
      )
    } catch (err) {
      toast.error(`Unable to ${confirmTarget.action}`, toErrorMessage(err, `Unable to ${confirmTarget.action} investment.`))
    } finally {
      setActionLoading(null)
      setConfirmTarget(null)
    }
  }

  async function handleRecordIncome(e: FormEvent) {
    e.preventDefault()
    if (!incInvestmentId) return
    setSubmitting(true)
    try {
      const payload: RecordIncomeRequest = {
        incomeType: incType as IncomeType,
        amount: Number(incAmount),
        incomeDate: incDate,
        ...(incRef.trim() ? { referenceNumber: incRef.trim() } : {}),
        ...(incNotes.trim() ? { notes: incNotes.trim() } : {}),
      }
      await recordIncome(incInvestmentId, payload, request)
      await loadData()
      setShowIncomeModal(false)
      setIncInvestmentId(''); setIncType(''); setIncAmount(''); setIncDate(''); setIncRef(''); setIncNotes('')
      toast.success('Income recorded', 'Investment income recorded successfully.')
    } catch (err) {
      toast.error('Record failed', toErrorMessage(err, 'Unable to record income.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRecordValuation(e: FormEvent) {
    e.preventDefault()
    if (!valInvestmentId) return
    setSubmitting(true)
    try {
      const payload: RecordValuationRequest = {
        ...(valMarketValue ? { marketValue: Number(valMarketValue) } : {}),
        ...(valNav ? { navPerUnit: Number(valNav) } : {}),
        valuationDate: valDate,
        source: valSource.trim(),
      }
      await recordValuation(valInvestmentId, payload, request)
      await loadData()
      setShowValuationModal(false)
      setValInvestmentId(''); setValMarketValue(''); setValNav(''); setValDate(''); setValSource('')
      toast.success('Valuation recorded', 'Investment valuation updated successfully.')
    } catch (err) {
      toast.error('Record failed', toErrorMessage(err, 'Unable to record valuation.'))
    } finally {
      setSubmitting(false)
    }
  }

  /* ─── columns ─── */

  const pendingColumns: ColumnDef<InvestmentResponse>[] = useMemo(() => [
    {
      key: 'ref', header: 'Reference', className: 'data',
      render: r => (
        <span className="inv-ref" role="button" tabIndex={0} onClick={() => navigate(`/investments/${r.id}`)}>
          {r.referenceNumber}
        </span>
      ),
    },
    { key: 'name', header: 'Name', render: r => <span className="inv-name">{r.name}</span> },
    { key: 'type', header: 'Type', className: 'data', render: r => INVESTMENT_TYPE_LABELS[r.investmentType] },
    { key: 'institution', header: 'Institution', className: 'data', render: r => r.institution },
    { key: 'amount', header: 'Amount (KES)', className: 'amount ledger-table-amount', headerClassName: 'ledger-table-amount', render: r => fmt(r.purchasePrice) },
    { key: 'date', header: 'Proposed', className: 'ledger-date', render: r => fmtDate(r.createdAt ?? r.purchaseDate) },
    {
      key: 'actions', header: 'Actions', className: 'datatable-col-actions',
      render: r => (
        <ActionMenu
          actions={[
            { label: 'View Details', onClick: () => navigate(`/investments/${r.id}`) },
            { label: 'Approve', onClick: () => setConfirmTarget({ id: r.id, action: 'approve' }) },
            { label: 'Reject', onClick: () => setConfirmTarget({ id: r.id, action: 'reject' }), variant: 'danger' as const },
          ]}
        />
      ),
    },
  ], [navigate])

  /* ─── render ─── */

  return (
    <div className="ops-page">
      <Breadcrumb items={[{ label: 'Operations', to: '/operations' }, { label: 'Investment Operations' }]} />

      <div className="page-header">
        <div>
          <h1 className="page-title">Investment Operations</h1>
        </div>
        <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
          <button className="btn btn--primary" onClick={() => setShowIncomeModal(true)}>Record Income</button>
          <button className="btn btn--secondary" onClick={() => setShowValuationModal(true)}>Update Valuation</button>
        </div>
      </div>

      <hr className="rule rule--strong" />

      {/* Stats */}
      <StatCardGrid
        items={[
          { label: 'Pending Approvals', value: String(pendingApprovals.length) },
          { label: 'Active Investments', value: String(activeInvestments.length) },
          { label: 'Total Investments', value: String(investments.length) },
        ]}
        columns={3}
      />

      {/* Pending Approvals */}
      <section className="page-section">
        <span className="page-section-title">Pending Approvals</span>
        <hr className="rule" />
        <div className="filter-bar">
          <div className="filter-search-wrap">
            <MagnifyingGlass size={16} className="filter-search-icon" />
            <input
              className="filter-search"
              placeholder="Search pending..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              aria-label="Search pending approvals"
            />
          </div>
        </div>
        {loading ? (
          <p style={{ padding: 'var(--space-4)', color: 'var(--ink-muted)' }}>Loading…</p>
        ) : (
          <DataTable<InvestmentResponse>
            columns={pendingColumns}
            data={filteredPending}
            getRowKey={r => r.id}
            emptyMessage="No pending investment proposals."
          />
        )}
      </section>

      <hr className="rule rule--strong" />

      {/* Record Income Modal */}
      <Modal
        open={showIncomeModal}
        onClose={() => { if (!submitting) setShowIncomeModal(false) }}
        title="Record Investment Income"
        subtitle="Record income for an active investment"
        footer={
          <>
            <button className="btn btn--secondary" type="button" onClick={() => setShowIncomeModal(false)} disabled={submitting}>Cancel</button>
            <button className="btn btn--primary" type="submit" form="ops-income-form" disabled={submitting}>
              {submitting ? 'Recording...' : 'Record Income'}
            </button>
          </>
        }
      >
        <form id="ops-income-form" className="modal-form" onSubmit={e => void handleRecordIncome(e)}>
          <div className="field">
            <label className="field-label field-label--required">Investment</label>
            <Select options={investmentSelectOptions} value={incInvestmentId} onChange={setIncInvestmentId} placeholder="Select investment..." required />
          </div>
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
        title="Update Investment Valuation"
        subtitle="Record a new market valuation for an active investment"
        footer={
          <>
            <button className="btn btn--secondary" type="button" onClick={() => setShowValuationModal(false)} disabled={submitting}>Cancel</button>
            <button className="btn btn--primary" type="submit" form="ops-valuation-form" disabled={submitting}>
              {submitting ? 'Recording...' : 'Record Valuation'}
            </button>
          </>
        }
      >
        <form id="ops-valuation-form" className="modal-form" onSubmit={e => void handleRecordValuation(e)}>
          <div className="field">
            <label className="field-label field-label--required">Investment</label>
            <Select options={investmentSelectOptions} value={valInvestmentId} onChange={setValInvestmentId} placeholder="Select investment..." required />
          </div>
          {isUnitTypeVal ? (
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

      {/* Confirm approve / reject */}
      <ConfirmDialog
        open={confirmTarget !== null}
        onClose={() => setConfirmTarget(null)}
        onConfirm={() => void handleApproveReject()}
        title={confirmTarget?.action === 'reject' ? 'Reject Investment' : 'Approve Investment'}
        description={
          confirmTarget?.action === 'reject'
            ? 'Are you sure you want to reject this investment proposal?'
            : 'Approve this investment proposal? It will proceed to funding.'
        }
        confirmLabel={confirmTarget?.action === 'reject' ? 'Reject' : 'Approve'}
        variant={confirmTarget?.action === 'reject' ? 'danger' : 'info'}
        loading={actionLoading !== null}
      />
    </div>
  )
}
