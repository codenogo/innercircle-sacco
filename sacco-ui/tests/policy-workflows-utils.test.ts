import { describe, expect, it } from 'vitest'
import {
  getMeetingAttendanceStatusMeta,
  getMeetingWorkflowGuidance,
  getMemberExitWorkflowGuidance,
  getWelfareWorkflowGuidance,
  summarizeMemberExitRequests,
  summarizeMeetingFines,
  summarizeWelfareWorkspace,
  toReviewableClaimStatus,
} from '../src/utils/policyWorkflows'

describe('policy workflow helpers', () => {
  it('summarizes meeting fine queue status and outstanding amount', () => {
    expect(summarizeMeetingFines([
      { amount: 500, settled: false, waived: false },
      { amount: 750, settled: true, waived: false },
      { amount: 300, settled: false, waived: true },
    ])).toEqual({
      openCount: 1,
      settledCount: 1,
      waivedCount: 1,
      outstandingAmount: 500,
    })
  })

  it('summarizes the current welfare workspace', () => {
    expect(summarizeWelfareWorkspace(
      [{ active: true }, { active: false }],
      [
        { status: 'SUBMITTED' },
        { status: 'UNDER_REVIEW' },
        { status: 'APPROVED' },
        { status: 'PROCESSED' },
      ],
    )).toEqual({
      beneficiaryCount: 2,
      activeBeneficiaryCount: 1,
      claimCount: 4,
      reviewQueueCount: 2,
      approvedCount: 1,
      processedCount: 1,
    })
  })

  it('guides meeting workflows based on session state', () => {
    expect(getMeetingWorkflowGuidance({
      hasMeeting: false,
      openFineCount: 0,
      hasSelectedMember: false,
    }).title).toBe('Create a meeting first.')

    expect(getMeetingWorkflowGuidance({
      hasMeeting: true,
      openFineCount: 2,
      hasSelectedMember: true,
    }).title).toBe('Resolve the fine queue.')

    expect(getMeetingWorkflowGuidance({
      hasMeeting: true,
      openFineCount: 0,
      hasSelectedMember: false,
    }).title).toBe('Capture attendance for the active meeting.')
  })

  it('describes which attendance statuses create meeting fines', () => {
    expect(getMeetingAttendanceStatusMeta('EXPECTED')).toMatchObject({
      shortLabel: 'Pending attendance',
      createsFineType: null,
      isPending: true,
      requiresArrival: false,
    })

    expect(getMeetingAttendanceStatusMeta('LATE')).toMatchObject({
      shortLabel: 'Late fine on finalize',
      createsFineType: 'LATE',
      requiresArrival: true,
    })

    expect(getMeetingAttendanceStatusMeta('ABSENT')).toMatchObject({
      shortLabel: 'Absence fine on finalize',
      createsFineType: 'ABSENCE',
      requiresArrival: false,
    })

    expect(getMeetingAttendanceStatusMeta('EXCUSED')).toMatchObject({
      shortLabel: 'Excused, no fine',
      createsFineType: null,
      isPending: false,
    })
  })

  it('guides welfare workflows based on household and claim state', () => {
    expect(getWelfareWorkflowGuidance({
      hasActiveMember: false,
      loadedBeneficiaryCount: 0,
      loadedMemberContext: false,
      hasActiveClaim: false,
      activeClaimStatus: null,
      hasSelectedBeneficiary: false,
    }).title).toBe('Start with the member household.')

    expect(getWelfareWorkflowGuidance({
      hasActiveMember: true,
      loadedBeneficiaryCount: 0,
      loadedMemberContext: true,
      hasActiveClaim: false,
      activeClaimStatus: null,
      hasSelectedBeneficiary: false,
    }).title).toBe('Confirm beneficiary coverage before claim intake.')

    expect(getWelfareWorkflowGuidance({
      hasActiveMember: true,
      loadedBeneficiaryCount: 1,
      loadedMemberContext: true,
      hasActiveClaim: true,
      activeClaimStatus: 'APPROVED',
      hasSelectedBeneficiary: true,
    }).title).toBe('This claim is ready for payout processing.')
  })

  it('summarizes member exit queues by status', () => {
    expect(summarizeMemberExitRequests([
      { status: 'REQUESTED' },
      { status: 'UNDER_REVIEW' },
      { status: 'IN_PROGRESS' },
      { status: 'COMPLETED' },
      { status: 'REJECTED' },
    ])).toEqual({
      total: 5,
      pending: 2,
      inProgress: 1,
      completed: 1,
      rejected: 1,
    })
  })

  it('guides member exit workflows for both member and admin perspectives', () => {
    expect(getMemberExitWorkflowGuidance({
      hasActiveMember: true,
      requestCount: 0,
      activeRequestStatus: null,
      canReviewExitRequest: false,
      canProcessInstallment: false,
      isMemberOnly: true,
    }).title).toBe('You can submit your exit request.')

    expect(getMemberExitWorkflowGuidance({
      hasActiveMember: true,
      requestCount: 1,
      activeRequestStatus: 'REQUESTED',
      canReviewExitRequest: true,
      canProcessInstallment: false,
      isMemberOnly: false,
    }).title).toBe('This request needs review.')

    expect(getMemberExitWorkflowGuidance({
      hasActiveMember: true,
      requestCount: 1,
      activeRequestStatus: 'APPROVED',
      canReviewExitRequest: true,
      canProcessInstallment: true,
      isMemberOnly: false,
    }).title).toBe('Settlement can begin.')
  })

  it('maps unsupported review statuses back to under review', () => {
    expect(toReviewableClaimStatus('SUBMITTED')).toBe('UNDER_REVIEW')
    expect(toReviewableClaimStatus('PROCESSED')).toBe('UNDER_REVIEW')
    expect(toReviewableClaimStatus('APPROVED')).toBe('APPROVED')
  })
})