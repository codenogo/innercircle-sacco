import type { StatCardItem } from '../components/StatCard'
import type { ConfigHealthResponse } from '../types/config'

export type SettingsSectionId = 'profile' | 'system' | 'loan-products' | 'schedules' | 'penalties'

export interface SettingsSectionDefinition {
  id: SettingsSectionId
  label: string
  title: string
  description: string
  countLabel: string
  countValue: string
}

interface SettingsSectionCounts {
  profileLoaded: boolean
  configReadRestricted: boolean
  trackedSystemKeyCount: number
  loanProductCount: number
  scheduleCount: number
  penaltyRuleCount: number
}

interface SettingsOverviewInput {
  configReadRestricted: boolean
  configHealth: ConfigHealthResponse | null
  loanProductCount: number
  scheduleCount: number
  penaltyRuleCount: number
}

export function buildSettingsSectionDefinitions(input: SettingsSectionCounts): SettingsSectionDefinition[] {
  const items: SettingsSectionDefinition[] = [
    {
      id: 'profile',
      label: 'Profile',
      title: 'Profile & access',
      description: 'Identity, role, and who is currently managing policy settings.',
      countLabel: 'Account',
      countValue: input.profileLoaded ? 'Loaded' : 'Pending',
    },
  ]

  if (!input.configReadRestricted) {
    items.push(
      {
        id: 'system',
        label: 'System',
        title: 'System controls',
        description: 'Core operating values like organization identity, batch timing, and policy thresholds.',
        countLabel: 'Tracked keys',
        countValue: String(input.trackedSystemKeyCount),
      },
      {
        id: 'loan-products',
        label: 'Loan products',
        title: 'Loan products',
        description: 'Defaults that shape borrowing terms, caps, rollover behavior, and guarantor requirements.',
        countLabel: 'Products',
        countValue: String(input.loanProductCount),
      },
      {
        id: 'schedules',
        label: 'Schedules',
        title: 'Contribution schedules',
        description: 'Recurring contribution plans, due days, penalties, and expected gross amounts.',
        countLabel: 'Schedules',
        countValue: String(input.scheduleCount),
      },
      {
        id: 'penalties',
        label: 'Penalty rules',
        title: 'Penalty rules',
        description: 'Late contribution and loan default penalties, tiers, frequencies, and activation state.',
        countLabel: 'Rules',
        countValue: String(input.penaltyRuleCount),
      },
    )
  }

  return items
}

export function buildSettingsOverviewItems(input: SettingsOverviewInput): StatCardItem[] {
  const managedAreas = input.configReadRestricted ? 1 : 5
  const healthyValue = input.configReadRestricted
    ? 'Restricted'
    : input.configHealth == null
      ? 'Unknown'
      : input.configHealth.healthy
        ? 'Healthy'
        : 'Needs review'

  return [
    { label: 'Visible areas', value: managedAreas },
    { label: 'Loan products', value: input.loanProductCount },
    { label: 'Schedules', value: input.scheduleCount },
    { label: 'Penalty rules', value: input.penaltyRuleCount },
    { label: 'Config health', value: healthyValue },
  ]
}

export function resolveActiveSettingsSection(activeSection: SettingsSectionId, sections: SettingsSectionDefinition[]): SettingsSectionId {
  return sections.some(section => section.id === activeSection) ? activeSection : (sections[0]?.id ?? 'profile')
}