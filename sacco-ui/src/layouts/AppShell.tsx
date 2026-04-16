import { useEffect, useMemo, useRef, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Sidebar } from '../components/Sidebar'
import { findNavItem, findSubRoute } from '../components/navItems'
import { List } from '@phosphor-icons/react'
import './AppShell.css'

const footerRoutes: Record<string, string> = {
  '/settings': 'Settings',
  '/profile': 'My Profile',
}

export function AppShell() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const mainRef = useRef<HTMLElement>(null)
  const location = useLocation()

  useEffect(() => {
    const heading = mainRef.current?.querySelector<HTMLElement>('.page-title')
    if (heading) {
      if (!heading.hasAttribute('tabindex')) heading.setAttribute('tabindex', '-1')
      heading.focus({ preventScroll: false })
    }
  }, [location.pathname])

  useEffect(() => {
    setSidebarOpen(false)
  }, [location.pathname])

  const currentPageLabel = useMemo(() => {
    if (footerRoutes[location.pathname]) {
      return footerRoutes[location.pathname]
    }

    const subRoute = findSubRoute(location.pathname)
    if (subRoute) {
      return subRoute.label
    }

    return findNavItem(location.pathname)?.label ?? ''
  }, [location.pathname])

  return (
    <div className="app-shell">
      <div className="mobile-header">
        <button className="mobile-menu-btn" onClick={() => setSidebarOpen(true)} aria-label="Open menu">
          <List size={18} />
        </button>
        <div className="mobile-brand">
          <span className="mobile-monogram">IC</span>
          <span className="mobile-title">{currentPageLabel || 'InnerCircle'}</span>
        </div>
      </div>
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <main className="app-main" ref={mainRef}>
        <Outlet />
      </main>
    </div>
  )
}
