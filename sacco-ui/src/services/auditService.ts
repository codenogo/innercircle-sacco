import { apiRequest } from './apiClient'
import type { CursorPage } from '../types/users'
import type { AuditEventResponse, AuditAction, AuditEntityType } from '../types/audit'

export interface AuditQueryParams {
  cursor?: string
  entityType?: AuditEntityType
  entityId?: string
  actor?: string
  action?: AuditAction
  startDate?: string
  endDate?: string
  limit?: number
}

export async function getAuditEvents(params: AuditQueryParams = {}): Promise<CursorPage<AuditEventResponse>> {
  const searchParams = new URLSearchParams()
  if (params.cursor) searchParams.set('cursor', params.cursor)
  if (params.entityType) searchParams.set('entityType', params.entityType)
  if (params.entityId) searchParams.set('entityId', params.entityId)
  if (params.actor) searchParams.set('actor', params.actor)
  if (params.action) searchParams.set('action', params.action)
  if (params.startDate) searchParams.set('startDate', params.startDate)
  if (params.endDate) searchParams.set('endDate', params.endDate)
  if (params.limit != null) searchParams.set('limit', String(params.limit))
  const query = searchParams.toString()
  return apiRequest<CursorPage<AuditEventResponse>>(`/api/v1/audit${query ? `?${query}` : ''}`)
}

export async function getEntityHistory(
  entityType: AuditEntityType,
  entityId: string,
  cursor?: string,
  limit?: number,
): Promise<CursorPage<AuditEventResponse>> {
  const params = new URLSearchParams()
  if (cursor) params.set('cursor', cursor)
  if (limit != null) params.set('limit', String(limit))
  const query = params.toString()
  return apiRequest<CursorPage<AuditEventResponse>>(
    `/api/v1/audit/${entityType}/${entityId}${query ? `?${query}` : ''}`,
  )
}

