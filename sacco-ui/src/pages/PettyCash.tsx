import { useCallback, useEffect, useMemo, useState } from 'react'
import { Plus, MagnifyingGlass } from '@phosphor-icons/react'
import { Spinner } from '../components/Spinner'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { MonthPicker } from '../components/MonthPicker'
import { DatePicker } from '../components/DatePicker'
import { Modal } from '../components/Modal'
import { Select } from '../components/Select'
import { MakerCheckerWarning } from '../components/MakerCheckerWarning'
import { ApiError } from '../services/apiClient'
import {
  approvePettyCashVoucher,
  createPettyCashVoucher,
  disbursePettyCashVoucher,
  getPettyCashSummary,
  getPettyCashVouchers,
  rejectPettyCashVoucher,
  settlePettyCashVoucher,
} from '../services/pettyCashService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { isMakerCheckerViolation } from '../types/makerChecker'
import type {
  PettyCashExpenseType,
  PettyCashSummaryResponse,
  PettyCashVoucherResponse,
  PettyCashVoucherStatus,
} from '../types/pettyCash'
import './PettyCash.css'

const PAGE_SIZE = 50

type StatusFilter = 'ALL' | PettyCashVoucherStatus
type FeedbackType = 'success' | 'error'

interface Feedback {
  type: FeedbackType
  text: string
}

const statusClass: Record<PettyCashVoucherStatus, string> = {
  SUBMITTED: 'badge--pending',
  APPROVED: 'badge--approved',
  DISBURSED: 'badge--processing',
  SETTLED: 'badge--completed',
  REJECTED: 'badge--rejected',
}

const expenseTypeLabel: Record<PettyCashExpenseType, string> = {
  OPERATIONS: 'Operations',
  ADMINISTRATION: 'Administration',
  TRANSPORT: 'Transport',
  UTILITIES: 'Utilities',
  MAINTENANCE: 'Maintenance',
  WELFARE: 'Welfare',
  OTHER: 'Other',
}

function currentMonth(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10)
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function fmtCurrency(value: number | string): string {
  const parsed = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(parsed)) return '0'
  return parsed.toLocaleString('en-KE')
}

function fmtDate(value: string | null): string {
  if (!value) return '\u2014'
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return '\u2014'
  return date.toLocaleDateString('en-KE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function normalizeErrorMessage(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1)
}

export function PettyCash() {
  const { request } = useAuthenticatedApi()
  const { canAccess } = useAuthorization()
  const isAdmin = canAccess(['ADMIN'])

  const [vouchers, setVouchers] = useState<PettyCashVoucherResponse[]>([])
  const [summary, setSummary] = useState<PettyCashSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  const [search, setSearch] = useState('')
  const [month, setMonth] = useState(currentMonth)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')

  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createAmount, setCreateAmount] = useState('')
  const [createPurpose, setCreatePurpose] = useState('')
  const [createExpenseType, setCreateExpenseType] = useState<PettyCashExpenseType>('OPERATIONS')
  const [createRequestDate, setCreateRequestDate] = useState(todayIsoDate)
  const [createNotes, setCreateNotes] = useState('')
  const [createSubmitting, setCreateSubmitting] = useState(false)

  const [settleTarget, setSettleTarget] = useState<PettyCashVoucherResponse | null>(null)
  const [settleReceiptNumber, setSettleReceiptNumber] = useState('')
  const [settleNotes, setSettleNotes] = useState('')
  const [settleSubmitting, setSettleSubmitting] = useState(false)

  const [rejectTarget, setRejectTarget] = useState<PettyCashVoucherResponse | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [rejectSubmitting, setRejectSubmitting] = useState(false)

  const [overrideTarget, setOverrideTarget] = useState<string | null>(null)

  const effectiveStatus = statusFilter === 'ALL' ? undefined : statusFilter

  const loadVouchers = useCallback(async (opts?: { append?: boolean; cursor?: string | null }) => {
    const append = Boolean(opts?.append)
    const cursor = opts?.cursor ?? undefined

    if (append) setLoadingMore(true)
    else setLoading(true)

    try {
      const page = await getPettyCashVouchers(
        {
          status: effectiveStatus,
          month,
          cursor,
          limit: PAGE_SIZE,
        },
        request,
      )

      setVouchers(prev => {
        if (!append) return page.items

        const merged = new Map<string, PettyCashVoucherResponse>()
        prev.forEach(v => merged.set(v.id, v))
        page.items.forEach(v => merged.set(v.id, v))
        return Array.from(merged.values())
      })

      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
    } catch (error) {
      setFeedback({
        type: 'error',
        text: toErrorMessage(error, 'Unable to load petty cash vouchers.'),
      })
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [effectiveStatus, month, request])

  const loadSummary = useCallback(async () => {
    try {
      const result = await getPettyCashSummary(month, effectiveStatus, request)
      setSummary(result)
    } catch (error) {
      setSummary(null)
      setFeedback({
        type: 'error',
        text: toErrorMessage(error, 'Unable to load petty cash summary.'),
      })
    }
  }, [effectiveStatus, month, request])

  const refreshData = useCallback(async () => {
    await Promise.all([
      loadVouchers({ append: false, cursor: null }),
      loadSummary(),
    ])
  }, [loadSummary, loadVouchers])

  useEffect(() => {
    void refreshData()
  }, [refreshData])

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return vouchers

    return vouchers.filter(voucher => {
      const text = [
        voucher.referenceNumber,
        voucher.purpose,
        expenseTypeLabel[voucher.expenseType],
        voucher.status,
        voucher.createdBy ?? '',
      ].join(' ').toLowerCase()
      return text.includes(query)
    })
  }, [search, vouchers])

  const voucherColumns = useMemo((): ColumnDef<PettyCashVoucherResponse>[] => [
    {
      key: 'reference',
      header: 'Reference',
      className: 'data',
      render: v => v.referenceNumber,
    },
    {
      key: 'purpose',
      header: 'Purpose',
      render: v => (
        <>
          <span className="petty-cash-purpose">{v.purpose}</span>
          <span className="petty-cash-purpose-sub">{v.notes || '\u2014'}</span>
        </>
      ),
    },
    {
      key: 'expenseType',
      header: 'Expense Type',
      render: v => expenseTypeLabel[v.expenseType],
    },
    {
      key: 'status',
      header: 'Status',
      render: v => <span className={`badge ${statusClass[v.status]}`}>{v.status}</span>,
    },
    {
      key: 'requested',
      header: 'Requested',
      className: 'data',
      render: v => fmtDate(v.requestDate),
    },
    {
      key: 'amount',
      header: 'Amount (KES)',
      className: 'amount ledger-table-amount',
      headerClassName: 'ledger-table-amount',
      render: v => fmtCurrency(v.amount),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: v => (
        <div className="petty-cash-actions">
          {v.status === 'SUBMITTED' && (
            <>
              <button
                type="button"
                className="btn btn--secondary btn--small"
                onClick={() => void handleApprove(v.id)}
                disabled={actionLoading === v.id}
              >
                {actionLoading === v.id ? 'Approving...' : 'Approve'}
              </button>
              <button
                type="button"
                className="btn btn--secondary btn--small"
                onClick={() => setRejectTarget(v)}
                disabled={actionLoading === v.id}
              >
                Reject
              </button>
            </>
          )}
          {v.status === 'APPROVED' && (
            <button
              type="button"
              className="btn btn--secondary btn--small"
              onClick={() => void handleDisburse(v.id)}
              disabled={actionLoading === v.id}
            >
              {actionLoading === v.id ? 'Disbursing...' : 'Disburse'}
            </button>
          )}
          {v.status === 'DISBURSED' && (
            <button
              type="button"
              className="btn btn--secondary btn--small"
              onClick={() => setSettleTarget(v)}
            >
              Settle
            </button>
          )}
        </div>
      ),
    },
  ], [actionLoading])

  async function handleCreateVoucher() {
    const amount = Number(createAmount)
    if (!Number.isFinite(amount) || amount <= 0) {
      setFeedback({ type: 'error', text: 'Enter a valid amount greater than zero.' })
      return
    }
    if (!createPurpose.trim()) {
      setFeedback({ type: 'error', text: 'Purpose is required.' })
      return
    }

    setCreateSubmitting(true)
    setFeedback(null)
    try {
      await createPettyCashVoucher({
        amount,
        purpose: createPurpose.trim(),
        expenseType: createExpenseType,
        requestDate: createRequestDate || undefined,
        notes: createNotes.trim() || undefined,
      }, request)

      setCreateAmount('')
      setCreatePurpose('')
      setCreateExpenseType('OPERATIONS')
      setCreateRequestDate(todayIsoDate())
      setCreateNotes('')
      setShowCreateModal(false)
      setFeedback({ type: 'success', text: 'Petty cash voucher created successfully.' })
      await refreshData()
    } catch (error) {
      setFeedback({
        type: 'error',
        text: toErrorMessage(error, 'Unable to create petty cash voucher.'),
      })
    } finally {
      setCreateSubmitting(false)
    }
  }

  async function handleApprove(voucherId: string) {
    setActionLoading(voucherId)
    setFeedback(null)
    try {
      await approvePettyCashVoucher(voucherId, undefined, request)
      setFeedback({ type: 'success', text: 'Voucher approved successfully.' })
      await refreshData()
    } catch (error) {
      if (isMakerCheckerViolation(error)) {
        setOverrideTarget(voucherId)
      } else {
        setFeedback({
          type: 'error',
          text: toErrorMessage(error, 'Unable to approve voucher.'),
        })
      }
    } finally {
      setActionLoading(null)
    }
  }

  async function handleApproveOverride(reason: string) {
    if (!overrideTarget) return
    setActionLoading(overrideTarget)
    setFeedback(null)
    try {
      await approvePettyCashVoucher(overrideTarget, { overrideReason: reason }, request)
      setFeedback({ type: 'success', text: 'Voucher approved with admin override.' })
      setOverrideTarget(null)
      await refreshData()
    } catch (error) {
      setFeedback({
        type: 'error',
        text: toErrorMessage(error, 'Unable to approve with override.'),
      })
    } finally {
      setActionLoading(null)
    }
  }

  async function handleDisburse(voucherId: string) {
    setActionLoading(voucherId)
    setFeedback(null)
    try {
      await disbursePettyCashVoucher(voucherId, request)
      setFeedback({ type: 'success', text: 'Voucher disbursed successfully.' })
      await refreshData()
    } catch (error) {
      setFeedback({
        type: 'error',
        text: toErrorMessage(error, 'Unable to disburse voucher.'),
      })
    } finally {
      setActionLoading(null)
    }
  }

  async function handleSettleVoucher() {
    if (!settleTarget) return
    if (!settleReceiptNumber.trim()) {
      setFeedback({ type: 'error', text: 'Receipt number is required to settle this voucher.' })
      return
    }

    setSettleSubmitting(true)
    setFeedback(null)
    try {
      await settlePettyCashVoucher(settleTarget.id, {
        receiptNumber: settleReceiptNumber.trim(),
        notes: settleNotes.trim() || undefined,
      }, request)
      setFeedback({ type: 'success', text: 'Voucher settled successfully.' })
      setSettleTarget(null)
      setSettleReceiptNumber('')
      setSettleNotes('')
      await refreshData()
    } catch (error) {
      setFeedback({
        type: 'error',
        text: toErrorMessage(error, 'Unable to settle voucher.'),
      })
    } finally {
      setSettleSubmitting(false)
    }
  }

  async function handleRejectVoucher() {
    if (!rejectTarget) return
    if (!rejectReason.trim()) {
      setFeedback({ type: 'error', text: 'A rejection reason is required.' })
      return
    }

    setRejectSubmitting(true)
    setFeedback(null)
    try {
      await rejectPettyCashVoucher(rejectTarget.id, { reason: rejectReason.trim() }, request)
      setFeedback({ type: 'success', text: 'Voucher rejected successfully.' })
      setRejectTarget(null)
      setRejectReason('')
      await refreshData()
    } catch (error) {
      setFeedback({
        type: 'error',
        text: toErrorMessage(error, 'Unable to reject voucher.'),
      })
    } finally {
      setRejectSubmitting(false)
    }
  }

  return (
    <div className="petty-cash-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Petty Cash</h1>
          <p className="page-subtitle">Voucher workflow: submit, approve, disburse, settle</p>
        </div>
        <button type="button" className="btn btn--primary" onClick={() => setShowCreateModal(true)}>
          <Plus size={14} weight="bold" />
          New Voucher
        </button>
      </div>

      <hr className="rule rule--strong" />

      <div className="page-summary petty-cash-summary">
        <span>Total: <strong>{summary?.totalCount ?? 0}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Submitted: <strong>{summary?.submittedCount ?? 0}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Approved: <strong>{summary?.approvedCount ?? 0}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Disbursed: <strong>{summary?.disbursedCount ?? 0}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Settled: <strong>{summary?.settledCount ?? 0}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Rejected: <strong>{summary?.rejectedCount ?? 0}</strong></span>
      </div>
      <div className="page-summary petty-cash-summary">
        <span>Disbursed Amount: <strong>KES {fmtCurrency(summary?.disbursedAmount ?? 0)}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Settled Amount: <strong>KES {fmtCurrency(summary?.settledAmount ?? 0)}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Outstanding Float: <strong>KES {fmtCurrency(summary?.outstandingAmount ?? 0)}</strong></span>
      </div>

      <hr className="rule" />

      {feedback && (
        <div className={`petty-cash-feedback petty-cash-feedback--${feedback.type}`} role="status">
          {normalizeErrorMessage(feedback.text)}
        </div>
      )}

      <section className="page-section">
        <div className="filter-bar">
          <div className="filter-search-wrap">
            <MagnifyingGlass size={16} className="filter-search-icon" />
            <input
              type="text"
              className="filter-search"
              placeholder="Search reference, purpose, expense..."
              value={search}
              onChange={event => setSearch(event.target.value)}
            />
          </div>
          <MonthPicker value={month} onChange={setMonth} />
          <div className="filter-select-wrap">
            <Select
              value={statusFilter}
              onChange={value => setStatusFilter(value as StatusFilter)}
              options={[
                { value: 'ALL', label: 'All Statuses' },
                { value: 'SUBMITTED', label: 'Submitted' },
                { value: 'APPROVED', label: 'Approved' },
                { value: 'DISBURSED', label: 'Disbursed' },
                { value: 'SETTLED', label: 'Settled' },
                { value: 'REJECTED', label: 'Rejected' },
              ]}
            />
          </div>
        </div>
      </section>

      <DataTable
        columns={voucherColumns}
        data={filtered}
        getRowKey={v => v.id}
        loading={loading}
        emptyMessage="No petty cash vouchers found."
        getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
      />

      {hasMore && (
        <div className="ops-pager">
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loadingMore || !nextCursor}
            onClick={() => void loadVouchers({ append: true, cursor: nextCursor })}
          >
            {loadingMore ? <><Spinner size="sm" /> Loading...</> : 'Load More'}
          </button>
        </div>
      )}

      <Modal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title="New Petty Cash Voucher"
        subtitle="Submit a petty cash request for approval"
        width="md"
        footer={(
          <>
            <button type="button" className="btn btn--secondary" onClick={() => setShowCreateModal(false)} disabled={createSubmitting}>
              Cancel
            </button>
            <button type="button" className="btn btn--primary" onClick={() => void handleCreateVoucher()} disabled={createSubmitting}>
              {createSubmitting ? 'Creating...' : 'Create Voucher'}
            </button>
          </>
        )}
      >
        <div className="petty-cash-form-grid">
          <label className="field-label" htmlFor="petty-cash-amount">Amount (KES)</label>
          <input
            id="petty-cash-amount"
            className="field-input"
            type="number"
            min={1}
            value={createAmount}
            onChange={event => setCreateAmount(event.target.value)}
            placeholder="0"
          />

          <label className="field-label" htmlFor="petty-cash-purpose">Purpose</label>
          <textarea
            id="petty-cash-purpose"
            className="field-input"
            rows={3}
            maxLength={500}
            value={createPurpose}
            onChange={event => setCreatePurpose(event.target.value)}
            placeholder="Describe what this petty cash will be used for"
          />

          <label className="field-label" htmlFor="petty-cash-expense-type">Expense Type</label>
          <select
            id="petty-cash-expense-type"
            className="filter-select"
            value={createExpenseType}
            onChange={event => setCreateExpenseType(event.target.value as PettyCashExpenseType)}
          >
            {(Object.keys(expenseTypeLabel) as PettyCashExpenseType[]).map(type => (
              <option key={type} value={type}>{expenseTypeLabel[type]}</option>
            ))}
          </select>

          <label className="field-label">Request Date</label>
          <DatePicker value={createRequestDate} onChange={setCreateRequestDate} required />

          <label className="field-label" htmlFor="petty-cash-notes">Notes</label>
          <textarea
            id="petty-cash-notes"
            className="field-input"
            rows={3}
            maxLength={500}
            value={createNotes}
            onChange={event => setCreateNotes(event.target.value)}
            placeholder="Optional notes"
          />
        </div>
      </Modal>

      <Modal
        open={settleTarget !== null}
        onClose={() => {
          setSettleTarget(null)
          setSettleReceiptNumber('')
          setSettleNotes('')
        }}
        title="Settle Voucher"
        subtitle={settleTarget ? settleTarget.referenceNumber : undefined}
        width="sm"
        footer={(
          <>
            <button
              type="button"
              className="btn btn--secondary"
              onClick={() => {
                setSettleTarget(null)
                setSettleReceiptNumber('')
                setSettleNotes('')
              }}
              disabled={settleSubmitting}
            >
              Cancel
            </button>
            <button type="button" className="btn btn--primary" onClick={() => void handleSettleVoucher()} disabled={settleSubmitting}>
              {settleSubmitting ? 'Settling...' : 'Settle'}
            </button>
          </>
        )}
      >
        <div className="petty-cash-form-grid">
          <label className="field-label" htmlFor="settle-receipt-number">Receipt Number</label>
          <input
            id="settle-receipt-number"
            className="field-input"
            type="text"
            maxLength={100}
            value={settleReceiptNumber}
            onChange={event => setSettleReceiptNumber(event.target.value)}
            placeholder="e.g. RCT-0001"
          />

          <label className="field-label" htmlFor="settle-notes">Notes</label>
          <textarea
            id="settle-notes"
            className="field-input"
            rows={3}
            maxLength={500}
            value={settleNotes}
            onChange={event => setSettleNotes(event.target.value)}
            placeholder="Optional settlement notes"
          />
        </div>
      </Modal>

      <Modal
        open={rejectTarget !== null}
        onClose={() => {
          setRejectTarget(null)
          setRejectReason('')
        }}
        title="Reject Voucher"
        subtitle={rejectTarget ? rejectTarget.referenceNumber : undefined}
        width="sm"
        footer={(
          <>
            <button
              type="button"
              className="btn btn--secondary"
              onClick={() => {
                setRejectTarget(null)
                setRejectReason('')
              }}
              disabled={rejectSubmitting}
            >
              Cancel
            </button>
            <button type="button" className="btn btn--primary" onClick={() => void handleRejectVoucher()} disabled={rejectSubmitting}>
              {rejectSubmitting ? 'Rejecting...' : 'Reject'}
            </button>
          </>
        )}
      >
        <div className="petty-cash-form-grid">
          <label className="field-label" htmlFor="reject-reason">Rejection Reason</label>
          <textarea
            id="reject-reason"
            className="field-input"
            rows={4}
            maxLength={500}
            value={rejectReason}
            onChange={event => setRejectReason(event.target.value)}
            placeholder="Explain why this voucher is being rejected"
          />
        </div>
      </Modal>

      <MakerCheckerWarning
        open={overrideTarget !== null}
        onClose={() => setOverrideTarget(null)}
        isAdmin={isAdmin}
        onOverride={reason => void handleApproveOverride(reason)}
        submitting={overrideTarget !== null && actionLoading === overrideTarget}
        action="approve"
      />
    </div>
  )
}
