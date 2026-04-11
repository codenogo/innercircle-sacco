import { describe, expect, it } from 'vitest'
import { buildWorkflowSteps } from '../src/utils/workflowWorkspace'

describe('buildWorkflowSteps', () => {
  const steps = [
    { id: 'one', label: 'One' },
    { id: 'two', label: 'Two' },
    { id: 'three', label: 'Three' },
  ] as const

  it('marks earlier steps complete when a later step is active', () => {
    const result = buildWorkflowSteps({ steps: [...steps], activeStepId: 'two' })
    expect(result.map(step => step.state)).toEqual(['complete', 'active', 'upcoming'])
  })

  it('allows explicit attention states to override the active sequence', () => {
    const result = buildWorkflowSteps({
      steps: [...steps],
      activeStepId: 'three',
      attentionStepIds: ['two'],
    })

    expect(result.map(step => step.state)).toEqual(['complete', 'attention', 'active'])
  })

  it('preserves explicit completion when no active step is provided', () => {
    const result = buildWorkflowSteps({
      steps: [...steps],
      completedStepIds: ['one'],
    })

    expect(result.map(step => step.state)).toEqual(['complete', 'upcoming', 'upcoming'])
  })
})