import { describe, expect, it } from 'vitest'
import { filterByMonth, summarizeContributions } from '../src/utils/contributions'

describe('contributions month filtering', () => {
  const rows = [
    { id: '1', month: '2026-02', amount: 15000, status: 'CONFIRMED' },
    { id: '2', month: '2026-02', amount: 10000, status: 'PENDING' },
    { id: '3', month: '2026-03', amount: 15000, status: 'CONFIRMED' },
  ]

  it('filters rows by selected month', () => {
    expect(filterByMonth(rows, '2026-02')).toEqual([
      { id: '1', month: '2026-02', amount: 15000, status: 'CONFIRMED' },
      { id: '2', month: '2026-02', amount: 10000, status: 'PENDING' },
    ])
  })

  it('supports backend contributionMonth values', () => {
    const backendRows = [
      { id: 'a', contributionMonth: '2026-02-01', amount: 1000, status: 'CONFIRMED' },
      { id: 'b', contributionMonth: '2026-03-01', amount: 2000, status: 'PENDING' },
    ]

    expect(filterByMonth(backendRows, '2026-02')).toEqual([
      { id: 'a', contributionMonth: '2026-02-01', amount: 1000, status: 'CONFIRMED' },
    ])
  })

  it('summarizes filtered rows', () => {
    const summary = summarizeContributions(filterByMonth(rows, '2026-02'))
    expect(summary).toEqual({
      expected: 30000,
      collected: 25000,
      outstanding: 5000,
      paid: 1,
      rate: 83.3,
    })
  })

  it('returns zero rate when no rows exist for the month', () => {
    expect(summarizeContributions(filterByMonth(rows, '2026-04')).rate).toBe(0)
  })
})
