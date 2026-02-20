export type LoanStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DISBURSED' | 'REPAYING' | 'CLOSED' | 'DEFAULTED'
export type InterestMethod = 'FLAT' | 'REDUCING_BALANCE'

export interface LoanResponse {
  id: string
  loanNumber: string | null
  memberId: string
  loanProductId: string
  principalAmount: number
  interestRate: number
  termMonths: number
  interestMethod: InterestMethod
  status: LoanStatus
  purpose: string
  approvedBy: string | null
  approvedAt: string | null
  disbursedAt: string | null
  totalRepaid: number
  outstandingBalance: number
  totalInterestAccrued: number
  totalInterestPaid: number
  totalPenalties: number
  createdAt: string
  updatedAt: string
}

export interface LoanApplicationRequest {
  memberId: string
  loanProductId: string
  principalAmount: number
  termMonths: number
  purpose: string
}

export interface RepaymentRequest {
  amount: number
  referenceNumber?: string
}

export interface LoanSummaryResponse {
  memberId: string
  totalLoans: number
  activeLoans: number
  closedLoans: number
  totalBorrowed: number
  totalRepaid: number
  totalOutstanding: number
  loans: LoanResponse[]
}

export interface RepaymentScheduleResponse {
  id: string
  loanId: string
  installmentNumber: number
  dueDate: string
  principalAmount: number
  interestAmount: number
  totalAmount: number
  paid: boolean
}

export interface MonthlyInterestSummary {
  month: string
  totalInterestAccrued: number
  totalInterestReceived: number
  totalInterestArrears: number
  activeLoansCount: number
  loansWithArrearsCount: number
}
