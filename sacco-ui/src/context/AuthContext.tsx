import { useMemo, useState, type ReactNode } from 'react'
import type { AuthSession } from '../types/auth'
import {
  clearSession as clearStoredSession,
  getStoredSession,
  login as loginRequest,
  refreshSession as refreshSessionRequest,
  storeSession,
} from '../services/authService'
import { AuthContext, type AuthContextValue } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => getStoredSession())

  const value = useMemo<AuthContextValue>(() => ({
    session,
    isAuthenticated: Boolean(session?.accessToken),
    async login(username: string, password: string) {
      const nextSession = await loginRequest(username, password)
      setSession(nextSession)
      return nextSession
    },
    logout() {
      clearStoredSession()
      setSession(null)
    },
    async refreshSession() {
      if (!session?.refreshToken) {
        clearStoredSession()
        setSession(null)
        return null
      }

      try {
        const nextSession = await refreshSessionRequest(session.refreshToken)
        storeSession(nextSession)
        setSession(nextSession)
        return nextSession
      } catch {
        clearStoredSession()
        setSession(null)
        return null
      }
    },
  }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
