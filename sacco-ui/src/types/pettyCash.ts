export type PettyCashVoucherStatus = 'SUBMITTED' | 'APPROVED' | 'DISBURSED' | 'SETTLED' | 'REJECTED'

export type PettyCashExpenseType =
  | 'OPERATIONS'
  | 'ADMINISTRATION'
  | 'TRANSPORT'
  | 'UTILITIES'
  | 'MAINTENANCE'
  | 'WELFARE'
  | 'OTHER'

export interface PettyCashVoucherResponse {
  id: string
  referenceNumber: string
  amount: number
  purpose: string
  expenseType: PettyCashExpenseType
  status: PettyCashVoucherStatus
  requestDate: string
  approvedBy: string | null
  disbursedBy: string | null
  settledBy: string | null
  rejectedBy: string | null
  disbursedAt: string | null
  settledAt: string | null
  rejectedAt: string | null
  receiptNumber: string | null
  rejectionReason: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
  createdBy: string | null
}

export interface PettyCashSummaryResponse {
  totalCount: number
  submittedCount: number
  approvedCount: number
  disbursedCount: number
  settledCount: number
  rejectedCount: number
  disbursedAmount: number
  settledAmount: number
  outstandingAmount: number
}

export interface CreatePettyCashVoucherRequest {
  amount: number
  purpose: string
  expenseType: PettyCashExpenseType
  requestDate?: string
  notes?: string
}

export interface ApprovePettyCashRequest {
  overrideReason?: string
}

export interface SettlePettyCashVoucherRequest {
  receiptNumber: string
  notes?: string
}

export interface RejectPettyCashVoucherRequest {
  reason: string
}
