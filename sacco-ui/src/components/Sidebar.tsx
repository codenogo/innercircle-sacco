import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard,
  Users,
  Wallet,
  HandCoins,
  Landmark,
  ArrowDownToLine,
  BookOpen,
  BarChart3,
  BriefcaseBusiness,
  Settings,
  UserCog,
  LogOut,
  X,
} from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { useAuthorization } from '../hooks/useAuthorization'
import { useCurrentUser } from '../hooks/useCurrentUser'
import type { UserRole } from '../types/roles'
import './Sidebar.css'

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Overview', allowed: ['ADMIN', 'TREASURER'] as UserRole[] },
  { to: '/members', icon: Users, label: 'Members', allowed: ['ADMIN', 'TREASURER', 'MEMBER'] as UserRole[] },
  { to: '/contributions', icon: Wallet, label: 'Contributions', allowed: ['ADMIN', 'TREASURER', 'MEMBER'] as UserRole[] },
  { to: '/loans', icon: Landmark, label: 'Loans', allowed: ['ADMIN', 'TREASURER', 'MEMBER'] as UserRole[] },
  { to: '/payouts', icon: ArrowDownToLine, label: 'Payouts', allowed: ['ADMIN', 'TREASURER', 'MEMBER'] as UserRole[] },
  { to: '/petty-cash', icon: HandCoins, label: 'Petty Cash', allowed: ['ADMIN', 'TREASURER'] as UserRole[] },
  { to: '/ledger', icon: BookOpen, label: 'Ledger', allowed: ['ADMIN', 'TREASURER'] as UserRole[] },
  { to: '/reports', icon: BarChart3, label: 'Reports', allowed: ['ADMIN', 'TREASURER'] as UserRole[] },
  { to: '/operations', icon: BriefcaseBusiness, label: 'Operations', allowed: ['ADMIN', 'TREASURER'] as UserRole[] },
]

interface SidebarProps {
  open: boolean
  onClose: () => void
}

export function Sidebar({ open, onClose }: SidebarProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const { logout } = useAuth()
  const { canAccess } = useAuthorization()
  const { profile } = useCurrentUser()

  function handleLogout() {
    logout()
    onClose()
    navigate('/login', { replace: true })
  }

  const visibleNavItems = navItems.filter(item => canAccess(item.allowed))

  const fullName = profile?.member
    ? `${profile.member.firstName} ${profile.member.lastName}`.trim()
    : profile?.username ?? 'User'
  const primaryRole = profile?.roles?.[0] ?? 'Member'
  const initials = fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(part => part[0]?.toUpperCase())
    .join('') || 'U'

  return (
    <>
      {open && <div className="sidebar-backdrop" onClick={onClose} />}
      <aside className={`sidebar ${open ? 'sidebar--open' : ''}`}>
        <div className="sidebar-header">
          <div className="sidebar-brand">
            <span className="sidebar-monogram">IC</span>
            <span className="sidebar-title">InnerCircle</span>
          </div>
          <button className="sidebar-close" onClick={onClose} aria-label="Close menu">
            <X size={16} strokeWidth={1.75} />
          </button>
        </div>

        <hr className="rule" />

        <nav className="sidebar-nav">
          {visibleNavItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `sidebar-link ${isActive ? 'sidebar-link--active' : ''}`
              }
              onClick={onClose}
            >
              <Icon size={16} strokeWidth={1.75} />
              <span>{label}</span>
              {to === '/' && location.pathname === '/' && (
                <span className="sidebar-indicator" />
              )}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <hr className="rule" />
          {canAccess(['ADMIN', 'TREASURER', 'MEMBER']) && (
            <NavLink
              to="/settings"
              className={({ isActive }) =>
                `sidebar-link ${isActive ? 'sidebar-link--active' : ''}`
              }
              onClick={onClose}
            >
              <Settings size={16} strokeWidth={1.75} />
              <span>Settings</span>
            </NavLink>
          )}
          <NavLink
            to="/profile"
            className={({ isActive }) =>
              `sidebar-link ${isActive ? 'sidebar-link--active' : ''}`
            }
            onClick={onClose}
          >
            <UserCog size={16} strokeWidth={1.75} />
            <span>My Profile</span>
          </NavLink>

          <div className="sidebar-user">
            <div className="sidebar-avatar">{initials}</div>
            <div className="sidebar-user-info">
              <span className="sidebar-user-name">{fullName}</span>
              <span className="sidebar-user-role">{primaryRole}</span>
            </div>
          </div>
          <button type="button" className="sidebar-logout" onClick={handleLogout}>
            <LogOut size={14} strokeWidth={1.75} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>
    </>
  )
}
