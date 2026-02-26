import { useState } from 'react'
import { Modal } from './Modal'
import { DatePicker } from './DatePicker'
import { Select } from './Select'
import { localISODate } from '../utils/date'

interface NewJournalEntryModalProps {
  open: boolean
  onClose: () => void
  accounts: string[]
}

export function NewJournalEntryModal({ open, onClose, accounts }: NewJournalEntryModalProps) {
  const [date, setDate] = useState(localISODate)
  const [account, setAccount] = useState('')
  const [description, setDescription] = useState('')
  const [debit, setDebit] = useState('')
  const [credit, setCredit] = useState('')
  const [error, setError] = useState('')

  const accountOptions = accounts.map(a => ({ value: a, label: a }))

  function reset() {
    setDate(localISODate()); setAccount(''); setDescription(''); setDebit(''); setCredit(''); setError('')
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const d = Number(debit) || 0
    const c = Number(credit) || 0
    if (d > 0 && c > 0) { setError('Only one of Debit or Credit can be non-zero'); return }
    if (d === 0 && c === 0) { setError('Enter either a Debit or Credit amount'); return }
    setError('')
    // Manual journal entry creation is not yet supported by the backend.
    // Entries are created automatically by domain events (contributions, loans, payouts, etc.).
    console.log('new-journal-entry (manual entry not supported)', { date, account, description, debit: d, credit: c })
    reset()
    onClose()
  }

  function handleClose() { reset(); onClose() }

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="New Journal Entry"
      subtitle="Record a ledger entry"
      width="md"
      footer={
        <>
          <button className="btn btn--secondary" type="button" onClick={handleClose}>Cancel</button>
          <button className="btn btn--primary" type="submit" form="new-journal-form">Record Entry</button>
        </>
      }
    >
      <form id="new-journal-form" className="modal-form" onSubmit={handleSubmit}>
        <div className="field-row">
          <div className="field">
            <label className="field-label field-label--required">Date</label>
            <DatePicker value={date} onChange={setDate} required />
          </div>
          <div className="field">
            <label className="field-label field-label--required">Account</label>
            <Select options={accountOptions} value={account} onChange={setAccount} placeholder="Select account" required />
          </div>
        </div>

        <div className="field">
          <label className="field-label field-label--required">Description</label>
          <input className="field-input" type="text" required value={description} onChange={e => setDescription(e.target.value)} placeholder="e.g. Contribution — Jane Wanjiku" />
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label">Debit Amount</label>
            <input className="field-input" type="number" min={0} value={debit} onChange={e => { setDebit(e.target.value); setError('') }} placeholder="0" />
            <span className="field-hint">KES</span>
          </div>
          <div className="field">
            <label className="field-label">Credit Amount</label>
            <input className="field-input" type="number" min={0} value={credit} onChange={e => { setCredit(e.target.value); setError('') }} placeholder="0" />
            <span className="field-hint">KES</span>
          </div>
        </div>

        {error && <p className="field-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
