import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, apiRequest } from '../services/apiClient'
import { requestWithAuthRetry } from './authenticatedRequest'
import { useAuth } from './useAuth'

function withAuthHeaders(headersInit: HeadersInit | undefined, token: string) {
  const headers = new Headers(headersInit)
  headers.set('Authorization', `Bearer ${token}`)
  return headers
}

export function useAuthenticatedApi() {
  const navigate = useNavigate()
  const { session, refreshSession, logout } = useAuth()
  const handleUnauthenticated = useCallback(() => {
    logout()
    navigate('/login', { replace: true })
  }, [logout, navigate])

  const request = useCallback(async <T>(
    path: string,
    options: RequestInit = {},
  ): Promise<T> => {
    return requestWithAuthRetry<T>({
      getAccessToken: () => session?.accessToken ?? null,
      execute: (token) => apiRequest<T>(path, {
        ...options,
        headers: withAuthHeaders(options.headers, token),
      }),
      refreshSession,
      onUnauthenticated: handleUnauthenticated,
    })
  }, [handleUnauthenticated, refreshSession, session?.accessToken])

  const requestBlob = useCallback(async (
    path: string,
    options: RequestInit = {},
  ): Promise<Blob> => {
    return requestWithAuthRetry<Blob>({
      getAccessToken: () => session?.accessToken ?? null,
      execute: async (token) => {
        const response = await fetch(path, {
          ...options,
          headers: withAuthHeaders(options.headers, token),
        })
        if (!response.ok) {
          throw new ApiError(`Request failed with status ${response.status}`, response.status)
        }
        return response.blob()
      },
      refreshSession,
      onUnauthenticated: handleUnauthenticated,
    })
  }, [handleUnauthenticated, refreshSession, session?.accessToken])

  return { request, requestBlob }
}
