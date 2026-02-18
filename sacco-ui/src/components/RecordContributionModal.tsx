import { useState, useEffect, type FormEvent } from 'react'
import { Modal } from './Modal'
import { DatePicker } from './DatePicker'
import { Select } from './Select'
import { localISODate } from '../utils/date'
import { getCategories } from '../services/contributionService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import type { RecordContributionRequest, ContributionCategoryResponse, PaymentMode } from '../types/contributions'

interface RecordContributionModalProps {
  open: boolean
  onClose: () => void
  members: { id: string; name: string }[]
  onSubmit: (payload: RecordContributionRequest) => Promise<void>
  isSubmitting: boolean
}

const fallbackCategories: ContributionCategoryResponse[] = [
  { id: 'monthly', name: 'Monthly', description: '', isMandatory: true, active: true },
  { id: 'special', name: 'Special', description: '', isMandatory: false, active: true },
  { id: 'registration', name: 'Registration', description: '', isMandatory: false, active: true },
]

const paymentModeOptions = [
  { value: 'MPESA', label: 'M-Pesa' },
  { value: 'BANK_TRANSFER', label: 'Bank Transfer' },
  { value: 'CASH', label: 'Cash' },
  { value: 'CHECK', label: 'Cheque' },
]

export function RecordContributionModal({ open, onClose, members, onSubmit, isSubmitting }: RecordContributionModalProps) {
  const { request } = useAuthenticatedApi()

  const [memberId, setMemberId] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [amount, setAmount] = useState('15000')
  const [date, setDate] = useState(localISODate)
  const [paymentMode, setPaymentMode] = useState<PaymentMode>('MPESA')
  const [referenceNumber, setReferenceNumber] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState('')

  const [categories, setCategories] = useState<ContributionCategoryResponse[]>(fallbackCategories)

  useEffect(() => {
    if (!open) return
    let cancelled = false

    async function loadCategories() {
      try {
        const data = await getCategories(true, request)
        if (!cancelled && data.length > 0) {
          setCategories(data)
        }
      } catch {
        // Fall back to hardcoded categories
      }
    }

    void loadCategories()
    return () => { cancelled = true }
  }, [open, request])

  const memberOptions = members.map(m => ({ value: m.id, label: m.name }))
  const categoryOptions = categories.map(c => ({ value: c.id, label: c.name }))

  function reset() {
    setMemberId('')
    setCategoryId('')
    setAmount('15000')
    setDate(localISODate())
    setPaymentMode('MPESA')
    setReferenceNumber('')
    setNotes('')
    setError('')
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')

    const payload: RecordContributionRequest = {
      memberId,
      amount: Number(amount),
      categoryId,
      paymentMode,
      contributionMonth: date.slice(0, 7) + '-01',
      contributionDate: date,
      referenceNumber: referenceNumber.trim() || undefined,
      notes: notes.trim() || undefined,
    }

    try {
      await onSubmit(payload)
      reset()
    } catch (submitError) {
      if (submitError instanceof Error) {
        setError(submitError.message)
      } else {
        setError('Unable to record contribution.')
      }
    }
  }

  function handleClose() {
    if (isSubmitting) return
    reset()
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="Record Contribution"
      subtitle="Log a member payment"
      width="md"
      footer={
        <>
          <button className="btn btn--secondary" type="button" onClick={handleClose} disabled={isSubmitting}>Cancel</button>
          <button className="btn btn--primary" type="submit" form="record-contrib-form" disabled={isSubmitting}>
            {isSubmitting ? 'Recording...' : 'Record Contribution'}
          </button>
        </>
      }
    >
      <form id="record-contrib-form" className="modal-form" onSubmit={event => void handleSubmit(event)}>
        {error && (
          <div className="ops-feedback ops-feedback--error" role="alert">
            {error}
          </div>
        )}

        <div className="field">
          <label className="field-label">Member</label>
          <Select options={memberOptions} value={memberId} onChange={setMemberId} placeholder="Select member" required searchable />
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label">Category</label>
            <Select options={categoryOptions} value={categoryId} onChange={setCategoryId} required />
          </div>
          <div className="field">
            <label className="field-label">Amount (KES)</label>
            <input className="field-input" type="number" min={1} required value={amount} onChange={e => setAmount(e.target.value)} disabled={isSubmitting} />
          </div>
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label">Payment Mode</label>
            <Select options={paymentModeOptions} value={paymentMode} onChange={v => setPaymentMode(v as PaymentMode)} />
          </div>
          <div className="field">
            <label className="field-label">Date</label>
            <DatePicker value={date} onChange={setDate} required />
          </div>
        </div>

        <div className="field">
          <label className="field-label">Reference Number</label>
          <input
            className="field-input"
            type="text"
            maxLength={100}
            value={referenceNumber}
            onChange={e => setReferenceNumber(e.target.value)}
            placeholder="M-Pesa code, bank ref, etc."
            disabled={isSubmitting}
          />
        </div>

        <div className="field">
          <label className="field-label">Notes</label>
          <textarea className="field-input" value={notes} onChange={e => setNotes(e.target.value)} placeholder="Optional notes..." disabled={isSubmitting} />
        </div>
      </form>
    </Modal>
  )
}
