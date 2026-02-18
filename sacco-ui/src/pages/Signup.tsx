import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { validatePasswordConfirmation } from '../utils/auth'

export function Signup() {
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  })
  const [error, setError] = useState('')

  function update(field: string) {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
      setForm(prev => ({ ...prev, [field]: e.target.value }))
      if (error) setError('')
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const validationError = validatePasswordConfirmation(form.password, form.confirmPassword)
    if (validationError) {
      setError(validationError)
      return
    }
    setError('Self-service signup is not enabled. Please contact an administrator to create your account.')
  }

  return (
    <>
      <form className="auth-form" onSubmit={handleSubmit}>
        <div>
          <h2 className="auth-heading">Create Account</h2>
          <p className="auth-subtext">Register to join the SACCO.</p>
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label" htmlFor="firstName">First Name</label>
            <input
              id="firstName"
              className="field-input"
              type="text"
              placeholder="Jane"
              value={form.firstName}
              onChange={update('firstName')}
              required
              autoFocus
            />
          </div>
          <div className="field">
            <label className="field-label" htmlFor="lastName">Last Name</label>
            <input
              id="lastName"
              className="field-input"
              type="text"
              placeholder="Wanjiku"
              value={form.lastName}
              onChange={update('lastName')}
              required
            />
          </div>
        </div>

        <div className="field">
          <label className="field-label" htmlFor="email">Email Address</label>
          <input
            id="email"
            className="field-input"
            type="email"
            placeholder="jane@example.com"
            value={form.email}
            onChange={update('email')}
            required
            autoComplete="email"
          />
        </div>

        <div className="field">
          <label className="field-label" htmlFor="phone">Phone Number</label>
          <input
            id="phone"
            className="field-input"
            type="tel"
            placeholder="0712 345 678"
            value={form.phone}
            onChange={update('phone')}
            required
          />
          <span className="field-hint">Used for M-Pesa and notifications</span>
        </div>

        <div className="field">
          <label className="field-label" htmlFor="password">Password</label>
          <input
            id="password"
            className="field-input"
            type="password"
            placeholder="Minimum 8 characters"
            value={form.password}
            onChange={update('password')}
            required
            minLength={8}
            autoComplete="new-password"
          />
        </div>

        <div className="field">
          <label className="field-label" htmlFor="confirmPassword">Confirm Password</label>
          <input
            id="confirmPassword"
            className="field-input"
            type="password"
            placeholder="Re-enter your password"
            value={form.confirmPassword}
            onChange={update('confirmPassword')}
            required
            minLength={8}
            autoComplete="new-password"
          />
        </div>
        {error && <p className="field-error" role="alert">{error}</p>}

        <button type="submit" className="auth-submit">Create Account</button>
      </form>

      <div className="auth-footer">
        <p className="auth-footer-text">
          Already have an account? <Link to="/login" className="auth-link">Sign In</Link>
        </p>
      </div>
    </>
  )
}
