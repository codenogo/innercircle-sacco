import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import type { AuthSession } from '../types/auth'
import {
  AUTH_STORAGE_KEY,
  clearSession as clearStoredSession,
  getStoredSession,
  login as loginRequest,
  refreshSession as refreshSessionRequest,
  storeSession,
} from '../services/authService'
import { AuthContext, type AuthContextValue } from './auth-context'
import { createRefreshCoordinator } from './refreshCoordinator'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => getStoredSession())
  const sessionRef = useRef<AuthSession | null>(session)

  const setCurrentSession = useCallback((nextSession: AuthSession | null) => {
    sessionRef.current = nextSession
    setSession(nextSession)
  }, [])

  const refreshCoordinatorRef = useRef(
    createRefreshCoordinator({
      getCurrentSession: () => sessionRef.current,
      setCurrentSession,
      getStoredSession,
      storeSession,
      clearStoredSession,
      refreshSessionRequest,
    }),
  )

  useEffect(() => {
    const handleStorage = (event: StorageEvent) => {
      if (event.storageArea !== window.localStorage || event.key !== AUTH_STORAGE_KEY) return
      refreshCoordinatorRef.current.syncFromStorageValue(event.newValue)
    }

    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const nextSession = await loginRequest(username, password)
    setCurrentSession(nextSession)
    return nextSession
  }, [setCurrentSession])

  const logout = useCallback(() => {
    clearStoredSession()
    setCurrentSession(null)
  }, [setCurrentSession])

  const refreshSession = useCallback(() => (
    refreshCoordinatorRef.current.refreshSession()
  ), [])

  const value = useMemo<AuthContextValue>(() => ({
    session,
    isAuthenticated: Boolean(session?.accessToken),
    login,
    logout,
    refreshSession,
  }), [login, logout, refreshSession, session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
