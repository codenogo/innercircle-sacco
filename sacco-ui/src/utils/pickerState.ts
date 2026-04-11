export interface ParsedDateValue {
  year: number
  month: number
  day: number
}

export interface ParsedMonthValue {
  year: number
  month: number
}

export function parseDateValue(iso: string, today = new Date()): ParsedDateValue {
  if (!iso) {
    return { year: today.getFullYear(), month: today.getMonth(), day: today.getDate() }
  }

  const [year, month, day] = iso.split('-').map(Number)
  if (Number.isNaN(year) || Number.isNaN(month) || Number.isNaN(day)) {
    return { year: today.getFullYear(), month: today.getMonth(), day: today.getDate() }
  }

  return { year, month: month - 1, day }
}

export function parseMonthValue(value: string, today = new Date()): ParsedMonthValue {
  if (!value) {
    return { year: today.getFullYear(), month: today.getMonth() }
  }

  const [year, month] = value.split('-').map(Number)
  if (Number.isNaN(year) || Number.isNaN(month) || month < 1 || month > 12) {
    return { year: today.getFullYear(), month: today.getMonth() }
  }

  return { year, month: month - 1 }
}