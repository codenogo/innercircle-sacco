import { useMemo } from 'react'
import type { UserRole } from '../types/roles'
import { useAuth } from './useAuth'
import {
  getDefaultAuthenticatedRoute,
  getRolesFromAccessToken,
  hasAnyRole,
} from '../utils/auth'

export function useAuthorization() {
  const { session } = useAuth()

  const roles = useMemo(
    () => getRolesFromAccessToken(session?.accessToken),
    [session?.accessToken],
  )

  const canAccess = useMemo(
    () => (allowedRoles: UserRole[]) => hasAnyRole(roles, allowedRoles),
    [roles],
  )

  const defaultRoute = useMemo(
    () => getDefaultAuthenticatedRoute(roles),
    [roles],
  )

  const isMemberOnly = useMemo(
    () => hasAnyRole(roles, ['MEMBER']) && !hasAnyRole(roles, ['ADMIN', 'TREASURER']),
    [roles],
  )

  return {
    roles,
    canAccess,
    defaultRoute,
    isMemberOnly,
  }
}

