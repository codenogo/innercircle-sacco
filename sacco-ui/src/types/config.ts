export interface SystemConfigResponse {
  id: string
  configKey: string
  configValue: string
  description: string
  createdAt: string
  updatedAt: string
}

export type InterestMethod = 'REDUCING_BALANCE' | 'FLAT_RATE'

export interface LoanProductRequest {
  name: string
  interestMethod: InterestMethod
  annualInterestRate: number
  minTermMonths: number
  maxTermMonths: number
  minAmount: number
  maxAmount: number
  contributionCapPercent?: number
  poolCapAmount?: number
  rolloverEnabled: boolean
  maxRolloverMonths?: number
  rolloverSurchargeRate?: number
  interestAccrualEnabled: boolean
  requiresGuarantor: boolean
  active: boolean
}

export interface LoanProductConfigResponse {
  id: string
  name: string
  interestMethod: InterestMethod
  annualInterestRate: number
  minTermMonths: number
  maxTermMonths: number
  minAmount: number
  maxAmount: number
  contributionCapPercent?: number
  poolCapAmount?: number
  rolloverEnabled: boolean
  maxRolloverMonths?: number
  rolloverSurchargeRate?: number
  interestAccrualEnabled: boolean
  requiresGuarantor: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export type ContributionFrequency = 'WEEKLY' | 'MONTHLY'

export interface ContributionScheduleRequest {
  name: string
  frequency: ContributionFrequency
  amount: number
  dueDayOfMonth: number
  gracePeriodDays: number
  mandatory: boolean
  expectedGrossAmount: number
  penaltyEnabled: boolean
  active: boolean
}

export interface ContributionScheduleConfigResponse {
  id: string
  name: string
  frequency: ContributionFrequency
  amount: number
  dueDayOfMonth: number
  gracePeriodDays: number
  mandatory: boolean
  expectedGrossAmount: number
  penaltyEnabled: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export type PenaltyType = 'LATE_CONTRIBUTION' | 'LOAN_DEFAULT'
export type CalculationMethod = 'FLAT' | 'PERCENTAGE'
export type PenaltyFrequency = 'DAILY' | 'MONTHLY' | 'ONCE'

export interface PenaltyTierRequest {
  sequence: number
  startOverdueDay: number
  endOverdueDay?: number
  frequency: PenaltyFrequency
  calculationMethod: CalculationMethod
  rate: number
  maxApplications?: number
  active: boolean
}

export interface PenaltyRuleRequest {
  name: string
  penaltyType: PenaltyType
  rate: number
  calculationMethod: CalculationMethod
  tiers: PenaltyTierRequest[]
  active: boolean
}

export interface PenaltyRuleResponse {
  id: string
  name: string
  penaltyType: PenaltyType
  rate: number
  calculationMethod: CalculationMethod
  tiers: PenaltyTierRequest[]
  active: boolean
  compounding: boolean
  createdAt: string
  updatedAt: string
}

export interface ConfigHealthResponse {
  healthy: boolean
  missingKeys: string[]
  invalidKeys: string[]
}
