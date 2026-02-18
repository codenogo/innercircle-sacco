export type PayoutStatus = 'PENDING' | 'APPROVED' | 'PROCESSING' | 'COMPLETED' | 'REJECTED'
export type PayoutType = 'BANK_TRANSFER' | 'MPESA' | 'CASH' | 'SHARE_WITHDRAWAL'

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
