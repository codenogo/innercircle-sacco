import type {
  MemberExitRequestResponse,
  MemberExitRequestStatus,
  MeetingAttendanceStatus,
  MeetingFineResponse,
  MeetingFineType,
  WelfareBeneficiaryResponse,
  WelfareClaimResponse,
  WelfareClaimStatus,
} from '../types/policyWorkflows'

type ReviewableClaimStatus = Extract<WelfareClaimStatus, 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED'>

interface MeetingWorkflowGuidanceOptions {
  hasMeeting: boolean
  openFineCount: number
  hasSelectedMember: boolean
}

interface WelfareWorkflowGuidanceOptions {
  hasActiveMember: boolean
  loadedBeneficiaryCount: number
  loadedMemberContext: boolean
  hasActiveClaim: boolean
  activeClaimStatus?: WelfareClaimStatus | null
  hasSelectedBeneficiary: boolean
}

interface MemberExitWorkflowGuidanceOptions {
  hasActiveMember: boolean
  requestCount: number
  activeRequestStatus?: MemberExitRequestStatus | null
  canReviewExitRequest: boolean
  canProcessInstallment: boolean
  isMemberOnly: boolean
}

export type MeetingWorkspaceTab = 'overview' | 'session' | 'attendance' | 'finalize' | 'fines'
export type MeetingWorkflowStep = Exclude<MeetingWorkspaceTab, 'overview'>

export type WelfareWorkspaceTab = 'overview' | 'household' | 'claim' | 'review' | 'processing'
export type WelfareWorkflowStep = Exclude<WelfareWorkspaceTab, 'overview'>

export type ExitWorkspaceTab = 'overview' | 'notice' | 'review' | 'settlement' | 'queue'
export type ExitWorkflowStep = 'notice' | 'review' | 'settlement' | 'closed'

interface MeetingWorkflowRoutingOptions {
  activeMeetingId?: string | null
  latestMeetingId?: string | null
  latestMeetingIsFinalized: boolean
  openFineCount: number
  hasSelectedMember: boolean
}

interface WelfareWorkflowRoutingOptions {
  hasActiveMember: boolean
  activeClaimStatus?: WelfareClaimStatus | null
}

interface MemberExitWorkflowRoutingOptions {
  hasActiveMember: boolean
  requestCount: number
  activeRequestStatus?: MemberExitRequestStatus | null
}

interface MemberExitAttentionOptions {
  pendingCount: number
  settlementQueueCount: number
  workflowStep: ExitWorkflowStep
}

export interface MeetingAttendanceStatusMeta {
  tone: 'default' | 'success' | 'warning'
  shortLabel: string
  title: string
  body: string
  requiresArrival: boolean
  createsFineType: MeetingFineType | null
  isPending: boolean
}

export function getMeetingWorkflowStep({
  activeMeetingId,
  latestMeetingId,
  latestMeetingIsFinalized,
  openFineCount,
  hasSelectedMember,
}: MeetingWorkflowRoutingOptions): MeetingWorkflowStep {
  const normalizedActiveMeetingId = activeMeetingId?.trim() ?? ''
  const normalizedLatestMeetingId = latestMeetingId?.trim() ?? ''

  if (!normalizedLatestMeetingId && !normalizedActiveMeetingId) {
    return 'session'
  }

  if (openFineCount > 0 && (!normalizedLatestMeetingId || latestMeetingIsFinalized || normalizedActiveMeetingId !== normalizedLatestMeetingId)) {
    return 'fines'
  }

  if (normalizedLatestMeetingId && !latestMeetingIsFinalized) {
    return hasSelectedMember ? 'finalize' : 'attendance'
  }

  return 'fines'
}

export function getMeetingRecommendedTab(workflowStep: MeetingWorkflowStep): MeetingWorkspaceTab {
  if (workflowStep === 'session') return 'session'
  if (workflowStep === 'attendance') return 'attendance'
  if (workflowStep === 'finalize') return 'finalize'
  return 'fines'
}

export function getMeetingAttentionStepIds({
  activeMeetingId,
  latestMeetingId,
  latestMeetingIsFinalized,
  openFineCount,
  hasSelectedMember,
}: MeetingWorkflowRoutingOptions): MeetingWorkflowStep[] {
  const normalizedActiveMeetingId = activeMeetingId?.trim() ?? ''
  const normalizedLatestMeetingId = latestMeetingId?.trim() ?? ''

  return [
    ...(!normalizedLatestMeetingId && !normalizedActiveMeetingId ? ['session' as const] : []),
    ...(normalizedLatestMeetingId && !latestMeetingIsFinalized && !hasSelectedMember ? ['attendance' as const] : []),
    ...(normalizedLatestMeetingId && !latestMeetingIsFinalized && hasSelectedMember ? ['finalize' as const] : []),
    ...(openFineCount > 0 ? ['fines' as const] : []),
  ]
}

export function getMeetingAttendanceStatusMeta(status: MeetingAttendanceStatus): MeetingAttendanceStatusMeta {
  switch (status) {
    case 'EXPECTED':
      return {
        tone: 'warning',
        shortLabel: 'Pending attendance',
        title: 'Attendance still pending.',
        body: 'Expected rows stay unresolved and do not create a fine. Change the row to Late or Absent only when it should drive a meeting fine.',
        requiresArrival: false,
        createsFineType: null,
        isPending: true,
      }
    case 'PRESENT':
      return {
        tone: 'success',
        shortLabel: 'No fine',
        title: 'Present recorded.',
        body: 'Present rows do not create fines. Add arrival details when you want the register to show when the member came in.',
        requiresArrival: true,
        createsFineType: null,
        isPending: false,
      }
    case 'LATE':
      return {
        tone: 'warning',
        shortLabel: 'Late fine on finalize',
        title: 'Late fine will be created.',
        body: 'Late rows stay on the roster and generate a late fine when the meeting is finalized.',
        requiresArrival: true,
        createsFineType: 'LATE',
        isPending: false,
      }
    case 'ABSENT':
      return {
        tone: 'warning',
        shortLabel: 'Absence fine on finalize',
        title: 'Absence fine will be created.',
        body: 'Absent rows generate an absence fine when the meeting is finalized.',
        requiresArrival: false,
        createsFineType: 'ABSENCE',
        isPending: false,
      }
    case 'EXCUSED':
      return {
        tone: 'success',
        shortLabel: 'Excused, no fine',
        title: 'Excused with no fine.',
        body: 'Excused rows remain on the roster for record keeping and do not create a fine when the meeting is finalized.',
        requiresArrival: false,
        createsFineType: null,
        isPending: false,
      }
    default:
      return {
        tone: 'default',
        shortLabel: status,
        title: 'Attendance recorded.',
        body: 'Review this row before finalization.',
        requiresArrival: false,
        createsFineType: null,
        isPending: false,
      }
  }
}

export function getWelfareClaimFocusTab(status: WelfareClaimStatus): WelfareWorkspaceTab {
  if (status === 'APPROVED' || status === 'PROCESSED') {
    return 'processing'
  }

  return 'review'
}

export function getWelfareWorkflowStep({
  hasActiveMember,
  activeClaimStatus,
}: WelfareWorkflowRoutingOptions): WelfareWorkflowStep {
  if (!hasActiveMember) return 'household'
  if (!activeClaimStatus) return 'claim'
  if (activeClaimStatus === 'APPROVED' || activeClaimStatus === 'PROCESSED') return 'processing'
  return 'review'
}

export function getWelfareRecommendedTab(workflowStep: WelfareWorkflowStep): WelfareWorkspaceTab {
  if (workflowStep === 'household') return 'household'
  if (workflowStep === 'claim') return 'claim'
  if (workflowStep === 'review') return 'review'
  return 'processing'
}

export function getMemberExitRecommendedTab({
  hasActiveMember,
  requestCount,
  activeRequestStatus,
}: MemberExitWorkflowRoutingOptions): ExitWorkspaceTab {
  if (!hasActiveMember) return 'notice'
  if (requestCount === 0) return 'notice'
  if (!activeRequestStatus) return 'queue'
  if (activeRequestStatus === 'REQUESTED' || activeRequestStatus === 'UNDER_REVIEW') return 'review'
  if (activeRequestStatus === 'APPROVED' || activeRequestStatus === 'IN_PROGRESS') return 'settlement'
  return 'queue'
}

export function getMemberExitWorkflowStep({
  hasActiveMember,
  requestCount,
  activeRequestStatus,
}: MemberExitWorkflowRoutingOptions): ExitWorkflowStep {
  if (!hasActiveMember || requestCount === 0) return 'notice'
  if (!activeRequestStatus) return 'review'
  if (activeRequestStatus === 'REQUESTED' || activeRequestStatus === 'UNDER_REVIEW') return 'review'
  if (activeRequestStatus === 'APPROVED' || activeRequestStatus === 'IN_PROGRESS') return 'settlement'
  return 'closed'
}

export function getMemberExitAttentionSteps({
  pendingCount,
  settlementQueueCount,
  workflowStep,
}: MemberExitAttentionOptions): ExitWorkflowStep[] {
  const steps: ExitWorkflowStep[] = []

  if (pendingCount > 0 && workflowStep !== 'review') {
    steps.push('review')
  }

  if (settlementQueueCount > 0 && workflowStep !== 'settlement') {
    steps.push('settlement')
  }

  return steps
}

export function getMemberExitSelectionTab(status: MemberExitRequestStatus): ExitWorkspaceTab {
  if (status === 'REQUESTED' || status === 'UNDER_REVIEW') return 'review'
  if (status === 'APPROVED' || status === 'IN_PROGRESS') return 'settlement'
  return 'queue'
}

export function summarizeMeetingFines(
  fines: Array<Pick<MeetingFineResponse, 'amount' | 'settled' | 'waived'>>,
) {
  return fines.reduce(
    (summary, fine) => {
      if (fine.waived) {
        summary.waivedCount += 1
        return summary
      }

      if (fine.settled) {
        summary.settledCount += 1
        return summary
      }

      summary.openCount += 1
      summary.outstandingAmount += fine.amount
      return summary
    },
    { openCount: 0, settledCount: 0, waivedCount: 0, outstandingAmount: 0 },
  )
}

export function getMeetingWorkflowGuidance({
  hasMeeting,
  openFineCount,
  hasSelectedMember,
}: MeetingWorkflowGuidanceOptions) {
  if (!hasMeeting) {
    return {
      title: 'Create a meeting first.',
      body: 'Open the meeting session before attendance, finalization, and fine resolution can stay aligned.',
    }
  }

  if (openFineCount > 0) {
    return {
      title: 'Resolve the fine queue.',
      body: 'Attendance is already closed for at least one meeting, so the next admin action is settling or waiving outstanding fines.',
    }
  }

  if (!hasSelectedMember) {
    return {
      title: 'Capture attendance for the active meeting.',
      body: 'Choose a member and attendance status so the meeting can be finalized with the right outcomes.',
    }
  }

  return {
    title: 'Finalize once attendance is complete.',
    body: 'After the meeting register is up to date, finalize the meeting to generate any penalties that should move into the fine queue.',
  }
}

export function summarizeWelfareWorkspace(
  beneficiaries: Array<Pick<WelfareBeneficiaryResponse, 'active'>>,
  claims: Array<Pick<WelfareClaimResponse, 'status'>>,
) {
  return claims.reduce(
    (summary, claim) => {
      summary.claimCount += 1

      if (claim.status === 'SUBMITTED' || claim.status === 'UNDER_REVIEW') {
        summary.reviewQueueCount += 1
      }

      if (claim.status === 'APPROVED') {
        summary.approvedCount += 1
      }

      if (claim.status === 'PROCESSED') {
        summary.processedCount += 1
      }

      return summary
    },
    {
      beneficiaryCount: beneficiaries.length,
      activeBeneficiaryCount: beneficiaries.filter(beneficiary => beneficiary.active).length,
      claimCount: 0,
      reviewQueueCount: 0,
      approvedCount: 0,
      processedCount: 0,
    },
  )
}

export function getWelfareWorkflowGuidance({
  hasActiveMember,
  loadedBeneficiaryCount,
  loadedMemberContext,
  hasActiveClaim,
  activeClaimStatus,
  hasSelectedBeneficiary,
}: WelfareWorkflowGuidanceOptions) {
  if (!hasActiveMember) {
    return {
      title: 'Start with the member household.',
      body: 'Load one member at a time so beneficiary setup, claim intake, review, and payout decisions all stay tied to the same person.',
    }
  }

  if (loadedMemberContext && loadedBeneficiaryCount === 0) {
    return {
      title: 'Confirm beneficiary coverage before claim intake.',
      body: 'If the event involves a dependant, add the beneficiary first; otherwise continue with a direct claim for the member.',
    }
  }

  if (!hasActiveClaim) {
    return {
      title: 'Submit the claim intake.',
      body: hasSelectedBeneficiary
        ? 'The beneficiary context is already set, so the next action is capturing the event details and requested amount.'
        : 'Capture the event details and requested amount, then move the case into the review queue.',
    }
  }

  if (activeClaimStatus === 'APPROVED') {
    return {
      title: 'This claim is ready for payout processing.',
      body: 'The review decision is complete, so the next admin action is releasing the approved claim into the payout step.',
    }
  }

  if (activeClaimStatus === 'PROCESSED') {
    return {
      title: 'This claim is complete.',
      body: 'Use the queue to move to the next household case that still needs review or payout work.',
    }
  }

  return {
    title: 'Finish the review decision.',
    body: 'Move the case to Approved or Rejected before attempting the final payout processing step.',
  }
}

export function summarizeMemberExitRequests(
  requests: Array<Pick<MemberExitRequestResponse, 'status'>>,
) {
  return requests.reduce(
    (summary, request) => {
      summary.total += 1

      if (request.status === 'REQUESTED' || request.status === 'UNDER_REVIEW') {
        summary.pending += 1
      }

      if (request.status === 'IN_PROGRESS') {
        summary.inProgress += 1
      }

      if (request.status === 'COMPLETED') {
        summary.completed += 1
      }

      if (request.status === 'REJECTED') {
        summary.rejected += 1
      }

      return summary
    },
    { total: 0, pending: 0, inProgress: 0, completed: 0, rejected: 0 },
  )
}

export function getMemberExitWorkflowGuidance({
  hasActiveMember,
  requestCount,
  activeRequestStatus,
  canReviewExitRequest,
  canProcessInstallment,
  isMemberOnly,
}: MemberExitWorkflowGuidanceOptions) {
  if (!hasActiveMember) {
    return {
      title: isMemberOnly ? 'Your member profile is still loading.' : 'Start by selecting a member.',
      body: isMemberOnly
        ? 'Once your member profile is available, this workspace will load your exit requests automatically.'
        : 'Load one member exit queue first so request intake, review, and installment processing stay on the same record.',
    }
  }

  if (requestCount === 0) {
    return {
      title: isMemberOnly ? 'You can submit your exit request.' : 'No exit request is loaded yet.',
      body: isMemberOnly
        ? 'Start by submitting your notice and effective date. SACCO officers will review the request after submission.'
        : 'Create a new request or load the correct member queue before review and settlement can begin.',
    }
  }

  if (!activeRequestStatus) {
    return {
      title: 'Pick the request you want to continue.',
      body: 'Use the queue to select one exit request so everyone is working on the same record and next action.',
    }
  }

  if (activeRequestStatus === 'REQUESTED' || activeRequestStatus === 'UNDER_REVIEW') {
    return {
      title: canReviewExitRequest ? 'This request needs review.' : 'Your request is waiting for SACCO review.',
      body: canReviewExitRequest
        ? 'Confirm the review outcome before any settlement installments can be processed.'
        : 'An admin or committee officer must review this request before settlement details can move forward.',
    }
  }

  if (activeRequestStatus === 'APPROVED') {
    return {
      title: canProcessInstallment ? 'Settlement can begin.' : 'Your request has been approved.',
      body: canProcessInstallment
        ? 'The next finance action is processing the first installment for this approved request.'
        : 'Finance will now schedule and process the settlement installments for this request.',
    }
  }

  if (activeRequestStatus === 'IN_PROGRESS') {
    return {
      title: canProcessInstallment ? 'Settlement is in progress.' : 'Your settlement is in progress.',
      body: canProcessInstallment
        ? 'Continue processing the remaining installments until the request reaches completion.'
        : 'Track the remaining installments here while finance completes the payout schedule.',
    }
  }

  if (activeRequestStatus === 'COMPLETED') {
    return {
      title: 'This exit workflow is complete.',
      body: isMemberOnly
        ? 'Your exit request has completed settlement.'
        : 'No further action is required unless you need to review the completed record.',
    }
  }

  return {
    title: 'This request is closed.',
    body: isMemberOnly
      ? 'Your request was not approved. Contact the SACCO office if you need clarification.'
      : 'This request was rejected, so review notes are the main record of the decision.',
  }
}

export function toReviewableClaimStatus(status: WelfareClaimStatus): ReviewableClaimStatus {
  if (status === 'APPROVED' || status === 'REJECTED' || status === 'UNDER_REVIEW') {
    return status
  }

  return 'UNDER_REVIEW'
}