import type { CursorPage } from '../types/users'
import type {
  LoanResponse,
  LoanApplicationRequest,
  RepaymentRequest,
  LoanSummaryResponse,
  RepaymentScheduleResponse,
  MonthlyInterestSummary,
} from '../types/loans'
import { apiRequest } from './apiClient'

export async function getLoans(cursor?: string, size?: number): Promise<CursorPage<LoanResponse>> {
  const params = new URLSearchParams()
  if (cursor) params.set('cursor', cursor)
  if (size != null) params.set('limit', String(size))
  const query = params.toString()
  return apiRequest<CursorPage<LoanResponse>>(`/api/v1/loans${query ? `?${query}` : ''}`)
}

export async function getLoan(id: string): Promise<LoanResponse> {
  return apiRequest<LoanResponse>(`/api/v1/loans/${id}`)
}

export async function applyForLoan(payload: LoanApplicationRequest): Promise<LoanResponse> {
  return apiRequest<LoanResponse>('/api/v1/loans/apply', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function approveLoan(id: string): Promise<LoanResponse> {
  return apiRequest<LoanResponse>(`/api/v1/loans/${id}/approve`, {
    method: 'PATCH',
  })
}

export async function rejectLoan(id: string): Promise<LoanResponse> {
  return apiRequest<LoanResponse>(`/api/v1/loans/${id}/reject`, {
    method: 'PATCH',
  })
}

export async function disburseLoan(id: string): Promise<LoanResponse> {
  return apiRequest<LoanResponse>(`/api/v1/loans/${id}/disburse`, {
    method: 'PATCH',
  })
}

export async function repayLoan(id: string, payload: RepaymentRequest): Promise<void> {
  await apiRequest<void>(`/api/v1/loans/${id}/repay`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function getLoanSchedule(id: string): Promise<RepaymentScheduleResponse[]> {
  return apiRequest<RepaymentScheduleResponse[]>(`/api/v1/loans/${id}/schedule`)
}

export async function getMemberLoanSummary(memberId: string): Promise<LoanSummaryResponse> {
  return apiRequest<LoanSummaryResponse>(`/api/v1/loans/member/${memberId}/summary`)
}

export async function getInterestSummary(month?: string): Promise<MonthlyInterestSummary> {
  const params = new URLSearchParams()
  if (month) params.set('month', month)
  const query = params.toString()
  return apiRequest<MonthlyInterestSummary>(`/api/v1/loans/interest/summary${query ? `?${query}` : ''}`)
}
