import type { CursorPage } from '../types/users'
import type {
  ApprovePettyCashRequest,
  CreatePettyCashVoucherRequest,
  PettyCashSummaryResponse,
  PettyCashVoucherResponse,
  PettyCashVoucherStatus,
  RejectPettyCashVoucherRequest,
  SettlePettyCashVoucherRequest,
} from '../types/pettyCash'
import { apiRequest } from './apiClient'

export type AuthenticatedRequest = <T>(path: string, options?: RequestInit) => Promise<T>

function callApi<T>(
  path: string,
  options: RequestInit | undefined,
  request?: AuthenticatedRequest,
): Promise<T> {
  if (request) return request<T>(path, options)
  return apiRequest<T>(path, options)
}

export async function getPettyCashVouchers(
  params: {
    status?: PettyCashVoucherStatus
    month?: string
    cursor?: string
    limit?: number
  } = {},
  request?: AuthenticatedRequest,
): Promise<CursorPage<PettyCashVoucherResponse>> {
  const query = new URLSearchParams()
  if (params.status) query.set('status', params.status)
  if (params.month) query.set('month', params.month)
  if (params.cursor) query.set('cursor', params.cursor)
  if (params.limit != null) query.set('limit', String(params.limit))
  const suffix = query.toString()
  return callApi<CursorPage<PettyCashVoucherResponse>>(
    `/api/v1/petty-cash/vouchers${suffix ? `?${suffix}` : ''}`,
    undefined,
    request,
  )
}

export async function getPettyCashSummary(
  month?: string,
  status?: PettyCashVoucherStatus,
  request?: AuthenticatedRequest,
): Promise<PettyCashSummaryResponse> {
  const query = new URLSearchParams()
  if (month) query.set('month', month)
  if (status) query.set('status', status)
  const suffix = query.toString()
  return callApi<PettyCashSummaryResponse>(`/api/v1/petty-cash/summary${suffix ? `?${suffix}` : ''}`, undefined, request)
}

export async function createPettyCashVoucher(
  payload: CreatePettyCashVoucherRequest,
  request?: AuthenticatedRequest,
): Promise<PettyCashVoucherResponse> {
  return callApi<PettyCashVoucherResponse>('/api/v1/petty-cash/vouchers', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function approvePettyCashVoucher(
  id: string,
  payload?: ApprovePettyCashRequest,
  request?: AuthenticatedRequest,
): Promise<PettyCashVoucherResponse> {
  return callApi<PettyCashVoucherResponse>(`/api/v1/petty-cash/vouchers/${id}/approve`, {
    method: 'PUT',
    ...(payload ? { body: JSON.stringify(payload) } : {}),
  }, request)
}

export async function disbursePettyCashVoucher(
  id: string,
  request?: AuthenticatedRequest,
): Promise<PettyCashVoucherResponse> {
  return callApi<PettyCashVoucherResponse>(`/api/v1/petty-cash/vouchers/${id}/disburse`, {
    method: 'PUT',
  }, request)
}

export async function settlePettyCashVoucher(
  id: string,
  payload: SettlePettyCashVoucherRequest,
  request?: AuthenticatedRequest,
): Promise<PettyCashVoucherResponse> {
  return callApi<PettyCashVoucherResponse>(`/api/v1/petty-cash/vouchers/${id}/settle`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, request)
}

export async function rejectPettyCashVoucher(
  id: string,
  payload: RejectPettyCashVoucherRequest,
  request?: AuthenticatedRequest,
): Promise<PettyCashVoucherResponse> {
  return callApi<PettyCashVoucherResponse>(`/api/v1/petty-cash/vouchers/${id}/reject`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, request)
}
