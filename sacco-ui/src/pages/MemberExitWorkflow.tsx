import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
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
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useToast } from '../hooks/useToast'
import type { MemberExitInstallmentResponse, MemberExitRequestResponse, MemberExitRequestStatus } from '../types/policyWorkflows'
import './Operations.css'

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
  const { canAccess, isMemberOnly } = useAuthorization()
  const { profile } = useCurrentUser()
  const toast = useToast()
  const ownMemberId = profile?.member?.id ?? ''

  const [lookupMemberId, setLookupMemberId] = useState('')
  const [requests, setRequests] = useState<MemberExitRequestResponse[]>([])
  const [latestInstallment, setLatestInstallment] = useState<MemberExitInstallmentResponse | null>(null)

  const [createForm, setCreateForm] = useState<CreateFormState>(EMPTY_CREATE_FORM)
  const [reviewForm, setReviewForm] = useState<ReviewFormState>(EMPTY_REVIEW_FORM)
  const [processForm, setProcessForm] = useState<ProcessFormState>(EMPTY_PROCESS_FORM)

  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const canReviewExitRequest = !isMemberOnly && canAccess(['ADMIN', 'TREASURER', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'])
  const canProcessInstallment = !isMemberOnly && canAccess(['ADMIN', 'TREASURER'])

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

  const loadRequests = useCallback(async (memberId: string) => {
    setLoading(true)
    try {
      const data = await getMemberExitRequests(memberId, request)
      setRequests(data)
    } catch (error) {
      toast.error('Unable to load exit requests', toErrorMessage(error, 'Unable to load exit requests.'))
    } finally {
      setLoading(false)
    }
  }, [request, toast])

  useEffect(() => {
    if (!isMemberOnly || !ownMemberId) return
    setLookupMemberId(ownMemberId)
    setCreateForm(prev => ({ ...prev, memberId: ownMemberId }))
    void loadRequests(ownMemberId)
  }, [isMemberOnly, ownMemberId, loadRequests])

  async function handleLookup(event: FormEvent) {
    event.preventDefault()
    if (!lookupMemberId.trim()) return
    await loadRequests(lookupMemberId.trim())
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    try {
      const created = await createMemberExitRequest(createForm.memberId.trim(), {
        noticeDate: createForm.noticeDate,
        effectiveDate: createForm.effectiveDate,
      }, request)
      if (lookupMemberId.trim() === created.memberId) {
        setRequests(prev => [created, ...prev])
      }
      setCreateForm(EMPTY_CREATE_FORM)
      toast.success('Member exit request created')
    } catch (error) {
      toast.error('Unable to create exit request', toErrorMessage(error, 'Unable to create exit request.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleReview(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    try {
      const updated = await reviewMemberExitRequest(reviewForm.memberId.trim(), reviewForm.requestId.trim(), {
        status: reviewForm.status,
        reviewNotes: reviewForm.reviewNotes.trim() || undefined,
      }, request)
      if (lookupMemberId.trim() === updated.memberId) {
        setRequests(prev => prev.map(item => (item.id === updated.id ? updated : item)))
      }
      toast.success('Exit request review updated')
    } catch (error) {
      toast.error('Unable to review exit request', toErrorMessage(error, 'Unable to review exit request.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleProcessInstallment(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    try {
      const processed = await processMemberExitInstallment(processForm.memberId.trim(), processForm.requestId.trim(), request)
      setLatestInstallment(processed)
      if (lookupMemberId.trim() === processForm.memberId.trim()) {
        await loadRequests(processForm.memberId.trim())
      }
      toast.success('Installment processed', `Installment ${processed.installmentNumber} processed.`)
    } catch (error) {
      toast.error('Unable to process installment', toErrorMessage(error, 'Unable to process installment.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">{isMemberOnly ? 'Exit Workflow' : 'Member Exit Workflow'}</h1>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <section className="page-section">
        <span className="page-section-title">{isMemberOnly ? 'Submit Exit Notice' : 'Create Exit Request'}</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleCreate(event)}>
          <div className="field-row">
            {!isMemberOnly && (
              <div className="field">
                <label className="field-label field-label--required">Member ID</label>
                <input className="field-input" value={createForm.memberId} onChange={event => setCreateForm(prev => ({ ...prev, memberId: event.target.value }))} required />
              </div>
            )}
            <div className="field">
              <label className="field-label field-label--required">Notice Date</label>
              <input className="field-input" type="date" value={createForm.noticeDate} onChange={event => setCreateForm(prev => ({ ...prev, noticeDate: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Effective Date</label>
              <input className="field-input" type="date" value={createForm.effectiveDate} onChange={event => setCreateForm(prev => ({ ...prev, effectiveDate: event.target.value }))} required />
            </div>
          </div>
          <button type="submit" className="btn btn--primary" disabled={submitting || (isMemberOnly && !ownMemberId)}>
            {isMemberOnly ? 'Submit Notice' : 'Create'}
          </button>
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
        <span className="page-section-title">{isMemberOnly ? 'My Exit Requests' : 'Exit Requests'}</span>
        <hr className="rule" />
        {!isMemberOnly && (
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
        )}
        {loading ? (
          <div className="ops-feedback">Loading requests...</div>
        ) : (
          <DataTable
            columns={columns}
            data={requests}
            getRowKey={row => row.id}
            emptyMessage={isMemberOnly ? 'No exit requests on file.' : 'No exit requests found for this member.'}
          />
        )}
      </section>
    </div>
  )
}
