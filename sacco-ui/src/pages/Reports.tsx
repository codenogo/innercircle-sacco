import { useCallback, useEffect, useMemo, useState } from 'react'
import { FileText, DownloadSimple, ChartBar, TrendUp, Users, Wallet, MagnifyingGlass, type Icon } from '@phosphor-icons/react'
import { Spinner } from '../components/Spinner'
import { SkeletonStat } from '../components/Skeleton'
import { Modal } from '../components/Modal'
import { DatePicker } from '../components/DatePicker'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useToast } from '../hooks/useToast'
import { ApiError } from '../services/apiClient'
import { getAllMembers } from '../services/memberService'
import { localISODate } from '../utils/date'
import {
  exportFinancialSummaryCsvUrl,
  exportMemberStatementPdfUrl,
} from '../services/reportService'
import type { SaccoStateResponse } from '../types/dashboard'
import type { MemberResponse } from '../types/members'
import './Reports.css'

interface ReportDefinition {
  id: string
  title: string
  description: string
  icon: Icon
  formats: string[]
}

const reports: ReportDefinition[] = [
  {
    id: 'financial-summary',
    title: 'Financial Summary',
    description: 'Overview of group fund, income, expenses, and balances for a given period.',
    icon: ChartBar,
    formats: ['CSV'],
  },
  {
    id: 'contribution-report',
    title: 'Contribution Report',
    description: 'Detailed breakdown of member contributions by month, category, and status.',
    icon: Wallet,
    formats: ['PDF', 'CSV'],
  },
  {
    id: 'loan-portfolio',
    title: 'Loan Portfolio Report',
    description: 'Active loans, repayment schedules, interest accrued, and arrears summary.',
    icon: TrendUp,
    formats: ['PDF', 'CSV'],
  },
  {
    id: 'member-statement',
    title: 'Member Statement',
    description: 'Individual member account statement — contributions, loans, payouts, and balances.',
    icon: FileText,
    formats: ['PDF'],
  },
  {
    id: 'member-register',
    title: 'Member Register',
    description: 'Complete list of all members with status, join date, shares, and contact information.',
    icon: Users,
    formats: ['PDF', 'CSV'],
  },
  {
    id: 'trial-balance',
    title: 'Trial Balance',
    description: 'Summary of all ledger account balances — debits and credits for the period.',
    icon: ChartBar,
    formats: ['PDF', 'CSV'],
  },
]

function fmtCurrency(value: number): string {
  return value.toLocaleString('en-KE')
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function defaultFromDate(now: Date = new Date()): string {
  return localISODate(new Date(now.getFullYear(), now.getMonth(), 1))
}

function isDownloadAvailable(reportId: string, format: string): boolean {
  return (
    (reportId === 'financial-summary' && format === 'CSV')
    || (reportId === 'member-statement' && format === 'PDF')
  )
}

export function Reports() {
  const { request, requestBlob } = useAuthenticatedApi()
  const toast = useToast()

  const [stats, setStats] = useState<SaccoStateResponse | null>(null)
  const [statsLoading, setStatsLoading] = useState(true)
  const [statsError, setStatsError] = useState<string | null>(null)

  // Per-button downloading state: key is "reportId-format"
  const [downloading, setDownloading] = useState<Record<string, boolean>>({})

  // Date range for reports
  const [fromDate, setFromDate] = useState(defaultFromDate)
  const [toDate, setToDate] = useState(() => localISODate())

  // Member search for Member Statement
  const [memberSearchOpen, setMemberSearchOpen] = useState(false)
  const [memberQuery, setMemberQuery] = useState('')
  const [members, setMembers] = useState<MemberResponse[]>([])
  const [membersLoading, setMembersLoading] = useState(false)

  const loadStats = useCallback(async () => {
    setStatsLoading(true)
    setStatsError(null)
    try {
      const data = await request<SaccoStateResponse>('/api/v1/dashboard/state')
      setStats(data)
    } catch (error) {
      setStatsError(toErrorMessage(error, 'Unable to load dashboard stats.'))
    } finally {
      setStatsLoading(false)
    }
  }, [request])

  useEffect(() => {
    void loadStats()
  }, [loadStats])

  const loadMembers = useCallback(async () => {
    setMembersLoading(true)
    try {
      const allMembers = await getAllMembers(request)
      setMembers(allMembers)
    } catch {
      setMembers([])
    } finally {
      setMembersLoading(false)
    }
  }, [request])

  useEffect(() => {
    if (memberSearchOpen && members.length === 0) {
      void loadMembers()
    }
  }, [memberSearchOpen, members.length, loadMembers])

  const filteredMembers = useMemo(() => {
    const q = memberQuery.trim().toLowerCase()
    if (!q) return members
    return members.filter(m => {
      const text = `${m.firstName} ${m.lastName} ${m.memberNumber} ${m.email}`.toLowerCase()
      return text.includes(q)
    })
  }, [memberQuery, members])

  async function triggerDownload(url: string, filename: string, downloadKey: string) {
    setDownloading(prev => ({ ...prev, [downloadKey]: true }))
    try {
      const blob = await requestBlob(url)
      const objectUrl = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = objectUrl
      a.download = filename
      a.click()
      URL.revokeObjectURL(objectUrl)
    } catch (error) {
      toast.error('Download failed', toErrorMessage(error, 'Download failed.'))
    } finally {
      setDownloading(prev => ({ ...prev, [downloadKey]: false }))
    }
  }

  function handleDownload(reportId: string, format: string) {
    const downloadKey = `${reportId}-${format}`

    if (reportId === 'member-statement') {
      setMemberSearchOpen(true)
      return
    }

    if (reportId === 'financial-summary' && format === 'CSV') {
      const url = exportFinancialSummaryCsvUrl(fromDate, toDate)
      void triggerDownload(url, `financial-summary-${fromDate}-${toDate}.csv`, downloadKey)
      return
    }

    // Other reports not yet wired — show info
    toast.info('Export not available', `${format} export for this report is not yet available.`)
  }

  function handleMemberSelect(member: MemberResponse) {
    const url = exportMemberStatementPdfUrl(member.id, fromDate, toDate)
    const filename = `statement-${member.firstName}-${member.lastName}-${fromDate}-${toDate}.pdf`
    setMemberSearchOpen(false)
    setMemberQuery('')
    void triggerDownload(url, filename, 'member-statement-PDF')
  }

  const quickStats = stats
    ? [
        { label: 'Total Savings', value: `KES ${fmtCurrency(stats.totalContributions)}` },
        { label: 'Loans Outstanding', value: `KES ${fmtCurrency(stats.totalOutstandingLoans)}` },
        { label: 'Total Share Capital', value: `KES ${fmtCurrency(stats.totalShareCapital)}` },
        { label: 'Active Members', value: String(stats.activeMembers) },
      ]
    : []

  return (
    <div className="reports-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Reports</h1>
          <p className="page-subtitle">Financial reports and analytics</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      {/* Date range picker */}
      <section className="page-section">
        <span className="page-section-title">Report Period</span>
        <hr className="rule" />
        <div className="reports-date-range">
          <label className="reports-date-label">
            From
            <DatePicker value={fromDate} onChange={setFromDate} />
          </label>
          <label className="reports-date-label">
            To
            <DatePicker value={toDate} onChange={setToDate} />
          </label>
        </div>
        <hr className="rule" />
      </section>

      {/* Quick stats */}
      <section className="page-section">
        <span className="page-section-title">At a Glance</span>
        <hr className="rule" />
        {statsLoading ? (
          <div className="reports-stats">
            <SkeletonStat />
            <SkeletonStat />
            <SkeletonStat />
            <SkeletonStat />
          </div>
        ) : statsError ? (
          <div className="reports-stats-error">{statsError}</div>
        ) : (
          <div className="reports-stats">
            {quickStats.map(s => (
              <div key={s.label} className="reports-stat">
                <span className="reports-stat-label">{s.label}</span>
                <span className="reports-stat-value data">{s.value}</span>
              </div>
            ))}
          </div>
        )}
        <hr className="rule" />
      </section>

      {/* Member search modal for Member Statement */}
      <Modal
        open={memberSearchOpen}
        onClose={() => { setMemberSearchOpen(false); setMemberQuery('') }}
        title="Select Member for Statement"
        subtitle="Choose a member to generate their statement"
        width="md"
      >
        <div className="reports-member-search-wrap">
          <MagnifyingGlass size={14} className="reports-member-search-icon" />
          <input
            type="text"
            className="reports-member-search"
            placeholder="Search by name, number, or email..."
            aria-label="Search members"
            value={memberQuery}
            onChange={e => setMemberQuery(e.target.value)}
            autoFocus
          />
        </div>
        <div className="reports-member-list">
          {membersLoading ? (
            <div className="reports-member-list-empty">
              <Spinner size="sm" />
              Loading members...
            </div>
          ) : filteredMembers.length === 0 ? (
            <div className="reports-member-list-empty">No members found.</div>
          ) : (
            filteredMembers.map(m => (
              <button
                key={m.id}
                type="button"
                className="reports-member-item"
                onClick={() => handleMemberSelect(m)}
              >
                <span className="reports-member-item-name">
                  {m.firstName} {m.lastName}
                </span>
                <span className="reports-member-item-sub">{m.memberNumber}</span>
              </button>
            ))
          )}
        </div>
      </Modal>

      {/* Available reports */}
      <section className="page-section">
        <span className="page-section-title">Available Reports</span>
        <hr className="rule" />
        <div className="reports-list">
          {reports.map(r => {
            const Icon = r.icon
            return (
              <div key={r.id} className="report-card">
                <div className="report-card-info">
                  <div className="report-card-header">
                    <Icon size={15} className="report-card-icon" />
                    <span className="report-card-title">{r.title}</span>
                  </div>
                  <span className="report-card-desc">{r.description}</span>
                </div>
                <div className="report-card-actions">
                  {r.formats.map(f => {
                    const key = `${r.id}-${f}`
                    const isDownloading = downloading[key] ?? false
                    const isAvailable = isDownloadAvailable(r.id, f)
                    return (
                      <button
                        key={f}
                        type="button"
                        className="btn btn--secondary btn--small"
                        disabled={isDownloading || !isAvailable}
                        onClick={() => {
                          if (!isAvailable) return
                          handleDownload(r.id, f)
                        }}
                        aria-label={`Download ${r.title} as ${f}`}
                        title={isAvailable ? undefined : `${f} export is coming soon`}
                      >
                        {isDownloading ? (
                          <Spinner size="sm" />
                        ) : (
                          <DownloadSimple size={11} weight="bold" />
                        )}
                        {isDownloading ? 'Downloading...' : isAvailable ? f : `${f} (Soon)`}
                      </button>
                    )
                  })}
                </div>
                {r.formats.some(f => !isDownloadAvailable(r.id, f)) && (
                  <span className="reports-note">Some formats are not available yet.</span>
                )}
              </div>
            )
          })}
        </div>
      </section>
    </div>
  )
}
