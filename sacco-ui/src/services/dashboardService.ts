import type {
  TreasurerDashboardResponse,
  AdminDashboardResponse,
  DashboardAnalyticsResponse,
  MonthlyDataPoint,
  SaccoStateResponse,
} from '../types/dashboard'
import { apiRequest } from './apiClient'

export async function getTreasurerDashboard(): Promise<TreasurerDashboardResponse> {
  return apiRequest<TreasurerDashboardResponse>('/api/v1/dashboard/treasurer')
}

export async function getAdminDashboard(): Promise<AdminDashboardResponse> {
  return apiRequest<AdminDashboardResponse>('/api/v1/dashboard/admin')
}

export async function getAnalytics(year?: number): Promise<DashboardAnalyticsResponse> {
  const params = new URLSearchParams()
  if (year != null) params.set('year', String(year))
  const query = params.toString()
  return apiRequest<DashboardAnalyticsResponse>(`/api/v1/dashboard/analytics${query ? `?${query}` : ''}`)
}

export async function getContributionAnalytics(year?: number): Promise<MonthlyDataPoint[]> {
  const params = new URLSearchParams()
  if (year != null) params.set('year', String(year))
  const query = params.toString()
  return apiRequest<MonthlyDataPoint[]>(`/api/v1/dashboard/analytics/contributions${query ? `?${query}` : ''}`)
}

export async function getLoanAnalytics(year?: number): Promise<MonthlyDataPoint[]> {
  const params = new URLSearchParams()
  if (year != null) params.set('year', String(year))
  const query = params.toString()
  return apiRequest<MonthlyDataPoint[]>(`/api/v1/dashboard/analytics/loans${query ? `?${query}` : ''}`)
}

export async function getSaccoState(): Promise<SaccoStateResponse> {
  return apiRequest<SaccoStateResponse>('/api/v1/dashboard/state')
}
