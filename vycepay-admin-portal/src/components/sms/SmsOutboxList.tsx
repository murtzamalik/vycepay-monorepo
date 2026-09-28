'use client'

import Link from 'next/link'
import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate } from '@/lib/format'
import type { Column } from '@/lib/columns/types'
import type { ListFilters } from '@/lib/hooks/useListQuery'

const STATUSES = ['', 'PENDING', 'SENDING', 'SENT', 'DEAD']

const columns: Column[] = [
  { key: 'id', label: 'ID', sortable: true },
  { key: 'status', label: 'Status', sortable: true, render: (r) => <StatusBadge status={String(r.status ?? '')} /> },
  { key: 'dedupeKey', label: 'Dedupe', mono: true, sortable: true, render: (r) => {
    const k = String(r.dedupeKey ?? '')
    return k.length > 24 ? k.slice(0, 24) + '…' : k || '—'
  }},
  { key: 'recipientMasked', label: 'Recipient', mono: true },
  { key: 'messagePreview', label: 'Message', render: (r) => String(r.messagePreview ?? '—') },
  { key: 'attemptCount', label: 'Attempts', sortable: true, render: (r) => `${r.attemptCount ?? 0}/${r.maxAttempts ?? '—'}` },
  { key: 'nextAttemptAt', label: 'Next try', sortable: true, render: (r) => formatDate(r.nextAttemptAt) },
  { key: 'lastError', label: 'Last error', render: (r) => {
    const e = String(r.lastError ?? '')
    return e.length > 32 ? e.slice(0, 32) + '…' : e || '—'
  }},
  { key: 'customerExternalId', label: 'Customer', mono: true, render: (r) => {
    const id = r.customerExternalId ?? r.customerId
    return id ? String(id).slice(0, 8) + (String(id).length > 8 ? '…' : '') : '—'
  }},
  { key: 'createdAt', label: 'Created', sortable: true, render: (r) => formatDate(r.createdAt) },
  { key: 'actions', label: '', render: (r) => (
    <EntityLink href={`/sms/outbox/${r.id}`}><span className="btn secondary btn-sm">View</span></EntityLink>
  )},
]

function OutboxFilters({
  filters,
  setFilters,
}: {
  filters: ListFilters
  setFilters: (patch: Partial<ListFilters>) => void
}) {
  return (
    <>
      <select
        className="input input-sm"
        value={filters.status ?? ''}
        onChange={(e) => setFilters({ status: e.target.value })}
        aria-label="Status"
      >
        <option value="">All statuses</option>
        {STATUSES.filter(Boolean).map((t) => (
          <option key={t} value={t}>{t}</option>
        ))}
      </select>
      <input
        className="input input-sm"
        placeholder="Recipient digits"
        defaultValue={filters.recipient ?? ''}
        onKeyDown={(e) => {
          if (e.key === 'Enter') setFilters({ recipient: (e.target as HTMLInputElement).value })
        }}
      />
      <input
        className="input input-sm"
        placeholder="TX:… dedupe key"
        defaultValue={filters.dedupeKey ?? ''}
        onKeyDown={(e) => {
          if (e.key === 'Enter') setFilters({ dedupeKey: (e.target as HTMLInputElement).value })
        }}
      />
      <input
        className="input input-sm"
        placeholder="Customer id / external"
        defaultValue={filters.customerId ?? ''}
        onKeyDown={(e) => {
          if (e.key === 'Enter') setFilters({ customerId: (e.target as HTMLInputElement).value })
        }}
      />
    </>
  )
}

export function SmsOutboxList() {
  return (
    <ListPage
      title="SMS outbox"
      description="Money-event SMS retries (0002/0003). Parked when MobiWave fails; job retries automatically. OTP/bulk stay under SMS."
      endpoint="/sms/outbox"
      columns={columns}
      showDateRange
      hideSearch
      headerActions={
        <div style={{ display: 'flex', gap: 8 }}>
          <Link className="btn secondary" href="/sms">SMS ledger</Link>
        </div>
      }
      filterSlot={({ filters, setFilters }) => (
        <OutboxFilters filters={filters} setFilters={setFilters} />
      )}
    />
  )
}
