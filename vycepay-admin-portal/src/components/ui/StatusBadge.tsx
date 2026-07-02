'use client'

const STATUS_MAP: Record<string, string> = {
  ACTIVE: 'success',
  APPROVED: 'success',
  COMPLETED: 'success',
  SUCCESS: 'success',
  PENDING: 'warning',
  SUSPENDED: 'danger',
  FROZEN: 'danger',
  FAILED: 'danger',
  REJECTED: 'danger',
  INACTIVE: 'muted',
}

export function StatusBadge({ status }: { status?: unknown }) {
  const value = String(status ?? '—')
  const variant = STATUS_MAP[value.toUpperCase()] ?? 'info'
  return <span className={`badge badge-${variant}`}>{value}</span>
}
