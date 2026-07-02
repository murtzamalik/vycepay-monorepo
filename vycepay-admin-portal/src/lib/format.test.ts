import { describe, expect, it } from 'vitest'
import { buildQuery, defaultDateRange } from '@/lib/api'
import { formatKes, formatPercent } from '@/lib/format'

describe('api helpers', () => {
  it('buildQuery omits empty values', () => {
    expect(buildQuery({ page: 0, search: '', status: 'ACTIVE' })).toBe('?page=0&status=ACTIVE')
  })

  it('defaultDateRange returns ISO dates', () => {
    const { fromDate, toDate } = defaultDateRange()
    expect(fromDate).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(toDate).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})

describe('formatters', () => {
  it('formats KES', () => {
    expect(formatKes(1234.5)).toContain('KES')
  })

  it('formats percent', () => {
    expect(formatPercent(76.7)).toBe('76.7%')
  })
})
