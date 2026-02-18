import type { ReactElement } from 'react'
import { Navigate } from 'react-router-dom'
import type { UserRole } from '../types/roles'
import { useAuthorization } from '../hooks/useAuthorization'

interface RequireRoleProps {
  allowed: UserRole[]
  children: ReactElement
}

export function RequireRole({ allowed, children }: RequireRoleProps) {
  const { canAccess, defaultRoute } = useAuthorization()

  if (!canAccess(allowed)) {
    return <Navigate to={defaultRoute} replace />
  }

  return children
}

