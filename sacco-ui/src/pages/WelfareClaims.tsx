import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { ApiError } from '../services/apiClient'
import {
  createWelfareBeneficiary,
  createWelfareClaim,
  getWelfareBeneficiariesForMember,
  getWelfareFundSummary,
  processWelfareClaim,
  reviewWelfareClaim,
} from '../services/policyWorkflowService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useToast } from '../hooks/useToast'
import type {
  WelfareBeneficiaryResponse,
  WelfareClaimResponse,
  WelfareClaimStatus,
  WelfareFundSummaryResponse,
} from '../types/policyWorkflows'
import './Operations.css'

interface BeneficiaryFormState {
  memberId: string
  fullName: string
  relationship: string
  dateOfBirth: string
  phone: string
  notes: string
}

interface ClaimFormState {
  memberId: string
  beneficiaryId: string
  eventCode: string
  eventDate: string
  requestedAmount: string
}

interface ReviewFormState {
  claimId: string
  status: WelfareClaimStatus
  approvedAmount: string
  decisionSource: string
  meetingReference: string
  decisionDate: string
  decisionNotes: string
}

const EMPTY_BENEFICIARY_FORM: BeneficiaryFormState = {
  memberId: '',
  fullName: '',
  relationship: '',
  dateOfBirth: '',
  phone: '',
  notes: '',
}

const EMPTY_CLAIM_FORM: ClaimFormState = {
  memberId: '',
  beneficiaryId: '',
  eventCode: '',
  eventDate: '',
  requestedAmount: '',
}

const EMPTY_REVIEW_FORM: ReviewFormState = {
  claimId: '',
  status: 'UNDER_REVIEW',
  approvedAmount: '',
  decisionSource: '',
  meetingReference: '',
  decisionDate: '',
  decisionNotes: '',
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function formatMoney(value: number) {
  return value.toLocaleString('en-KE')
}

export function WelfareClaims() {
  const { request } = useAuthenticatedApi()
  const toast = useToast()

  const [beneficiaryLookupMemberId, setBeneficiaryLookupMemberId] = useState('')
  const [beneficiaries, setBeneficiaries] = useState<WelfareBeneficiaryResponse[]>([])
  const [claims, setClaims] = useState<WelfareClaimResponse[]>([])
  const [fundSummary, setFundSummary] = useState<WelfareFundSummaryResponse | null>(null)

  const [beneficiaryForm, setBeneficiaryForm] = useState<BeneficiaryFormState>(EMPTY_BENEFICIARY_FORM)
  const [claimForm, setClaimForm] = useState<ClaimFormState>(EMPTY_CLAIM_FORM)
  const [reviewForm, setReviewForm] = useState<ReviewFormState>(EMPTY_REVIEW_FORM)
  const [processClaimId, setProcessClaimId] = useState('')

  const [loadingSummary, setLoadingSummary] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const beneficiaryColumns = useMemo<ColumnDef<WelfareBeneficiaryResponse>[]>(() => [
    { key: 'name', header: 'Name', render: row => row.fullName },
    { key: 'relationship', header: 'Relationship', render: row => row.relationship },
    { key: 'member', header: 'Member ID', render: row => row.memberId },
    { key: 'active', header: 'Status', render: row => row.active ? 'Active' : 'Inactive' },
  ], [])

  const claimColumns = useMemo<ColumnDef<WelfareClaimResponse>[]>(() => [
    { key: 'id', header: 'Claim', render: row => row.id },
    { key: 'member', header: 'Member', render: row => row.memberId },
    { key: 'event', header: 'Event', render: row => row.eventCode },
    { key: 'amount', header: 'Requested', className: 'ledger-table-amount', render: row => `KES ${formatMoney(row.requestedAmount)}` },
    { key: 'status', header: 'Status', render: row => row.status },
    { key: 'approved', header: 'Approved', className: 'ledger-table-amount', render: row => row.approvedAmount == null ? '-' : `KES ${formatMoney(row.approvedAmount)}` },
  ], [])

  useEffect(() => {
    let cancelled = false
    async function loadSummary() {
      setLoadingSummary(true)
      try {
        const data = await getWelfareFundSummary(request)
        if (!cancelled) setFundSummary(data)
      } catch (error) {
        if (!cancelled) {
          toast.error('Unable to load welfare fund summary', toErrorMessage(error, 'Unable to load welfare fund summary.'))
        }
      } finally {
        if (!cancelled) setLoadingSummary(false)
      }
    }

    void loadSummary()
    return () => { cancelled = true }
  }, [request, toast])

  async function refreshSummary() {
    const data = await getWelfareFundSummary(request)
    setFundSummary(data)
  }

  async function handleLoadBeneficiaries(event: FormEvent) {
    event.preventDefault()
    if (!beneficiaryLookupMemberId.trim()) return
    setSubmitting(true)
    try {
      const rows = await getWelfareBeneficiariesForMember(beneficiaryLookupMemberId.trim(), request)
      setBeneficiaries(rows)
      toast.success('Beneficiaries loaded', `${rows.length} found.`)
    } catch (error) {
      toast.error('Unable to load beneficiaries', toErrorMessage(error, 'Unable to load beneficiaries.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCreateBeneficiary(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    try {
      const created = await createWelfareBeneficiary({
        memberId: beneficiaryForm.memberId.trim(),
        fullName: beneficiaryForm.fullName.trim(),
        relationship: beneficiaryForm.relationship.trim(),
        dateOfBirth: beneficiaryForm.dateOfBirth || undefined,
        phone: beneficiaryForm.phone.trim() || undefined,
        notes: beneficiaryForm.notes.trim() || undefined,
        active: true,
      }, request)
      if (beneficiaryLookupMemberId.trim() === created.memberId) {
        setBeneficiaries(prev => [created, ...prev])
      }
      setBeneficiaryForm(EMPTY_BENEFICIARY_FORM)
      toast.success('Welfare beneficiary created')
    } catch (error) {
      toast.error('Unable to create beneficiary', toErrorMessage(error, 'Unable to create beneficiary.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCreateClaim(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    try {
      const created = await createWelfareClaim({
        memberId: claimForm.memberId.trim(),
        beneficiaryId: claimForm.beneficiaryId.trim() || undefined,
        eventCode: claimForm.eventCode.trim(),
        eventDate: claimForm.eventDate,
        requestedAmount: Number(claimForm.requestedAmount),
      }, request)
      setClaims(prev => [created, ...prev])
      setClaimForm(EMPTY_CLAIM_FORM)
      await refreshSummary()
      toast.success('Welfare claim submitted')
    } catch (error) {
      toast.error('Unable to submit claim', toErrorMessage(error, 'Unable to submit claim.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleReviewClaim(event: FormEvent) {
    event.preventDefault()
    if (!reviewForm.claimId.trim()) return
    setSubmitting(true)
    try {
      const updated = await reviewWelfareClaim(reviewForm.claimId.trim(), {
        status: reviewForm.status,
        approvedAmount: reviewForm.approvedAmount ? Number(reviewForm.approvedAmount) : undefined,
        decisionSource: reviewForm.decisionSource.trim() || undefined,
        meetingReference: reviewForm.meetingReference.trim() || undefined,
        decisionDate: reviewForm.decisionDate || undefined,
        decisionNotes: reviewForm.decisionNotes.trim() || undefined,
      }, request)
      setClaims(prev => {
        const existing = prev.find(item => item.id === updated.id)
        if (existing) return prev.map(item => (item.id === updated.id ? updated : item))
        return [updated, ...prev]
      })
      toast.success('Welfare claim review recorded')
    } catch (error) {
      toast.error('Unable to review claim', toErrorMessage(error, 'Unable to review claim.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleProcessClaim(event: FormEvent) {
    event.preventDefault()
    if (!processClaimId.trim()) return
    setSubmitting(true)
    try {
      const processed = await processWelfareClaim(processClaimId.trim(), request)
      setClaims(prev => {
        const existing = prev.find(item => item.id === processed.id)
        if (existing) return prev.map(item => (item.id === processed.id ? processed : item))
        return [processed, ...prev]
      })
      await refreshSummary()
      toast.success('Welfare claim processed')
    } catch (error) {
      toast.error('Unable to process claim', toErrorMessage(error, 'Unable to process claim.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Welfare Operations</h1>
          <p className="page-subtitle">Manage beneficiaries, claims, reviews, and fund position.</p>
        </div>
      </div>

      <hr className="rule rule--strong" />


      <section className="page-section">
        <span className="page-section-title">Fund Summary</span>
        <hr className="rule" />
        {loadingSummary ? (
          <div className="ops-feedback">Loading welfare summary...</div>
        ) : (
          <div className="settings-row-block">
            <div className="settings-row">
              <span className="settings-row-label">Total Contributions</span>
              <span className="settings-row-value">KES {formatMoney(fundSummary?.totalWelfareContributions ?? 0)}</span>
            </div>
            <div className="settings-row">
              <span className="settings-row-label">Total Payouts</span>
              <span className="settings-row-value">KES {formatMoney(fundSummary?.totalWelfarePayouts ?? 0)}</span>
            </div>
            <div className="settings-row">
              <span className="settings-row-label">Available Balance</span>
              <span className="settings-row-value">KES {formatMoney(fundSummary?.availableBalance ?? 0)}</span>
            </div>
            <div className="settings-row">
              <span className="settings-row-label">Pending Claims</span>
              <span className="settings-row-value">{String(fundSummary?.pendingClaims ?? 0)}</span>
            </div>
          </div>
        )}
      </section>

      <section className="page-section">
        <span className="page-section-title">Create Beneficiary</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleCreateBeneficiary(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Member ID</label>
              <input className="field-input" value={beneficiaryForm.memberId} onChange={event => setBeneficiaryForm(prev => ({ ...prev, memberId: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Full Name</label>
              <input className="field-input" value={beneficiaryForm.fullName} onChange={event => setBeneficiaryForm(prev => ({ ...prev, fullName: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Relationship</label>
              <input className="field-input" value={beneficiaryForm.relationship} onChange={event => setBeneficiaryForm(prev => ({ ...prev, relationship: event.target.value }))} required />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label">Date of Birth</label>
              <input className="field-input" type="date" value={beneficiaryForm.dateOfBirth} onChange={event => setBeneficiaryForm(prev => ({ ...prev, dateOfBirth: event.target.value }))} />
            </div>
            <div className="field">
              <label className="field-label">Phone</label>
              <input className="field-input" value={beneficiaryForm.phone} onChange={event => setBeneficiaryForm(prev => ({ ...prev, phone: event.target.value }))} />
            </div>
            <div className="field">
              <label className="field-label">Notes</label>
              <input className="field-input" value={beneficiaryForm.notes} onChange={event => setBeneficiaryForm(prev => ({ ...prev, notes: event.target.value }))} />
            </div>
          </div>
          <button type="submit" className="btn btn--primary" disabled={submitting}>Create Beneficiary</button>
        </form>
      </section>

      <section className="page-section">
        <span className="page-section-title">Beneficiaries</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleLoadBeneficiaries(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Member ID</label>
              <input className="field-input" value={beneficiaryLookupMemberId} onChange={event => setBeneficiaryLookupMemberId(event.target.value)} required />
            </div>
            <div className="field" style={{ alignSelf: 'end' }}>
              <button type="submit" className="btn btn--secondary" disabled={submitting}>Load</button>
            </div>
          </div>
        </form>
        <DataTable
          columns={beneficiaryColumns}
          data={beneficiaries}
          getRowKey={row => row.id}
          emptyMessage="No beneficiaries loaded."
        />
      </section>

      <section className="page-section">
        <span className="page-section-title">Create Claim</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleCreateClaim(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Member ID</label>
              <input className="field-input" value={claimForm.memberId} onChange={event => setClaimForm(prev => ({ ...prev, memberId: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label">Beneficiary ID</label>
              <input className="field-input" value={claimForm.beneficiaryId} onChange={event => setClaimForm(prev => ({ ...prev, beneficiaryId: event.target.value }))} />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Event Code</label>
              <input className="field-input" value={claimForm.eventCode} onChange={event => setClaimForm(prev => ({ ...prev, eventCode: event.target.value }))} required />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Event Date</label>
              <input className="field-input" type="date" value={claimForm.eventDate} onChange={event => setClaimForm(prev => ({ ...prev, eventDate: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Requested Amount (KES)</label>
              <input className="field-input" type="number" min={0.01} step="0.01" value={claimForm.requestedAmount} onChange={event => setClaimForm(prev => ({ ...prev, requestedAmount: event.target.value }))} required />
            </div>
          </div>
          <button type="submit" className="btn btn--primary" disabled={submitting}>Submit Claim</button>
        </form>
      </section>

      <section className="page-section">
        <span className="page-section-title">Review and Process Claims</span>
        <hr className="rule" />
        <form className="modal-form" onSubmit={event => void handleReviewClaim(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Claim ID</label>
              <input className="field-input" value={reviewForm.claimId} onChange={event => setReviewForm(prev => ({ ...prev, claimId: event.target.value }))} required />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Status</label>
              <select className="field-input" value={reviewForm.status} onChange={event => setReviewForm(prev => ({ ...prev, status: event.target.value as WelfareClaimStatus }))}>
                <option value="UNDER_REVIEW">Under Review</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </div>
            <div className="field">
              <label className="field-label">Approved Amount (KES)</label>
              <input className="field-input" type="number" min={0} step="0.01" value={reviewForm.approvedAmount} onChange={event => setReviewForm(prev => ({ ...prev, approvedAmount: event.target.value }))} />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label">Decision Source</label>
              <input className="field-input" value={reviewForm.decisionSource} onChange={event => setReviewForm(prev => ({ ...prev, decisionSource: event.target.value }))} />
            </div>
            <div className="field">
              <label className="field-label">Meeting Reference</label>
              <input className="field-input" value={reviewForm.meetingReference} onChange={event => setReviewForm(prev => ({ ...prev, meetingReference: event.target.value }))} />
            </div>
            <div className="field">
              <label className="field-label">Decision Date</label>
              <input className="field-input" type="date" value={reviewForm.decisionDate} onChange={event => setReviewForm(prev => ({ ...prev, decisionDate: event.target.value }))} />
            </div>
          </div>
          <div className="field">
            <label className="field-label">Decision Notes</label>
            <input className="field-input" value={reviewForm.decisionNotes} onChange={event => setReviewForm(prev => ({ ...prev, decisionNotes: event.target.value }))} />
          </div>
          <button type="submit" className="btn btn--secondary" disabled={submitting}>Save Review</button>
        </form>

        <form className="modal-form" onSubmit={event => void handleProcessClaim(event)}>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Claim ID to Process</label>
              <input className="field-input" value={processClaimId} onChange={event => setProcessClaimId(event.target.value)} required />
            </div>
            <div className="field" style={{ alignSelf: 'end' }}>
              <button type="submit" className="btn btn--primary" disabled={submitting}>Process Claim</button>
            </div>
          </div>
        </form>

        <DataTable
          columns={claimColumns}
          data={claims}
          getRowKey={row => row.id}
          emptyMessage="Claims you create/review/process in this session will appear here."
        />
      </section>
    </div>
  )
}
