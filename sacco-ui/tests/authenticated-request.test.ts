import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../src/services/apiClient'
import { requestWithAuthRetry } from '../src/hooks/authenticatedRequest'
import type { AuthSession } from '../src/types/auth'

function makeSession(accessToken: string, refreshToken: string): AuthSession {
  return {
    accessToken,
    refreshToken,
    tokenType: 'Bearer',
    expiresIn: 3600,
  }
}

describe('requestWithAuthRetry', () => {
  it('retries once after a 401 with the refreshed access token', async () => {
    const refreshedSession = makeSession('access-2', 'refresh-2')
    const execute = vi.fn(async (token: string) => token)
    execute
      .mockRejectedValueOnce(new ApiError('Unauthorized', 401))
      .mockResolvedValueOnce('ok')
    const refreshSession = vi.fn(async () => refreshedSession)
    const onUnauthenticated = vi.fn()

    const result = await requestWithAuthRetry({
      getAccessToken: () => 'access-1',
      execute,
      refreshSession,
      onUnauthenticated,
    })

    expect(result).toBe('ok')
    expect(refreshSession).toHaveBeenCalledTimes(1)
    expect(execute).toHaveBeenCalledTimes(2)
    expect(execute).toHaveBeenNthCalledWith(1, 'access-1')
    expect(execute).toHaveBeenNthCalledWith(2, 'access-2')
    expect(onUnauthenticated).not.toHaveBeenCalled()
  })

  it('marks user unauthenticated when refresh fails', async () => {
    const execute = vi.fn(async (token: string) => token)
    execute.mockRejectedValueOnce(new ApiError('Unauthorized', 401))
    const refreshSession = vi.fn(async () => null)
    const onUnauthenticated = vi.fn()

    await expect(
      requestWithAuthRetry({
        getAccessToken: () => 'access-1',
        execute,
        refreshSession,
        onUnauthenticated,
      }),
    ).rejects.toBeInstanceOf(ApiError)

    expect(refreshSession).toHaveBeenCalledTimes(1)
    expect(execute).toHaveBeenCalledTimes(1)
    expect(onUnauthenticated).toHaveBeenCalledTimes(1)
  })

  it('does not trigger a second refresh attempt when retry also returns 401', async () => {
    const refreshedSession = makeSession('access-2', 'refresh-2')
    const execute = vi.fn(async (token: string) => token)
    execute
      .mockRejectedValueOnce(new ApiError('Unauthorized', 401))
      .mockRejectedValueOnce(new ApiError('Unauthorized', 401))
    const refreshSession = vi.fn(async () => refreshedSession)
    const onUnauthenticated = vi.fn()

    await expect(
      requestWithAuthRetry({
        getAccessToken: () => 'access-1',
        execute,
        refreshSession,
        onUnauthenticated,
      }),
    ).rejects.toBeInstanceOf(ApiError)

    expect(refreshSession).toHaveBeenCalledTimes(1)
    expect(execute).toHaveBeenCalledTimes(2)
    expect(onUnauthenticated).toHaveBeenCalledTimes(1)
  })
})
