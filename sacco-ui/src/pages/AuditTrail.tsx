import { useCallback, useEffect, useRef, useState } from 'react'
import { Download } from 'lucide-react'
import { ApiError } from '../services/apiClient'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { localISODate } from '../utils/date'
import type { CursorPage } from '../types/users'
import type { AuditEventResponse, AuditEntityType } from '../types/audit'
import './Operations.css'

type EntityTypeFilter = 'ALL' | AuditEntityType

const PAGE_SIZE = 50

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function fmtTimestamp(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('en-KE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function AuditTrail() {
  const { request, requestBlob } = useAuthenticatedApi()

  const [events, setEvents] = useState<AuditEventResponse[]>([])
  const [typeFilter, setTypeFilter] = useState<EntityTypeFilter>('ALL')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const buildQueryString = useCallback((cursor?: string | null) => {
    const params = new URLSearchParams()
    params.set('limit', String(PAGE_SIZE))
    if (cursor) params.set('cursor', cursor)
    if (typeFilter !== 'ALL') params.set('entityType', typeFilter)
    if (startDate) params.set('startDate', startDate)
    if (endDate) params.set('endDate', endDate)
    return params.toString()
  }, [typeFilter, startDate, endDate])

  const loadEvents = useCallback(async (opts?: { append?: boolean; cursor?: string | null }) => {
    const append = Boolean(opts?.append)
    const cursor = opts?.cursor

    if (append) setLoadingMore(true)
    else setLoading(true)

    try {
      const query = buildQueryString(cursor)
      const page = await request<CursorPage<AuditEventResponse>>(`/api/v1/audit?${query}`)

      setEvents(prev => {
        if (!append) return page.items

        const merged = new Map<string, AuditEventResponse>()
        prev.forEach(event => merged.set(event.id, event))
        page.items.forEach(event => merged.set(event.id, event))
        return Array.from(merged.values())
      })
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
      setError(null)
    } catch (err) {
      setError(toErrorMessage(err, 'Unable to load audit events.'))
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [request, buildQueryString])

  useEffect(() => {
    void loadEvents({ append: false, cursor: null })
  }, [loadEvents])

  const handleExportCsv = useCallback(async () => {
    setExporting(true)
    try {
      const params = new URLSearchParams()
      if (typeFilter !== 'ALL') params.set('entityType', typeFilter)
      if (startDate) params.set('startDate', startDate)
      if (endDate) params.set('endDate', endDate)
      const query = params.toString()

      const blob = await requestBlob(`/api/v1/audit/export${query ? `?${query}` : ''}`, {
        headers: { Accept: 'text/csv' },
      })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `audit-export-${localISODate()}.csv`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(toErrorMessage(err, 'Unable to export audit events.'))
    } finally {
      setExporting(false)
    }
  }, [typeFilter, startDate, endDate, requestBlob])

  // Track whether it's the initial mount to avoid double-fetch
  const mountedRef = useRef(false)

  useEffect(() => {
    if (!mountedRef.current) {
      mountedRef.current = true
      return
    }
    setEvents([])
    setNextCursor(null)
    setHasMore(false)
  }, [typeFilter, startDate, endDate])

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Audit Trail</h1>
          <p className="page-subtitle">Event search, entity history, and export</p>
        </div>
        <button
          type="button"
          className="btn btn--secondary"
          disabled={exporting}
          onClick={() => void handleExportCsv()}
        >
          <Download size={14} strokeWidth={2} />
          {exporting ? 'Exporting...' : 'Export CSV'}
        </button>
      </div>

      <hr className="rule rule--strong" />

      {error && (
        <div className="ops-feedback ops-feedback--error" role="status">
          {error}
        </div>
      )}

      <div className="filter-bar">
        <span className="page-section-title page-section-title--inline">Entity Type</span>
        <select
          className="filter-select"
          value={typeFilter}
          onChange={e => setTypeFilter(e.target.value as EntityTypeFilter)}
        >
          <option value="ALL">All</option>
          <option value="MEMBER">Member</option>
          <option value="LOAN">Loan</option>
          <option value="CONTRIBUTION">Contribution</option>
          <option value="PAYOUT">Payout</option>
          <option value="CONFIG">Config</option>
          <option value="SECURITY">Security</option>
        </select>

        <span className="page-section-title page-section-title--inline">From</span>
        <input
          type="date"
          className="filter-select"
          value={startDate}
          onChange={e => setStartDate(e.target.value)}
        />

        <span className="page-section-title page-section-title--inline">To</span>
        <input
          type="date"
          className="filter-select"
          value={endDate}
          onChange={e => setEndDate(e.target.value)}
        />
      </div>

      <table className="ledger-table">
        <thead>
          <tr>
            <th className="label">Event ID</th>
            <th className="label">Time</th>
            <th className="label">Actor</th>
            <th className="label">Entity</th>
            <th className="label">Action</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr><td colSpan={5} className="table-empty">Loading audit events...</td></tr>
          ) : events.length === 0 ? (
            <tr><td colSpan={5} className="table-empty">No events found.</td></tr>
          ) : events.map((event, i) => (
            <tr key={event.id} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
              <td className="data">{event.id}</td>
              <td className="data">{fmtTimestamp(event.timestamp)}</td>
              <td>{event.actorName}</td>
              <td className="data">{event.entityType}:{event.entityId}</td>
              <td><span className="badge badge--completed">{event.action}</span></td>
            </tr>
          ))}
        </tbody>
      </table>

      {hasMore && (
        <div className="ops-pager">
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loadingMore || !nextCursor}
            onClick={() => void loadEvents({ append: true, cursor: nextCursor })}
          >
            {loadingMore ? 'Loading...' : 'Load More'}
          </button>
        </div>
      )}

      <hr className="rule rule--strong" />
    </div>
  )
}
