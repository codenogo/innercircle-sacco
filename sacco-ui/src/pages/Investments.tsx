import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { MagnifyingGlass, Plus } from '@phosphor-icons/react'
import { ActionMenu } from '../components/ActionMenu'
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
  createInvestment,
  getInvestmentSummary,
  getInvestments,
  rejectInvestment,
} from '../services/investmentService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { useToast } from '../hooks/useToast'
import type {
  InvestmentResponse,
  InvestmentStatus,
  InvestmentType,
  InvestmentSummary,
  CreateInvestmentRequest,
} from '../types/investments'
import { INVESTMENT_TYPE_LABELS } from '../types/investments'
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

function normalizeSearchText(value: string): string {
  return value
    .toLowerCase()
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const statusClass: Record<InvestmentStatus, string> = {
  PROPOSED: 'badge--pending',
  APPROVED: 'badge--pending',
  ACTIVE: 'badge--active',
  MATURED: 'badge--completed',
  PARTIALLY_DISPOSED: 'badge--pending',
  ROLLED_OVER: 'badge--active',
  CLOSED: 'badge--completed',
  REJECTED: 'badge--defaulted',
}

const TYPE_OPTIONS = [
  { value: 'ALL', label: 'All Types' },
  ...Object.entries(INVESTMENT_TYPE_LABELS).map(([value, label]) => ({ value, label })),
]

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'All Statuses' },
  { value: 'PROPOSED', label: 'Proposed' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'MATURED', label: 'Matured' },
  { value: 'CLOSED', label: 'Closed' },
]

const UNIT_TYPES: InvestmentType[] = ['UNIT_TRUST', 'EQUITY']

/* ─── component ─── */

export function Investments() {
  const { request } = useAuthenticatedApi()
  const { canAccess } = useAuthorization()
  const navigate = useNavigate()
  const toast = useToast()
  const canManage = canAccess(['ADMIN', 'TREASURER'])

  /* state */
  const [investments, setInvestments] = useState<InvestmentResponse[]>([])
  const [summary, setSummary] = useState<InvestmentSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')

  /* modal state */
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [confirmTarget, setConfirmTarget] = useState<{ id: string; action: 'approve' | 'reject' | 'activate' } | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  /* form fields */
  const [formName, setFormName] = useState('')
  const [formType, setFormType] = useState<string>('')
  const [formInstitution, setFormInstitution] = useState('')
  const [formFaceValue, setFormFaceValue] = useState('')
  const [formPurchasePrice, setFormPurchasePrice] = useState('')
  const [formRate, setFormRate] = useState('')
  const [formPurchaseDate, setFormPurchaseDate] = useState('')
  const [formMaturityDate, setFormMaturityDate] = useState('')
  const [formUnits, setFormUnits] = useState('')
  const [formNav, setFormNav] = useState('')
  const [formNotes, setFormNotes] = useState('')

  /* ─── data loading ─── */

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [invData, sumData] = await Promise.all([
        getInvestments(request),
        getInvestmentSummary(request),
      ])
      setInvestments(invData)
      setSummary(sumData)
    } catch (err) {
      toast.error('Unable to load investments', toErrorMessage(err, 'Unable to load investments.'))
      setInvestments([])
      setSummary(null)
    } finally {
      setLoading(false)
    }
  }, [request, toast])

  useEffect(() => { void loadData() }, [loadData])

  /* ─── handlers ─── */

  function resetForm() {
    setFormName(''); setFormType(''); setFormInstitution(''); setFormFaceValue('')
    setFormPurchasePrice(''); setFormRate(''); setFormPurchaseDate('')
    setFormMaturityDate(''); setFormUnits(''); setFormNav(''); setFormNotes('')
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    try {
      const payload: CreateInvestmentRequest = {
        name: formName.trim(),
        investmentType: formType as InvestmentType,
        institution: formInstitution.trim(),
        faceValue: Number(formFaceValue),
        purchasePrice: Number(formPurchasePrice),
        interestRate: Number(formRate) || 0,
        purchaseDate: formPurchaseDate,
        ...(formMaturityDate ? { maturityDate: formMaturityDate } : {}),
        ...(formUnits ? { units: Number(formUnits) } : {}),
        ...(formNav ? { navPerUnit: Number(formNav) } : {}),
        ...(formNotes.trim() ? { notes: formNotes.trim() } : {}),
      }
      await createInvestment(payload, request)
      await loadData()
      setShowCreateModal(false)
      resetForm()
      toast.success('Investment created', 'Investment proposal submitted successfully.')
    } catch (err) {
      toast.error('Create failed', toErrorMessage(err, 'Unable to create investment.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleAction(id: string, action: 'approve' | 'reject' | 'activate') {
    setActionLoading(id)
    try {
      if (action === 'approve') {
        await approveInvestment(id, request)
      } else if (action === 'reject') {
        await rejectInvestment(id, undefined, request)
      } else {
        await activateInvestment(id, request)
      }
      await loadData()
      toast.success(
        `Investment ${action}d`,
        `Investment ${action === 'approve' ? 'approved' : action === 'reject' ? 'rejected' : 'activated'} successfully.`,
      )
    } catch (err) {
      toast.error(`Unable to ${action}`, toErrorMessage(err, `Unable to ${action} investment.`))
    } finally {
      setActionLoading(null)
    }
  }

  /* ─── derived data ─── */

  const filtered = useMemo(() => {
    let list = investments
    if (typeFilter !== 'ALL') list = list.filter(inv => inv.investmentType === typeFilter)
    if (statusFilter !== 'ALL') list = list.filter(inv => inv.status === statusFilter)
    const terms = normalizeSearchText(search).split(' ').filter(Boolean)
    if (terms.length > 0) {
      list = list.filter(inv => {
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
    }
    return list
  }, [investments, typeFilter, statusFilter, search])

  const isUnitType = UNIT_TYPES.includes(formType as InvestmentType)

  const columns: ColumnDef<InvestmentResponse>[] = useMemo(() => {
    const cols: ColumnDef<InvestmentResponse>[] = [
      {
        key: 'ref', header: 'Ref', className: 'data inv-ref',
        render: inv => inv.referenceNumber,
      },
      {
        key: 'name', header: 'Investment',
        render: inv => (
          <>
            <span className="inv-name">{inv.name}</span>
            <span className="inv-institution">{inv.institution}</span>
          </>
        ),
      },
      {
        key: 'type', header: 'Type', className: 'data',
        render: inv => INVESTMENT_TYPE_LABELS[inv.investmentType],
      },
      {
        key: 'invested', header: 'Invested', className: 'amount ledger-table-amount', headerClassName: 'ledger-table-amount',
        render: inv => fmt(inv.purchasePrice),
      },
      {
        key: 'currentValue', header: 'Current Value', className: 'amount ledger-table-amount', headerClassName: 'ledger-table-amount',
        render: inv => fmt(inv.currentValue),
      },
      {
        key: 'return', header: 'Return', className: 'data',
        render: inv => {
          const gain = inv.currentValue - inv.purchasePrice
          const pct = inv.purchasePrice > 0 ? ((gain / inv.purchasePrice) * 100).toFixed(1) : '0.0'
          return (
            <span className={gain >= 0 ? 'amount--positive' : 'amount--negative'}>
              {gain >= 0 ? '+' : ''}{pct}%
            </span>
          )
        },
      },
      {
        key: 'maturity', header: 'Maturity', className: 'ledger-date',
        render: inv => fmtDate(inv.maturityDate),
      },
      {
        key: 'status', header: 'Status',
        render: inv => <span className={`badge ${statusClass[inv.status]}`}>{inv.status}</span>,
      },
    ]

    if (canManage) {
      cols.push({
        key: 'actions', header: '', width: '52px',
        headerClassName: 'datatable-col-actions', className: 'datatable-col-actions',
        render: inv => {
          const items: { label: string; onClick: () => void; variant?: 'danger'; disabled?: boolean }[] = [
            { label: 'View Details', onClick: () => navigate(`/investments/${inv.id}`) },
          ]
          if (inv.status === 'PROPOSED') {
            items.push(
              { label: 'Approve', onClick: () => setConfirmTarget({ id: inv.id, action: 'approve' }), disabled: actionLoading === inv.id },
              { label: 'Reject', onClick: () => setConfirmTarget({ id: inv.id, action: 'reject' }), variant: 'danger', disabled: actionLoading === inv.id },
            )
          } else if (inv.status === 'APPROVED') {
            items.push(
              { label: 'Activate', onClick: () => setConfirmTarget({ id: inv.id, action: 'activate' }), disabled: actionLoading === inv.id },
            )
          }
          return <ActionMenu actions={items} />
        },
      })
    }
    return cols
  }, [canManage, actionLoading, navigate])


  /* ─── render ─── */

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Investments</h1>
        </div>
        {canManage && (
          <button className="btn btn--primary" onClick={() => setShowCreateModal(true)}>
            <Plus size={14} weight="bold" /> New Investment
          </button>
        )}
      </div>

      <hr className="rule rule--strong" />

      {/* Portfolio summary */}
      {summary && (
        <section className="page-section">
          <span className="page-section-title">Portfolio Summary</span>
          <hr className="rule" />
          <StatCardGrid
            items={[
              { label: 'Total Invested', value: `KES ${fmt(summary.totalInvested)}` },
              { label: 'Current Value', value: `KES ${fmt(summary.currentValue)}` },
              { label: 'Unrealised Gain', value: `KES ${fmt(summary.unrealisedGain)}`, valueClassName: summary.unrealisedGain >= 0 ? 'amount--positive' : 'amount--negative' },
              { label: 'Income YTD', value: `KES ${fmt(summary.incomeYtd)}`, valueClassName: 'amount--positive' },
            ]}
            columns={4}
          />
          <StatCardGrid
            items={[
              { label: 'Active', value: String(summary.activeCount) },
              { label: 'Matured', value: String(summary.maturedCount) },
              { label: 'Proposed', value: String(summary.proposedCount) },
              { label: 'Closed', value: String(summary.closedCount) },
            ]}
            columns={4}
          />

          {/* Asset allocation bar */}
          {summary.byType.length > 0 && (
            <>
              <span className="page-section-title" style={{ marginTop: 'var(--space-3)' }}>Asset Allocation</span>
              <hr className="rule" />
              <div className="inv-alloc-bar">
                {summary.byType.map(seg => (
                  <div
                    key={seg.type}
                    className={`inv-alloc-segment inv-alloc-segment--${seg.type}`}
                    style={{ width: `${seg.percentage}%` }}
                    title={`${INVESTMENT_TYPE_LABELS[seg.type]}: ${seg.percentage}%`}
                  />
                ))}
              </div>
              <div className="inv-alloc-legend">
                {summary.byType.map(seg => (
                  <span key={seg.type} className="inv-alloc-legend-item">
                    <span className={`inv-alloc-dot inv-alloc-dot--${seg.type}`} />
                    {INVESTMENT_TYPE_LABELS[seg.type]} {seg.percentage}%
                  </span>
                ))}
              </div>
            </>
          )}
          <hr className="rule rule--strong" />
        </section>
      )}

      {/* Filters */}
      <div className="filter-bar">
        <div className="filter-search-wrap">
          <MagnifyingGlass size={16} className="filter-search-icon" />
          <input type="text" className="filter-search" placeholder="Search investments..." aria-label="Search investments" value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <Select value={typeFilter} onChange={setTypeFilter} options={TYPE_OPTIONS} />
        <Select value={statusFilter} onChange={setStatusFilter} options={STATUS_OPTIONS} />
      </div>

      {/* Holdings table */}
      <section className="page-section">
        <span className="page-section-title">All Holdings</span>
        <hr className="rule" />
        <DataTable<InvestmentResponse>
          columns={columns}
          data={filtered}
          getRowKey={row => row.id}
          loading={loading}
          emptyMessage={
            investments.length === 0
              ? <div className="empty-state empty-state--illustrated"><h3 className="empty-state-heading">No investments yet</h3><p className="empty-state-text">Create a new investment to get started.</p></div>
              : 'No investments match your filters.'
          }
        />
        <hr className="rule rule--strong" />
      </section>

      {/* Create Investment Modal */}
      <Modal
        open={showCreateModal}
        onClose={() => { if (!submitting) { setShowCreateModal(false); resetForm() } }}
        title="New Investment"
        subtitle="Submit a new investment proposal"
        width="lg"
        footer={
          <>
            <button className="btn btn--secondary" type="button" onClick={() => { setShowCreateModal(false); resetForm() }} disabled={submitting}>Cancel</button>
            <button className="btn btn--primary" type="submit" form="new-investment-form" disabled={submitting}>
              {submitting ? 'Submitting...' : 'Submit Proposal'}
            </button>
          </>
        }
      >
        <form id="new-investment-form" className="modal-form" onSubmit={e => void handleCreate(e)}>
          <div className="field">
            <label className="field-label field-label--required">Investment Name</label>
            <input className="field-input" required disabled={submitting} value={formName} onChange={e => setFormName(e.target.value)} placeholder="e.g. 364-Day Treasury Bill" />
          </div>

          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Type</label>
              <Select options={Object.entries(INVESTMENT_TYPE_LABELS).map(([v, l]) => ({ value: v, label: l }))} value={formType} onChange={setFormType} placeholder="Select type..." required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Institution</label>
              <input className="field-input" required disabled={submitting} value={formInstitution} onChange={e => setFormInstitution(e.target.value)} placeholder="e.g. KCB Bank" />
            </div>
          </div>

          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Face Value (KES)</label>
              <input className="field-input" type="number" min={0} required disabled={submitting} value={formFaceValue} onChange={e => setFormFaceValue(e.target.value)} placeholder="0" />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Purchase Price (KES)</label>
              <input className="field-input" type="number" min={0} required disabled={submitting} value={formPurchasePrice} onChange={e => setFormPurchasePrice(e.target.value)} placeholder="0" />
            </div>
            <div className="field">
              <label className="field-label">Interest Rate (%)</label>
              <input className="field-input" type="number" min={0} step="0.01" disabled={submitting} value={formRate} onChange={e => setFormRate(e.target.value)} placeholder="0" />
            </div>
          </div>

          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Purchase Date</label>
              <DatePicker value={formPurchaseDate} onChange={setFormPurchaseDate} required />
            </div>
            <div className="field">
              <label className="field-label">Maturity Date</label>
              <DatePicker value={formMaturityDate} onChange={setFormMaturityDate} />
              <span className="field-hint">Leave blank for open-ended investments</span>
            </div>
          </div>

          {isUnitType && (
            <div className="field-row">
              <div className="field">
                <label className="field-label field-label--required">Units Purchased</label>
                <input className="field-input" type="number" min={0} step="0.01" required={isUnitType} disabled={submitting} value={formUnits} onChange={e => setFormUnits(e.target.value)} placeholder="0" />
              </div>
              <div className="field">
                <label className="field-label field-label--required">NAV per Unit (KES)</label>
                <input className="field-input" type="number" min={0} step="0.01" required={isUnitType} disabled={submitting} value={formNav} onChange={e => setFormNav(e.target.value)} placeholder="0.00" />
              </div>
            </div>
          )}

          <div className="field">
            <label className="field-label">Notes</label>
            <textarea className="field-input" disabled={submitting} value={formNotes} onChange={e => setFormNotes(e.target.value)} placeholder="Additional details..." />
          </div>
        </form>
      </Modal>

      {/* Confirm action */}
      <ConfirmDialog
        open={confirmTarget !== null}
        onClose={() => setConfirmTarget(null)}
        onConfirm={() => {
          if (!confirmTarget) return
          setConfirmTarget(null)
          void handleAction(confirmTarget.id, confirmTarget.action)
        }}
        title={
          confirmTarget?.action === 'reject'
            ? 'Reject Investment'
            : confirmTarget?.action === 'activate'
              ? 'Activate Investment'
              : 'Approve Investment'
        }
        description={
          confirmTarget?.action === 'reject'
            ? 'Are you sure you want to reject this investment proposal?'
            : confirmTarget?.action === 'activate'
              ? 'Activate this investment? This will post it as an active holding.'
              : 'Approve this investment proposal? It will proceed to activation.'
        }
        confirmLabel={
          confirmTarget?.action === 'reject'
            ? 'Reject'
            : confirmTarget?.action === 'activate'
              ? 'Activate'
              : 'Approve'
        }
        variant={confirmTarget?.action === 'reject' ? 'danger' : 'info'}
        loading={actionLoading !== null}
      />
    </div>
  )
}
