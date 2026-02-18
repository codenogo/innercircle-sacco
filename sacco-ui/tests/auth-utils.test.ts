import { describe, expect, it } from 'vitest'
import {
  getDefaultAuthenticatedRoute,
  getRolesFromAccessToken,
  hasAnyRole,
  validatePasswordConfirmation,
} from '../src/utils/auth'

describe('validatePasswordConfirmation', () => {
  it('returns an error when passwords do not match', () => {
    expect(validatePasswordConfirmation('password123', 'password124')).toBe('Passwords do not match.')
  })

  it('returns null when passwords match', () => {
    expect(validatePasswordConfirmation('password123', 'password123')).toBeNull()
  })
})

describe('auth role helpers', () => {
  function tokenWithPayload(payload: Record<string, unknown>): string {
    const base64 = btoa(JSON.stringify(payload))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/g, '')
    return `h.${base64}.s`
  }

  it('extracts normalized roles from claims', () => {
    const token = tokenWithPayload({
      roles: ['ADMIN'],
      authorities: ['ROLE_MEMBER'],
    })

    expect(getRolesFromAccessToken(token).sort()).toEqual(['ADMIN', 'MEMBER'])
  })

  it('checks allowed roles', () => {
    expect(hasAnyRole(['MEMBER'], ['ADMIN'])).toBe(false)
    expect(hasAnyRole(['MEMBER'], ['MEMBER', 'ADMIN'])).toBe(true)
  })

  it('resolves default route by role', () => {
    expect(getDefaultAuthenticatedRoute(['ADMIN'])).toBe('/')
    expect(getDefaultAuthenticatedRoute(['TREASURER'])).toBe('/')
    expect(getDefaultAuthenticatedRoute(['MEMBER'])).toBe('/profile')
  })
})
