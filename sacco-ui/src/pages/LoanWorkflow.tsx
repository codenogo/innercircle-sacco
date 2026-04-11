import { useCallback, useEffect, useMemo, useState } from 'react'
import { Bank, MagnifyingGlass } from '@phosphor-icons/react'
import { Breadcrumb } from '../components/Breadcrumb'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { ActionMenu, type ActionMenuItem } from '../components/ActionMenu'
import { Spinner } from '../components/Spinner'
import { Modal } from '../components/Modal'
import { NewLoanModal } from '../components/NewLoanModal'
import { MakerCheckerWarning } from '../components/MakerCheckerWarning'
import { ApiError } from '../services/apiClient'
import { getAllMembers } from '../services/memberService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { isMakerCheckerViolation } from '../types/makerChecker'
import type { CursorPage } from '../types/users'
import type { LoanApplicationRequest, LoanResponse, LoanStatus, RepaymentScheduleResponse } from '../types/loans'
import type { MemberResponse } from '../types/members'
import './LoanWorkflow.css'

const PAGE_SIZE = 50

const statusClass: Record<LoanStatus, string> = {
  DISBURSED: 'badge--active',
  REPAYING: 'badge--active',
  PENDING: 'badge--pending',
  APPROVED: 'badge--approved',
  CLOSED: 'badge--completed',
  DEFAULTED: 'badge--defaulted',
  REJECTED: 'badge--rejected',
}

function fmtAmount(value: number | string): string {
  const parsed = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(parsed)) return '0'
  return parsed.toLocaleString('en-KE')
}

function fmtDate(value: string | null): string {
  if (!value) return '\u2014'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '\u2014'
  return date.toLocaleDateString('en-KE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

export function LoanWorkflow() {
  const { request } = useAuthenticatedApi()
  const { canAccess } = useAuthorization()
  const isAdmin = canAccess(['ADMIN'])

  const [loans, setLoans] = useState<LoanResponse[]>([])
  const [members, setMembers] = useState<MemberResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)

  const [search, setSearch] = useState('')
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  const [showApplyModal, setShowApplyModal] = useState(false)
  const [submittingApply, setSubmittingApply] = useState(false)

  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [overrideTarget, setOverrideTarget] = useState<{ id: string; action: 'approve' | 'reject' } | null>(null)

  const [repayTarget, setRepayTarget] = useState<LoanResponse | null>(null)
  const [repayAmount, setRepayAmount] = useState('')
  const [repayReference, setRepayReference] = useState('')
  const [submittingRepay, setSubmittingRepay] = useState(false)

  const [scheduleTarget, setScheduleTarget] = useState<LoanResponse | null>(null)
  const [scheduleRows, setScheduleRows] = useState<RepaymentScheduleResponse[]>([])
  const [loadingSchedule, setLoadingSchedule] = useState(false)
  const [scheduleError, setScheduleError] = useState<string | null>(null)

  const memberIndex = useMemo(() => {
    const map = new Map<string, MemberResponse>()
    members.forEach(member => map.set(member.id, member))
    return map
  }, [members])

  const memberOptions = useMemo(
    () =>
      members.map(member => ({
        id: member.id,
        name: `${member.firstName} ${member.lastName} (${member.memberNumber})`.trim(),
      })),
    [members],
  )

  const loadLoans = useCallback(async (opts?: { append?: boolean; cursor?: string | null }) => {
    const append = Boolean(opts?.append)
    const cursor = opts?.cursor

    if (append) setLoadingMore(true)
    else setLoading(true)

    try {
      const path = cursor
        ? `/api/v1/loans?limit=${PAGE_SIZE}&cursor=${encodeURIComponent(cursor)}`
        : `/api/v1/loans?limit=${PAGE_SIZE}`
      const page = await request<CursorPage<LoanResponse>>(path)

      setLoans(prev => {
        if (!append) return page.items
        const merged = new Map<string, LoanResponse>()
        prev.forEach(loan => merged.set(loan.id, loan))
        page.items.forEach(loan => merged.set(loan.id, loan))
        return Array.from(merged.values())
      })

      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
    } catch (error) {
      setFeedback({
        type: 'error',
        text: toErrorMessage(error, 'Unable to load loan applications.'),
      })
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [request])

  useEffect(() => {
    void loadLoans({ append: false, cursor: null })
  }, [loadLoans])

  useEffect(() => {
    let cancelled = false

    async function loadMembers() {
      try {
        const all = await getAllMembers(request)
        if (!cancelled) {
          setMembers(all)
        }
      } catch {
        // Non-blocking for workflow table.
      }
    }

    void loadMembers()
    return () => {
      cancelled = true
    }
  }, [request])

  async function handleApplyLoan(payload: LoanApplicationRequest) {
    setSubmittingApply(true)
    setFeedback(null)
    try {
      const created = await request<LoanResponse>('/api/v1/loans/apply', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setLoans(prev => [created, ...prev])
      setShowApplyModal(false)
      setFeedback({ type: 'success', text: 'Loan application submitted successfully.' })
    } catch (error) {
      const message = toErrorMessage(error, 'Unable to submit loan application.')
      setFeedback({ type: 'error', text: message })
      throw error instanceof Error ? error : new Error(message)
    } finally {
      setSubmittingApply(false)
    }
  }

  const handleLoanAction = useCallback(async (
    loanId: string,
    action: 'approve' | 'reject' | 'disburse',
    overrideReason?: string,
  ) => {
    const loadingKey = `${action}:${loanId}`
    setActionLoading(loadingKey)
    setFeedback(null)

    try {
      const body =
        overrideReason != null && overrideReason.trim().length > 0
          ? JSON.stringify({ overrideReason: overrideReason.trim() })
          : undefined

      const updated = await request<LoanResponse>(`/api/v1/loans/${loanId}/${action}`, {
        method: 'PATCH',
        ...(body ? { body } : {}),
      })

      setLoans(prev => prev.map(loan => (loan.id === loanId ? updated : loan)))
      setFeedback({
        type: 'success',
        text:
          action === 'approve'
            ? 'Loan approved successfully.'
            : action === 'reject'
              ? 'Loan rejected successfully.'
              : 'Loan disbursed successfully.',
      })
    } catch (error) {
      if ((action === 'approve' || action === 'reject') && isMakerCheckerViolation(error)) {
        setOverrideTarget({ id: loanId, action })
      } else {
        setFeedback({
          type: 'error',
          text: toErrorMessage(error, `Unable to ${action} loan.`),
        })
      }
    } finally {
      setActionLoading(null)
    }
  }, [request])

  async function handleOverride(reason: string) {
    if (!overrideTarget) return
    const { id, action } = overrideTarget
    try {
      await handleLoanAction(id, action, reason)
    } finally {
      setOverrideTarget(null)
    }
  }

  const openRepayModal = useCallback((loan: LoanResponse) => {
    setRepayTarget(loan)
    setRepayAmount('')
    setRepayReference('')
  }, [])

  async function submitRepayment() {
    if (!repayTarget) return

    const amount = Number(repayAmount)
    if (!Number.isFinite(amount) || amount <= 0) {
      setFeedback({ type: 'error', text: 'Repayment amount must be greater than zero.' })
      return
    }

    setSubmittingRepay(true)
    setFeedback(null)
    try {
      await request<void>(`/api/v1/loans/${repayTarget.id}/repay`, {
        method: 'POST',
        body: JSON.stringify({
          amount,
          referenceNumber: repayReference.trim() || undefined,
        }),
      })
      setFeedback({ type: 'success', text: 'Repayment recorded successfully.' })
      setRepayTarget(null)
      await loadLoans({ append: false, cursor: null })
    } catch (error) {
      setFeedback({
        type: 'error',
        text: toErrorMessage(error, 'Unable to record repayment.'),
      })
    } finally {
      setSubmittingRepay(false)
    }
  }

  const openSchedule = useCallback(async (loan: LoanResponse) => {
    setScheduleTarget(loan)
    setScheduleRows([])
    setScheduleError(null)
    setLoadingSchedule(true)
    try {
      const rows = await request<RepaymentScheduleResponse[]>(`/api/v1/loans/${loan.id}/schedule`)
      setScheduleRows(rows)
    } catch (error) {
      setScheduleError(toErrorMessage(error, 'Unable to load repayment schedule.'))
    } finally {
      setLoadingSchedule(false)
    }
  }, [request])

  const filteredLoans = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return loans

    return loans.filter(loan => {
      const member = memberIndex.get(loan.memberId)
      const text = [
        loan.loanNumber ?? '',
        loan.id,
        member?.memberNumber ?? '',
        member?.firstName ?? '',
        member?.lastName ?? '',
        loan.status,
        loan.purpose,
        String(loan.principalAmount),
        String(loan.outstandingBalance),
      ].join(' ').toLowerCase()
      return text.includes(query)
    })
  }, [loans, memberIndex, search])

  const pendingCount = loans.filter(loan => loan.status === 'PENDING').length
  const approvedCount = loans.filter(loan => loan.status === 'APPROVED').length
  const repayingCount = loans.filter(loan => loan.status === 'REPAYING').length

  const loanColumns: ColumnDef<LoanResponse>[] = useMemo(
    () => [
      {
        key: 'loanNumber',
        header: 'Loan Number',
        className: 'data loan-id',
        render: loan => loan.loanNumber ?? loan.id,
      },
      {
        key: 'member',
        header: 'Member',
        render: loan => {
          const member = memberIndex.get(loan.memberId)
          return (
            <>
              <span className="loan-workflow-member-number">{member?.memberNumber ?? loan.memberId}</span>
              <span className="loan-workflow-member-name">
                {member ? `${member.firstName} ${member.lastName}` : '\u2014'}
              </span>
            </>
          )
        },
      },
      {
        key: 'principal',
        header: 'Principal (KES)',
        className: 'amount ledger-table-amount',
        headerClassName: 'ledger-table-amount',
        render: loan => fmtAmount(loan.principalAmount),
      },
      {
        key: 'status',
        header: 'Status',
        render: loan => <span className={`badge ${statusClass[loan.status]}`}>{loan.status}</span>,
      },
      {
        key: 'disbursedAt',
        header: 'Disbursed',
        className: 'data',
        render: loan => fmtDate(loan.disbursedAt),
      },
      {
        key: 'balance',
        header: 'Outstanding (KES)',
        className: 'amount ledger-table-amount',
        headerClassName: 'ledger-table-amount',
        render: loan => fmtAmount(loan.outstandingBalance),
      },
      {
        key: 'actions',
        header: '',
        width: '52px',
        headerClassName: 'datatable-col-actions',
        className: 'datatable-col-actions',
        render: loan => {
          const busy = actionLoading !== null
          const items: ActionMenuItem[] = []
          if (loan.status === 'PENDING') {
            items.push(
              { label: 'Approve', onClick: () => void handleLoanAction(loan.id, 'approve'), disabled: busy },
              { label: 'Reject', onClick: () => void handleLoanAction(loan.id, 'reject'), variant: 'danger', disabled: busy },
            )
          }
          if (loan.status === 'APPROVED') {
            items.push({ label: 'Disburse', onClick: () => void handleLoanAction(loan.id, 'disburse'), disabled: busy })
          }
          if (loan.status === 'REPAYING' || loan.status === 'DISBURSED') {
            items.push({ label: 'Repay', onClick: () => openRepayModal(loan) })
          }
          items.push({ label: 'Schedule', onClick: () => void openSchedule(loan) })
          return <ActionMenu actions={items} />
        },
      },
    ],
    [actionLoading, handleLoanAction, memberIndex, openRepayModal, openSchedule],
  )

  const scheduleColumns: ColumnDef<RepaymentScheduleResponse>[] = useMemo(
    () => [
      {
        key: 'installment',
        header: '#',
        className: 'data',
        render: row => row.installmentNumber,
      },
      {
        key: 'dueDate',
        header: 'Due Date',
        className: 'data',
        render: row => fmtDate(row.dueDate),
      },
      {
        key: 'principal',
        header: 'Principal (KES)',
        className: 'amount ledger-table-amount',
        headerClassName: 'ledger-table-amount',
        render: row => fmtAmount(row.principalAmount),
      },
      {
        key: 'interest',
        header: 'Interest (KES)',
        className: 'amount ledger-table-amount',
        headerClassName: 'ledger-table-amount',
        render: row => fmtAmount(row.interestAmount),
      },
      {
        key: 'total',
        header: 'Total (KES)',
        className: 'amount ledger-table-amount',
        headerClassName: 'ledger-table-amount',
        render: row => fmtAmount(row.totalAmount),
      },
      {
        key: 'paid',
        header: 'Status',
        className: 'data',
        render: row => (row.paid ? 'PAID' : 'PENDING'),
      },
    ],
    [],
  )

  return (
    <div className="loan-workflow-page">
      <Breadcrumb items={[
        { label: 'Operations', to: '/operations' },
        { label: 'Loan Workflow' },
      ]} />
      <div className="page-header">
        <div>
          <h1 className="page-title">Loan Workflow</h1>
          <p className="page-subtitle">Application, approval, disbursement, repayment, and schedules</p>
        </div>
        <button className="btn btn--primary" onClick={() => setShowApplyModal(true)}>
          <Bank size={14} weight="bold" />
          New Loan Application
        </button>
      </div>

      <hr className="rule rule--strong" />

      <section className="page-section">
        <span className="page-section-title">Pipeline</span>
        <hr className="rule" />
        <div className="loan-workflow-summary">
          <div className="dot-leader">
            <span>Pending Approval</span>
            <span className="dot-leader-value">{pendingCount}</span>
          </div>
          <div className="dot-leader">
            <span>Approved Awaiting Disbursement</span>
            <span className="dot-leader-value">{approvedCount}</span>
          </div>
          <div className="dot-leader">
            <span>Repaying</span>
            <span className="dot-leader-value">{repayingCount}</span>
          </div>
        </div>
        <hr className="rule rule--strong" />
      </section>

      {feedback && (
        <div className={`ops-feedback ops-feedback--${feedback.type}`} role="status">
          {feedback.text}
        </div>
      )}

      <div className="filter-bar">
        <div className="filter-search-wrap">
          <MagnifyingGlass size={16} className="filter-search-icon" />
          <input
            type="text"
            className="filter-search"
            placeholder="Search by loan number, member number, status..."
            value={search}
            onChange={event => setSearch(event.target.value)}
          />
        </div>
      </div>

      <section className="page-section">
        <span className="page-section-title">Loans</span>
        <hr className="rule" />

        <DataTable<LoanResponse>
          columns={loanColumns}
          data={filteredLoans}
          getRowKey={row => row.id}
          loading={loading}
          emptyMessage="No loan applications found."
        />

        {hasMore && (
          <div className="ops-pager">
            <button
              type="button"
              className="btn btn--secondary"
              disabled={loadingMore || !nextCursor}
              onClick={() => void loadLoans({ append: true, cursor: nextCursor })}
            >
              {loadingMore ? <><Spinner size="sm" /> Loading...</> : 'Load More'}
            </button>
          </div>
        )}

        <hr className="rule rule--strong" />
      </section>

      <NewLoanModal
        open={showApplyModal}
        onClose={() => setShowApplyModal(false)}
        members={memberOptions}
        onSubmit={handleApplyLoan}
        isSubmitting={submittingApply}
      />

      <Modal
        open={repayTarget != null}
        onClose={() => {
          if (submittingRepay) return
          setRepayTarget(null)
        }}
        title="Record Repayment"
        subtitle={repayTarget ? `${repayTarget.loanNumber ?? repayTarget.id}` : undefined}
        width="sm"
        footer={(
          <>
            <button
              type="button"
              className="btn btn--secondary"
              onClick={() => setRepayTarget(null)}
              disabled={submittingRepay}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => void submitRepayment()}
              disabled={submittingRepay}
            >
              {submittingRepay ? 'Saving...' : 'Save Repayment'}
            </button>
          </>
        )}
      >
        <div className="modal-form">
          <div className="field">
            <label className="field-label">Amount (KES)</label>
            <input
              className="field-input"
              type="number"
              min={0}
              value={repayAmount}
              onChange={event => setRepayAmount(event.target.value)}
              placeholder="0"
              disabled={submittingRepay}
            />
          </div>

          <div className="field">
            <label className="field-label">Reference Number (optional)</label>
            <input
              className="field-input"
              value={repayReference}
              onChange={event => setRepayReference(event.target.value)}
              placeholder="e.g. MPESA-123456"
              disabled={submittingRepay}
            />
          </div>
        </div>
      </Modal>

      <Modal
        open={scheduleTarget != null}
        onClose={() => setScheduleTarget(null)}
        title="Repayment Schedule"
        subtitle={scheduleTarget ? `${scheduleTarget.loanNumber ?? scheduleTarget.id}` : undefined}
        width="lg"
      >
        {loadingSchedule ? (
          <div className="loan-workflow-modal-loading">
            <Spinner size="sm" /> Loading schedule...
          </div>
        ) : scheduleError ? (
          <div className="ops-feedback ops-feedback--error">{scheduleError}</div>
        ) : (
          <DataTable<RepaymentScheduleResponse>
            columns={scheduleColumns}
            data={scheduleRows}
            getRowKey={row => row.id}
            maxHeight="320px"
            emptyMessage="No schedule entries available."
          />
        )}
      </Modal>

      <MakerCheckerWarning
        open={overrideTarget != null}
        onClose={() => setOverrideTarget(null)}
        isAdmin={isAdmin}
        onOverride={handleOverride}
        submitting={actionLoading !== null}
        action={overrideTarget?.action ?? 'approve'}
      />
    </div>
  )
}
