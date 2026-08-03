'use client'

import Link from 'next/link'
import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { PermissionGuard } from '@/components/shared/PermissionGuard'
import { formatDate } from '@/lib/format'
import type { Column } from '@/lib/columns/types'
import type { ListFilters } from '@/lib/hooks/useListQuery'

const PUSH_TYPES = [
  '',
  'TRANSACTION_RESULT',
  'KYC_ONBOARDING_RESULT',
  'KYC_DOCUMENT_CHECK',
  'STATEMENT_READY',
  'ACCOUNT_STATUS',
  'ADMIN_MESSAGE',
]

const SOURCES = ['', 'CALLBACK', 'ADMIN_COMPOSE']

const columns: Column[] = [
  { key: 'id', label: 'ID', sortable: true },
  { key: 'customerId', label: 'Customer', mono: true, sortable: true, render: (r) => String(r.customerExternalId ?? r.customerId ?? '—') },
  { key: 'pushType', label: 'Type', sortable: true },
  { key: 'source', label: 'Source', sortable: true, render: (r) => <StatusBadge status={String(r.source ?? '')} /> },
  { key: 'title', label: 'Title' },
  { key: 'batchId', label: 'Batch', mono: true, render: (r) => r.batchId ? String(r.batchId).slice(0, 8) + '…' : '—' },
  { key: 'createdAt', label: 'Created', sortable: true, render: (r) => formatDate(r.createdAt) },
  { key: 'actions', label: '', render: (r) => <EntityLink href={`/notifications/${r.id}`}><span className="btn secondary btn-sm">View</span></EntityLink> },
]

function NotificationFilters({
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
        placeholder="Customer ID / external ID"
        defaultValue={filters.customerId ?? ''}
        onKeyDown={(e) => {
          if (e.key === 'Enter') setFilters({ customerId: (e.target as HTMLInputElement).value })
        }}
      />
      <select
        className="input input-sm"
        value={filters.pushType ?? ''}
        onChange={(e) => setFilters({ pushType: e.target.value })}
        aria-label="Push type"
      >
        <option value="">All push types</option>
        {PUSH_TYPES.filter(Boolean).map((t) => (
          <option key={t} value={t}>{t}</option>
        ))}
      </select>
      <select
        className="input input-sm"
        value={filters.source ?? ''}
        onChange={(e) => setFilters({ source: e.target.value })}
        aria-label="Source"
      >
        <option value="">All sources</option>
        {SOURCES.filter(Boolean).map((s) => (
          <option key={s} value={s}>{s}</option>
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

export function NotificationList() {
  return (
    <ListPage
      title="Notifications"
      description="Customer inbox messages and FCM delivery attempts. Filter by customer, type, source, or batch."
      endpoint="/notifications"
      columns={columns}
      showDateRange
      hideSearch
      headerActions={
        <div style={{ display: 'flex', gap: 8 }}>
          <Link className="btn secondary" href="/notifications/summary">Summary</Link>
          <PermissionGuard permission="notification:compose">
            <Link className="btn" href="/notifications/compose">Compose</Link>
          </PermissionGuard>
        </div>
      }
      filterSlot={({ filters, setFilters }) => (
        <NotificationFilters filters={filters} setFilters={setFilters} />
      )}
    />
  )
}
