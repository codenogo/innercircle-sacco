import { Link } from 'react-router-dom'
import type { Icon } from '@phosphor-icons/react'
import {
  UsersThree,
  UserSquare,
  HandCoins,
  CurrencyCircleDollar,
  Bank,
  Stack,
  Gift,
  Wallet,
  TrendUp,
  CalendarCheck,
  Heartbeat,
  SignOut,
  BookOpen,
  DownloadSimple,
  SquaresFour,
  SlidersHorizontal,
  ClockCounterClockwise,
  UserGear,
} from '@phosphor-icons/react'
import { useAuthorization } from '../hooks/useAuthorization'
import type { UserRole } from '../types/roles'
import './Operations.css'

interface OperationLink {
  to: string
  title: string
  description: string
  api: string
  icon: Icon
  allowed: UserRole[]
  status: 'ready' | 'preview'
  previewNote?: string
}

const operationLinks: OperationLink[] = [
  {
    to: '/users-admin',
    title: 'User Administration',
    description: 'Manage user accounts, roles, lock states, and admin resets.',
    api: '/api/v1/users, /api/v1/admin/users',
    icon: UsersThree,
    allowed: ['ADMIN'],
    status: 'ready',
  },
  {
    to: '/members',
    title: 'Member Profile',
    description: 'Open member details, lifecycle actions, and account snapshots.',
    api: '/api/v1/members/{id}',
    icon: UserSquare,
    allowed: ['ADMIN', 'TREASURER', 'MEMBER'],
    status: 'ready',
  },
  {
    to: '/contribution-categories',
    title: 'Contribution Categories',
    description: 'Create and maintain contribution category definitions.',
    api: '/api/v1/contribution-categories',
    icon: HandCoins,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'ready',
  },
  {
    to: '/contribution-ops',
    title: 'Contribution Operations',
    description: 'Bulk processing, confirmation, and reversals.',
    api: '/api/v1/contributions/*',
    icon: CurrencyCircleDollar,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'ready',
  },
  {
    to: '/loan-workflow',
    title: 'Loan Workflow',
    description: 'Application, approval, disbursement, repayment, and schedules.',
    api: '/api/v1/loans/*',
    icon: Bank,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'ready',
  },
  {
    to: '/loan-batch',
    title: 'Loan Batch',
    description: 'Monthly batch processing, unpaid loans, and reversals.',
    api: '/api/v1/loans/batch/*',
    icon: Stack,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'preview',
    previewNote: 'UI preview only',
  },
  {
    to: '/loan-benefits',
    title: 'Loan Benefits',
    description: 'Member and loan benefit entitlement snapshots.',
    api: '/api/v1/loan-benefits',
    icon: Gift,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'preview',
    previewNote: 'UI preview only',
  },
  {
    to: '/payout-ops',
    title: 'Payout Operations',
    description: 'Payout approvals, bank withdrawal reconciliation, and signoff.',
    api: '/api/v1/payouts, /bank-withdrawals, /cash-disbursements, /share-withdrawals',
    icon: Wallet,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'preview',
    previewNote: 'UI preview only',
  },
  {
    to: '/petty-cash',
    title: 'Petty Cash Workflow',
    description: 'Voucher submission, approval, disbursement, settlement, and rejection.',
    api: '/api/v1/petty-cash/*',
    icon: Wallet,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'ready',
  },
  {
    to: '/investment-ops',
    title: 'Investment Operations',
    description: 'Create, approve, record income, dispose, and manage investment portfolio.',
    api: '/api/v1/investments/*',
    icon: TrendUp,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'ready',
  },
  {
    to: '/meetings-fines',
    title: 'Meetings & Fines',
    description: 'Create meetings, capture attendance, generate and settle fines.',
    api: '/api/v1/meetings/*',
    icon: CalendarCheck,
    allowed: ['ADMIN', 'TREASURER', 'SECRETARY', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'],
    status: 'ready',
  },
  {
    to: '/welfare-claims',
    title: 'Welfare Claims',
    description: 'Manage beneficiaries, claims, reviews, and welfare payouts.',
    api: '/api/v1/welfare/*',
    icon: Heartbeat,
    allowed: ['ADMIN', 'TREASURER', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'],
    status: 'ready',
  },
  {
    to: '/member-exit',
    title: 'Member Exit Workflow',
    description: 'Capture notice, review approvals, and process settlement installments.',
    api: '/api/v1/members/{memberId}/exit-requests/*',
    icon: SignOut,
    allowed: ['ADMIN', 'TREASURER', 'MEMBER', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'],
    status: 'ready',
  },
  {
    to: '/ledger-statements',
    title: 'Ledger Statements',
    description: 'Trial balance, income statement, and balance sheet views.',
    api: '/api/v1/ledger/*',
    icon: BookOpen,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'preview',
    previewNote: 'UI preview only',
  },
  {
    to: '/export-center',
    title: 'Export Center',
    description: 'Generate PDF/CSV statement and summary exports.',
    api: '/api/v1/export/*',
    icon: DownloadSimple,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'preview',
    previewNote: 'UI preview only',
  },
  {
    to: '/role-dashboards',
    title: 'Role Dashboards',
    description: 'Member, treasurer, admin views plus analytics endpoints.',
    api: '/api/v1/dashboard/*',
    icon: SquaresFour,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'preview',
    previewNote: 'UI preview only',
  },
  {
    to: '/system-config',
    title: 'System Configuration',
    description: 'System keys, loan products, schedules, and penalty rules.',
    api: '/api/v1/config/*',
    icon: SlidersHorizontal,
    allowed: ['ADMIN'],
    status: 'preview',
    previewNote: 'UI preview only',
  },
  {
    to: '/audit-trail',
    title: 'Audit Trail',
    description: 'Audit event search, entity history, and CSV export.',
    api: '/api/v1/audit/*',
    icon: ClockCounterClockwise,
    allowed: ['ADMIN'],
    status: 'ready',
  },
  {
    to: '/profile',
    title: 'My Profile',
    description: 'Current authenticated user details and security state.',
    api: '/api/v1/me',
    icon: UserGear,
    allowed: ['ADMIN', 'TREASURER', 'MEMBER'],
    status: 'ready',
  },
]

export function Operations() {
  const { canAccess } = useAuthorization()
  const visibleLinks = operationLinks.filter(item => canAccess(item.allowed))

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Operations</h1>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <section className="page-section">
        <span className="page-section-title">Available Workflows</span>
        <hr className="rule" />
        <div className="ops-grid">
          {visibleLinks.map(item => {
            const Icon = item.icon
            if (item.status === 'preview') {
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className="report-card ops-link-card ops-link-card--disabled"
                  aria-disabled="true"
                  tabIndex={-1}
                  onClick={e => e.preventDefault()}
                >
                  <div className="report-card-info">
                    <div className="ops-card-title-row">
                      <Icon size={15} className="ops-card-icon" />
                      <span className="report-card-title">{item.title}</span>
                      <span className="badge badge--pending">Preview</span>
                    </div>
                    <span className="report-card-desc">{item.description}</span>
                    <span className="ops-card-api data">{item.api}</span>
                    {item.previewNote && <span className="ops-note">{item.previewNote}</span>}
                  </div>
                </Link>
              )
            }

            return (
              <Link key={item.to} to={item.to} className="report-card ops-link-card">
                <div className="report-card-info">
                  <div className="ops-card-title-row">
                    <Icon size={15} className="ops-card-icon" />
                    <span className="report-card-title">{item.title}</span>
                  </div>
                  <span className="report-card-desc">{item.description}</span>
                  <span className="ops-card-api data">{item.api}</span>
                </div>
              </Link>
            )
          })}
        </div>
      </section>
    </div>
  )
}
