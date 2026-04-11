import { apiRequest } from './apiClient'
import type {
  SystemConfigResponse,
  LoanProductConfigResponse,
  LoanProductRequest,
  ContributionScheduleConfigResponse,
  ContributionScheduleRequest,
  ConfigHealthResponse,
  PenaltyRuleResponse,
  PenaltyRuleRequest,
} from '../types/config'

export type AuthenticatedRequest = <T>(path: string, options?: RequestInit) => Promise<T>

function callApi<T>(
  path: string,
  options: RequestInit | undefined,
  request?: AuthenticatedRequest,
): Promise<T> {
  if (request) return request<T>(path, options)
  return apiRequest<T>(path, options)
}

export async function getSystemConfigs(request?: AuthenticatedRequest): Promise<SystemConfigResponse[]> {
  return callApi<SystemConfigResponse[]>('/api/v1/config/system', undefined, request)
}

export async function getSystemConfig(key: string, request?: AuthenticatedRequest): Promise<SystemConfigResponse> {
  return callApi<SystemConfigResponse>(`/api/v1/config/system/${encodeURIComponent(key)}`, undefined, request)
}

export async function getSystemConfigHealth(request?: AuthenticatedRequest): Promise<ConfigHealthResponse> {
  return callApi<ConfigHealthResponse>('/api/v1/config/system/health', undefined, request)
}

export async function createSystemConfig(
  configKey: string,
  configValue: string,
  description?: string,
  request?: AuthenticatedRequest,
): Promise<SystemConfigResponse> {
  const params = new URLSearchParams({ configKey, configValue })
  if (description) params.set('description', description)
  return callApi<SystemConfigResponse>(`/api/v1/config/system?${params.toString()}`, { method: 'POST' }, request)
}

export async function updateSystemConfig(
  configKey: string,
  configValue: string,
  request?: AuthenticatedRequest,
): Promise<SystemConfigResponse> {
  return callApi<SystemConfigResponse>(`/api/v1/config/system/${encodeURIComponent(configKey)}`, {
    method: 'PUT',
    body: JSON.stringify({ configValue }),
  }, request)
}

export async function getLoanProducts(activeOnly?: boolean, request?: AuthenticatedRequest): Promise<LoanProductConfigResponse[]> {
  const params = new URLSearchParams()
  if (activeOnly !== undefined) params.set('activeOnly', String(activeOnly))
  const query = params.toString()
  return callApi<LoanProductConfigResponse[]>(`/api/v1/config/loan-products${query ? `?${query}` : ''}`, undefined, request)
}

export async function createLoanProduct(
  payload: LoanProductRequest,
  request?: AuthenticatedRequest,
): Promise<LoanProductConfigResponse> {
  return callApi<LoanProductConfigResponse>('/api/v1/config/loan-products', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function updateLoanProduct(
  id: string,
  payload: LoanProductRequest,
  request?: AuthenticatedRequest,
): Promise<LoanProductConfigResponse> {
  return callApi<LoanProductConfigResponse>(`/api/v1/config/loan-products/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, request)
}

export async function getContributionSchedules(activeOnly?: boolean, request?: AuthenticatedRequest): Promise<ContributionScheduleConfigResponse[]> {
  const params = new URLSearchParams()
  if (activeOnly !== undefined) params.set('activeOnly', String(activeOnly))
  const query = params.toString()
  return callApi<ContributionScheduleConfigResponse[]>(`/api/v1/config/contribution-schedules${query ? `?${query}` : ''}`, undefined, request)
}

export async function createContributionSchedule(
  payload: ContributionScheduleRequest,
  request?: AuthenticatedRequest,
): Promise<ContributionScheduleConfigResponse> {
  return callApi<ContributionScheduleConfigResponse>('/api/v1/config/contribution-schedules', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function updateContributionSchedule(
  id: string,
  payload: ContributionScheduleRequest,
  request?: AuthenticatedRequest,
): Promise<ContributionScheduleConfigResponse> {
  return callApi<ContributionScheduleConfigResponse>(`/api/v1/config/contribution-schedules/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, request)
}

export async function getPenaltyRules(activeOnly?: boolean, request?: AuthenticatedRequest): Promise<PenaltyRuleResponse[]> {
  const params = new URLSearchParams()
  if (activeOnly !== undefined) params.set('activeOnly', String(activeOnly))
  const query = params.toString()
  return callApi<PenaltyRuleResponse[]>(`/api/v1/config/penalty-rules${query ? `?${query}` : ''}`, undefined, request)
}

export async function createPenaltyRule(
  payload: PenaltyRuleRequest,
  request?: AuthenticatedRequest,
): Promise<PenaltyRuleResponse> {
  return callApi<PenaltyRuleResponse>('/api/v1/config/penalty-rules', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function updatePenaltyRule(
  id: string,
  payload: PenaltyRuleRequest,
  request?: AuthenticatedRequest,
): Promise<PenaltyRuleResponse> {
  return callApi<PenaltyRuleResponse>(`/api/v1/config/penalty-rules/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, request)
}
