import type { AuthSession } from '../types/auth'

interface RefreshCoordinatorDeps {
  getCurrentSession: () => AuthSession | null
  setCurrentSession: (session: AuthSession | null) => void
  getStoredSession: () => AuthSession | null
  storeSession: (session: AuthSession) => void
  clearStoredSession: () => void
  refreshSessionRequest: (refreshToken: string) => Promise<AuthSession>
}

function isValidSession(session: AuthSession | null | undefined): session is AuthSession {
  return Boolean(session?.accessToken && session?.refreshToken)
}

export function parseSessionFromStorageValue(rawValue: string | null): AuthSession | null {
  if (!rawValue) return null

  try {
    const parsed = JSON.parse(rawValue) as AuthSession
    return isValidSession(parsed) ? parsed : null
  } catch {
    return null
  }
}

export function createRefreshCoordinator(deps: RefreshCoordinatorDeps) {
  let refreshInFlight: Promise<AuthSession | null> | null = null

  const persistAndSet = (nextSession: AuthSession) => {
    deps.storeSession(nextSession)
    deps.setCurrentSession(nextSession)
  }

  const clearAndSet = () => {
    deps.clearStoredSession()
    deps.setCurrentSession(null)
  }

  return {
    async refreshSession(): Promise<AuthSession | null> {
      if (refreshInFlight) return refreshInFlight

      refreshInFlight = (async () => {
        const activeSession = deps.getCurrentSession() ?? deps.getStoredSession()
        if (!isValidSession(activeSession)) {
          clearAndSet()
          return null
        }

        try {
          const nextSession = await deps.refreshSessionRequest(activeSession.refreshToken)
          persistAndSet(nextSession)
          return nextSession
        } catch {
          const latestStored = deps.getStoredSession()
          if (
            isValidSession(latestStored) &&
            (latestStored.refreshToken !== activeSession.refreshToken ||
              latestStored.accessToken !== activeSession.accessToken)
          ) {
            deps.setCurrentSession(latestStored)
            return latestStored
          }

          clearAndSet()
          return null
        } finally {
          refreshInFlight = null
        }
      })()

      return refreshInFlight
    },

    syncFromStorageValue(rawValue: string | null): AuthSession | null {
      const nextSession = parseSessionFromStorageValue(rawValue)
      deps.setCurrentSession(nextSession)
      return nextSession
    },
  }
}
