import { useState, useEffect, type FormEvent } from 'react'
import { Modal } from './Modal'
import { DatePicker } from './DatePicker'
import { Select } from './Select'
import { localISODate } from '../utils/date'
import { getCategories, getWelfarePolicy } from '../services/contributionService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import type {
  RecordContributionRequest,
  ContributionCategoryResponse,
  ContributionWelfarePolicyResponse,
  PaymentMode,
} from '../types/contributions'

interface RecordContributionModalProps {
  open: boolean
  onClose: () => void
  members: { id: string; name: string }[]
  onSubmit: (payload: RecordContributionRequest) => Promise<void>
  isSubmitting: boolean
}

const paymentModeOptions = [
  { value: 'MPESA', label: 'M-Pesa' },
  { value: 'BANK', label: 'Bank Transfer' },
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

  const [amountError, setAmountError] = useState('')

  const [categories, setCategories] = useState<ContributionCategoryResponse[]>([])
  const [categoriesLoading, setCategoriesLoading] = useState(false)
  const [categoriesError, setCategoriesError] = useState('')
  const [welfarePolicy, setWelfarePolicy] = useState<ContributionWelfarePolicyResponse>({ enabled: false, fixedAmount: 0 })

  useEffect(() => {
    if (!open) return
    let cancelled = false

    async function loadCategories() {
      setCategoriesLoading(true)
      setCategoriesError('')
      try {
        const [data, policy] = await Promise.all([
          getCategories(true, request),
          getWelfarePolicy(request).catch(() => ({ enabled: false, fixedAmount: 0 })),
        ])
        if (cancelled) return

        setCategories(data)
        setWelfarePolicy(policy)
        if (data.length === 0) {
          setCategoriesError('No active contribution categories are configured.')
          setCategoryId('')
          return
        }

        setCategoryId(prev => prev || data[0].id)
      } catch {
        if (!cancelled) {
          setCategories([])
          setCategoryId('')
          setCategoriesError('Unable to load contribution categories.')
        }
      } finally {
        if (!cancelled) setCategoriesLoading(false)
      }
    }

    void loadCategories()
    return () => { cancelled = true }
  }, [open, request])

  const memberOptions = members.map(m => ({ value: m.id, label: m.name }))
  const categoryOptions = categories.map(c => ({ value: c.id, label: c.name }))
  const selectedCategory = categories.find(c => c.id === categoryId)
  const welfareApplies = Boolean(selectedCategory?.welfareEligible) && welfarePolicy.enabled
  const amountValue = Number(amount)
  const welfareAmount = welfareApplies ? welfarePolicy.fixedAmount : 0
  const netAmount = Number.isNaN(amountValue) ? 0 : amountValue - welfareAmount

  function fmtAmount(value: number): string {
    return value.toLocaleString('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }

  function validateAmount(val: string): string {
    const n = Number(val)
    if (!val || Number.isNaN(n) || n <= 0) return 'Enter a positive amount.'
    if (welfareApplies && n < welfarePolicy.fixedAmount) {
      return `Amount must be at least KES ${fmtAmount(welfarePolicy.fixedAmount)} for welfare-eligible contributions.`
    }
    return ''
  }

  function blurAmount() {
    setAmountError(validateAmount(amount))
  }

  function reset() {
    setMemberId('')
    setCategoryId(categories[0]?.id ?? '')
    setAmount('15000')
    setDate(localISODate())
    setPaymentMode('MPESA')
    setReferenceNumber('')
    setNotes('')
    setError('')
    setAmountError('')
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')

    const amtErr = validateAmount(amount)
    if (amtErr) { setAmountError(amtErr); return }

    if (categories.length === 0 || !categoryId) {
      setError('No selectable contribution category is available.')
      return
    }

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
          <button
            className="btn btn--primary"
            type="submit"
            form="record-contrib-form"
            disabled={isSubmitting || categoriesLoading || categories.length === 0}
          >
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
          <label className="field-label field-label--required">Member</label>
          <Select options={memberOptions} value={memberId} onChange={setMemberId} placeholder="Select member" required searchable />
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label field-label--required">Category</label>
            <Select options={categoryOptions} value={categoryId} onChange={setCategoryId} required />
            {categoriesLoading && <span className="field-hint">Loading categories...</span>}
            {!categoriesLoading && categoriesError && <span className="field-hint">{categoriesError}</span>}
          </div>
          <div className="field">
            <label className="field-label field-label--required">Amount (KES)</label>
            <input
              className={`field-input${amountError ? ' field-input--error' : ''}`}
              type="number"
              min={1}
              required
              value={amount}
              onChange={e => { setAmount(e.target.value); if (amountError) setAmountError('') }}
              onBlur={blurAmount}
              disabled={isSubmitting}
              aria-invalid={!!amountError}
              aria-describedby={amountError ? 'contrib-amount-error' : undefined}
            />
            {amountError && <span id="contrib-amount-error" className="field-error">{amountError}</span>}
            {welfareApplies && !amountError && (
              <span className="field-hint">
                Split preview: Gross KES {fmtAmount(amountValue || 0)} | Welfare KES {fmtAmount(welfareAmount)} | Net KES {fmtAmount(Math.max(netAmount, 0))}
              </span>
            )}
            {!welfarePolicy.enabled && selectedCategory?.welfareEligible && (
              <span className="field-hint">
                Welfare split is currently disabled until a positive fixed amount is configured in Settings.
              </span>
            )}
          </div>
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label field-label--required">Payment Mode</label>
            <Select options={paymentModeOptions} value={paymentMode} onChange={v => setPaymentMode(v as PaymentMode)} />
          </div>
          <div className="field">
            <label className="field-label field-label--required">Date</label>
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
