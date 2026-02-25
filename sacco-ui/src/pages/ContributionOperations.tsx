import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { ActionMenu, type ActionMenuItem } from '../components/ActionMenu'
import { Breadcrumb } from '../components/Breadcrumb'
import { MonthPicker } from '../components/MonthPicker'
import { Spinner } from '../components/Spinner'
import { Select } from '../components/Select'
import { DatePicker } from '../components/DatePicker'
import { ApiError } from '../services/apiClient'
import {
  confirmContribution,
  getCategories,
  getContributions,
  recordBulkContributions,
  reverseContribution,
} from '../services/contributionService'
import { getAllMembers } from '../services/memberService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import type { MemberResponse } from '../types/members'
import type {
  ContributionResponse,
  ContributionStatus,
  ContributionCategoryResponse,
  BulkContributionRequest,
  PaymentMode,
} from '../types/contributions'
import './Operations.css'

function currentMonth(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function fmt(n: number) { return n.toLocaleString('en-KE') }

function fmtDate(value: string): string {
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('en-KE', { day: '2-digit', month: 'short', year: 'numeric' })
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

const statusClass: Record<ContributionStatus, string> = {
  PENDING: 'badge--pending',
  CONFIRMED: 'badge--completed',
  REVERSED: 'badge--defaulted',
}

const PAYMENT_MODE_OPTIONS = [
  { value: 'MPESA', label: 'M-Pesa' },
  { value: 'BANK', label: 'Bank Transfer' },
  { value: 'CASH', label: 'Cash' },
  { value: 'CHECK', label: 'Check' },
]

const STATUS_FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Statuses' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'REVERSED', label: 'Reversed' },
]

interface BulkRow {
  memberId: string
  amount: string
}

export function ContributionOperations() {
  const { request } = useAuthenticatedApi()

  // --- Operations table state ---
  const [contributions, setContributions] = useState<ContributionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)

  const [members, setMembers] = useState<MemberResponse[]>([])
  const [month, setMonth] = useState(currentMonth)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null)
  const [actionInProgress, setActionInProgress] = useState<string | null>(null)

  // --- Bulk form state ---
  const [showBulkForm, setShowBulkForm] = useState(false)
  const [bulkPaymentMode, setBulkPaymentMode] = useState<PaymentMode>('MPESA')
  const [bulkMonth, setBulkMonth] = useState(currentMonth)
  const [bulkDate, setBulkDate] = useState('')
  const [bulkCategoryId, setBulkCategoryId] = useState('')
  const [bulkRows, setBulkRows] = useState<BulkRow[]>([{ memberId: '', amount: '' }])
  const [categories, setCategories] = useState<ContributionCategoryResponse[]>([])
  const [submittingBulk, setSubmittingBulk] = useState(false)

  // --- Member map ---
  const memberMap = useMemo(() => {
    const map = new Map<string, string>()
    for (const m of members) {
      map.set(m.id, `${m.firstName} ${m.lastName}`)
    }
    return map
  }, [members])

  const memberOptions = useMemo(
    () => members.map(m => ({ value: m.id, label: `${m.firstName} ${m.lastName}` })),
    [members],
  )

  const categoryOptions = useMemo(
    () => categories.map(c => ({ value: c.id, label: c.name })),
    [categories],
  )

  // --- Fetch contributions ---
  const fetchContributions = useCallback(async (cursor?: string) => {
    if (cursor) setLoadingMore(true)
    else setLoading(true)

    try {
      const page = await getContributions(cursor, 50, request, month)
      setContributions(prev => {
        if (!cursor) return page.items
        const merged = new Map<string, ContributionResponse>()
        prev.forEach(c => merged.set(c.id, c))
        page.items.forEach(c => merged.set(c.id, c))
        return Array.from(merged.values())
      })
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
    } catch (err) {
      setFeedback({ type: 'error', message: toErrorMessage(err, 'Failed to load contributions') })
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [month, request])

  // --- Fetch members ---
  const fetchMembers = useCallback(async () => {
    try {
      const allMembers = await getAllMembers(request)
      setMembers(allMembers)
    } catch {
      // Members are non-critical for display; silent fallback
    }
  }, [request])

  // --- Fetch categories ---
  const fetchCategories = useCallback(async () => {
    try {
      const data = await getCategories(true, request)
      setCategories(data)
    } catch {
      // Silent fallback
    }
  }, [request])

  // --- Initial load ---
  useEffect(() => {
    void fetchContributions()
    void fetchMembers()
  }, [fetchContributions, fetchMembers])

  // --- Load categories when bulk form opens ---
  useEffect(() => {
    if (showBulkForm && categories.length === 0) {
      void fetchCategories()
    }
  }, [showBulkForm, categories.length, fetchCategories])

  // --- Confirm action ---
  const handleConfirm = useCallback(async (id: string) => {
    setActionInProgress(id)
    setFeedback(null)
    try {
      const updated = await confirmContribution(id, request)
      setContributions(prev => prev.map(c => (c.id === id ? updated : c)))
      setFeedback({ type: 'success', message: 'Contribution confirmed successfully.' })
    } catch (err) {
      setFeedback({ type: 'error', message: toErrorMessage(err, 'Failed to confirm contribution') })
    } finally {
      setActionInProgress(null)
    }
  }, [request])

  // --- Reverse action ---
  const handleReverse = useCallback(async (id: string) => {
    if (!window.confirm('Are you sure you want to reverse this contribution?')) return
    setActionInProgress(id)
    setFeedback(null)
    try {
      const updated = await reverseContribution(id, request)
      setContributions(prev => prev.map(c => (c.id === id ? updated : c)))
      setFeedback({ type: 'success', message: 'Contribution reversed successfully.' })
    } catch (err) {
      setFeedback({ type: 'error', message: toErrorMessage(err, 'Failed to reverse contribution') })
    } finally {
      setActionInProgress(null)
    }
  }, [request])

  // --- Columns ---
  const contribColumns = useMemo((): ColumnDef<ContributionResponse>[] => [
    { key: 'ref', header: 'Ref', render: c => <span className="data">{c.id.slice(0, 8)}</span> },
    { key: 'member', header: 'Member', render: c => memberMap.get(c.memberId) ?? c.memberId.slice(0, 8) },
    { key: 'category', header: 'Category', render: c => <span className="data">{c.category.name}</span> },
    { key: 'status', header: 'Status', render: c => <span className={`badge ${statusClass[c.status]}`}>{c.status}</span> },
    { key: 'date', header: 'Date', render: c => <span className="data">{fmtDate(c.contributionDate)}</span> },
    { key: 'amount', header: 'Gross (KES)', headerClassName: 'ledger-table-amount', className: 'amount ledger-table-amount', render: c => fmt(c.amount) },
    {
      key: 'contributionAmount',
      header: 'Net (KES)',
      headerClassName: 'ledger-table-amount',
      className: 'amount ledger-table-amount',
      render: c => fmt(c.contributionAmount ?? c.amount),
    },
    {
      key: 'welfareAmount',
      header: 'Welfare (KES)',
      headerClassName: 'ledger-table-amount',
      className: 'amount ledger-table-amount',
      render: c => fmt(c.welfareAmount ?? 0),
    },
    {
      key: 'actions',
      header: '',
      width: '52px',
      headerClassName: 'datatable-col-actions',
      className: 'datatable-col-actions',
      render: c => {
        const busy = actionInProgress === c.id
        const items: ActionMenuItem[] = []
        if (c.status === 'PENDING') {
          items.push({ label: 'Confirm', onClick: () => void handleConfirm(c.id), disabled: busy })
        }
        if (c.status === 'PENDING' || c.status === 'CONFIRMED') {
          items.push({ label: 'Reverse', onClick: () => void handleReverse(c.id), variant: 'danger', disabled: busy })
        }
        return <ActionMenu actions={items} />
      },
    },
  ], [memberMap, actionInProgress, handleConfirm, handleReverse])

  // --- Filtered contributions ---
  const filtered = useMemo(() => {
    return contributions.filter(c => {
      if (c.contributionMonth.slice(0, 7) !== month) return false
      if (statusFilter !== 'ALL' && c.status !== statusFilter) return false
      return true
    })
  }, [contributions, month, statusFilter])

  // --- Bulk form handlers ---
  function updateBulkRow(index: number, field: keyof BulkRow, value: string) {
    setBulkRows(prev => prev.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
  }

  function addBulkRow() {
    setBulkRows(prev => [...prev, { memberId: '', amount: '' }])
  }

  function removeBulkRow(index: number) {
    setBulkRows(prev => prev.filter((_, i) => i !== index))
  }

  async function handleBulkSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmittingBulk(true)
    setFeedback(null)

    const payload: BulkContributionRequest = {
      paymentMode: bulkPaymentMode,
      contributionMonth: bulkMonth + '-01',
      contributionDate: bulkDate,
      categoryId: bulkCategoryId,
      contributions: bulkRows
        .filter(r => r.memberId && r.amount)
        .map(r => ({ memberId: r.memberId, amount: Number(r.amount) })),
    }

    try {
      await recordBulkContributions(payload, request)
      setFeedback({
        type: 'success',
        message: `Bulk entry submitted: ${payload.contributions.length} contribution(s) recorded.`,
      })
      setBulkRows([{ memberId: '', amount: '' }])
      setShowBulkForm(false)
      void fetchContributions()
    } catch (err) {
      setFeedback({ type: 'error', message: toErrorMessage(err, 'Bulk submission failed') })
    } finally {
      setSubmittingBulk(false)
    }
  }

  return (
    <div className="ops-page">
      <Breadcrumb items={[
        { label: 'Operations', to: '/operations' },
        { label: 'Contribution Operations' },
      ]} />
      <div className="page-header">
        <div>
          <h1 className="page-title">Contribution Operations</h1>
          <p className="page-subtitle">Confirm, reverse, and bulk-record contributions</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      {feedback && (
        <div className={`ops-feedback ops-feedback--${feedback.type}`} role="status">
          {feedback.message}
        </div>
      )}

      {/* --- Filters --- */}
      <div className="filter-bar">
        <span className="page-section-title page-section-title--inline">Month</span>
        <MonthPicker value={month} onChange={setMonth} />
        <Select
          value={statusFilter}
          onChange={setStatusFilter}
          options={STATUS_FILTER_OPTIONS}
        />
      </div>

      {/* --- Operations Table --- */}
      <DataTable<ContributionResponse>
        columns={contribColumns}
        data={filtered}
        getRowKey={row => row.id}
        loading={loading}
        emptyMessage="No contributions found for the selected filters."
        getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
      />

      {/* --- Load More --- */}
      {hasMore && (
        <div className="ops-pager">
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loadingMore}
            onClick={() => { if (nextCursor) void fetchContributions(nextCursor) }}
          >
            {loadingMore ? <><Spinner size="sm" /> Loading...</> : 'Load More'}
          </button>
        </div>
      )}

      {/* --- Bulk Processing Section --- */}
      <section className="page-section">
        <span className="page-section-title">Bulk Contribution Entry</span>
        <hr className="rule" />

        {!showBulkForm ? (
          <button
            type="button"
            className="btn btn--primary"
            onClick={() => setShowBulkForm(true)}
          >
            New Bulk Entry
          </button>
        ) : (
          <form onSubmit={e => void handleBulkSubmit(e)}>
            {/* Shared defaults */}
            <div className="field-row">
              <div className="field">
                <label className="field-label">Payment Mode</label>
                <Select
                  value={bulkPaymentMode}
                  onChange={v => setBulkPaymentMode(v as PaymentMode)}
                  options={PAYMENT_MODE_OPTIONS}
                />
              </div>
              <div className="field">
                <label className="field-label">Month</label>
                <MonthPicker value={bulkMonth} onChange={setBulkMonth} />
              </div>
              <div className="field">
                <label className="field-label">Date</label>
                <DatePicker value={bulkDate} onChange={setBulkDate} required />
              </div>
              <div className="field">
                <label className="field-label">Category</label>
                <Select
                  value={bulkCategoryId}
                  onChange={setBulkCategoryId}
                  options={categoryOptions}
                  placeholder="Select category..."
                  required
                />
              </div>
            </div>

            {/* Dynamic rows */}
            {bulkRows.map((row, i) => (
              <div className="field-row" key={i}>
                <div className="field">
                  <label className="field-label">Member</label>
                  <Select
                    value={row.memberId}
                    onChange={v => updateBulkRow(i, 'memberId', v)}
                    options={memberOptions}
                    placeholder="Select member..."
                    searchable
                    required
                  />
                </div>
                <div className="field">
                  <label className="field-label">Amount (KES)</label>
                  <input
                    type="number"
                    className="field-input"
                    value={row.amount}
                    onChange={e => updateBulkRow(i, 'amount', e.target.value)}
                    min="1"
                    step="0.01"
                    required
                  />
                </div>
                <div className="field">
                  {bulkRows.length > 1 && (
                    <button
                      type="button"
                      className="btn btn--secondary btn--small"
                      onClick={() => removeBulkRow(i)}
                    >
                      Remove
                    </button>
                  )}
                </div>
              </div>
            ))}

            <div className="ops-inline-actions">
              <button type="button" className="btn btn--secondary btn--small" onClick={addBulkRow}>
                Add Row
              </button>
              <button
                type="submit"
                className="btn btn--primary"
                disabled={submittingBulk}
              >
                {submittingBulk ? 'Submitting...' : 'Submit Bulk Entry'}
              </button>
              <button
                type="button"
                className="btn btn--secondary"
                onClick={() => setShowBulkForm(false)}
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        <p className="ops-note">
          Record multiple contributions at once via POST /api/v1/contributions/bulk.
        </p>
      </section>
    </div>
  )
}
