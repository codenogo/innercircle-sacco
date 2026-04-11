export type WorkflowStepState = 'complete' | 'active' | 'upcoming' | 'attention'

export interface WorkflowStepDefinition<T extends string = string> {
  id: T
  label: string
  owner?: string
  detail?: string
}

export interface WorkflowStepPresentation<T extends string = string> extends WorkflowStepDefinition<T> {
  state: WorkflowStepState
}

interface BuildWorkflowStepsOptions<T extends string> {
  steps: WorkflowStepDefinition<T>[]
  activeStepId?: T
  completedStepIds?: T[]
  attentionStepIds?: T[]
}

export function buildWorkflowSteps<T extends string>({
  steps,
  activeStepId,
  completedStepIds = [],
  attentionStepIds = [],
}: BuildWorkflowStepsOptions<T>): WorkflowStepPresentation<T>[] {
  const completed = new Set(completedStepIds)
  const attention = new Set(attentionStepIds)
  const activeIndex = activeStepId == null ? -1 : steps.findIndex(step => step.id === activeStepId)

  return steps.map((step, index) => {
    let state: WorkflowStepState = 'upcoming'

    if (attention.has(step.id)) {
      state = 'attention'
    } else if (step.id === activeStepId) {
      state = 'active'
    } else if (completed.has(step.id) || (activeIndex >= 0 && index < activeIndex)) {
      state = 'complete'
    }

    return { ...step, state }
  })
}