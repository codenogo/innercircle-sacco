import type { UserRole } from '../types/roles'

export type OperationStatus = 'ready' | 'preview'

export type OperationSectionKey = 'member-services' | 'money-workflows' | 'oversight'

export interface OperationSummaryInput {
  section: OperationSectionKey
  status: OperationStatus
}

export const operationSectionMeta: Record<OperationSectionKey, { title: string; description: string }> = {
  'member-services': {
    title: 'Member services & governance',
    description: 'Open the workflows used for member records, welfare, meetings, exits, and day-to-day service requests.',
  },
  'money-workflows': {
    title: 'Money movement & portfolio work',
    description: 'Start here for collections, loans, payouts, petty cash, and investment operations.',
  },
  oversight: {
    title: 'Administration, controls & oversight',
    description: 'Use these workflows for user control, reporting, configuration, audit review, and exports.',
  },
}

const operationSectionOrder: OperationSectionKey[] = ['member-services', 'money-workflows', 'oversight']

const roleLabels: Record<UserRole, string> = {
  ADMIN: 'Admin',
  TREASURER: 'Treasurer',
  VICE_TREASURER: 'Vice Treasurer',
  SECRETARY: 'Secretary',
  CHAIRPERSON: 'Chairperson',
  VICE_CHAIRPERSON: 'Vice Chairperson',
  MEMBER: 'Member',
}

export function summarizeOperations(items: OperationSummaryInput[]) {
  return items.reduce((summary, item) => {
    summary.visibleCount += 1
    if (item.status === 'ready') summary.readyCount += 1
    if (item.status === 'preview') summary.previewCount += 1
    summary.sections.add(item.section)
    return summary
  }, {
    visibleCount: 0,
    readyCount: 0,
    previewCount: 0,
    sections: new Set<OperationSectionKey>(),
  })
}

export function groupOperations<T extends OperationSummaryInput>(items: T[]) {
  return operationSectionOrder
    .map(section => ({
      key: section,
      title: operationSectionMeta[section].title,
      description: operationSectionMeta[section].description,
      items: items.filter(item => item.section === section),
    }))
    .filter(section => section.items.length > 0)
}

export function formatAllowedRoles(roles: UserRole[]): string {
  return roles.map(role => roleLabels[role]).join(', ')
}