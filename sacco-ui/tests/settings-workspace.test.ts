import { describe, expect, it } from 'vitest'
import {
  buildSettingsOverviewItems,
  buildSettingsSectionDefinitions,
  resolveActiveSettingsSection,
} from '../src/utils/settingsWorkspace'

describe('settingsWorkspace helpers', () => {
  it('builds the full section list when config access is available', () => {
    const sections = buildSettingsSectionDefinitions({
      profileLoaded: true,
      configReadRestricted: false,
      trackedSystemKeyCount: 8,
      loanProductCount: 2,
      scheduleCount: 3,
      penaltyRuleCount: 4,
    })

    expect(sections.map(section => section.id)).toEqual(['profile', 'system', 'loan-products', 'schedules', 'penalties'])
    expect(sections[1]?.countValue).toBe('8')
  })

  it('collapses to profile when config reads are restricted', () => {
    const sections = buildSettingsSectionDefinitions({
      profileLoaded: false,
      configReadRestricted: true,
      trackedSystemKeyCount: 8,
      loanProductCount: 2,
      scheduleCount: 3,
      penaltyRuleCount: 4,
    })

    expect(sections).toHaveLength(1)
    expect(sections[0]).toMatchObject({ id: 'profile', countValue: 'Pending' })
    expect(resolveActiveSettingsSection('penalties', sections)).toBe('profile')
  })

  it('builds overview cards with the correct health label', () => {
    expect(buildSettingsOverviewItems({
      configReadRestricted: false,
      configHealth: { healthy: true, missingKeys: [], invalidKeys: [] },
      loanProductCount: 1,
      scheduleCount: 2,
      penaltyRuleCount: 3,
    })[4]).toEqual({ label: 'Config health', value: 'Healthy' })

    expect(buildSettingsOverviewItems({
      configReadRestricted: true,
      configHealth: null,
      loanProductCount: 1,
      scheduleCount: 2,
      penaltyRuleCount: 3,
    })[4]).toEqual({ label: 'Config health', value: 'Restricted' })
  })
})