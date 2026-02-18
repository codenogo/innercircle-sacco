export type ContributionStatus = 'PENDING' | 'CONFIRMED' | 'REVERSED'
export type PaymentMode = 'MPESA' | 'BANK_TRANSFER' | 'CASH' | 'CHECK'

export interface ContributionResponse {
  id: string
  memberId: string
  amount: number
  category: string
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

export interface BulkContributionRequest {
  contributions: RecordContributionRequest[]
}

export interface ContributionCategoryResponse {
  id: string
  name: string
  description: string
  mandatory: boolean
  active: boolean
  createdAt: string
}
