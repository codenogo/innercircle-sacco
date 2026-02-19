import type { CursorPage } from '../types/users'
import type { PayoutResponse, PayoutRequest, PayoutStatus } from '../types/payouts'
import { apiRequest } from './apiClient'

export async function getPayouts(cursor?: string, size?: number): Promise<CursorPage<PayoutResponse>> {
  const params = new URLSearchParams()
  if (cursor) params.set('cursor', cursor)
  if (size != null) params.set('limit', String(size))
  const query = params.toString()
  return apiRequest<CursorPage<PayoutResponse>>(`/api/v1/payouts${query ? `?${query}` : ''}`)
}

export async function getPayoutsByStatus(status: PayoutStatus, cursor?: string, size?: number): Promise<CursorPage<PayoutResponse>> {
  const params = new URLSearchParams()
  if (cursor) params.set('cursor', cursor)
  if (size != null) params.set('limit', String(size))
  const query = params.toString()
  return apiRequest<CursorPage<PayoutResponse>>(`/api/v1/payouts/status/${status}${query ? `?${query}` : ''}`)
}

export async function getMemberPayouts(memberId: string, cursor?: string, size?: number): Promise<CursorPage<PayoutResponse>> {
  const params = new URLSearchParams()
  if (cursor) params.set('cursor', cursor)
  if (size != null) params.set('limit', String(size))
  const query = params.toString()
  return apiRequest<CursorPage<PayoutResponse>>(`/api/v1/payouts/member/${memberId}${query ? `?${query}` : ''}`)
}

export async function createPayout(payload: PayoutRequest): Promise<PayoutResponse> {
  return apiRequest<PayoutResponse>('/api/v1/payouts', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function approvePayout(id: string, overrideReason?: string): Promise<PayoutResponse> {
  return apiRequest<PayoutResponse>(`/api/v1/payouts/${id}/approve`, {
    method: 'PUT',
    ...(overrideReason ? { body: JSON.stringify({ overrideReason }) } : {}),
  })
}

export async function processPayout(id: string): Promise<PayoutResponse> {
  return apiRequest<PayoutResponse>(`/api/v1/payouts/${id}/process`, {
    method: 'PUT',
  })
}
