import { useState, type FormEvent } from 'react'
import { Modal } from './Modal'
import type { CreateMemberRequest } from '../types/members'
import { localISODate } from '../utils/date'

interface AddMemberModalProps {
  open: boolean
  onClose: () => void
  onSubmit: (payload: CreateMemberRequest) => Promise<void>
  isSubmitting: boolean
}

interface AddMemberFormState {
  memberNumber: string
  firstName: string
  lastName: string
  email: string
  phone: string
  nationalId: string
  dateOfBirth: string
  joinDate: string
}

function defaultFormState(): AddMemberFormState {
  return {
    memberNumber: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    nationalId: '',
    dateOfBirth: '',
    joinDate: localISODate(),
  }
}

export function AddMemberModal({ open, onClose, onSubmit, isSubmitting }: AddMemberModalProps) {
  const [form, setForm] = useState<AddMemberFormState>(defaultFormState)
  const [error, setError] = useState('')

  function updateField<K extends keyof AddMemberFormState>(field: K, value: AddMemberFormState[K]) {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  function reset() {
    setForm(defaultFormState())
    setError('')
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')

    try {
      await onSubmit({
        memberNumber: form.memberNumber.trim(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phone: form.phone.replace(/\s+/g, ''),
        nationalId: form.nationalId.trim(),
        dateOfBirth: form.dateOfBirth,
        joinDate: form.joinDate,
      })
      reset()
      onClose()
    } catch (submitError) {
      if (submitError instanceof Error) {
        setError(submitError.message)
      } else {
        setError('Unable to create member.')
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
      title="Add Member"
      subtitle="Register a new SACCO member"
      width="md"
      footer={
        <>
          <button className="btn btn--secondary" type="button" onClick={handleClose} disabled={isSubmitting}>
            Cancel
          </button>
          <button className="btn btn--primary" type="submit" form="add-member-form" disabled={isSubmitting}>
            {isSubmitting ? 'Adding...' : 'Add Member'}
          </button>
        </>
      }
    >
      <form id="add-member-form" className="modal-form" onSubmit={event => void handleSubmit(event)}>
        {error && (
          <div className="ops-feedback ops-feedback--error" role="alert">
            {error}
          </div>
        )}

        <div className="field-row">
          <div className="field">
            <label className="field-label" htmlFor="member-number">Member Number</label>
            <input
              id="member-number"
              className="field-input"
              type="text"
              maxLength={50}
              required
              disabled={isSubmitting}
              value={form.memberNumber}
              onChange={event => updateField('memberNumber', event.target.value)}
              placeholder="MBR-0001"
            />
          </div>
          <div className="field">
            <label className="field-label" htmlFor="national-id">National ID</label>
            <input
              id="national-id"
              className="field-input"
              type="text"
              maxLength={50}
              required
              disabled={isSubmitting}
              value={form.nationalId}
              onChange={event => updateField('nationalId', event.target.value)}
              placeholder="ID number"
            />
          </div>
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label" htmlFor="first-name">First Name</label>
            <input
              id="first-name"
              className="field-input"
              type="text"
              required
              disabled={isSubmitting}
              value={form.firstName}
              onChange={event => updateField('firstName', event.target.value)}
              placeholder="Jane"
            />
          </div>
          <div className="field">
            <label className="field-label" htmlFor="last-name">Last Name</label>
            <input
              id="last-name"
              className="field-input"
              type="text"
              required
              disabled={isSubmitting}
              value={form.lastName}
              onChange={event => updateField('lastName', event.target.value)}
              placeholder="Wanjiku"
            />
          </div>
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label" htmlFor="email">Email</label>
            <input
              id="email"
              className="field-input"
              type="email"
              required
              disabled={isSubmitting}
              value={form.email}
              onChange={event => updateField('email', event.target.value)}
              placeholder="jane@example.com"
            />
          </div>
          <div className="field">
            <label className="field-label" htmlFor="phone">Phone</label>
            <input
              id="phone"
              className="field-input"
              type="tel"
              pattern="^\\+?[0-9]{10,20}$"
              required
              disabled={isSubmitting}
              value={form.phone}
              onChange={event => updateField('phone', event.target.value)}
              placeholder="254712345678"
            />
            <span className="field-hint">Digits only, optional leading +</span>
          </div>
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label" htmlFor="date-of-birth">Date of Birth</label>
            <input
              id="date-of-birth"
              className="field-input"
              type="date"
              max={localISODate()}
              required
              disabled={isSubmitting}
              value={form.dateOfBirth}
              onChange={event => updateField('dateOfBirth', event.target.value)}
            />
          </div>
          <div className="field">
            <label className="field-label" htmlFor="join-date">Join Date</label>
            <input
              id="join-date"
              className="field-input"
              type="date"
              required
              disabled={isSubmitting}
              value={form.joinDate}
              onChange={event => updateField('joinDate', event.target.value)}
            />
          </div>
        </div>
      </form>
    </Modal>
  )
}
