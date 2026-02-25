import { useState, type FormEvent } from 'react'
import { PhoneNumberFormat, PhoneNumberUtil } from 'google-libphonenumber'
import { Modal } from './Modal'
import { DatePicker } from './DatePicker'
import { Select } from './Select'
import type { CreateMemberRequest } from '../types/members'
import { localISODate } from '../utils/date'

interface AddMemberModalProps {
  open: boolean
  onClose: () => void
  onSubmit: (payload: CreateMemberRequest) => Promise<void>
  isSubmitting: boolean
}

interface AddMemberFormState {
  firstName: string
  lastName: string
  email: string
  phoneCountry: string
  phone: string
  nationalId: string
  dateOfBirth: string
  joinDate: string
}

interface PhoneCountryOption {
  regionCode: string
  dialCode: string
  name: string
}

const phoneUtil = PhoneNumberUtil.getInstance()

const PHONE_COUNTRIES: PhoneCountryOption[] = [
  { regionCode: 'KE', dialCode: '+254', name: 'Kenya' },
  { regionCode: 'UG', dialCode: '+256', name: 'Uganda' },
  { regionCode: 'TZ', dialCode: '+255', name: 'Tanzania' },
  { regionCode: 'RW', dialCode: '+250', name: 'Rwanda' },
  { regionCode: 'SS', dialCode: '+211', name: 'South Sudan' },
  { regionCode: 'ET', dialCode: '+251', name: 'Ethiopia' },
  { regionCode: 'US', dialCode: '+1', name: 'United States' },
  { regionCode: 'GB', dialCode: '+44', name: 'United Kingdom' },
]

function countryFlag(regionCode: string): string {
  return regionCode
    .toUpperCase()
    .replace(/./g, char => String.fromCodePoint(127397 + char.charCodeAt(0)))
}

function normalizePhoneInput(value: string): string {
  return value.replace(/[^\d+]/g, '')
}

function toE164Phone(phone: string, regionCode: string): string | null {
  const normalized = normalizePhoneInput(phone)
  if (!normalized) return null

  try {
    const parsed = phoneUtil.parse(normalized, regionCode)
    if (!phoneUtil.isValidNumberForRegion(parsed, regionCode)) return null
    return phoneUtil.format(parsed, PhoneNumberFormat.E164)
  } catch {
    return null
  }
}

function generateMemberNumber(): string {
  const now = new Date()
  const yy = String(now.getFullYear()).slice(-2)
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const seq = String(Math.floor(Math.random() * 9000) + 1000)
  return `MBR-${yy}${mm}${seq}`
}

function defaultFormState(): AddMemberFormState {
  return {
    firstName: '',
    lastName: '',
    email: '',
    phoneCountry: 'KE',
    phone: '',
    nationalId: '',
    dateOfBirth: '',
    joinDate: localISODate(),
  }
}

type FieldErrors = Partial<Record<keyof AddMemberFormState, string>>

function validateEmail(email: string): string | undefined {
  if (!email) return undefined
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'Enter a valid email address.'
  return undefined
}

function validatePhone(phone: string, regionCode: string): string | undefined {
  if (!phone) return undefined
  const e164 = toE164Phone(phone, regionCode)
  if (!e164) return 'Enter a valid phone number for the selected country.'
  return undefined
}

function validateDateOfBirth(dob: string): string | undefined {
  if (!dob) return undefined
  if (dob > localISODate()) return 'Date of birth cannot be in the future.'
  return undefined
}

export function AddMemberModal({ open, onClose, onSubmit, isSubmitting }: AddMemberModalProps) {
  const [form, setForm] = useState<AddMemberFormState>(defaultFormState)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const phoneCountryOptions = PHONE_COUNTRIES.map(option => ({
    value: option.regionCode,
    label: `${countryFlag(option.regionCode)} ${option.name} (${option.dialCode})`,
  }))
  const selectedPhoneCountry = PHONE_COUNTRIES.find(option => option.regionCode === form.phoneCountry) ?? PHONE_COUNTRIES[0]

  function updateField<K extends keyof AddMemberFormState>(field: K, value: AddMemberFormState[K]) {
    setForm(prev => ({ ...prev, [field]: value }))
    if (fieldErrors[field]) setFieldErrors(prev => ({ ...prev, [field]: undefined }))
  }

  function blurField<K extends keyof AddMemberFormState>(field: K) {
    let err: string | undefined
    if (field === 'email') err = validateEmail(form.email)
    else if (field === 'phone') err = validatePhone(form.phone, form.phoneCountry)
    else if (field === 'dateOfBirth') err = validateDateOfBirth(form.dateOfBirth)
    if (err) setFieldErrors(prev => ({ ...prev, [field]: err }))
  }

  function reset() {
    setForm(defaultFormState())
    setError('')
    setFieldErrors({})
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')

    const errors: FieldErrors = {}
    const emailErr = validateEmail(form.email)
    if (emailErr) errors.email = emailErr
    const phoneErr = validatePhone(form.phone, form.phoneCountry)
    if (phoneErr) errors.phone = phoneErr
    const dobErr = validateDateOfBirth(form.dateOfBirth)
    if (dobErr) errors.dateOfBirth = dobErr

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    const e164Phone = toE164Phone(form.phone, form.phoneCountry)!

    try {
      await onSubmit({
        memberNumber: generateMemberNumber(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phone: e164Phone,
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
            <label className="field-label field-label--required" htmlFor="first-name">First Name</label>
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
            <label className="field-label field-label--required" htmlFor="last-name">Last Name</label>
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

        <div className="field">
          <label className="field-label field-label--required" htmlFor="national-id">National ID</label>
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

        <div className="field">
          <label className="field-label field-label--required" htmlFor="email">Email</label>
          <input
            id="email"
            className={`field-input${fieldErrors.email ? ' field-input--error' : ''}`}
            type="email"
            required
            disabled={isSubmitting}
            value={form.email}
            onChange={event => updateField('email', event.target.value)}
            onBlur={() => blurField('email')}
            placeholder="jane@example.com"
            aria-invalid={!!fieldErrors.email}
            aria-describedby={fieldErrors.email ? 'email-error' : undefined}
          />
          {fieldErrors.email && <span id="email-error" className="field-error">{fieldErrors.email}</span>}
        </div>

        <div className="field">
          <label className="field-label field-label--required" htmlFor="phone-local">Phone</label>
          <div className="phone-input-wrap">
            <div className="phone-country-picker">
              <Select
                value={form.phoneCountry}
                onChange={value => updateField('phoneCountry', value)}
                options={phoneCountryOptions}
                required
                searchable
              />
            </div>
            <input
              id="phone-local"
              className={`field-input phone-local-input${fieldErrors.phone ? ' field-input--error' : ''}`}
              type="tel"
              required
              disabled={isSubmitting}
              value={form.phone}
              onChange={event => updateField('phone', event.target.value)}
              onBlur={() => blurField('phone')}
              placeholder="712345678"
              autoComplete="tel-national"
              aria-invalid={!!fieldErrors.phone}
              aria-describedby={fieldErrors.phone ? 'phone-error' : undefined}
            />
          </div>
          {fieldErrors.phone
            ? <span id="phone-error" className="field-error">{fieldErrors.phone}</span>
            : <span className="field-hint">{selectedPhoneCountry.dialCode} prefix selected. Enter the number in national format.</span>}
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label field-label--required">Date of Birth</label>
            <DatePicker value={form.dateOfBirth} onChange={value => updateField('dateOfBirth', value)} required />
            {fieldErrors.dateOfBirth && <span className="field-error">{fieldErrors.dateOfBirth}</span>}
          </div>
          <div className="field">
            <label className="field-label field-label--required">Join Date</label>
            <DatePicker value={form.joinDate} onChange={value => updateField('joinDate', value)} required />
          </div>
        </div>
      </form>
    </Modal>
  )
}
