import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  Gear,
  UserGear,
  Power,
  X,
} from '@phosphor-icons/react'
import { useAuth } from '../hooks/useAuth'
import { useAuthorization } from '../hooks/useAuthorization'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { findSubRoute, navItems } from './navItems'
import './Sidebar.css'

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
  const activeParentTo = findSubRoute(location.pathname)?.parentTo ?? null

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
            <X size={16} />
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
                `sidebar-link ${isActive || activeParentTo === to ? 'sidebar-link--active' : ''}`
              }
              onClick={onClose}
            >
              <Icon size={16} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <hr className="rule" />
          {canAccess(['ADMIN', 'TREASURER', 'MEMBER', 'SECRETARY', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER']) && (
            <NavLink
              to="/settings"
              className={({ isActive }) =>
                `sidebar-link ${isActive ? 'sidebar-link--active' : ''}`
              }
              onClick={onClose}
            >
              <Gear size={16} />
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
            <UserGear size={16} />
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
            <Power size={14} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>
    </>
  )
}
