import './WorkflowWorkspace.css'
import type { WorkflowStepPresentation } from '../utils/workflowWorkspace'

export interface WorkflowTab<T extends string = string> {
  id: T
  label: string
  badge?: string
  disabled?: boolean
}

interface WorkflowTrackerProps<T extends string = string> {
  steps: WorkflowStepPresentation<T>[]
}

interface WorkflowTabsProps<T extends string = string> {
  tabs: WorkflowTab<T>[]
  activeTab: T
  onChange: (tabId: T) => void
}

export function WorkflowTracker<T extends string>({ steps }: WorkflowTrackerProps<T>) {
  return (
    <ol className="workflow-tracker" aria-label="Workflow progress tracker">
      {steps.map((step, index) => (
        <li key={step.id} className={`workflow-step workflow-step--${step.state}`}>
          <span className="workflow-step__marker" aria-hidden="true">{index + 1}</span>
          <div className="workflow-step__content">
            <span className="workflow-step__label">{step.label}</span>
            {step.owner && <span className="workflow-step__owner">Owner · {step.owner}</span>}
            {step.detail && <span className="workflow-step__detail">{step.detail}</span>}
          </div>
        </li>
      ))}
    </ol>
  )
}

export function WorkflowTabs<T extends string>({ tabs, activeTab, onChange }: WorkflowTabsProps<T>) {
  return (
    <div className="workflow-tabs" role="tablist" aria-label="Workflow sections">
      {tabs.map(tab => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={tab.id === activeTab}
          className={`workflow-tab ${tab.id === activeTab ? 'workflow-tab--active' : ''}`}
          disabled={tab.disabled}
          onClick={() => onChange(tab.id)}
        >
          <span>{tab.label}</span>
          {tab.badge && <span className="workflow-tab__badge">{tab.badge}</span>}
        </button>
      ))}
    </div>
  )
}