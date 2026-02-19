export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE'

export interface AccountResponse {
  id: string
  accountCode: string
  accountName: string
  accountType: AccountType
  balance: number
}

export interface JournalLineDto {
  id: string
  accountCode: string
  accountName: string
  debitAmount: string
  creditAmount: string
  description: string
}

export interface JournalEntryResponse {
  id: string
  entryNumber: string
  transactionDate: string
  description: string
  transactionType: string
  referenceId: string | null
  posted: boolean
  postedAt: string | null
  journalLines: JournalLineDto[]
  createdAt: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export type TransactionType =
  | 'CONTRIBUTION'
  | 'LOAN_DISBURSEMENT'
  | 'LOAN_REPAYMENT'
  | 'PAYOUT'
  | 'PETTY_CASH_DISBURSEMENT'
  | 'PETTY_CASH_SETTLEMENT'
  | 'PENALTY'
  | 'INTEREST_ACCRUAL'
  | 'MANUAL_ADJUSTMENT'
  | 'LOAN_REVERSAL'
  | 'CONTRIBUTION_REVERSAL'
  | 'PENALTY_WAIVER'
  | 'BENEFIT_DISTRIBUTION'

export interface JournalEntryFilters {
  entryNumber?: string
  description?: string
  dateFrom?: string
  dateTo?: string
  transactionType?: TransactionType
  accountId?: string
}

export interface TrialBalanceEntry {
  accountCode: string
  accountName: string
  debit: number
  credit: number
}

export interface TrialBalanceResponse {
  asOfDate: string
  entries: TrialBalanceEntry[]
  totalDebits: number
  totalCredits: number
}

export interface AccountLineItem {
  accountCode: string
  accountName: string
  amount: string
}

export interface IncomeStatementResponse {
  startDate: string
  endDate: string
  revenue: AccountLineItem[]
  expenses: AccountLineItem[]
  totalRevenue: string
  totalExpenses: string
  netIncome: string
}

export interface BalanceSheetResponse {
  asOfDate: string
  assets: AccountLineItem[]
  liabilities: AccountLineItem[]
  equity: AccountLineItem[]
  totalAssets: string
  totalLiabilities: string
  totalEquity: string
  totalLiabilitiesAndEquity: string
  balanced: boolean
}
