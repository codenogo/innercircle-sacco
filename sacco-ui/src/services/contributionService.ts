import { apiRequest } from './apiClient'
import type { CursorPage } from '../types/users'
import type {
  ContributionResponse,
  ContributionSummaryResponse,
  RecordContributionRequest,
  BulkContributionRequest,
  ContributionCategoryResponse,
  ContributionCategoryRequest,
} from '../types/contributions'

export type AuthenticatedRequest = <T>(path: string, options?: RequestInit) => Promise<T>

function callApi<T>(
  path: string,
  options: RequestInit | undefined,
  request?: AuthenticatedRequest,
): Promise<T> {
  if (request) return request<T>(path, options)
  return apiRequest<T>(path, options)
}

export async function getContributions(
  cursor?: string,
  size = 50,
  request?: AuthenticatedRequest,
): Promise<CursorPage<ContributionResponse>> {
  const params = new URLSearchParams({ size: String(size) })
  if (cursor) params.set('cursor', cursor)
  return callApi<CursorPage<ContributionResponse>>(`/api/v1/contributions?${params}`, undefined, request)
}

export async function getMemberContributions(
  memberId: string,
  cursor?: string,
  size = 50,
  request?: AuthenticatedRequest,
): Promise<CursorPage<ContributionResponse>> {
  const params = new URLSearchParams({ size: String(size) })
  if (cursor) params.set('cursor', cursor)
  return callApi<CursorPage<ContributionResponse>>(
    `/api/v1/contributions/member/${memberId}?${params}`,
    undefined,
    request,
  )
}

export async function getMemberContributionSummary(
  memberId: string,
  request?: AuthenticatedRequest,
): Promise<ContributionSummaryResponse> {
  return callApi<ContributionSummaryResponse>(
    `/api/v1/contributions/member/${memberId}/summary`,
    undefined,
    request,
  )
}

export async function recordContribution(
  payload: RecordContributionRequest,
  request?: AuthenticatedRequest,
): Promise<ContributionResponse> {
  return callApi<ContributionResponse>('/api/v1/contributions', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function recordBulkContributions(
  payload: BulkContributionRequest,
  request?: AuthenticatedRequest,
): Promise<ContributionResponse[]> {
  return callApi<ContributionResponse[]>('/api/v1/contributions/bulk', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function confirmContribution(id: string, request?: AuthenticatedRequest): Promise<ContributionResponse> {
  return callApi<ContributionResponse>(`/api/v1/contributions/${id}/confirm`, {
    method: 'PATCH',
  }, request)
}

export async function reverseContribution(id: string, request?: AuthenticatedRequest): Promise<ContributionResponse> {
  return callApi<ContributionResponse>(`/api/v1/contributions/${id}/reverse`, {
    method: 'PATCH',
  }, request)
}

export async function getCategories(
  activeOnly = true,
  request?: AuthenticatedRequest,
): Promise<ContributionCategoryResponse[]> {
  const params = new URLSearchParams({ activeOnly: String(activeOnly) })
  return callApi<ContributionCategoryResponse[]>(`/api/v1/contribution-categories?${params}`, undefined, request)
}

export async function createCategory(
  payload: ContributionCategoryRequest,
  request?: AuthenticatedRequest,
): Promise<ContributionCategoryResponse> {
  return callApi<ContributionCategoryResponse>('/api/v1/contribution-categories', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function updateCategory(
  id: string,
  payload: ContributionCategoryRequest,
  request?: AuthenticatedRequest,
): Promise<ContributionCategoryResponse> {
  return callApi<ContributionCategoryResponse>(`/api/v1/contribution-categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, request)
}

export async function deleteCategory(id: string, request?: AuthenticatedRequest): Promise<void> {
  return callApi<void>(`/api/v1/contribution-categories/${id}`, {
    method: 'DELETE',
  }, request)
}
