'use client'

import { defaultDateRange } from '@/lib/api'

export function DateRangeFilter({
  fromDate,
  toDate,
  onChange,
}: {
  fromDate?: string
  toDate?: string
  onChange: (from: string, to: string) => void
}) {
  const defaults = defaultDateRange()
  const from = fromDate ?? defaults.fromDate
  const to = toDate ?? defaults.toDate

  function apply() {
    const f = (document.getElementById('date-from') as HTMLInputElement)?.value
    const t = (document.getElementById('date-to') as HTMLInputElement)?.value
    if (f && t && f <= t) onChange(f, t)
  }

  function preset(days: number) {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - days)
    onChange(start.toISOString().slice(0, 10), end.toISOString().slice(0, 10))
  }

  return (
    <div className="filter-bar date-range">
      <input className="input input-sm" id="date-from" type="date" defaultValue={from} key={`from-${from}`} />
      <span className="muted">to</span>
      <input className="input input-sm" id="date-to" type="date" defaultValue={to} key={`to-${to}`} />
      <button className="btn secondary btn-sm" type="button" onClick={apply}>Apply</button>
      <button className="btn secondary btn-sm" type="button" onClick={() => preset(7)}>7d</button>
      <button className="btn secondary btn-sm" type="button" onClick={() => preset(30)}>30d</button>
      <button className="btn secondary btn-sm" type="button" onClick={() => preset(90)}>90d</button>
    </div>
  )
}
