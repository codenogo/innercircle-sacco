import { apiRequest } from './apiClient'
import type {
  SystemConfigResponse,
  LoanProductConfigResponse,
  ContributionScheduleConfigResponse,
  PenaltyRuleResponse,
} from '../types/config'

export async function getSystemConfigs(): Promise<SystemConfigResponse[]> {
  return apiRequest<SystemConfigResponse[]>('/api/v1/config/system')
}

export async function getSystemConfig(key: string): Promise<SystemConfigResponse> {
  return apiRequest<SystemConfigResponse>(`/api/v1/config/system/${key}`)
}

export async function getLoanProducts(activeOnly?: boolean): Promise<LoanProductConfigResponse[]> {
  const params = new URLSearchParams()
  if (activeOnly !== undefined) params.set('activeOnly', String(activeOnly))
  const query = params.toString()
  return apiRequest<LoanProductConfigResponse[]>(`/api/v1/config/loan-products${query ? `?${query}` : ''}`)
}

export async function getContributionSchedules(activeOnly?: boolean): Promise<ContributionScheduleConfigResponse[]> {
  const params = new URLSearchParams()
  if (activeOnly !== undefined) params.set('activeOnly', String(activeOnly))
  const query = params.toString()
  return apiRequest<ContributionScheduleConfigResponse[]>(`/api/v1/config/contribution-schedules${query ? `?${query}` : ''}`)
}

export async function getPenaltyRules(activeOnly?: boolean): Promise<PenaltyRuleResponse[]> {
  const params = new URLSearchParams()
  if (activeOnly !== undefined) params.set('activeOnly', String(activeOnly))
  const query = params.toString()
  return apiRequest<PenaltyRuleResponse[]>(`/api/v1/config/penalty-rules${query ? `?${query}` : ''}`)
}
