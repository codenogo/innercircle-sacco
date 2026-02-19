import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { SkeletonRow, SkeletonStat } from '../components/Skeleton'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import type { MemberResponse } from '../types/members'
import './Operations.css'

interface Feedback {
  type: 'success' | 'error'
  text: string
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function fmtCurrency(value: number | string): string {
  const parsed = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(parsed)) return '0'
  return parsed.toLocaleString('en-KE')
}

function fmtDate(value: string): string {
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('en-KE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function fmtDateTime(value: string): string {
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

function statusClass(status: MemberResponse['status']): string {
  if (status === 'ACTIVE') return 'badge--active'
  if (status === 'SUSPENDED') return 'badge--suspended'
  return 'badge--inactive'
}

export function MemberProfile() {
  const { id } = useParams<{ id: string }>()
  const { request } = useAuthenticatedApi()
  const { canAccess } = useAuthorization()
  const canManageLifecycle = canAccess(['ADMIN'])

  const [member, setMember] = useState<MemberResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [busyAction, setBusyAction] = useState<'suspend' | 'reactivate' | null>(null)

  const loadMember = useCallback(async () => {
    if (!id) {
      setMember(null)
      setError('Member ID is missing.')
      setLoading(false)
      return
    }

    setLoading(true)
    setError('')

    try {
      const data = await request<MemberResponse>(`/api/v1/members/${id}`)
      setMember(data)
    } catch (loadError) {
      setError(toErrorMessage(loadError, 'Unable to load member.'))
      setMember(null)
    } finally {
      setLoading(false)
    }
  }, [id, request])

  useEffect(() => {
    void loadMember()
  }, [loadMember])

  async function suspendMember() {
    if (!canManageLifecycle) return
    if (!member) return

    setBusyAction('suspend')
    setFeedback(null)
    try {
      const updated = await request<MemberResponse>(`/api/v1/members/${member.id}/suspend`, {
        method: 'PATCH',
      })
      setMember(updated)
      setFeedback({ type: 'success', text: 'Member suspended successfully.' })
    } catch (suspendError) {
      setFeedback({ type: 'error', text: toErrorMessage(suspendError, 'Unable to suspend member.') })
    } finally {
      setBusyAction(null)
    }
  }

  async function reactivateMember() {
    if (!canManageLifecycle) return
    if (!member) return

    setBusyAction('reactivate')
    setFeedback(null)
    try {
      const updated = await request<MemberResponse>(`/api/v1/members/${member.id}/reactivate`, {
        method: 'PATCH',
      })
      setMember(updated)
      setFeedback({ type: 'success', text: 'Member reactivated successfully.' })
    } catch (reactivateError) {
      setFeedback({ type: 'error', text: toErrorMessage(reactivateError, 'Unable to reactivate member.') })
    } finally {
      setBusyAction(null)
    }
  }

  if (loading) {
    return (
      <div className="ops-page">
        <div className="page-header">
          <div>
            <h1 className="page-title">Member Profile</h1>
            <p className="page-subtitle">Loading member details...</p>
          </div>
        </div>
        <hr className="rule rule--strong" />
        <div className="empty-state ops-empty">
          <SkeletonStat /><SkeletonRow cells={4} /><SkeletonRow cells={4} />
        </div>
      </div>
    )
  }

  if (!member) {
    return (
      <div className="ops-page">
        <div className="page-header">
          <div>
            <h1 className="page-title">Member Profile</h1>
            <p className="page-subtitle">Member detail and lifecycle actions</p>
          </div>
        </div>
        <hr className="rule rule--strong" />
        <div className="empty-state ops-empty">
          <h2 className="empty-state-heading">Member not found</h2>
          <p className="empty-state-text">{error || 'Select a member from the members list.'}</p>
          <p className="ops-note"><Link to="/members">Back to members</Link></p>
        </div>
      </div>
    )
  }

  const canSuspend = canManageLifecycle && member.status === 'ACTIVE'
  const canReactivate = canManageLifecycle && member.status === 'SUSPENDED'

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">{member.firstName} {member.lastName}</h1>
          <p className="page-subtitle">Member detail page ({member.memberNumber})</p>
        </div>
        <div className="ops-inline-actions">
          {canSuspend && (
            <button
              type="button"
              className="btn btn--secondary"
              disabled={busyAction !== null}
              onClick={() => void suspendMember()}
            >
              {busyAction === 'suspend' ? 'Suspending...' : 'Suspend'}
            </button>
          )}
          {canReactivate && (
            <button
              type="button"
              className="btn btn--secondary"
              disabled={busyAction !== null}
              onClick={() => void reactivateMember()}
            >
              {busyAction === 'reactivate' ? 'Reactivating...' : 'Reactivate'}
            </button>
          )}
          <button type="button" className="btn btn--secondary" onClick={() => void loadMember()} disabled={busyAction !== null}>
            Refresh
          </button>
        </div>
      </div>

      <hr className="rule rule--strong" />

      {feedback && (
        <div className={`ops-feedback ops-feedback--${feedback.type}`} role="status">
          {feedback.text}
        </div>
      )}

      <div className="ops-kpi-grid">
        <div className="ops-kpi">
          <span className="ops-kpi-label">Status</span>
          <span className="ops-kpi-value">
            <span className={`badge ${statusClass(member.status)}`}>{member.status}</span>
          </span>
        </div>
        <div className="ops-kpi">
          <span className="ops-kpi-label">Joined</span>
          <span className="ops-kpi-value">{fmtDate(member.joinDate)}</span>
        </div>
        <div className="ops-kpi">
          <span className="ops-kpi-label">Shares (KES)</span>
          <span className="ops-kpi-value">{fmtCurrency(member.shareBalance)}</span>
        </div>
      </div>

      <section className="page-section">
        <span className="page-section-title">Profile</span>
        <hr className="rule" />
        <div className="settings-group">
          <div className="settings-row">
            <div>
              <span className="settings-row-label">Member Number</span>
            </div>
            <span className="settings-row-value data">{member.memberNumber}</span>
          </div>
          <div className="settings-row">
            <div>
              <span className="settings-row-label">Email</span>
            </div>
            <span className="settings-row-value">{member.email}</span>
          </div>
          <div className="settings-row">
            <div>
              <span className="settings-row-label">Phone</span>
            </div>
            <span className="settings-row-value data">{member.phone}</span>
          </div>
          <div className="settings-row">
            <div>
              <span className="settings-row-label">National ID</span>
            </div>
            <span className="settings-row-value data">{member.nationalId}</span>
          </div>
          <div className="settings-row">
            <div>
              <span className="settings-row-label">Date of Birth</span>
            </div>
            <span className="settings-row-value data">{fmtDate(member.dateOfBirth)}</span>
          </div>
        </div>
      </section>

      <section className="page-section">
        <span className="page-section-title">Record</span>
        <hr className="rule" />
        <div className="settings-group">
          <div className="settings-row">
            <div>
              <span className="settings-row-label">Member ID</span>
            </div>
            <span className="settings-row-value data">{member.id}</span>
          </div>
          <div className="settings-row">
            <div>
              <span className="settings-row-label">Created</span>
            </div>
            <span className="settings-row-value data">{fmtDateTime(member.createdAt)}</span>
          </div>
          <div className="settings-row">
            <div>
              <span className="settings-row-label">Updated</span>
            </div>
            <span className="settings-row-value data">{fmtDateTime(member.updatedAt)}</span>
          </div>
        </div>
      </section>
    </div>
  )
}
