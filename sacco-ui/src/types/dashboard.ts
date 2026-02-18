export interface TreasurerDashboardResponse {
  totalCollectionsThisMonth: number
  totalDisbursementsThisMonth: number
  pendingApprovals: number
  overdueLoans: number
  cashPosition: number
  activeMemberCount: number
  totalShareCapital: number
}

export interface AdminDashboardResponse {
  totalMembers: number
  activeMembers: number
  totalAssets: number
  totalLiabilities: number
  totalLoanProducts: number
  totalActiveLoans: number
  totalOutstandingLoans: number
  recentAuditEventsCount: number
}

export interface SaccoStateResponse {
  totalMembers: number
  activeMembers: number
  totalShareCapital: number
  totalOutstandingLoans: number
  totalContributions: number
  totalPayouts: number
  loanRecoveryRate: number
  memberGrowthRate: number
}

export interface MonthlyDataPoint {
  month: number
  monthName: string
  amount: number
}

export interface DashboardAnalyticsResponse {
  year: number
  loansDisbursed: MonthlyDataPoint[]
  amountRepaid: MonthlyDataPoint[]
  interestAccrued: MonthlyDataPoint[]
  contributionsReceived: MonthlyDataPoint[]
  payoutsProcessed: MonthlyDataPoint[]
}
