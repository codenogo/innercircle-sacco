import { describe, expect, it } from 'vitest'
import {
  formatAllowedRoles,
  groupOperations,
  summarizeOperations,
} from '../src/utils/operations'

describe('operations helpers', () => {
  it('summarizes visible operations by readiness', () => {
    const summary = summarizeOperations([
      { section: 'member-services', status: 'ready' },
      { section: 'money-workflows', status: 'ready' },
      { section: 'oversight', status: 'preview' },
    ])

    expect(summary.visibleCount).toBe(3)
    expect(summary.readyCount).toBe(2)
    expect(summary.previewCount).toBe(1)
    expect(Array.from(summary.sections)).toEqual(['member-services', 'money-workflows', 'oversight'])
  })

  it('groups operations in section order and omits empty sections', () => {
    const sections = groupOperations([
      { title: 'Audit Trail', section: 'oversight', status: 'ready' },
      { title: 'Contribution Operations', section: 'money-workflows', status: 'ready' },
    ])

    expect(sections.map(section => section.key)).toEqual(['money-workflows', 'oversight'])
    expect(sections[0]?.items.map(item => item.title)).toEqual(['Contribution Operations'])
    expect(sections[1]?.items.map(item => item.title)).toEqual(['Audit Trail'])
  })

  it('formats role labels for card metadata', () => {
    expect(formatAllowedRoles(['ADMIN', 'VICE_TREASURER', 'MEMBER'])).toBe('Admin, Vice Treasurer, Member')
  })
})