import { describe, expect, it } from 'vitest'
import { localISODate } from '../src/utils/date'

describe('localISODate', () => {
  it('formats using local date parts', () => {
    const localDate = new Date(2026, 1, 18, 23, 45, 0)
    expect(localISODate(localDate)).toBe('2026-02-18')
  })
})
