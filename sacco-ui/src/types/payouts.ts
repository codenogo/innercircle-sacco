export type PayoutStatus = 'PENDING' | 'APPROVED' | 'PROCESSED' | 'FAILED'
export type PayoutType = 'MERRY_GO_ROUND' | 'AD_HOC' | 'DIVIDEND'

export interface PayoutResponse {
  id: string
  memberId: string
  amount: number
  type: PayoutType
  status: PayoutStatus
  approvedBy: string | null
  processedAt: string | null
  referenceNumber: string | null
  createdAt: string
  updatedAt: string
}

export interface PayoutRequest {
  memberId: string
  amount: number
  type: PayoutType
}
