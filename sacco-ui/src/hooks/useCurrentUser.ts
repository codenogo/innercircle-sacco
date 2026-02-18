import { useCallback, useEffect, useState } from 'react'
import type { MeResponse } from '../types/auth'
import { useAuthenticatedApi } from './useAuthenticatedApi'
import { useAuth } from './useAuth'

interface CurrentUserState {
  profile: MeResponse | null
  loading: boolean
  error: string
  refresh: () => Promise<void>
}

export function useCurrentUser(): CurrentUserState {
  const { request } = useAuthenticatedApi()
  const { isAuthenticated } = useAuth()

  const [profile, setProfile] = useState<MeResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    if (!isAuthenticated) {
      setProfile(null)
      setLoading(false)
      setError('')
      return
    }

    setLoading(true)
    setError('')
    try {
      const me = await request<MeResponse>('/api/v1/me')
      setProfile(me)
    } catch (loadError) {
      setProfile(null)
      if (loadError instanceof Error) setError(loadError.message)
      else setError('Unable to load current user.')
    } finally {
      setLoading(false)
    }
  }, [isAuthenticated, request])

  useEffect(() => {
    void load()
  }, [load])

  return {
    profile,
    loading,
    error,
    refresh: load,
  }
}

