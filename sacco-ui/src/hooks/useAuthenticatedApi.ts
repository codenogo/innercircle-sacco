import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, apiRequest } from '../services/apiClient'
import { useAuth } from './useAuth'

function withAuthHeaders(headersInit: HeadersInit | undefined, token: string) {
  const headers = new Headers(headersInit)
  headers.set('Authorization', `Bearer ${token}`)
  return headers
}

export function useAuthenticatedApi() {
  const navigate = useNavigate()
  const { session, refreshSession, logout } = useAuth()

  const request = useCallback(async <T>(
    path: string,
    options: RequestInit = {},
  ): Promise<T> => {
    const token = session?.accessToken
    if (!token) {
      logout()
      navigate('/login', { replace: true })
      throw new ApiError('Not authenticated', 401)
    }

    try {
      return await apiRequest<T>(path, {
        ...options,
        headers: withAuthHeaders(options.headers, token),
      })
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) {
        throw error
      }

      const refreshed = await refreshSession()
      if (!refreshed?.accessToken) {
        logout()
        navigate('/login', { replace: true })
        throw error
      }

      return apiRequest<T>(path, {
        ...options,
        headers: withAuthHeaders(options.headers, refreshed.accessToken),
      })
    }
  }, [logout, navigate, refreshSession, session?.accessToken])

  const requestBlob = useCallback(async (
    path: string,
    options: RequestInit = {},
  ): Promise<Blob> => {
    const token = session?.accessToken
    if (!token) {
      logout()
      navigate('/login', { replace: true })
      throw new ApiError('Not authenticated', 401)
    }

    const attempt = async (t: string) => {
      const response = await fetch(path, {
        ...options,
        headers: withAuthHeaders(options.headers, t),
      })
      if (!response.ok) {
        throw new ApiError(`Request failed with status ${response.status}`, response.status)
      }
      return response.blob()
    }

    try {
      return await attempt(token)
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) {
        throw error
      }

      const refreshed = await refreshSession()
      if (!refreshed?.accessToken) {
        logout()
        navigate('/login', { replace: true })
        throw error
      }

      return attempt(refreshed.accessToken)
    }
  }, [logout, navigate, refreshSession, session?.accessToken])

  return { request, requestBlob }
}
