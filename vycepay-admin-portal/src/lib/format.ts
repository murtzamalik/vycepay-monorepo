import { format, formatDistanceToNow, parseISO, isValid } from 'date-fns'

export function formatKes(value: unknown): string {
  const n = Number(value)
  if (Number.isNaN(n)) return '—'
  return `KES ${n.toLocaleString('en-KE', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

export function formatDate(value: unknown): string {
  if (!value) return '—'
  const d = typeof value === 'string' ? parseISO(value) : value instanceof Date ? value : null
  if (!d || !isValid(d)) return String(value)
  return format(d, 'MMM d, yyyy')
}

export function formatDateTime(value: unknown): string {
  if (!value) return '—'
  const d = typeof value === 'string' ? parseISO(value) : value instanceof Date ? value : null
  if (!d || !isValid(d)) return String(value)
  return format(d, 'MMM d, yyyy · h:mm a')
}

export function formatRelative(value: unknown): string {
  if (!value) return '—'
  const d = typeof value === 'string' ? parseISO(value) : value instanceof Date ? value : null
  if (!d || !isValid(d)) return String(value)
  return formatDistanceToNow(d, { addSuffix: true })
}

export function formatPercent(value: unknown): string {
  const n = Number(value)
  if (Number.isNaN(n)) return '—'
  return `${n.toFixed(1)}%`
}

export function initials(first?: unknown, last?: unknown): string {
  const a = String(first ?? '').trim()[0] ?? ''
  const b = String(last ?? '').trim()[0] ?? ''
  return (a + b).toUpperCase() || '?'
}

export function formatCell(value: unknown): string {
  if (value === null || value === undefined) return '—'
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}
