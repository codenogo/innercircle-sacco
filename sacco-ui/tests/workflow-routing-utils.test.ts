import { describe, expect, it } from 'vitest'
import {
  getMeetingAttentionStepIds,
  getMeetingRecommendedTab,
  getMeetingWorkflowStep,
  getMemberExitAttentionSteps,
  getMemberExitRecommendedTab,
  getMemberExitSelectionTab,
  getMemberExitWorkflowStep,
  getWelfareClaimFocusTab,
  getWelfareRecommendedTab,
  getWelfareWorkflowStep,
} from '../src/utils/policyWorkflows'

describe('workflow routing helpers', () => {
  describe('meetings routing', () => {
    it('routes empty meeting context to session setup', () => {
      const step = getMeetingWorkflowStep({
        activeMeetingId: '',
        latestMeetingId: null,
        latestMeetingIsFinalized: false,
        openFineCount: 0,
        hasSelectedMember: false,
      })

      expect(step).toBe('session')
      expect(getMeetingRecommendedTab(step)).toBe('session')
    })

    it('routes an open meeting without a selected member to attendance', () => {
      const step = getMeetingWorkflowStep({
        activeMeetingId: 'meeting-1',
        latestMeetingId: 'meeting-1',
        latestMeetingIsFinalized: false,
        openFineCount: 0,
        hasSelectedMember: false,
      })

      expect(step).toBe('attendance')
      expect(getMeetingRecommendedTab(step)).toBe('attendance')
    })

    it('routes an open meeting with a selected member to finalization', () => {
      const step = getMeetingWorkflowStep({
        activeMeetingId: 'meeting-1',
        latestMeetingId: 'meeting-1',
        latestMeetingIsFinalized: false,
        openFineCount: 0,
        hasSelectedMember: true,
      })

      expect(step).toBe('finalize')
      expect(getMeetingRecommendedTab(step)).toBe('finalize')
    })

    it('routes finalized or off-context work with open fines to the fine queue', () => {
      const step = getMeetingWorkflowStep({
        activeMeetingId: 'meeting-older',
        latestMeetingId: 'meeting-latest',
        latestMeetingIsFinalized: true,
        openFineCount: 2,
        hasSelectedMember: false,
      })

      expect(step).toBe('fines')
      expect(getMeetingRecommendedTab(step)).toBe('fines')
      expect(getMeetingAttentionStepIds({
        activeMeetingId: 'meeting-older',
        latestMeetingId: 'meeting-latest',
        latestMeetingIsFinalized: true,
        openFineCount: 2,
        hasSelectedMember: false,
      })).toEqual(['fines'])
    })
  })

  describe('welfare routing', () => {
    it('focuses approved and processed claims on processing', () => {
      expect(getWelfareClaimFocusTab('APPROVED')).toBe('processing')
      expect(getWelfareClaimFocusTab('PROCESSED')).toBe('processing')
      expect(getWelfareClaimFocusTab('SUBMITTED')).toBe('review')
    })

    it('routes no active member to household setup', () => {
      const step = getWelfareWorkflowStep({ hasActiveMember: false, activeClaimStatus: null })
      expect(step).toBe('household')
      expect(getWelfareRecommendedTab(step)).toBe('household')
    })

    it('routes a loaded member without a claim to claim intake', () => {
      const step = getWelfareWorkflowStep({ hasActiveMember: true, activeClaimStatus: null })
      expect(step).toBe('claim')
      expect(getWelfareRecommendedTab(step)).toBe('claim')
    })

    it('routes submitted claims to review and approved claims to processing', () => {
      const reviewStep = getWelfareWorkflowStep({ hasActiveMember: true, activeClaimStatus: 'UNDER_REVIEW' })
      const processingStep = getWelfareWorkflowStep({ hasActiveMember: true, activeClaimStatus: 'APPROVED' })

      expect(reviewStep).toBe('review')
      expect(getWelfareRecommendedTab(reviewStep)).toBe('review')
      expect(processingStep).toBe('processing')
      expect(getWelfareRecommendedTab(processingStep)).toBe('processing')
    })
  })

  describe('member exit routing', () => {
    it('routes missing member or empty queue back to notice', () => {
      expect(getMemberExitWorkflowStep({ hasActiveMember: false, requestCount: 0, activeRequestStatus: null })).toBe('notice')
      expect(getMemberExitRecommendedTab({ hasActiveMember: false, requestCount: 0, activeRequestStatus: null })).toBe('notice')
      expect(getMemberExitWorkflowStep({ hasActiveMember: true, requestCount: 0, activeRequestStatus: null })).toBe('notice')
    })

    it('distinguishes queue focus from review stage when requests exist but none is selected', () => {
      expect(getMemberExitRecommendedTab({
        hasActiveMember: true,
        requestCount: 2,
        activeRequestStatus: null,
      })).toBe('queue')
      expect(getMemberExitWorkflowStep({
        hasActiveMember: true,
        requestCount: 2,
        activeRequestStatus: null,
      })).toBe('review')
    })

    it('routes requested exits to review and approved exits to settlement', () => {
      expect(getMemberExitRecommendedTab({
        hasActiveMember: true,
        requestCount: 1,
        activeRequestStatus: 'REQUESTED',
      })).toBe('review')
      expect(getMemberExitWorkflowStep({
        hasActiveMember: true,
        requestCount: 1,
        activeRequestStatus: 'APPROVED',
      })).toBe('settlement')
    })

    it('routes completed requests to closed workflow state and queue tab', () => {
      expect(getMemberExitWorkflowStep({
        hasActiveMember: true,
        requestCount: 1,
        activeRequestStatus: 'COMPLETED',
      })).toBe('closed')
      expect(getMemberExitRecommendedTab({
        hasActiveMember: true,
        requestCount: 1,
        activeRequestStatus: 'COMPLETED',
      })).toBe('queue')
    })

    it('maps row selection to the correct follow-up tab', () => {
      expect(getMemberExitSelectionTab('UNDER_REVIEW')).toBe('review')
      expect(getMemberExitSelectionTab('IN_PROGRESS')).toBe('settlement')
      expect(getMemberExitSelectionTab('REJECTED')).toBe('queue')
    })

    it('flags pending review and settlement queues as attention steps', () => {
      expect(getMemberExitAttentionSteps({
        pendingCount: 2,
        settlementQueueCount: 1,
        workflowStep: 'notice',
      })).toEqual(['review', 'settlement'])
    })
  })
})