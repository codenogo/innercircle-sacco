import { createContext } from 'react'
import type { AuthSession } from '../types/auth'

export interface AuthContextValue {
  session: AuthSession | null
  isAuthenticated: boolean
  login: (username: string, password: string) => Promise<AuthSession>
  logout: () => void
  refreshSession: () => Promise<AuthSession | null>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
