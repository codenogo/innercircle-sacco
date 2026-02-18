import { apiRequest } from './apiClient'
import type {
  FinancialSummaryResponse,
  MemberStatementResponse,
} from '../types/reports'

export async function getFinancialSummary(fromDate: string, toDate: string): Promise<FinancialSummaryResponse> {
  const params = new URLSearchParams({ fromDate, toDate })
  return apiRequest<FinancialSummaryResponse>(`/api/v1/reports/financial-summary?${params}`)
}

export async function getMemberStatement(memberId: string, fromDate: string, toDate: string): Promise<MemberStatementResponse> {
  const params = new URLSearchParams({ fromDate, toDate })
  return apiRequest<MemberStatementResponse>(`/api/v1/reports/member-statement/${memberId}?${params}`)
}

export function exportMemberStatementPdfUrl(memberId: string, fromDate: string, toDate: string): string {
  const params = new URLSearchParams({ fromDate, toDate })
  return `/api/v1/export/member-statement/${memberId}/pdf?${params}`
}

export function exportMemberStatementCsvUrl(memberId: string, fromDate: string, toDate: string): string {
  const params = new URLSearchParams({ fromDate, toDate })
  return `/api/v1/export/member-statement/${memberId}/csv?${params}`
}

export function exportFinancialSummaryCsvUrl(fromDate: string, toDate: string): string {
  const params = new URLSearchParams({ fromDate, toDate })
  return `/api/v1/export/financial-summary/csv?${params}`
}

export async function downloadExport(url: string, token: string): Promise<Blob> {
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) {
    throw new Error(`Export failed with status ${response.status}`)
  }
  return response.blob()
}
