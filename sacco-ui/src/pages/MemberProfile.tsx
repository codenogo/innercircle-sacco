import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { SkeletonRow, SkeletonStat } from '../components/Skeleton'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Breadcrumb } from '../components/Breadcrumb'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { useToast } from '../hooks/useToast'
import type { MemberResponse } from '../types/members'
import './Operations.css'

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
  const toast = useToast()
  const [busyAction, setBusyAction] = useState<'suspend' | 'reactivate' | null>(null)
  const [confirmAction, setConfirmAction] = useState<'suspend' | 'reactivate' | null>(null)

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
    try {
      const updated = await request<MemberResponse>(`/api/v1/members/${member.id}/suspend`, {
        method: 'PATCH',
      })
      setMember(updated)
      toast.success('Member suspended', 'Member suspended successfully.')
    } catch (suspendError) {
      toast.error('Unable to suspend member', toErrorMessage(suspendError, 'Unable to suspend member.'))
    } finally {
      setBusyAction(null)
    }
  }

  async function reactivateMember() {
    if (!canManageLifecycle) return
    if (!member) return

    setBusyAction('reactivate')
    try {
      const updated = await request<MemberResponse>(`/api/v1/members/${member.id}/reactivate`, {
        method: 'PATCH',
      })
      setMember(updated)
      toast.success('Member reactivated', 'Member reactivated successfully.')
    } catch (reactivateError) {
      toast.error('Unable to reactivate member', toErrorMessage(reactivateError, 'Unable to reactivate member.'))
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
      <Breadcrumb items={[
        { label: 'Members', to: '/members' },
        { label: `${member.firstName} ${member.lastName} (${member.memberNumber})` },
      ]} />
      <div className="page-header">
        <div>
          <h1 className="page-title">{member.firstName} {member.lastName}</h1>
        </div>
        <div className="ops-inline-actions">
          {canSuspend && (
            <button
              type="button"
              className="btn btn--secondary"
              disabled={busyAction !== null}
              onClick={() => setConfirmAction('suspend')}
            >
              {busyAction === 'suspend' ? 'Suspending...' : 'Suspend'}
            </button>
          )}
          {canReactivate && (
            <button
              type="button"
              className="btn btn--secondary"
              disabled={busyAction !== null}
              onClick={() => setConfirmAction('reactivate')}
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

      <ConfirmDialog
        open={confirmAction === 'suspend'}
        onClose={() => setConfirmAction(null)}
        onConfirm={() => { setConfirmAction(null); void suspendMember() }}
        title="Suspend Member"
        description={`Are you sure you want to suspend ${member.firstName} ${member.lastName}? They will lose access to SACCO services until reactivated.`}
        confirmLabel="Suspend Member"
        variant="danger"
        loading={busyAction === 'suspend'}
      />

      <ConfirmDialog
        open={confirmAction === 'reactivate'}
        onClose={() => setConfirmAction(null)}
        onConfirm={() => { setConfirmAction(null); void reactivateMember() }}
        title="Reactivate Member"
        description={`Reactivate ${member.firstName} ${member.lastName}? They will regain access to all SACCO services.`}
        confirmLabel="Reactivate"
        variant="info"
        loading={busyAction === 'reactivate'}
      />
    </div>
  )
}
