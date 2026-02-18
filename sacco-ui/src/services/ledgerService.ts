import { apiRequest } from './apiClient'
import type {
  AccountResponse,
  JournalEntryResponse,
  Page,
  TrialBalanceResponse,
  IncomeStatementResponse,
  BalanceSheetResponse,
} from '../types/ledger'

export async function getAccounts(): Promise<AccountResponse[]> {
  return apiRequest<AccountResponse[]>('/api/v1/ledger/accounts')
}

export async function getJournalEntries(page = 0, size = 20, sort?: string): Promise<Page<JournalEntryResponse>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (sort) params.set('sort', sort)
  return apiRequest<Page<JournalEntryResponse>>(`/api/v1/ledger/journal-entries?${params}`)
}

export async function getTrialBalance(asOfDate?: string): Promise<TrialBalanceResponse> {
  const params = new URLSearchParams()
  if (asOfDate) params.set('asOfDate', asOfDate)
  const query = params.toString()
  return apiRequest<TrialBalanceResponse>(`/api/v1/ledger/trial-balance${query ? `?${query}` : ''}`)
}

export async function getIncomeStatement(startDate: string, endDate: string): Promise<IncomeStatementResponse> {
  const params = new URLSearchParams({ startDate, endDate })
  return apiRequest<IncomeStatementResponse>(`/api/v1/ledger/income-statement?${params}`)
}

export async function getBalanceSheet(asOfDate?: string): Promise<BalanceSheetResponse> {
  const params = new URLSearchParams()
  if (asOfDate) params.set('asOfDate', asOfDate)
  const query = params.toString()
  return apiRequest<BalanceSheetResponse>(`/api/v1/ledger/balance-sheet${query ? `?${query}` : ''}`)
}
