import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Check } from '@phosphor-icons/react'
import { validatePasswordConfirmation } from '../utils/auth'
import { resetPassword } from '../services/authService'
import { ApiError } from '../services/apiClient'

export function ResetPassword() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const validationError = validatePasswordConfirmation(password, confirmPassword)
    if (validationError) {
      setError(validationError)
      return
    }
    if (!token) {
      setError('This reset link is invalid or expired.')
      return
    }

    setSubmitting(true)
    setError('')

    try {
      await resetPassword(token, password)
      setDone(true)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Unable to reset password right now.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (!token) {
    return (
      <div className="auth-success auth-state-top">
        <h2 className="auth-heading auth-state-center">Invalid Link</h2>
        <p className="auth-subtext auth-state-center">
          This password reset link is invalid or has expired.
        </p>
        <div className="auth-footer">
          <p className="auth-footer-text">
            <Link to="/forgot-password" className="auth-link">Request a new link</Link>
          </p>
        </div>
      </div>
    )
  }

  if (done) {
    return (
      <div className="auth-success auth-state-top">
        <div className="auth-success-icon">
          <Check size={20} strokeWidth={2} />
        </div>
        <h2 className="auth-success-heading">Password Updated</h2>
        <p className="auth-success-text">
          Your password has been reset. You can now sign in with your new password.
        </p>
        <div className="auth-footer">
          <p className="auth-footer-text">
            <Link to="/login" className="auth-link">Sign In</Link>
          </p>
        </div>
      </div>
    )
  }

  return (
    <>
      <form className="auth-form" onSubmit={handleSubmit}>
        <div>
          <h2 className="auth-heading">Set New Password</h2>
          <p className="auth-subtext">Choose a new password for your account.</p>
        </div>

        <div className="field">
          <label className="field-label" htmlFor="password">New Password</label>
          <input
            id="password"
            className="field-input"
            type="password"
            placeholder="Minimum 8 characters"
            value={password}
            onChange={e => { setPassword(e.target.value); if (error) setError('') }}
            required
            minLength={8}
            autoComplete="new-password"
            autoFocus
          />
        </div>

        <div className="field">
          <label className="field-label" htmlFor="confirmPassword">Confirm Password</label>
          <input
            id="confirmPassword"
            className="field-input"
            type="password"
            placeholder="Re-enter your new password"
            value={confirmPassword}
            onChange={e => { setConfirmPassword(e.target.value); if (error) setError('') }}
            required
            minLength={8}
            autoComplete="new-password"
          />
        </div>
        {error && <p className="field-error" role="alert">{error}</p>}

        <button type="submit" className="auth-submit" disabled={submitting}>
          {submitting ? 'Resetting...' : 'Reset Password'}
        </button>
      </form>

      <div className="auth-footer">
        <p className="auth-footer-text">
          <Link to="/login" className="auth-link">Back to Sign In</Link>
        </p>
      </div>
    </>
  )
}
