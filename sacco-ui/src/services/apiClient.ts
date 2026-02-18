import type { ApiResponse } from '../types/api'

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

function isApiEnvelope<T>(value: unknown): value is ApiResponse<T> {
  return typeof value === 'object' && value !== null && 'success' in value
}

function getErrorMessage(payload: unknown, fallback: string) {
  if (typeof payload === 'string' && payload) return payload
  if (typeof payload === 'object' && payload !== null) {
    if ('message' in payload && typeof payload.message === 'string' && payload.message) {
      return payload.message
    }
    if ('error' in payload && typeof payload.error === 'string' && payload.error) {
      return payload.error
    }
  }
  return fallback
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  if (!headers.has('Accept')) headers.set('Accept', 'application/json')
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  const response = await fetch(path, { ...options, headers })
  const rawText = await response.text()

  let payload: unknown = null
  if (rawText) {
    try {
      payload = JSON.parse(rawText)
    } catch {
      payload = rawText
    }
  }

  if (!response.ok) {
    throw new ApiError(
      getErrorMessage(payload, `Request failed with status ${response.status}`),
      response.status,
    )
  }

  if (isApiEnvelope<T>(payload)) {
    if (!payload.success) {
      throw new ApiError(payload.message ?? 'Request failed', response.status)
    }
    return payload.data
  }

  return payload as T
}
