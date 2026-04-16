import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { MagnifyingGlass, UserPlus } from '@phosphor-icons/react'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { ActionMenu, type ActionMenuItem } from '../components/ActionMenu'
import { Spinner } from '../components/Spinner'
import { Modal } from '../components/Modal'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useToast } from '../hooks/useToast'
import type { CursorPage, UserResponse } from '../types/users'
import './Operations.css'

type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED'
type StatusFilter = 'ALL' | UserStatus

interface CreateUserState {
  username: string
  email: string
  memberId: string
  roleNames: string[]
  sendPasswordResetEmail: boolean
}

const PAGE_SIZE = 20
const ROLE_OPTIONS = [
  'ADMIN',
  'TREASURER',
  'VICE_TREASURER',
  'SECRETARY',
  'CHAIRPERSON',
  'VICE_CHAIRPERSON',
  'MEMBER',
]

function deriveStatus(user: UserResponse): UserStatus {
  if (!user.enabled) return 'INACTIVE'
  if (!user.accountNonLocked) return 'LOCKED'
  return 'ACTIVE'
}

function statusClass(status: UserStatus): string {
  if (status === 'ACTIVE') return 'badge--active'
  if (status === 'LOCKED') return 'badge--defaulted'
  return 'badge--inactive'
}

function roleBadgeClass(role: string): string {
  if (role === 'ADMIN') return 'badge--active'
  if (
    role === 'TREASURER'
    || role === 'VICE_TREASURER'
    || role === 'SECRETARY'
    || role === 'CHAIRPERSON'
    || role === 'VICE_CHAIRPERSON'
  ) return 'badge--completed'
  return 'badge--pending'
}

function formatDateTime(value: string): string {
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

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function defaultCreateUserState(): CreateUserState {
  return {
    username: '',
    email: '',
    memberId: '',
    roleNames: ['MEMBER'],
    sendPasswordResetEmail: true,
  }
}

export function UsersAdmin() {
  const { request } = useAuthenticatedApi()
  const toast = useToast()

  const [users, setUsers] = useState<UserResponse[]>([])
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [busyAction, setBusyAction] = useState<string | null>(null)

  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createUserState, setCreateUserState] = useState<CreateUserState>(defaultCreateUserState)

  const [editingRolesUser, setEditingRolesUser] = useState<UserResponse | null>(null)
  const [editingRoleNames, setEditingRoleNames] = useState<string[]>([])
  const [detailsUserId, setDetailsUserId] = useState<string | null>(null)
  const [detailsUser, setDetailsUser] = useState<UserResponse | null>(null)
  const [detailsLoading, setDetailsLoading] = useState(false)
  const [detailsError, setDetailsError] = useState('')

  const loadRequestId = useRef(0)

  const loadUsers = useCallback(async (
    opts?: { cursor?: string | null; append?: boolean; query?: string },
  ) => {
    const append = Boolean(opts?.append)
    const cursor = opts?.cursor
    const query = opts?.query ?? searchQuery
    const requestId = ++loadRequestId.current

    if (append) setLoadingMore(true)
    else setLoading(true)

    try {
      const base = query
        ? `/api/v1/users/search?q=${encodeURIComponent(query)}&size=${PAGE_SIZE}`
        : `/api/v1/users?size=${PAGE_SIZE}`
      const path = cursor ? `${base}&cursor=${encodeURIComponent(cursor)}` : base
      const page = await request<CursorPage<UserResponse>>(path)

      if (loadRequestId.current !== requestId) return

      setUsers(prev => {
        if (!append) return page.items
        const merged = new Map<string, UserResponse>()
        prev.forEach(user => merged.set(user.id, user))
        page.items.forEach(user => merged.set(user.id, user))
        return Array.from(merged.values())
      })
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
    } catch (error) {
      if (loadRequestId.current !== requestId) return
      toast.error('Unable to load users', toErrorMessage(error, 'Unable to load users.'))
    } finally {
      if (loadRequestId.current === requestId) {
        setLoading(false)
        setLoadingMore(false)
      }
    }
  }, [request, searchQuery, toast])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setSearchQuery(searchInput.trim())
    }, 300)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    void loadUsers({ append: false, cursor: null, query: searchQuery })
  }, [loadUsers, searchQuery])

  const filteredUsers = useMemo(() => {
    if (statusFilter === 'ALL') return users
    return users.filter(user => deriveStatus(user) === statusFilter)
  }, [statusFilter, users])

  const counts = useMemo(() => {
    const active = users.filter(user => deriveStatus(user) === 'ACTIVE').length
    const inactive = users.filter(user => deriveStatus(user) === 'INACTIVE').length
    const locked = users.filter(user => deriveStatus(user) === 'LOCKED').length
    return { active, inactive, locked, total: users.length }
  }, [users])

  function setRoleSelection(role: string, checked: boolean, source: string[], setter: (roles: string[]) => void) {
    if (checked) setter(Array.from(new Set([...source, role])))
    else setter(source.filter(item => item !== role))
  }

  const updateUserInList = useCallback(async (path: string, method: 'PATCH' | 'PUT', successMessage: string) => {
    setBusyAction(path)
    try {
      const updated = await request<UserResponse>(path, { method })
      setUsers(prev => prev.map(user => (user.id === updated.id ? updated : user)))
      toast.success(successMessage)
    } catch (error) {
      toast.error('Action failed', toErrorMessage(error, 'Action failed.'))
    } finally {
      setBusyAction(null)
    }
  }, [request, toast])

  const handlePasswordReset = useCallback(async (userId: string) => {
    const actionKey = `reset:${userId}`
    setBusyAction(actionKey)
    try {
      await request<null>(`/api/v1/admin/users/${userId}/password-reset`, { method: 'POST' })
      toast.success('Password reset email sent')
    } catch (error) {
      toast.error('Unable to send password reset email', toErrorMessage(error, 'Unable to send password reset email.'))
    } finally {
      setBusyAction(null)
    }
  }, [request, toast])

  const handleDeleteUser = useCallback(async (userId: string, username: string) => {
    const confirmed = window.confirm(`Deactivate user "${username}"?`)
    if (!confirmed) return

    const actionKey = `delete:${userId}`
    setBusyAction(actionKey)
    try {
      await request<null>(`/api/v1/users/${userId}`, { method: 'DELETE' })
      setUsers(prev => prev.filter(user => user.id !== userId))
      toast.success('User deactivated')
    } catch (error) {
      toast.error('Unable to deactivate user', toErrorMessage(error, 'Unable to deactivate user.'))
    } finally {
      setBusyAction(null)
    }
  }, [request, toast])

  async function handleCreateUser(e: FormEvent) {
    e.preventDefault()
    if (createUserState.roleNames.length === 0) {
      toast.error('Select at least one role')
      return
    }

    setBusyAction('create')
    try {
      const created = await request<UserResponse>('/api/v1/admin/users', {
        method: 'POST',
        body: JSON.stringify({
          username: createUserState.username.trim(),
          email: createUserState.email.trim(),
          roleNames: createUserState.roleNames,
          memberId: createUserState.memberId.trim() || null,
          sendPasswordResetEmail: createUserState.sendPasswordResetEmail,
        }),
      })

      setUsers(prev => [created, ...prev])
      setShowCreateModal(false)
      setCreateUserState(defaultCreateUserState())
      toast.success('User created successfully')
    } catch (error) {
      toast.error('Unable to create user', toErrorMessage(error, 'Unable to create user.'))
    } finally {
      setBusyAction(null)
    }
  }

  const openRolesEditor = useCallback((user: UserResponse) => {
    setEditingRolesUser(user)
    setEditingRoleNames(user.roles ?? [])
  }, [])

  async function handleRolesSubmit(e: FormEvent) {
    e.preventDefault()
    if (!editingRolesUser) return
    if (editingRoleNames.length === 0) {
      toast.error('At least one role is required')
      return
    }

    const actionKey = `roles:${editingRolesUser.id}`
    setBusyAction(actionKey)
    try {
      const updated = await request<UserResponse>(`/api/v1/users/${editingRolesUser.id}/roles`, {
        method: 'PUT',
        body: JSON.stringify({ roleNames: editingRoleNames }),
      })
      setUsers(prev => prev.map(user => (user.id === updated.id ? updated : user)))
      setEditingRolesUser(null)
      setEditingRoleNames([])
      toast.success('Roles updated successfully')
    } catch (error) {
      toast.error('Unable to update roles', toErrorMessage(error, 'Unable to update roles.'))
    } finally {
      setBusyAction(null)
    }
  }

  const openUserDetails = useCallback(async (userId: string) => {
    setDetailsUserId(userId)
    setDetailsUser(null)
    setDetailsError('')
    setDetailsLoading(true)

    try {
      const user = await request<UserResponse>(`/api/v1/users/${userId}`)
      setDetailsUser(user)
      setUsers(prev => prev.map(item => (item.id === user.id ? user : item)))
    } catch (error) {
      setDetailsError(toErrorMessage(error, 'Unable to load user details.'))
    } finally {
      setDetailsLoading(false)
    }
  }, [request])

  function closeUserDetails() {
    setDetailsUserId(null)
    setDetailsUser(null)
    setDetailsError('')
    setDetailsLoading(false)
  }

  const userColumns = useMemo((): ColumnDef<UserResponse>[] => [
    {
      key: 'user',
      header: 'User',
      headerClassName: 'ops-col-user',
      className: 'ops-cell-user',
      render: user => (
        <>
          <span className="ops-member-name">{user.username}</span>
          <span className="ops-member-sub">{user.email}</span>
        </>
      ),
    },
    {
      key: 'roles',
      header: 'Roles',
      headerClassName: 'ops-col-roles',
      className: 'ops-cell-roles',
      render: user => (
        <div className="ops-inline-actions">
          {(user.roles ?? []).map(role => (
            <span key={role} className={`badge ${roleBadgeClass(role)}`}>{role}</span>
          ))}
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      headerClassName: 'ops-col-status',
      className: 'ops-cell-status',
      render: user => {
        const status = deriveStatus(user)
        return <span className={`badge ${statusClass(status)}`}>{status}</span>
      },
    },
    {
      key: 'updated',
      header: 'Updated',
      headerClassName: 'ops-col-updated',
      className: 'data ops-cell-updated',
      render: user => formatDateTime(user.updatedAt),
    },
    {
      key: 'actions',
      header: '',
      width: '52px',
      headerClassName: 'datatable-col-actions',
      className: 'datatable-col-actions',
      render: user => {
        const status = deriveStatus(user)
        const rowBusy = Boolean(busyAction && busyAction.includes(user.id))
        const items: ActionMenuItem[] = []

        if (status === 'INACTIVE') {
          items.push({ label: 'Activate', onClick: () => void updateUserInList(`/api/v1/users/${user.id}/activate`, 'PATCH', 'User activated.'), disabled: rowBusy })
        } else {
          items.push({ label: 'Deactivate', onClick: () => void updateUserInList(`/api/v1/users/${user.id}/deactivate`, 'PATCH', 'User deactivated.'), disabled: rowBusy })
        }
        if (status === 'ACTIVE') {
          items.push({ label: 'Lock', onClick: () => void updateUserInList(`/api/v1/users/${user.id}/lock`, 'PATCH', 'User locked.'), disabled: rowBusy })
        }
        if (status === 'LOCKED') {
          items.push({ label: 'Unlock', onClick: () => void updateUserInList(`/api/v1/users/${user.id}/unlock`, 'PATCH', 'User unlocked.'), disabled: rowBusy })
        }
        items.push(
          { label: 'View details', onClick: () => void openUserDetails(user.id), disabled: rowBusy },
          { label: 'Edit roles', onClick: () => openRolesEditor(user), disabled: rowBusy },
          { label: 'Send reset', onClick: () => void handlePasswordReset(user.id), disabled: rowBusy },
          { label: 'Delete user', onClick: () => void handleDeleteUser(user.id, user.username), variant: 'danger', disabled: rowBusy },
        )
        return <ActionMenu actions={items} />
      },
    },
  ], [busyAction, handleDeleteUser, handlePasswordReset, openRolesEditor, openUserDetails, updateUserInList])

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">User Administration</h1>
        </div>
        <button type="button" className="btn btn--primary" onClick={() => setShowCreateModal(true)}>
          <UserPlus size={14} weight="bold" />
          Create User
        </button>
      </div>

      <hr className="rule rule--strong" />

      <div className="page-summary">
        <span>Accounts: <strong>{counts.total}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Active: <strong>{counts.active}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Inactive: <strong>{counts.inactive}</strong></span>
        <span className="page-summary-divider">|</span>
        <span>Locked: <strong>{counts.locked}</strong></span>
      </div>

      <hr className="rule" />

      <div className="filter-bar">
        <div className="filter-search-wrap">
          <MagnifyingGlass size={16} className="filter-search-icon" />
          <input
            type="text"
            className="filter-search"
            placeholder="Search username or email..."
            value={searchInput}
            onChange={event => setSearchInput(event.target.value)}
          />
        </div>

        <span className="page-section-title page-section-title--inline">Status</span>
        <select
          className="filter-select"
          value={statusFilter}
          onChange={event => setStatusFilter(event.target.value as StatusFilter)}
        >
          <option value="ALL">All</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
          <option value="LOCKED">Locked</option>
        </select>
      </div>

      <DataTable<UserResponse>
        columns={userColumns}
        data={filteredUsers}
        getRowKey={row => row.id}
        loading={loading}
        emptyMessage={
          users.length === 0
            ? <div className="empty-state empty-state--illustrated">
                <h3 className="empty-state-heading">No users registered</h3>
                <p className="empty-state-text">Create user accounts so members can log in and access the system.</p>
              </div>
            : 'No users match your search.'
        }
        getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
      />

      {hasMore && (
        <div className="ops-pager">
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loadingMore || !nextCursor}
            onClick={() => void loadUsers({ append: true, cursor: nextCursor, query: searchQuery })}
          >
            {loadingMore ? <><Spinner size="sm" /> Loading...</> : 'Load More'}
          </button>
        </div>
      )}

      <Modal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title="Create User"
        subtitle="Provision a user account and initial roles"
        width="md"
        footer={(
          <>
            <button type="button" className="btn btn--secondary" onClick={() => setShowCreateModal(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" form="create-user-form" disabled={busyAction === 'create'}>
              {busyAction === 'create' ? 'Creating...' : 'Create User'}
            </button>
          </>
        )}
      >
        <form id="create-user-form" className="modal-form" onSubmit={event => void handleCreateUser(event)}>
          <div className="field">
            <label className="field-label field-label--required" htmlFor="create-username">Username</label>
            <input
              id="create-username"
              className="field-input"
              type="text"
              minLength={3}
              required
              disabled={busyAction === 'create'}
              value={createUserState.username}
              onChange={event => setCreateUserState(prev => ({ ...prev, username: event.target.value }))}
            />
          </div>

          <div className="field">
            <label className="field-label field-label--required" htmlFor="create-email">Email</label>
            <input
              id="create-email"
              className="field-input"
              type="email"
              required
              disabled={busyAction === 'create'}
              value={createUserState.email}
              onChange={event => setCreateUserState(prev => ({ ...prev, email: event.target.value }))}
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="create-member-id">Member ID (optional)</label>
            <input
              id="create-member-id"
              className="field-input"
              type="text"
              value={createUserState.memberId}
              onChange={event => setCreateUserState(prev => ({ ...prev, memberId: event.target.value }))}
              placeholder="UUID"
            />
          </div>

          <fieldset className="ops-role-fieldset">
            <legend className="field-label">Roles</legend>
            <div className="ops-role-grid">
              {ROLE_OPTIONS.map(role => {
                const checked = createUserState.roleNames.includes(role)
                return (
                  <label key={role} className="ops-role-option">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={event => setRoleSelection(
                        role,
                        event.target.checked,
                        createUserState.roleNames,
                        roles => setCreateUserState(prev => ({ ...prev, roleNames: roles })),
                      )}
                    />
                    <span>{role}</span>
                  </label>
                )
              })}
            </div>
          </fieldset>

          <label className="ops-checkbox-row">
            <input
              type="checkbox"
              checked={createUserState.sendPasswordResetEmail}
              onChange={event => setCreateUserState(prev => ({ ...prev, sendPasswordResetEmail: event.target.checked }))}
            />
            <span>Send password reset email immediately</span>
          </label>
        </form>
      </Modal>

      <Modal
        open={Boolean(editingRolesUser)}
        onClose={() => setEditingRolesUser(null)}
        title="Update Roles"
        subtitle={editingRolesUser ? `Manage roles for ${editingRolesUser.username}` : 'Manage roles'}
        width="sm"
        footer={(
          <>
            <button type="button" className="btn btn--secondary" onClick={() => setEditingRolesUser(null)}>
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn--primary"
              form="edit-roles-form"
              disabled={Boolean(editingRolesUser && busyAction === `roles:${editingRolesUser.id}`)}
            >
              Save Roles
            </button>
          </>
        )}
      >
        <form id="edit-roles-form" className="modal-form" onSubmit={event => void handleRolesSubmit(event)}>
          <fieldset className="ops-role-fieldset">
            <legend className="field-label">Role Selection</legend>
            <div className="ops-role-grid">
              {ROLE_OPTIONS.map(role => {
                const checked = editingRoleNames.includes(role)
                return (
                  <label key={role} className="ops-role-option">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={event => setRoleSelection(role, event.target.checked, editingRoleNames, setEditingRoleNames)}
                    />
                    <span>{role}</span>
                  </label>
                )
              })}
            </div>
          </fieldset>
        </form>
      </Modal>

      <Modal
        open={Boolean(detailsUserId)}
        onClose={closeUserDetails}
        title="User Details"
        subtitle={detailsUser ? detailsUser.username : 'Account metadata'}
        width="md"
        footer={(
          <>
            {detailsUserId && (
              <button
                type="button"
                className="btn btn--secondary"
                onClick={() => void openUserDetails(detailsUserId)}
                disabled={detailsLoading}
              >
                {detailsLoading ? 'Refreshing...' : 'Refresh'}
              </button>
            )}
            <button type="button" className="btn btn--primary" onClick={closeUserDetails}>Close</button>
          </>
        )}
      >
        {detailsLoading ? (
          <div className="empty-state ops-empty">
            <h2 className="empty-state-heading">Loading details...</h2>
          </div>
        ) : detailsError ? (
          <div className="empty-state ops-empty">
            <h2 className="empty-state-heading">Unable to load user</h2>
            <p className="empty-state-text">{detailsError}</p>
          </div>
        ) : detailsUser ? (
          <div className="settings-group ops-user-details">
            <div className="settings-row">
              <div>
                <span className="settings-row-label">Username</span>
                <span className="settings-row-desc">Primary login identifier</span>
              </div>
              <span className="settings-row-value data">{detailsUser.username}</span>
            </div>
            <div className="settings-row">
              <div>
                <span className="settings-row-label">Email</span>
                <span className="settings-row-desc">Password reset and notifications</span>
              </div>
              <span className="settings-row-value">{detailsUser.email}</span>
            </div>
            <div className="settings-row">
              <div>
                <span className="settings-row-label">User ID</span>
                <span className="settings-row-desc">Internal UUID</span>
              </div>
              <span className="settings-row-value data">{detailsUser.id}</span>
            </div>
            <div className="settings-row">
              <div>
                <span className="settings-row-label">Status</span>
                <span className="settings-row-desc">Enabled and lock-state derived status</span>
              </div>
              <span className={`badge ${statusClass(deriveStatus(detailsUser))}`}>{deriveStatus(detailsUser)}</span>
            </div>
            <div className="settings-row">
              <div>
                <span className="settings-row-label">Roles</span>
                <span className="settings-row-desc">Granted authorization roles</span>
              </div>
              <div className="ops-inline-actions">
                {detailsUser.roles.map(role => (
                  <span key={role} className={`badge ${roleBadgeClass(role)}`}>{role}</span>
                ))}
              </div>
            </div>
            <div className="settings-row">
              <div>
                <span className="settings-row-label">Created</span>
                <span className="settings-row-desc">Account creation timestamp</span>
              </div>
              <span className="settings-row-value data">{formatDateTime(detailsUser.createdAt)}</span>
            </div>
            <div className="settings-row">
              <div>
                <span className="settings-row-label">Updated</span>
                <span className="settings-row-desc">Last update timestamp</span>
              </div>
              <span className="settings-row-value data">{formatDateTime(detailsUser.updatedAt)}</span>
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  )
}
