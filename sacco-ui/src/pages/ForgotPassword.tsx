import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Check } from 'lucide-react'
import { forgotPassword } from '../services/authService'
import { ApiError } from '../services/apiClient'

export function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      await forgotPassword(email.trim())
      setSent(true)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Unable to send reset link right now.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (sent) {
    return (
      <div className="auth-success auth-state-top">
        <div className="auth-success-icon">
          <Check size={20} strokeWidth={2} />
        </div>
        <h2 className="auth-success-heading">Check Your Email</h2>
        <p className="auth-success-text">
          If an account exists for <strong>{email}</strong>, we've sent a password reset link.
        </p>
        <div className="auth-footer">
          <p className="auth-footer-text">
            <Link to="/login" className="auth-link">Back to Sign In</Link>
          </p>
        </div>
      </div>
    )
  }

  return (
    <>
      <form className="auth-form" onSubmit={handleSubmit}>
        <div>
          <h2 className="auth-heading">Forgot Password</h2>
          <p className="auth-subtext">
            Enter your email address and we'll send you a link to reset your password.
          </p>
        </div>

        <div className="field">
          <label className="field-label" htmlFor="email">Email Address</label>
          <input
            id="email"
            className="field-input"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={e => {
              setEmail(e.target.value)
              if (error) setError('')
            }}
            required
            autoComplete="email"
            autoFocus
          />
        </div>
        {error && <p className="field-error" role="alert">{error}</p>}

        <button type="submit" className="auth-submit" disabled={submitting}>
          {submitting ? 'Sending...' : 'Send Reset Link'}
        </button>
      </form>

      <div className="auth-footer">
        <p className="auth-footer-text">
          Remember your password? <Link to="/login" className="auth-link">Sign In</Link>
        </p>
      </div>
    </>
  )
}
