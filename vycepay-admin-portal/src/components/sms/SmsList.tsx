'use client'

import Link from 'next/link'
import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { PermissionGuard } from '@/components/shared/PermissionGuard'
import { formatDate } from '@/lib/format'
import type { Column } from '@/lib/columns/types'
import type { ListFilters } from '@/lib/hooks/useListQuery'

const PURPOSES = ['', 'AUTH_OTP', 'ADMIN_BULK']
const STATUSES = ['', 'PENDING', 'SENT', 'FAILED', 'SKIPPED']

const columns: Column[] = [
  { key: 'id', label: 'ID', sortable: true },
  { key: 'recipientMasked', label: 'Recipient', mono: true },
  { key: 'purpose', label: 'Purpose', sortable: true },
  { key: 'status', label: 'Status', sortable: true, render: (r) => <StatusBadge status={String(r.status ?? '')} /> },
  { key: 'messageRedacted', label: 'Message', render: (r) => {
    const text = String(r.messageRedacted ?? '')
    return text.length > 48 ? text.slice(0, 48) + '…' : text || '—'
  }},
  { key: 'providerUid', label: 'Provider UID', mono: true, render: (r) => r.providerUid ? String(r.providerUid).slice(0, 10) + '…' : '—' },
  { key: 'batchId', label: 'Batch', mono: true, render: (r) => r.batchId ? String(r.batchId).slice(0, 8) + '…' : '—' },
  { key: 'createdAt', label: 'Created', sortable: true, render: (r) => formatDate(r.createdAt) },
  { key: 'actions', label: '', render: (r) => <EntityLink href={`/sms/${r.id}`}><span className="btn secondary btn-sm">View</span></EntityLink> },
]

function SmsFilters({
  filters,
  setFilters,
}: {
  filters: ListFilters
  setFilters: (patch: Partial<ListFilters>) => void
}) {
  return (
    <>
      <input
        className="input input-sm"
        placeholder="Recipient digits"
        defaultValue={filters.recipient ?? ''}
        onKeyDown={(e) => {
          if (e.key === 'Enter') setFilters({ recipient: (e.target as HTMLInputElement).value })
        }}
      />
      <select
        className="input input-sm"
        value={filters.purpose ?? ''}
        onChange={(e) => setFilters({ purpose: e.target.value })}
        aria-label="Purpose"
      >
        <option value="">All purposes</option>
        {PURPOSES.filter(Boolean).map((t) => (
          <option key={t} value={t}>{t}</option>
        ))}
      </select>
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
        placeholder="Batch ID"
        defaultValue={filters.batchId ?? ''}
        onKeyDown={(e) => {
          if (e.key === 'Enter') setFilters({ batchId: (e.target as HTMLInputElement).value })
        }}
      />
    </>
  )
}

export function SmsList() {
  return (
    <ListPage
      title="SMS"
      description="Outbound SMS ledger (auth OTPs and admin bulk). Filter by status, purpose, recipient, or batch."
      endpoint="/sms"
      columns={columns}
      showDateRange
      hideSearch
      headerActions={
        <div style={{ display: 'flex', gap: 8 }}>
          <PermissionGuard permission="sms:bulk">
            <Link className="btn" href="/sms/bulk">Bulk SMS</Link>
          </PermissionGuard>
        </div>
      }
      filterSlot={({ filters, setFilters }) => (
        <SmsFilters filters={filters} setFilters={setFilters} />
      )}
    />
  )
}
