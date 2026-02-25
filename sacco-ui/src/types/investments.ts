export type InvestmentType =
  | 'TREASURY_BILL'
  | 'TREASURY_BOND'
  | 'FIXED_DEPOSIT'
  | 'MONEY_MARKET'
  | 'UNIT_TRUST'
  | 'REAL_ESTATE'
  | 'CORPORATE_BOND'
  | 'EQUITY'
  | 'SACCO_PLACEMENT'

export type InvestmentStatus =
  | 'PROPOSED'
  | 'APPROVED'
  | 'REJECTED'
  | 'ACTIVE'
  | 'MATURED'
  | 'PARTIALLY_DISPOSED'
  | 'ROLLED_OVER'
  | 'CLOSED'

export type IncomeType = 'INTEREST' | 'DIVIDEND' | 'RENT' | 'CAPITAL_GAIN' | 'COUPON'

export interface InvestmentResponse {
  id: string
  referenceNumber: string
  name: string
  investmentType: InvestmentType
  status: InvestmentStatus
  institution: string
  faceValue: number
  purchasePrice: number
  currentValue: number
  interestRate: number
  purchaseDate: string
  maturityDate: string | null
  /** Unit trust specific */
  units: number | null
  navPerUnit: number | null
  notes: string | null
  approvedBy: string | null
  approvedAt: string | null
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface CreateInvestmentRequest {
  name: string
  investmentType: InvestmentType
  institution: string
  faceValue: number
  purchasePrice: number
  interestRate: number
  purchaseDate: string
  maturityDate?: string
  /** Unit trust specific */
  units?: number
  navPerUnit?: number
  notes?: string
}

export interface InvestmentIncomeResponse {
  id: string
  investmentId: string
  incomeType: IncomeType
  amount: number
  incomeDate: string
  referenceNumber: string | null
  notes: string | null
  recordedBy: string
  createdAt: string
}

export interface RecordIncomeRequest {
  incomeType: IncomeType
  amount: number
  incomeDate: string
  referenceNumber?: string
  notes?: string
}

export interface InvestmentValuationResponse {
  id: string
  investmentId: string
  marketValue: number
  navPerUnit: number | null
  valuationDate: string
  source: string
  createdAt: string
}

export interface RecordValuationRequest {
  marketValue?: number
  navPerUnit?: number
  valuationDate: string
  source: string
}

export interface DisposeInvestmentRequest {
  disposalType: 'FULL' | 'PARTIAL' | 'MATURITY'
  proceedsAmount: number
  fees: number
  disposalDate: string
  unitsRedeemed?: number
  notes?: string
}

export interface RollOverRequest {
  newMaturityDate: string
  newInterestRate: number
  notes?: string
}

export interface InvestmentSummary {
  totalInvested: number
  currentValue: number
  unrealisedGain: number
  incomeYtd: number
  activeCount: number
  maturedCount: number
  proposedCount: number
  closedCount: number
  byType: { type: InvestmentType; amount: number; percentage: number }[]
}

export const INVESTMENT_TYPE_LABELS: Record<InvestmentType, string> = {
  TREASURY_BILL: 'Treasury Bill',
  TREASURY_BOND: 'Treasury Bond',
  FIXED_DEPOSIT: 'Fixed Deposit',
  MONEY_MARKET: 'Money Market Fund',
  UNIT_TRUST: 'Unit Trust / Fund',
  REAL_ESTATE: 'Real Estate',
  CORPORATE_BOND: 'Corporate Bond',
  EQUITY: 'Equity / Shares',
  SACCO_PLACEMENT: 'SACCO Placement',
}

export const INCOME_TYPE_LABELS: Record<IncomeType, string> = {
  INTEREST: 'Interest',
  DIVIDEND: 'Dividend',
  RENT: 'Rent',
  CAPITAL_GAIN: 'Capital Gain',
  COUPON: 'Coupon',
}

