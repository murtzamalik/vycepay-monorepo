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

export function formatMobile(countryCode?: unknown, mobile?: unknown): string {
  const code = String(countryCode ?? '').trim()
  const number = String(mobile ?? '').trim()
  if (!number) return '—'
  return code ? `+${code} ${number}` : number
}

export function formatFullName(first?: unknown, middle?: unknown, last?: unknown): string {
  return [first, middle, last].map((part) => String(part ?? '').trim()).filter(Boolean).join(' ') || '—'
}

export function formatKycStatus(status?: unknown, label?: unknown): string {
  if (label) return String(label)
  const raw = String(status ?? '')
  if (!raw || raw === 'NOT_STARTED') return 'Not started'
  if (raw === '1') return 'Pending review'
  if (raw === '7') return 'Approved'
  return raw
}

export function formatIdType(idType?: unknown, label?: unknown): string {
  if (label) return String(label)
  const raw = String(idType ?? '')
  if (!raw) return '—'
  if (raw === '101') return 'National ID'
  if (raw === '102') return 'Alien ID'
  if (raw === '103') return 'Passport'
  return raw
}

export function formatGender(gender?: unknown, label?: unknown): string {
  if (label) return String(label)
  if (gender === null || gender === undefined || gender === '') return '—'
  const value = Number(gender)
  if (Number.isNaN(value)) return String(gender)
  return value === 0 ? 'Female' : 'Male'
}
