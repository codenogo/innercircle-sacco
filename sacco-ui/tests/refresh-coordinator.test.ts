import { describe, expect, it, vi } from 'vitest'
import type { AuthSession } from '../src/types/auth'
import { createRefreshCoordinator, parseSessionFromStorageValue } from '../src/context/refreshCoordinator'

function makeSession(accessToken: string, refreshToken: string): AuthSession {
  return {
    accessToken,
    refreshToken,
    tokenType: 'Bearer',
    expiresIn: 3600,
  }
}

describe('refreshCoordinator', () => {
  it('runs refresh as single-flight for concurrent calls', async () => {
    let currentSession: AuthSession | null = makeSession('access-1', 'refresh-1')
    let storedSession: AuthSession | null = currentSession
    const nextSession = makeSession('access-2', 'refresh-2')

    const refreshSessionRequest = vi.fn(async () => nextSession)
    const coordinator = createRefreshCoordinator({
      getCurrentSession: () => currentSession,
      setCurrentSession: (session) => {
        currentSession = session
      },
      getStoredSession: () => storedSession,
      storeSession: (session) => {
        storedSession = session
      },
      clearStoredSession: () => {
        storedSession = null
      },
      refreshSessionRequest,
    })

    const [first, second] = await Promise.all([
      coordinator.refreshSession(),
      coordinator.refreshSession(),
    ])

    expect(refreshSessionRequest).toHaveBeenCalledTimes(1)
    expect(first).toEqual(nextSession)
    expect(second).toEqual(nextSession)
    expect(currentSession).toEqual(nextSession)
    expect(storedSession).toEqual(nextSession)
  })

  it('adopts newer stored session when refresh fails', async () => {
    const activeSession = makeSession('access-1', 'refresh-1')
    const newerStoredSession = makeSession('access-2', 'refresh-2')
    let currentSession: AuthSession | null = activeSession
    let storedSession: AuthSession | null = activeSession
    const clearStoredSession = vi.fn(() => {
      storedSession = null
    })

    const refreshSessionRequest = vi.fn(async () => {
      storedSession = newerStoredSession
      throw new Error('invalid refresh')
    })

    const coordinator = createRefreshCoordinator({
      getCurrentSession: () => currentSession,
      setCurrentSession: (session) => {
        currentSession = session
      },
      getStoredSession: () => storedSession,
      storeSession: (session) => {
        storedSession = session
      },
      clearStoredSession,
      refreshSessionRequest,
    })

    const result = await coordinator.refreshSession()

    expect(result).toEqual(newerStoredSession)
    expect(currentSession).toEqual(newerStoredSession)
    expect(clearStoredSession).not.toHaveBeenCalled()
  })

  it('clears session when refresh fails and no newer storage value exists', async () => {
    const activeSession = makeSession('access-1', 'refresh-1')
    let currentSession: AuthSession | null = activeSession
    let storedSession: AuthSession | null = activeSession
    const clearStoredSession = vi.fn(() => {
      storedSession = null
    })

    const coordinator = createRefreshCoordinator({
      getCurrentSession: () => currentSession,
      setCurrentSession: (session) => {
        currentSession = session
      },
      getStoredSession: () => storedSession,
      storeSession: (session) => {
        storedSession = session
      },
      clearStoredSession,
      refreshSessionRequest: vi.fn(async () => {
        throw new Error('invalid refresh')
      }),
    })

    const result = await coordinator.refreshSession()

    expect(result).toBeNull()
    expect(currentSession).toBeNull()
    expect(storedSession).toBeNull()
    expect(clearStoredSession).toHaveBeenCalledTimes(1)
  })

  it('syncs state from storage payload and clears state on null payload', () => {
    let currentSession: AuthSession | null = makeSession('access-1', 'refresh-1')
    let storedSession: AuthSession | null = currentSession
    const coordinator = createRefreshCoordinator({
      getCurrentSession: () => currentSession,
      setCurrentSession: (session) => {
        currentSession = session
      },
      getStoredSession: () => storedSession,
      storeSession: (session) => {
        storedSession = session
      },
      clearStoredSession: () => {
        storedSession = null
      },
      refreshSessionRequest: vi.fn(),
    })

    const nextSession = makeSession('access-2', 'refresh-2')
    const synced = coordinator.syncFromStorageValue(JSON.stringify(nextSession))
    expect(synced).toEqual(nextSession)
    expect(currentSession).toEqual(nextSession)

    const cleared = coordinator.syncFromStorageValue(null)
    expect(cleared).toBeNull()
    expect(currentSession).toBeNull()
  })
})

describe('parseSessionFromStorageValue', () => {
  it('returns null for invalid payload values', () => {
    expect(parseSessionFromStorageValue('')).toBeNull()
    expect(parseSessionFromStorageValue('bad-json')).toBeNull()
    expect(parseSessionFromStorageValue(JSON.stringify({ accessToken: 'x' }))).toBeNull()
  })
})
