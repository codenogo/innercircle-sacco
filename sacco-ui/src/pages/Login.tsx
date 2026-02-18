import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { ApiError } from '../services/apiClient'
import { getDefaultAuthenticatedRoute, getRolesFromAccessToken } from '../utils/auth'

export function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      const session = await login(username.trim(), password)
      const fallback = getDefaultAuthenticatedRoute(getRolesFromAccessToken(session.accessToken))
      const from = (location.state as { from?: string } | null)?.from
      const next = from && from !== '/login' ? from : fallback
      navigate(next, { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Unable to sign in. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <form className="auth-form" onSubmit={handleSubmit}>
        <div>
          <h2 className="auth-heading">Sign In</h2>
          <p className="auth-subtext">Enter your credentials to access the ledger.</p>
        </div>

        <div className="field">
          <label className="field-label" htmlFor="username">Username</label>
          <input
            id="username"
            className="field-input"
            type="text"
            placeholder="Enter your username"
            value={username}
            onChange={e => setUsername(e.target.value)}
            required
            autoComplete="username"
            autoFocus
          />
        </div>

        <div className="field">
          <div className="field-label-row">
            <label className="field-label" htmlFor="password">Password</label>
            <Link to="/forgot-password" className="auth-inline-link">Forgot password?</Link>
          </div>
          <input
            id="password"
            className="field-input"
            type="password"
            placeholder="Enter your password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
        </div>
        {error && <p className="field-error" role="alert">{error}</p>}

        <button type="submit" className="auth-submit" disabled={submitting}>
          {submitting ? 'Signing In...' : 'Sign In'}
        </button>
      </form>

      <div className="auth-footer">
        <p className="auth-footer-text">
          Don't have an account? <Link to="/signup" className="auth-link">Sign Up</Link>
        </p>
      </div>
    </>
  )
}
