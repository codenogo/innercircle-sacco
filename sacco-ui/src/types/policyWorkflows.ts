export type MeetingAttendanceStatus = 'PRESENT' | 'LATE' | 'ABSENT' | 'EXCUSED'
export type MeetingFineType = 'ABSENCE' | 'LATE' | 'UNPAID_DAILY_PENALTY'
export type WelfareClaimStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'PROCESSED'
export type MemberExitRequestStatus = 'REQUESTED' | 'UNDER_REVIEW' | 'APPROVED' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED'

export interface CreateMeetingRequest {
  title: string
  meetingDate: string
  lateThresholdMinutes?: number
  absenceFineAmount?: number
  lateFineAmount?: number
  unpaidDailyPenaltyAmount?: number
  notes?: string
}

export interface RecordMeetingAttendanceRequest {
  entries: {
    memberId: string
    attendanceStatus: MeetingAttendanceStatus
    arrivedAt?: string
    remarks?: string
  }[]
}

export interface MeetingResponse {
  id: string
  title: string
  meetingDate: string
  status: string
  lateThresholdMinutes: number | null
  absenceFineAmount: number | null
  lateFineAmount: number | null
  unpaidDailyPenaltyAmount: number | null
  finalizedAt: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface MeetingFineResponse {
  id: string
  meetingId: string
  attendanceId: string | null
  memberId: string
  fineType: MeetingFineType
  baseFineId: string | null
  amount: number
  dueDate: string | null
  settled: boolean
  settledAt: string | null
  waived: boolean
  waivedBy: string | null
  waivedAt: string | null
  waivedReason: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateWelfareBeneficiaryRequest {
  memberId: string
  fullName: string
  relationship: string
  dateOfBirth?: string
  phone?: string
  active?: boolean
  notes?: string
}

export interface WelfareBeneficiaryResponse {
  id: string
  memberId: string
  fullName: string
  relationship: string
  dateOfBirth: string | null
  phone: string | null
  active: boolean
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateWelfareClaimRequest {
  memberId: string
  beneficiaryId?: string
  benefitCatalogId?: string
  eventCode: string
  eventDate: string
  requestedAmount: number
}

export interface ReviewWelfareClaimRequest {
  status: WelfareClaimStatus
  approvedAmount?: number
  decisionSource?: string
  meetingReference?: string
  decisionDate?: string
  decisionNotes?: string
}

export interface WelfareClaimResponse {
  id: string
  memberId: string
  beneficiaryId: string | null
  benefitCatalogId: string | null
  eventCode: string
  eventDate: string
  requestedAmount: number
  approvedAmount: number | null
  status: WelfareClaimStatus
  decisionSource: string | null
  meetingReference: string | null
  decisionDate: string | null
  decisionNotes: string | null
  reviewedBy: string | null
  processedPayoutId: string | null
  createdAt: string
  updatedAt: string
}

export interface WelfareFundSummaryResponse {
  totalWelfareContributions: number
  totalWelfarePayouts: number
  availableBalance: number
  pendingClaims: number
}

export interface CreateMemberExitRequestRequest {
  noticeDate: string
  effectiveDate: string
}

export interface ReviewMemberExitRequestRequest {
  status: MemberExitRequestStatus
  reviewNotes?: string
}

export interface MemberExitRequestResponse {
  id: string
  memberId: string
  noticeDate: string
  effectiveDate: string
  status: MemberExitRequestStatus
  grossSettlementAmount: number | null
  liabilityOffsetAmount: number | null
  exitFeeAmount: number | null
  netSettlementAmount: number | null
  installmentCount: number | null
  installmentsProcessed: number | null
  nextInstallmentDueDate: string | null
  reviewNotes: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface MemberExitInstallmentResponse {
  id: string
  exitRequestId: string
  installmentNumber: number
  dueDate: string
  amount: number
  processed: boolean
  processedAt: string | null
  payoutId: string | null
  createdAt: string
  updatedAt: string
}
