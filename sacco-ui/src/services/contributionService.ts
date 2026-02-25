import { apiRequest } from './apiClient'
import type { CursorPage } from '../types/users'
import type {
  ContributionResponse,
  ContributionSummaryResponse,
  RecordContributionRequest,
  BulkContributionRequest,
  ContributionCategoryResponse,
  ContributionCategoryRequest,
  ContributionWelfarePolicyResponse,
} from '../types/contributions'

export type AuthenticatedRequest = <T>(path: string, options?: RequestInit) => Promise<T>

function normalizeContributionMonth(contributionMonth: string): string {
  return contributionMonth.length === 7 ? `${contributionMonth}-01` : contributionMonth
}

function callApi<T>(
  path: string,
  options: RequestInit | undefined,
  request?: AuthenticatedRequest,
): Promise<T> {
  if (request) return request<T>(path, options)
  return apiRequest<T>(path, options)
}

function normalizeCategory(category: ContributionCategoryResponse): ContributionCategoryResponse {
  const mandatory = category.isMandatory ?? category.mandatory ?? false
  return {
    ...category,
    mandatory,
    isMandatory: mandatory,
    welfareEligible: Boolean(category.welfareEligible),
  }
}

export async function getContributions(
  cursor?: string,
  size = 50,
  request?: AuthenticatedRequest,
  contributionMonth?: string,
): Promise<CursorPage<ContributionResponse>> {
  const params = new URLSearchParams({ size: String(size) })
  if (cursor) params.set('cursor', cursor)
  if (contributionMonth) params.set('contributionMonth', normalizeContributionMonth(contributionMonth))
  return callApi<CursorPage<ContributionResponse>>(`/api/v1/contributions?${params}`, undefined, request)
}

export async function getMemberContributions(
  memberId: string,
  cursor?: string,
  size = 50,
  request?: AuthenticatedRequest,
  contributionMonth?: string,
): Promise<CursorPage<ContributionResponse>> {
  const params = new URLSearchParams({ size: String(size) })
  if (cursor) params.set('cursor', cursor)
  if (contributionMonth) params.set('contributionMonth', normalizeContributionMonth(contributionMonth))
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
  const categories = await callApi<ContributionCategoryResponse[]>(`/api/v1/contribution-categories?${params}`, undefined, request)
  return categories.map(normalizeCategory)
}

export async function createCategory(
  payload: ContributionCategoryRequest,
  request?: AuthenticatedRequest,
): Promise<ContributionCategoryResponse> {
  const body = {
    ...payload,
    mandatory: payload.mandatory ?? payload.isMandatory ?? false,
  }
  const created = await callApi<ContributionCategoryResponse>('/api/v1/contribution-categories', {
    method: 'POST',
    body: JSON.stringify(body),
  }, request)
  return normalizeCategory(created)
}

export async function updateCategory(
  id: string,
  payload: ContributionCategoryRequest,
  request?: AuthenticatedRequest,
): Promise<ContributionCategoryResponse> {
  const body = {
    ...payload,
    mandatory: payload.mandatory ?? payload.isMandatory ?? false,
  }
  const updated = await callApi<ContributionCategoryResponse>(`/api/v1/contribution-categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  }, request)
  return normalizeCategory(updated)
}

export async function deleteCategory(id: string, request?: AuthenticatedRequest): Promise<void> {
  return callApi<void>(`/api/v1/contribution-categories/${id}`, {
    method: 'DELETE',
  }, request)
}

export async function getWelfarePolicy(request?: AuthenticatedRequest): Promise<ContributionWelfarePolicyResponse> {
  return callApi<ContributionWelfarePolicyResponse>('/api/v1/contributions/welfare-policy', undefined, request)
}
