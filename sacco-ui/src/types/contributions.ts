export type ContributionStatus = 'PENDING' | 'CONFIRMED' | 'REVERSED'
export type PaymentMode = 'MPESA' | 'BANK' | 'CASH' | 'CHECK'

export interface ContributionResponse {
  id: string
  memberId: string
  amount: number
  contributionAmount: number
  welfareAmount: number
  welfareSplitApplied: boolean
  category: ContributionCategoryResponse
  paymentMode: PaymentMode
  contributionMonth: string
  status: ContributionStatus
  contributionDate: string
  referenceNumber: string
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface ContributionSummaryResponse {
  memberId: string
  totalContributed: number
  totalPending: number
  totalPenalties: number
  lastContributionDate: string | null
}

export interface RecordContributionRequest {
  memberId: string
  amount: number
  categoryId: string
  paymentMode: PaymentMode
  contributionMonth: string
  contributionDate: string
  referenceNumber?: string
  notes?: string
}

export interface BulkContributionItemRequest {
  memberId: string
  amount: number
  paymentMode?: PaymentMode
  contributionMonth?: string
  contributionDate?: string
  referenceNumber?: string
  notes?: string
}

export interface BulkContributionRequest {
  paymentMode: PaymentMode
  contributionMonth: string
  contributionDate: string
  categoryId: string
  batchReference?: string
  contributions: BulkContributionItemRequest[]
}

export interface ContributionCategoryRequest {
  name: string
  description?: string
  active?: boolean
  mandatory?: boolean
  isMandatory?: boolean
  welfareEligible?: boolean
}

export interface ContributionCategoryResponse {
  id: string
  name: string
  description: string
  mandatory?: boolean
  isMandatory?: boolean
  welfareEligible: boolean
  active: boolean
}

export interface ContributionWelfarePolicyResponse {
  enabled: boolean
  fixedAmount: number
}
