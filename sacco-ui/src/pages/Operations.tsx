import { Link } from 'react-router-dom'
import type { LucideIcon } from 'lucide-react'
import {
  UsersRound,
  UserSquare2,
  HandCoins,
  CircleDollarSign,
  Landmark,
  Blocks,
  Gift,
  Wallet,
  BookOpen,
  Download,
  LayoutDashboard,
  SlidersHorizontal,
  History,
  UserCog,
} from 'lucide-react'
import { useAuthorization } from '../hooks/useAuthorization'
import type { UserRole } from '../types/roles'
import './Operations.css'

interface OperationLink {
  to: string
  title: string
  description: string
  api: string
  icon: LucideIcon
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
    icon: UsersRound,
    allowed: ['ADMIN'],
    status: 'ready',
  },
  {
    to: '/members',
    title: 'Member Profile',
    description: 'Open member details, lifecycle actions, and account snapshots.',
    api: '/api/v1/members/{id}',
    icon: UserSquare2,
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
    icon: CircleDollarSign,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'ready',
  },
  {
    to: '/loan-workflow',
    title: 'Loan Workflow',
    description: 'Application, approval, disbursement, repayment, and schedules.',
    api: '/api/v1/loans/*',
    icon: Landmark,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'preview',
    previewNote: 'UI preview only',
  },
  {
    to: '/loan-batch',
    title: 'Loan Batch',
    description: 'Monthly batch processing, unpaid loans, and reversals.',
    api: '/api/v1/loans/batch/*',
    icon: Blocks,
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
    icon: Download,
    allowed: ['ADMIN', 'TREASURER'],
    status: 'preview',
    previewNote: 'UI preview only',
  },
  {
    to: '/role-dashboards',
    title: 'Role Dashboards',
    description: 'Member, treasurer, admin views plus analytics endpoints.',
    api: '/api/v1/dashboard/*',
    icon: LayoutDashboard,
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
    icon: History,
    allowed: ['ADMIN'],
    status: 'ready',
  },
  {
    to: '/profile',
    title: 'My Profile',
    description: 'Current authenticated user details and security state.',
    api: '/api/v1/me',
    icon: UserCog,
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
          <p className="page-subtitle">Frontend coverage for backend modules</p>
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
                <div key={item.to} className="report-card ops-link-card ops-link-card--disabled" aria-disabled="true">
                  <div className="report-card-info">
                    <div className="ops-card-title-row">
                      <Icon size={15} strokeWidth={1.75} className="ops-card-icon" />
                      <span className="report-card-title">{item.title}</span>
                      <span className="badge badge--pending">Preview</span>
                    </div>
                    <span className="report-card-desc">{item.description}</span>
                    <span className="ops-card-api data">{item.api}</span>
                    {item.previewNote && <span className="ops-note">{item.previewNote}</span>}
                  </div>
                </div>
              )
            }

            return (
              <Link key={item.to} to={item.to} className="report-card ops-link-card">
                <div className="report-card-info">
                  <div className="ops-card-title-row">
                    <Icon size={15} strokeWidth={1.75} className="ops-card-icon" />
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
