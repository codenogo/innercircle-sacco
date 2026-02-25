import { apiRequest } from './apiClient'
import type {
  InvestmentResponse,
  CreateInvestmentRequest,
  InvestmentIncomeResponse,
  RecordIncomeRequest,
  InvestmentValuationResponse,
  RecordValuationRequest,
  DisposeInvestmentRequest,
  RollOverRequest,
  InvestmentSummary,
} from '../types/investments'

const BASE = '/api/v1/investments'

export type AuthenticatedRequest = <T>(path: string, options?: RequestInit) => Promise<T>

function callApi<T>(
  path: string,
  options: RequestInit | undefined,
  request?: AuthenticatedRequest,
): Promise<T> {
  if (request) return request<T>(path, options)
  return apiRequest<T>(path, options)
}

/* Portfolio */

export async function getInvestments(request?: AuthenticatedRequest): Promise<InvestmentResponse[]> {
  return callApi<InvestmentResponse[]>(BASE, undefined, request)
}

export async function getInvestment(id: string, request?: AuthenticatedRequest): Promise<InvestmentResponse> {
  return callApi<InvestmentResponse>(`${BASE}/${id}`, undefined, request)
}

export async function getInvestmentSummary(request?: AuthenticatedRequest): Promise<InvestmentSummary> {
  return callApi<InvestmentSummary>(`${BASE}/summary`, undefined, request)
}

/* Lifecycle */

export async function createInvestment(
  payload: CreateInvestmentRequest,
  request?: AuthenticatedRequest,
): Promise<InvestmentResponse> {
  return callApi<InvestmentResponse>(BASE, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function approveInvestment(id: string, request?: AuthenticatedRequest): Promise<InvestmentResponse> {
  return callApi<InvestmentResponse>(`${BASE}/${id}/approve`, { method: 'PATCH' }, request)
}

export async function rejectInvestment(
  id: string,
  reason?: string,
  request?: AuthenticatedRequest,
): Promise<InvestmentResponse> {
  return callApi<InvestmentResponse>(`${BASE}/${id}/reject`, {
    method: 'PATCH',
    ...(reason ? { body: JSON.stringify({ reason }) } : {}),
  }, request)
}

export async function activateInvestment(id: string, request?: AuthenticatedRequest): Promise<InvestmentResponse> {
  return callApi<InvestmentResponse>(`${BASE}/${id}/activate`, { method: 'PATCH' }, request)
}

export async function disposeInvestment(
  id: string,
  payload: DisposeInvestmentRequest,
  request?: AuthenticatedRequest,
): Promise<InvestmentResponse> {
  return callApi<InvestmentResponse>(`${BASE}/${id}/dispose`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function rollOverInvestment(
  id: string,
  payload: RollOverRequest,
  request?: AuthenticatedRequest,
): Promise<InvestmentResponse> {
  return callApi<InvestmentResponse>(`${BASE}/${id}/rollover`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

/* Income */

export async function getInvestmentIncome(
  investmentId: string,
  request?: AuthenticatedRequest,
): Promise<InvestmentIncomeResponse[]> {
  return callApi<InvestmentIncomeResponse[]>(`${BASE}/${investmentId}/income`, undefined, request)
}

export async function recordIncome(
  investmentId: string,
  payload: RecordIncomeRequest,
  request?: AuthenticatedRequest,
): Promise<InvestmentIncomeResponse> {
  return callApi<InvestmentIncomeResponse>(`${BASE}/${investmentId}/income`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

/* Valuations */

export async function getValuations(
  investmentId: string,
  request?: AuthenticatedRequest,
): Promise<InvestmentValuationResponse[]> {
  return callApi<InvestmentValuationResponse[]>(`${BASE}/${investmentId}/valuations`, undefined, request)
}

export async function recordValuation(
  investmentId: string,
  payload: RecordValuationRequest,
  request?: AuthenticatedRequest,
): Promise<InvestmentValuationResponse> {
  return callApi<InvestmentValuationResponse>(`${BASE}/${investmentId}/valuations`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}
