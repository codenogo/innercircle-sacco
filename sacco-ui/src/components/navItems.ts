import {
  SquaresFour,
  Users,
  Wallet,
  HandCoins,
  Bank,
  ArrowLineDown,
  TrendUp,
  CalendarCheck,
  Heartbeat,
  SignOut,
  BookOpen,
  ChartBar,
  Briefcase,
} from '@phosphor-icons/react'
import type { UserRole } from '../types/roles'

export interface NavItem {
  to: string
  icon: typeof SquaresFour
  label: string
  allowed: UserRole[]
}

export interface SubRoute {
  path: string
  parentTo: string
  label: string
}

export const navItems: NavItem[] = [
  { to: '/', icon: SquaresFour, label: 'Overview', allowed: ['ADMIN', 'TREASURER', 'MEMBER', 'SECRETARY', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'] },
  { to: '/members', icon: Users, label: 'Members', allowed: ['ADMIN', 'TREASURER', 'MEMBER'] },
  { to: '/contributions', icon: Wallet, label: 'Contributions', allowed: ['ADMIN', 'TREASURER', 'MEMBER'] },
  { to: '/loans', icon: Bank, label: 'Loans', allowed: ['ADMIN', 'TREASURER', 'MEMBER'] },
  { to: '/payouts', icon: ArrowLineDown, label: 'Payouts', allowed: ['ADMIN', 'TREASURER', 'MEMBER'] },
  { to: '/petty-cash', icon: HandCoins, label: 'Petty Cash', allowed: ['ADMIN', 'TREASURER'] },
  { to: '/investments', icon: TrendUp, label: 'Investments', allowed: ['ADMIN', 'TREASURER', 'MEMBER'] },
  { to: '/meetings-fines', icon: CalendarCheck, label: 'Meetings', allowed: ['ADMIN', 'TREASURER', 'SECRETARY', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'] },
  { to: '/welfare-claims', icon: Heartbeat, label: 'Welfare', allowed: ['ADMIN', 'TREASURER', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'] },
  { to: '/member-exit', icon: SignOut, label: 'Member Exit', allowed: ['ADMIN', 'TREASURER', 'MEMBER', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'] },
  { to: '/ledger', icon: BookOpen, label: 'Ledger', allowed: ['ADMIN', 'TREASURER'] },
  { to: '/reports', icon: ChartBar, label: 'Reports', allowed: ['ADMIN', 'TREASURER'] },
  { to: '/operations', icon: Briefcase, label: 'Operations', allowed: ['ADMIN', 'TREASURER', 'SECRETARY', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER'] },
]

export const subRoutes: SubRoute[] = [
  { path: '/members/', parentTo: '/members', label: 'Member Profile' },
  { path: '/contribution-categories', parentTo: '/contributions', label: 'Contribution Categories' },
  { path: '/contribution-ops', parentTo: '/contributions', label: 'Contribution Ops' },
  { path: '/loan-workflow', parentTo: '/loans', label: 'Loan Workflow' },
  { path: '/loan-batch', parentTo: '/loans', label: 'Loan Batch' },
  { path: '/loan-benefits', parentTo: '/loans', label: 'Loan Benefits' },
  { path: '/payout-ops', parentTo: '/payouts', label: 'Payout Ops' },
  { path: '/investments/', parentTo: '/investments', label: 'Investment Detail' },
  { path: '/investment-ops', parentTo: '/investments', label: 'Investment Ops' },
  { path: '/ledger-statements', parentTo: '/ledger', label: 'Ledger Statements' },
  { path: '/export-center', parentTo: '/reports', label: 'Export Center' },
  { path: '/users-admin', parentTo: '/operations', label: 'Users Admin' },
  { path: '/role-dashboards', parentTo: '/operations', label: 'Role Dashboards' },
  { path: '/system-config', parentTo: '/operations', label: 'System Config' },
  { path: '/audit-trail', parentTo: '/operations', label: 'Audit Trail' },
]

export function findSubRoute(pathname: string): SubRoute | undefined {
  return subRoutes.find(subRoute =>
    subRoute.path.endsWith('/')
      ? pathname.startsWith(subRoute.path)
      : pathname === subRoute.path
  )
}

export function findNavItem(pathname: string): NavItem | undefined {
  return [...navItems]
    .filter(item => (item.to === '/' ? pathname === '/' : pathname.startsWith(item.to)))
    .sort((left, right) => right.to.length - left.to.length)[0]
}
