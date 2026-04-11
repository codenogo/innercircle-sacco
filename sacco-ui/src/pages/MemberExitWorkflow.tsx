import { useMemo, useState, type FormEvent } from 'react'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { ApiError } from '../services/apiClient'
import {
  createMemberExitRequest,
  getMemberExitRequests,
  processMemberExitInstallment,
  reviewMemberExitRequest,
} from '../services/policyWorkflowService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import type { MemberExitInstallmentResponse, MemberExitRequestResponse, MemberExitRequestStatus } from '../types/policyWorkflows'
import './Operations.css'

interface Feedback {
  type: 'success' | 'error'
  text: string
}

interface CreateFormState {
  memberId: string
  noticeDate: string
  effectiveDate: string
}

interface ReviewFormState {
  memberId: string
  requestId: string
  status: MemberExitRequestStatus
  reviewNotes: string
}

interface ProcessFormState {
  memberId: string
  requestId: string
}

const EMPTY_CREATE_FORM: CreateFormState = {
  memberId: '',
  noticeDate: '',
  effectiveDate: '',
}

const EMPTY_REVIEW_FORM: ReviewFormState = {
  memberId: '',
  requestId: '',
  status: 'UNDER_REVIEW',
  reviewNotes: '',
}

const EMPTY_PROCESS_FORM: ProcessFormState = {
  memberId: '',
  requestId: '',
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function formatMoney(value: number | null | undefined) {
  if (value == null) return '-'
  return value.toLocaleString('en-KE')
}

export function MemberExitWorkflow() {
  const { request } = useAuthenticatedApi()
  const { canAccess } = useAuthorization()

  const [lookupMemberId, setLookupMemberId] = useState('')
  const [requests, setRequests] = useState<MemberExitRequestResponse[]>([])
  const [latestInstallment, setLatestInstallment] = useState<MemberExitInstallmentResponse | null>(null)

  const [createForm, setCreateForm] = useState<CreateFormState>(EMPTY_CREATE_FORM)
  const [reviewForm, setReviewForm] = useState<ReviewFormState>(EMPTY_REVIEW_FORM)
  const [processForm, setProcessForm] = useState<ProcessFormState>(EMPTY_PROCESS_FORM)

  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  const canReviewExitRequest = canAccess(['ADMIN', 'TREASURER', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'])
  const canProcessInstallment = canAccess(['ADMIN', 'TREASURER'])

  const columns = useMemo<ColumnDef<MemberExitRequestResponse>[]>(() => [
    { key: 'id', header: 'Request', render: row => row.id },
    { key: 'status', header: 'Status', render: row => row.status },
    { key: 'member', header: 'Member', render: row => row.memberId },
    { key: 'notice', header: 'Notice', render: row => row.noticeDate },
    { key: 'effective', header: 'Effective', render: row => row.effectiveDate },
    { key: 'net', header: 'Net Settlement', className: 'ledger-table-amount', render: row => `KES ${formatMoney(row.netSettlementAmount)}` },
    {
      key: 'installments',
      header: 'Installments',
      render: row => `${row.installmentsProcessed ?? 0} / ${row.installmentCount ?? 0}`,
    },
    { key: 'next', header: 'Next Installment Due', render: row => row.nextInstallmentDueDate ?? '-' },
  ], [])

  async function loadRequests(memberId: string) {
    setLoading(true)
    try {
      const data = await getMemberExitRequests(memberId, request)
      setRequests(data)
      setFeedback(null)
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to load exit requests.') })
    } finally {
      setLoading(false)
    }
  }

  async function handleLookup(event: FormEvent) {
    event.preventDefault()
    if (!lookupMemberId.trim()) return
    await loadRequests(lookupMemberId.trim())
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setFeedback(null)
    try {
      const created = await createMemberExitRequest(createForm.memberId.trim(), {
        noticeDate: createForm.noticeDate,
        effectiveDate: createForm.effectiveDate,
      }, request)
      if (lookupMemberId.trim() === created.memberId) {
        setRequests(prev => [created, ...prev])
      }
      setCreateForm(EMPTY_CREATE_FORM)
      setFeedback({ type: 'success', text: 'Member exit request created.' })
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to create exit request.') })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleReview(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setFeedback(null)
    try {
      const updated = await reviewMemberExitRequest(reviewForm.memberId.trim(), reviewForm.requestId.trim(), {
        status: reviewForm.status,
        reviewNotes: reviewForm.reviewNotes.trim() || undefined,
      }, request)
      if (lookupMemberId.trim() === updated.memberId) {
        setRequests(prev => prev.map(item => (item.id === updated.id ? updated : item)))
      }
      setFeedback({ type: 'success', text: 'Exit request review updated.' })
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to review exit request.') })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleProcessInstallment(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setFeedback(null)
    try {
      const processed = await processMemberExitInstallment(processForm.memberId.trim(), processForm.requestId.trim(), request)
      setLatestInstallment(processed)
      if (lookupMemberId.trim() === processForm.memberId.trim()) {
        await loadRequests(processForm.memberId.trim())
      }
      setFeedback({ type: 'success', text: `Installment ${processed.installmentNumber} processed.` })
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to process installment.') })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Member Exit Workflow</h1>
          <p className="page-subtitle">Handle notice, review, and installment processing for member exits.</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      {feedback && (
        <div className={`ops-feedback ops-feedback--${feedback.type}`} role="status">{feedback.text}</div>
      )}

      <section className="page-section">
        <span className="page-section-title">Create Exit Request</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleCreate(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Member ID</label>
              <input className="field-input" value={createForm.memberId} onChange={event => setCreateForm(prev => ({ ...prev, memberId: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Notice Date</label>
              <input className="field-input" type="date" value={createForm.noticeDate} onChange={event => setCreateForm(prev => ({ ...prev, noticeDate: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Effective Date</label>
              <input className="field-input" type="date" value={createForm.effectiveDate} onChange={event => setCreateForm(prev => ({ ...prev, effectiveDate: event.target.value }))} required />
            </div>
          </div>
          <button type="submit" className="btn btn--primary" disabled={submitting}>Create</button>
        </form>
      </section>

      {canReviewExitRequest && (
        <section className="page-section">
          <span className="page-section-title">Review Exit Request</span>
          <hr className="rule" />
          <form className="modal-form" onSubmit={event => void handleReview(event)}>
            <div className="field-row">
              <div className="field">
                <label className="field-label field-label--required">Member ID</label>
                <input className="field-input" value={reviewForm.memberId} onChange={event => setReviewForm(prev => ({ ...prev, memberId: event.target.value }))} required />
              </div>
              <div className="field">
                <label className="field-label field-label--required">Request ID</label>
                <input className="field-input" value={reviewForm.requestId} onChange={event => setReviewForm(prev => ({ ...prev, requestId: event.target.value }))} required />
              </div>
              <div className="field">
                <label className="field-label field-label--required">Status</label>
                <select className="field-input" value={reviewForm.status} onChange={event => setReviewForm(prev => ({ ...prev, status: event.target.value as MemberExitRequestStatus }))}>
                  <option value="UNDER_REVIEW">Under Review</option>
                  <option value="APPROVED">Approved</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="REJECTED">Rejected</option>
                </select>
              </div>
            </div>
            <div className="field">
              <label className="field-label">Review Notes</label>
              <input className="field-input" value={reviewForm.reviewNotes} onChange={event => setReviewForm(prev => ({ ...prev, reviewNotes: event.target.value }))} />
            </div>
            <button type="submit" className="btn btn--secondary" disabled={submitting}>Save Review</button>
          </form>
        </section>
      )}

      {canProcessInstallment && (
        <section className="page-section">
          <span className="page-section-title">Process Installment</span>
          <hr className="rule" />
          <form className="modal-form" onSubmit={event => void handleProcessInstallment(event)}>
            <div className="field-row">
              <div className="field">
                <label className="field-label field-label--required">Member ID</label>
                <input className="field-input" value={processForm.memberId} onChange={event => setProcessForm(prev => ({ ...prev, memberId: event.target.value }))} required />
              </div>
              <div className="field">
                <label className="field-label field-label--required">Request ID</label>
                <input className="field-input" value={processForm.requestId} onChange={event => setProcessForm(prev => ({ ...prev, requestId: event.target.value }))} required />
              </div>
              <div className="field" style={{ alignSelf: 'end' }}>
                <button type="submit" className="btn btn--primary" disabled={submitting}>Process Next Installment</button>
              </div>
            </div>
          </form>
          {latestInstallment && (
            <div className="settings-row settings-row--compact">
              <span className="settings-row-label">Latest Installment</span>
              <span className="settings-row-value">
                #{latestInstallment.installmentNumber} - KES {formatMoney(latestInstallment.amount)} ({latestInstallment.processed ? 'Processed' : 'Pending'})
              </span>
            </div>
          )}
        </section>
      )}

      <section className="page-section">
        <span className="page-section-title">Exit Requests</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleLookup(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Member ID</label>
              <input className="field-input" value={lookupMemberId} onChange={event => setLookupMemberId(event.target.value)} required />
            </div>
            <div className="field" style={{ alignSelf: 'end' }}>
              <button type="submit" className="btn btn--secondary" disabled={loading || submitting}>Load Requests</button>
            </div>
          </div>
        </form>
        {loading ? (
          <div className="ops-feedback">Loading requests...</div>
        ) : (
          <DataTable
            columns={columns}
            data={requests}
            getRowKey={row => row.id}
            emptyMessage="No exit requests found for this member."
          />
        )}
      </section>
    </div>
  )
}
