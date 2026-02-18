import { apiRequest } from './apiClient'
import type { CursorPage } from '../types/users'
import type {
  ContributionResponse,
  ContributionSummaryResponse,
  RecordContributionRequest,
  BulkContributionRequest,
  ContributionCategoryResponse,
} from '../types/contributions'

export async function getContributions(cursor?: string, size = 50): Promise<CursorPage<ContributionResponse>> {
  const params = new URLSearchParams({ size: String(size) })
  if (cursor) params.set('cursor', cursor)
  return apiRequest<CursorPage<ContributionResponse>>(`/api/v1/contributions?${params}`)
}

export async function getMemberContributions(memberId: string, cursor?: string, size = 50): Promise<CursorPage<ContributionResponse>> {
  const params = new URLSearchParams({ size: String(size) })
  if (cursor) params.set('cursor', cursor)
  return apiRequest<CursorPage<ContributionResponse>>(`/api/v1/contributions/member/${memberId}?${params}`)
}

export async function getMemberContributionSummary(memberId: string): Promise<ContributionSummaryResponse> {
  return apiRequest<ContributionSummaryResponse>(`/api/v1/contributions/member/${memberId}/summary`)
}

export async function recordContribution(payload: RecordContributionRequest): Promise<ContributionResponse> {
  return apiRequest<ContributionResponse>('/api/v1/contributions', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function recordBulkContributions(payload: BulkContributionRequest): Promise<ContributionResponse[]> {
  return apiRequest<ContributionResponse[]>('/api/v1/contributions/bulk', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function confirmContribution(id: string): Promise<ContributionResponse> {
  return apiRequest<ContributionResponse>(`/api/v1/contributions/${id}/confirm`, {
    method: 'PATCH',
  })
}

export async function reverseContribution(id: string): Promise<ContributionResponse> {
  return apiRequest<ContributionResponse>(`/api/v1/contributions/${id}/reverse`, {
    method: 'PATCH',
  })
}

export async function getCategories(activeOnly = true): Promise<ContributionCategoryResponse[]> {
  const params = new URLSearchParams({ activeOnly: String(activeOnly) })
  return apiRequest<ContributionCategoryResponse[]>(`/api/v1/contribution-categories?${params}`)
}
