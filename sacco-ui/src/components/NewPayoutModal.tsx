import { useState, type FormEvent } from 'react'
import { Modal } from './Modal'
import { Select } from './Select'
import type { PayoutRequest, PayoutType } from '../types/payouts'

interface NewPayoutModalProps {
  open: boolean
  onClose: () => void
  onSubmit: (payload: PayoutRequest) => Promise<void>
  isSubmitting: boolean
  members: { id: string; name: string }[]
}

const PAYOUT_TYPES: { value: PayoutType; label: string }[] = [
  { value: 'MERRY_GO_ROUND', label: 'Merry-Go-Round' },
  { value: 'AD_HOC', label: 'Ad Hoc' },
  { value: 'DIVIDEND', label: 'Dividend' },
]

export function NewPayoutModal({ open, onClose, onSubmit, isSubmitting, members }: NewPayoutModalProps) {
  const [memberId, setMemberId] = useState('')
  const [payoutType, setPayoutType] = useState<PayoutType>('MERRY_GO_ROUND')
  const [amount, setAmount] = useState('')
  const [error, setError] = useState('')

  const memberOptions = members.map(m => ({ value: m.id, label: m.name }))

  function reset() {
    setMemberId('')
    setPayoutType('MERRY_GO_ROUND')
    setAmount('')
    setError('')
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')

    try {
      await onSubmit({
        memberId,
        amount: Number(amount),
        type: payoutType,
      })
      reset()
    } catch (submitError) {
      if (submitError instanceof Error) {
        setError(submitError.message)
      } else {
        setError('Unable to create payout.')
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
      title="New Payout"
      subtitle="Submit a disbursement request"
      width="md"
      footer={
        <>
          <button className="btn btn--secondary" type="button" onClick={handleClose} disabled={isSubmitting}>
            Cancel
          </button>
          <button className="btn btn--primary" type="submit" form="new-payout-form" disabled={isSubmitting}>
            {isSubmitting ? 'Submitting...' : 'Submit Payout'}
          </button>
        </>
      }
    >
      <form id="new-payout-form" className="modal-form" onSubmit={event => void handleSubmit(event)}>
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
            <label className="field-label">Payout Type</label>
            <Select options={PAYOUT_TYPES} value={payoutType} onChange={v => setPayoutType(v as PayoutType)} />
          </div>
          <div className="field">
            <label className="field-label">Amount</label>
            <input className="field-input" type="number" min={0} required disabled={isSubmitting} value={amount} onChange={e => setAmount(e.target.value)} placeholder="0" />
            <span className="field-hint">KES</span>
          </div>
        </div>

      </form>
    </Modal>
  )
}
