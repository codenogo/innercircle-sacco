export interface MemberStatementEntry {
  date: string
  type: string
  description: string
  debit: number
  credit: number
  runningBalance: number
  referenceId: string
}

export interface MemberStatementResponse {
  memberId: string
  memberName: string
  fromDate: string
  toDate: string
  openingBalance: number
  closingBalance: number
  totalContributions: number
  totalLoansReceived: number
  totalRepayments: number
  totalPayouts: number
  totalPenalties: number
  entries: MemberStatementEntry[]
}

export interface FinancialSummaryResponse {
  fromDate: string
  toDate: string
  totalContributions: number
  totalLoansDisbursed: number
  totalRepayments: number
  totalPayouts: number
  totalPenaltiesCollected: number
  netPosition: number
  activeMemberCount: number
  activeLoansCount: number
  outstandingLoanBalance: number
}
