import type { AuthSession } from '../types/auth'
import { ApiError } from '../services/apiClient'

interface RequestWithAuthRetryArgs<T> {
  getAccessToken: () => string | null
  execute: (accessToken: string) => Promise<T>
  refreshSession: () => Promise<AuthSession | null>
  onUnauthenticated: () => void
}

export async function requestWithAuthRetry<T>({
  getAccessToken,
  execute,
  refreshSession,
  onUnauthenticated,
}: RequestWithAuthRetryArgs<T>): Promise<T> {
  const token = getAccessToken()
  if (!token) {
    onUnauthenticated()
    throw new ApiError('Not authenticated', 401)
  }

  try {
    return await execute(token)
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) {
      throw error
    }

    const refreshed = await refreshSession()
    if (!refreshed?.accessToken) {
      onUnauthenticated()
      throw error
    }

    try {
      return await execute(refreshed.accessToken)
    } catch (retryError) {
      if (retryError instanceof ApiError && retryError.status === 401) {
        onUnauthenticated()
      }
      throw retryError
    }
  }
}
