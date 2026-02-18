export interface SystemConfigResponse {
  id: string
  configKey: string
  configValue: string
  description: string
  createdAt: string
  updatedAt: string
}

export type InterestMethod = 'REDUCING_BALANCE' | 'FLAT_RATE'

export interface LoanProductConfigResponse {
  id: string
  name: string
  interestMethod: InterestMethod
  annualInterestRate: number
  maxTermMonths: number
  maxAmount: number
  requiresGuarantor: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export type ContributionFrequency = 'WEEKLY' | 'MONTHLY'

export interface ContributionScheduleConfigResponse {
  id: string
  name: string
  frequency: ContributionFrequency
  amount: number
  penaltyEnabled: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export type PenaltyType = 'LATE_CONTRIBUTION' | 'LOAN_DEFAULT'
export type CalculationMethod = 'FLAT' | 'PERCENTAGE'

export interface PenaltyRuleResponse {
  id: string
  name: string
  penaltyType: PenaltyType
  rate: number
  calculationMethod: CalculationMethod
  active: boolean
  compounding: boolean
  createdAt: string
  updatedAt: string
}
