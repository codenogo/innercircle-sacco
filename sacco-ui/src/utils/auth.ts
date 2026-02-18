import type { UserRole } from '../types/roles'

interface JwtClaims {
  roles?: unknown
  authorities?: unknown
}

const KNOWN_ROLES: UserRole[] = ['ADMIN', 'TREASURER', 'SECRETARY', 'MEMBER']

export function validatePasswordConfirmation(password: string, confirmPassword: string): string | null {
  if (!password || !confirmPassword) return null
  if (password !== confirmPassword) return 'Passwords do not match.'
  return null
}

export function decodeJwtClaims(token: string): JwtClaims | null {
  const segments = token.split('.')
  if (segments.length < 2) return null

  try {
    const payload = segments[1]
      .replace(/-/g, '+')
      .replace(/_/g, '/')
    const padded = payload.padEnd(Math.ceil(payload.length / 4) * 4, '=')
    const json = atob(padded)
    const parsed = JSON.parse(json)

    if (typeof parsed !== 'object' || parsed === null) return null
    return parsed as JwtClaims
  } catch {
    return null
  }
}

function normalizeRole(value: string): UserRole | null {
  const normalized = value.startsWith('ROLE_') ? value.slice(5) : value
  return (KNOWN_ROLES as string[]).includes(normalized) ? (normalized as UserRole) : null
}

export function getRolesFromAccessToken(token?: string | null): UserRole[] {
  if (!token) return []
  const claims = decodeJwtClaims(token)
  if (!claims) return []

  const values = new Set<string>()

  if (Array.isArray(claims.roles)) {
    claims.roles.forEach(role => {
      if (typeof role === 'string') values.add(role)
    })
  }

  if (Array.isArray(claims.authorities)) {
    claims.authorities.forEach(authority => {
      if (typeof authority === 'string') values.add(authority)
    })
  }

  const resolved = new Set<UserRole>()
  values.forEach(value => {
    const role = normalizeRole(value)
    if (role) resolved.add(role)
  })

  return Array.from(resolved)
}

export function hasAnyRole(userRoles: UserRole[], allowedRoles: UserRole[]): boolean {
  if (allowedRoles.length === 0) return true
  const roleSet = new Set(userRoles)
  return allowedRoles.some(role => roleSet.has(role))
}

export function getDefaultAuthenticatedRoute(userRoles: UserRole[]): string {
  if (hasAnyRole(userRoles, ['ADMIN', 'TREASURER'])) return '/'
  return '/profile'
}
