import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { ApiError } from '../services/apiClient'
import { getMe } from '../services/authService'
import type { MeResponse } from '../types/auth'
import './Operations.css'

function formatDateTime(value: string | undefined) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('en-KE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function roleBadgeClass(role: string) {
  if (role.includes('ADMIN')) return 'badge--active'
  if (role.includes('TREASURER') || role.includes('SECRETARY')) return 'badge--completed'
  if (role.includes('MEMBER')) return 'badge--pending'
  return 'badge--inactive'
}

export function MyProfile() {
  const navigate = useNavigate()
  const { session, refreshSession, logout } = useAuth()
  const [profile, setProfile] = useState<MeResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function loadProfile() {
      if (!session?.accessToken) {
        if (!cancelled) {
          setError('Not authenticated.')
          setLoading(false)
        }
        return
      }

      setLoading(true)
      setError('')

      try {
        const me = await getMe(session.accessToken)
        if (!cancelled) setProfile(me)
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          const refreshed = await refreshSession()
          if (!refreshed?.accessToken) {
            if (!cancelled) {
              setError('Session expired. Please sign in again.')
              setLoading(false)
            }
            return
          }

          try {
            const me = await getMe(refreshed.accessToken)
            if (!cancelled) setProfile(me)
          } catch (retryErr) {
            if (!cancelled) {
              setError(retryErr instanceof Error ? retryErr.message : 'Unable to load profile.')
            }
          }
        } else if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Unable to load profile.')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void loadProfile()

    return () => {
      cancelled = true
    }
  }, [refreshSession, session?.accessToken])

  const displayName = useMemo(() => {
    if (!profile) return '-'
    if (profile.member) return `${profile.member.firstName} ${profile.member.lastName}`
    return profile.username
  }, [profile])

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  if (loading) {
    return (
      <div className="ops-page">
        <div className="page-header">
          <div>
            <h1 className="page-title">My Profile</h1>
            <p className="page-subtitle">Authenticated user details from /api/v1/me</p>
          </div>
        </div>
        <hr className="rule rule--strong" />
        <div className="empty-state ops-empty">
          <h2 className="empty-state-heading">Loading profile...</h2>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="ops-page">
        <div className="page-header">
          <div>
            <h1 className="page-title">My Profile</h1>
            <p className="page-subtitle">Authenticated user details from /api/v1/me</p>
          </div>
        </div>
        <hr className="rule rule--strong" />
        <div className="empty-state ops-empty">
          <h2 className="empty-state-heading">Unable to load profile</h2>
          <p className="empty-state-text">{error}</p>
          <div className="ops-inline-actions">
            <button type="button" className="btn btn--secondary" onClick={() => window.location.reload()}>
              Retry
            </button>
            <button type="button" className="btn btn--primary" onClick={handleLogout}>
              Sign Out
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">My Profile</h1>
          <p className="page-subtitle">Authenticated user details from /api/v1/me</p>
        </div>
        <button type="button" className="btn btn--secondary" onClick={handleLogout}>Sign Out</button>
      </div>

      <hr className="rule rule--strong" />

      <div className="settings-group">
        <h2 className="settings-group-title">Identity</h2>
        <hr className="rule" />
        <div className="settings-row">
          <div><span className="settings-row-label">Name</span></div>
          <span className="settings-row-value">{displayName}</span>
        </div>
        <div className="settings-row">
          <div><span className="settings-row-label">Username</span></div>
          <span className="settings-row-value data">{profile?.username ?? '-'}</span>
        </div>
        <div className="settings-row">
          <div><span className="settings-row-label">Email</span></div>
          <span className="settings-row-value">{profile?.email ?? '-'}</span>
        </div>
        <div className="settings-row">
          <div><span className="settings-row-label">Roles</span></div>
          <div className="ops-inline-actions">
            {(profile?.roles ?? []).map(role => (
              <span key={role} className={`badge ${roleBadgeClass(role)}`}>{role}</span>
            ))}
          </div>
        </div>
        {profile?.member && (
          <div className="settings-row">
            <div><span className="settings-row-label">Linked Member Number</span></div>
            <span className="settings-row-value data">{profile.member.memberNumber}</span>
          </div>
        )}
      </div>

      <div className="settings-group profile-settings-group">
        <h2 className="settings-group-title">Security</h2>
        <hr className="rule" />
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Account Created</span>
            <span className="settings-row-desc">User account creation timestamp</span>
          </div>
          <span className="settings-row-value data">{formatDateTime(profile?.createdAt)}</span>
        </div>
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Profile Updated</span>
            <span className="settings-row-desc">Last account update timestamp</span>
          </div>
          <span className="settings-row-value data">{formatDateTime(profile?.updatedAt)}</span>
        </div>
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Token Type</span>
            <span className="settings-row-desc">Current authentication scheme</span>
          </div>
          <span className="settings-row-value data">{session?.tokenType ?? '-'}</span>
        </div>
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Access Token TTL</span>
            <span className="settings-row-desc">Configured token lifetime (seconds)</span>
          </div>
          <span className="settings-row-value data">{session?.expiresIn ?? '-'}</span>
        </div>
      </div>
    </div>
  )
}
