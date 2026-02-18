export interface ContributionSummaryInput {
  amount: number
  status: string
}

type MonthField = {
  month?: string
  contributionMonth?: string
}

function resolveMonthValue(row: MonthField): string {
  const raw = row.month ?? row.contributionMonth ?? ''
  return raw.slice(0, 7)
}

export function filterByMonth<T extends MonthField>(rows: T[], month: string): T[] {
  return rows.filter(row => resolveMonthValue(row) === month)
}

export function summarizeContributions(
  rows: ContributionSummaryInput[],
  monthlyTarget = 15000,
) {
  const expected = rows.length * monthlyTarget
  const collected = rows.reduce((sum, row) => sum + row.amount, 0)
  const outstanding = expected - collected
  const paid = rows.filter(row => row.status === 'CONFIRMED').length
  const rate = expected === 0 ? 0 : Math.round((collected / expected) * 1000) / 10

  return { expected, collected, outstanding, paid, rate }
}
