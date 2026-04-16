import { useCallback, useEffect, useMemo, useState } from 'react'
import { MagnifyingGlass, UserPlus } from '@phosphor-icons/react'
import { Spinner } from '../components/Spinner'
import { Link } from 'react-router-dom'
import { AddMemberModal } from '../components/AddMemberModal'
import { Select } from '../components/Select'
import { StatCardGrid } from '../components/StatCard'
import { DataTable } from '../components/DataTable'
import type { ColumnDef } from '../components/DataTable'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { useToast } from '../hooks/useToast'
import type { CursorPage } from '../types/users'
import type { CreateMemberRequest, MemberResponse, MemberStatus } from '../types/members'
import './Members.css'

type StatusFilter = 'all' | MemberStatus

const PAGE_SIZE = 50

const statusClass: Record<MemberStatus, string> = {
  ACTIVE: 'badge--active',
  SUSPENDED: 'badge--suspended',
  DEACTIVATED: 'badge--inactive',
}

function fullName(member: MemberResponse): string {
  return `${member.firstName} ${member.lastName}`.trim()
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

function fmtJoinDate(value: string): string {
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('en-KE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export function Members() {
  const { request } = useAuthenticatedApi()
  const { canAccess, isMemberOnly } = useAuthorization()
  const canCreateMember = canAccess(['ADMIN'])
  const toast = useToast()

  const [members, setMembers] = useState<MemberResponse[]>([])
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<StatusFilter>('all')
  const [showModal, setShowModal] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [creatingMember, setCreatingMember] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)

  const loadMembers = useCallback(async (opts?: { append?: boolean; cursor?: string | null }) => {
    const append = Boolean(opts?.append)
    const cursor = opts?.cursor

    if (append) setLoadingMore(true)
    else setLoading(true)

    try {
      const path = cursor
        ? `/api/v1/members?size=${PAGE_SIZE}&cursor=${encodeURIComponent(cursor)}`
        : `/api/v1/members?size=${PAGE_SIZE}`
      const page = await request<CursorPage<MemberResponse>>(path)

      setMembers(prev => {
        if (!append) return page.items

        const merged = new Map<string, MemberResponse>()
        prev.forEach(member => merged.set(member.id, member))
        page.items.forEach(member => merged.set(member.id, member))
        return Array.from(merged.values())
      })
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
    } catch (error) {
      toast.error('Unable to load members', toErrorMessage(error, 'Unable to load members.'))
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [request, toast])

  useEffect(() => {
    void loadMembers({ append: false, cursor: null })
  }, [loadMembers])

  async function handleCreateMember(payload: CreateMemberRequest) {
    if (!canCreateMember) {
      throw new Error('Only administrators can add members.')
    }

    setCreatingMember(true)
    try {
      const created = await request<MemberResponse>('/api/v1/members', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setMembers(prev => [created, ...prev])
      setShowModal(false)
      toast.success('Member created', 'MEMBER user account provisioning has been triggered.')
    } catch (error) {
      const message = toErrorMessage(error, 'Unable to create member.')
      toast.error('Unable to create member', message)
      throw error instanceof Error ? error : new Error(message)
    } finally {
      setCreatingMember(false)
    }
  }

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase()

    return members.filter(member => {
      if (filter !== 'all' && member.status !== filter) return false
      if (!query) return true

      const text = [
        fullName(member),
        member.email,
        member.phone,
        member.memberNumber,
      ].join(' ').toLowerCase()

      return text.includes(query)
    })
  }, [filter, members, search])

  const memberColumns: ColumnDef<MemberResponse>[] = useMemo(() => {
    const cols: ColumnDef<MemberResponse>[] = [
      {
        key: 'name',
        header: 'Name',
        render: (member) => (
          <>
            <Link to={`/members/${member.id}`} className="member-link">
              <span className="member-name">{fullName(member)}</span>
            </Link>
            <span className="member-sub">{member.memberNumber}</span>
            {!isMemberOnly && <span className="member-email">{member.email}</span>}
          </>
        ),
      },
      {
        key: 'phone',
        header: 'Phone',
        className: 'data',
        render: (member) => member.phone,
      },
      {
        key: 'status',
        header: 'Status',
        render: (member) => (
          <span className={`badge ${statusClass[member.status]}`}>{member.status}</span>
        ),
      },
      {
        key: 'joined',
        header: 'Joined',
        className: 'ledger-date',
        render: (member) => fmtJoinDate(member.joinDate),
      },
    ]

    if (!isMemberOnly) {
      cols.push({
        key: 'shares',
        header: 'Shares (KES)',
        className: 'amount ledger-table-amount',
        headerClassName: 'ledger-table-amount',
        render: (member) => fmtCurrency(member.shareBalance),
      })
    }

    return cols
  }, [isMemberOnly])

  const active = members.filter(member => member.status === 'ACTIVE').length
  const suspended = members.filter(member => member.status === 'SUSPENDED').length
  const deactivated = members.filter(member => member.status === 'DEACTIVATED').length
  const totalShares = members.reduce((sum, member) => sum + Number(member.shareBalance ?? 0), 0)

  return (
    <div className="members-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">{isMemberOnly ? 'Member Directory' : 'Members'}</h1>
          <p className="page-subtitle">{members.length} registered members</p>
        </div>
        {canCreateMember && (
          <button className="btn btn--primary" onClick={() => setShowModal(true)}>
            <UserPlus size={14} weight="bold" />
            Add Member
          </button>
        )}
      </div>

      <hr className="rule rule--strong" />

      <StatCardGrid
        items={isMemberOnly ? [
          { label: 'Active Members', value: String(active) },
          { label: 'Total Registered', value: String(members.length) },
        ] : [
          { label: 'Active', value: String(active) },
          { label: 'Suspended', value: String(suspended) },
          { label: 'Deactivated', value: String(deactivated) },
          { label: 'Total Shares', value: `KES ${fmtCurrency(totalShares)}` },
        ]}
        columns={isMemberOnly ? 2 : 4}
      />

      <hr className="rule" />

      <div className="filter-bar">
        <div className="filter-search-wrap">
          <MagnifyingGlass size={16} className="filter-search-icon" />
          <input
            type="text"
            className="filter-search"
            placeholder="Search members..."
            aria-label="Search members"
            value={search}
            onChange={event => setSearch(event.target.value)}
          />
        </div>
        <div className="filter-select-wrap">
          <Select
            value={filter}
            onChange={value => setFilter(value as StatusFilter)}
            options={[
              { value: 'all', label: 'All Status' },
              { value: 'ACTIVE', label: 'Active' },
              { value: 'SUSPENDED', label: 'Suspended' },
              { value: 'DEACTIVATED', label: 'Deactivated' },
            ]}
          />
        </div>
      </div>

      <DataTable<MemberResponse>
        columns={memberColumns}
        data={filtered}
        getRowKey={row => row.id}
        loading={loading}
        emptyMessage={
          members.length === 0
            ? <div className="empty-state empty-state--illustrated">
                <h3 className="empty-state-heading">No members yet</h3>
                <p className="empty-state-text">Add your first member to start tracking contributions and loans.</p>
              </div>
            : 'No members match your search.'
        }
      />

      {hasMore && (
        <div className="ops-pager">
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loadingMore || !nextCursor}
            onClick={() => void loadMembers({ append: true, cursor: nextCursor })}
          >
            {loadingMore ? <><Spinner size="sm" /> Loading...</> : 'Load More'}
          </button>
        </div>
      )}

      <hr className="rule rule--strong" />

      {canCreateMember && (
        <AddMemberModal
          open={showModal}
          onClose={() => setShowModal(false)}
          onSubmit={handleCreateMember}
          isSubmitting={creatingMember}
        />
      )}
    </div>
  )
}
