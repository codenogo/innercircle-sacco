import { describe, expect, it } from 'vitest'
import { parseDateValue, parseMonthValue } from '../src/utils/pickerState'

describe('pickerState helpers', () => {
  const fallbackDate = new Date(2026, 1, 18)

  it('parses a valid ISO date into picker coordinates', () => {
    expect(parseDateValue('2026-03-09', fallbackDate)).toEqual({ year: 2026, month: 2, day: 9 })
  })

  it('falls back to today when the date value is empty or invalid', () => {
    expect(parseDateValue('', fallbackDate)).toEqual({ year: 2026, month: 1, day: 18 })
    expect(parseDateValue('bad-input', fallbackDate)).toEqual({ year: 2026, month: 1, day: 18 })
  })

  it('parses valid and invalid month values consistently', () => {
    expect(parseMonthValue('2026-12', fallbackDate)).toEqual({ year: 2026, month: 11 })
    expect(parseMonthValue('2026-13', fallbackDate)).toEqual({ year: 2026, month: 1 })
  })
})