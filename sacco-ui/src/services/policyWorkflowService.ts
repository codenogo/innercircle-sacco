import { apiRequest } from './apiClient'
import type {
  CreateMeetingRequest,
  RecordMeetingAttendanceRequest,
  MeetingResponse,
  MeetingFineResponse,
  CreateWelfareBeneficiaryRequest,
  WelfareBeneficiaryResponse,
  CreateWelfareClaimRequest,
  ReviewWelfareClaimRequest,
  WelfareClaimResponse,
  WelfareFundSummaryResponse,
  CreateMemberExitRequestRequest,
  ReviewMemberExitRequestRequest,
  MemberExitRequestResponse,
  MemberExitInstallmentResponse,
} from '../types/policyWorkflows'

export type AuthenticatedRequest = <T>(path: string, options?: RequestInit) => Promise<T>

function callApi<T>(
  path: string,
  options: RequestInit | undefined,
  request?: AuthenticatedRequest,
): Promise<T> {
  if (request) return request<T>(path, options)
  return apiRequest<T>(path, options)
}

export async function createMeeting(
  payload: CreateMeetingRequest,
  request?: AuthenticatedRequest,
): Promise<MeetingResponse> {
  return callApi<MeetingResponse>('/api/v1/meetings', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function recordMeetingAttendance(
  meetingId: string,
  payload: RecordMeetingAttendanceRequest,
  request?: AuthenticatedRequest,
): Promise<void> {
  return callApi<void>(`/api/v1/meetings/${meetingId}/attendance`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function finalizeMeeting(
  meetingId: string,
  request?: AuthenticatedRequest,
): Promise<MeetingResponse> {
  return callApi<MeetingResponse>(`/api/v1/meetings/${meetingId}/finalize`, { method: 'POST' }, request)
}

export async function getMeetingFines(
  memberId?: string,
  request?: AuthenticatedRequest,
): Promise<MeetingFineResponse[]> {
  const params = new URLSearchParams()
  if (memberId) params.set('memberId', memberId)
  const query = params.toString()
  return callApi<MeetingFineResponse[]>(`/api/v1/meetings/fines${query ? `?${query}` : ''}`, undefined, request)
}

export async function settleMeetingFine(
  fineId: string,
  request?: AuthenticatedRequest,
): Promise<MeetingFineResponse> {
  return callApi<MeetingFineResponse>(`/api/v1/meetings/fines/${fineId}/settle`, { method: 'POST' }, request)
}

export async function waiveMeetingFine(
  fineId: string,
  reason?: string,
  request?: AuthenticatedRequest,
): Promise<MeetingFineResponse> {
  return callApi<MeetingFineResponse>(`/api/v1/meetings/fines/${fineId}/waive`, {
    method: 'PATCH',
    body: JSON.stringify(reason ? { reason } : {}),
  }, request)
}

export async function createWelfareBeneficiary(
  payload: CreateWelfareBeneficiaryRequest,
  request?: AuthenticatedRequest,
): Promise<WelfareBeneficiaryResponse> {
  return callApi<WelfareBeneficiaryResponse>('/api/v1/welfare/beneficiaries', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function getWelfareBeneficiariesForMember(
  memberId: string,
  request?: AuthenticatedRequest,
): Promise<WelfareBeneficiaryResponse[]> {
  return callApi<WelfareBeneficiaryResponse[]>(`/api/v1/welfare/beneficiaries/member/${memberId}`, undefined, request)
}

export async function createWelfareClaim(
  payload: CreateWelfareClaimRequest,
  request?: AuthenticatedRequest,
): Promise<WelfareClaimResponse> {
  return callApi<WelfareClaimResponse>('/api/v1/welfare/claims', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function reviewWelfareClaim(
  claimId: string,
  payload: ReviewWelfareClaimRequest,
  request?: AuthenticatedRequest,
): Promise<WelfareClaimResponse> {
  return callApi<WelfareClaimResponse>(`/api/v1/welfare/claims/${claimId}/review`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  }, request)
}

export async function processWelfareClaim(
  claimId: string,
  request?: AuthenticatedRequest,
): Promise<WelfareClaimResponse> {
  return callApi<WelfareClaimResponse>(`/api/v1/welfare/claims/${claimId}/process`, { method: 'POST' }, request)
}

export async function getWelfareFundSummary(
  request?: AuthenticatedRequest,
): Promise<WelfareFundSummaryResponse> {
  return callApi<WelfareFundSummaryResponse>('/api/v1/welfare/fund-summary', undefined, request)
}

export async function createMemberExitRequest(
  memberId: string,
  payload: CreateMemberExitRequestRequest,
  request?: AuthenticatedRequest,
): Promise<MemberExitRequestResponse> {
  return callApi<MemberExitRequestResponse>(`/api/v1/members/${memberId}/exit-requests`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, request)
}

export async function reviewMemberExitRequest(
  memberId: string,
  requestId: string,
  payload: ReviewMemberExitRequestRequest,
  request?: AuthenticatedRequest,
): Promise<MemberExitRequestResponse> {
  return callApi<MemberExitRequestResponse>(`/api/v1/members/${memberId}/exit-requests/${requestId}/review`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  }, request)
}

export async function processMemberExitInstallment(
  memberId: string,
  requestId: string,
  request?: AuthenticatedRequest,
): Promise<MemberExitInstallmentResponse> {
  return callApi<MemberExitInstallmentResponse>(`/api/v1/members/${memberId}/exit-requests/${requestId}/process-installment`, {
    method: 'POST',
  }, request)
}

export async function getMemberExitRequests(
  memberId: string,
  request?: AuthenticatedRequest,
): Promise<MemberExitRequestResponse[]> {
  return callApi<MemberExitRequestResponse[]>(`/api/v1/members/${memberId}/exit-requests`, undefined, request)
}
