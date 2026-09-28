'use client'

import Link from 'next/link'
import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import type { Column } from '@/lib/columns/types'
import type { ListFilters } from '@/lib/hooks/useListQuery'

const CATEGORIES = ['', 'OTP', 'TRANSACTION']

const columns: Column[] = [
  { key: 'templateKey', label: 'Key', mono: true, sortable: true },
  { key: 'category', label: 'Category', sortable: true },
  { key: 'name', label: 'Name', sortable: true },
  { key: 'active', label: 'Active', sortable: true, render: (r) => (
    <StatusBadge status={r.active === true || r.active === 1 ? 'ACTIVE' : 'INACTIVE'} />
  )},
  { key: 'body', label: 'Body', render: (r) => {
    const text = String(r.body ?? '')
    return text.length > 56 ? text.slice(0, 56) + '…' : text || '—'
  }},
  { key: 'actions', label: '', render: (r) => (
    <EntityLink href={`/sms/templates/${encodeURIComponent(String(r.templateKey))}`}>
      <span className="btn secondary btn-sm">Edit</span>
    </EntityLink>
  )},
]

function TemplateFilters({
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
        value={filters.category ?? ''}
        onChange={(e) => setFilters({ category: e.target.value })}
        aria-label="Category"
      >
        <option value="">All categories</option>
        {CATEGORIES.filter(Boolean).map((t) => (
          <option key={t} value={t}>{t}</option>
        ))}
      </select>
      <select
        className="input input-sm"
        value={filters.active ?? ''}
        onChange={(e) => setFilters({ active: e.target.value })}
        aria-label="Active"
      >
        <option value="">All</option>
        <option value="true">Active</option>
        <option value="false">Inactive</option>
      </select>
    </>
  )
}

export function SmsTemplateList() {
  return (
    <ListPage
      title="SMS templates"
      description="System OTP and money-event SMS bodies. Edit copy only — keys are seeded. FCM/push and bulk SMS are separate."
      endpoint="/sms/templates"
      columns={columns}
      hideSearch
      clientSort
      headerActions={
        <div style={{ display: 'flex', gap: 8 }}>
          <Link className="btn secondary" href="/sms">SMS ledger</Link>
          <Link className="btn secondary" href="/sms/outbox">Outbox</Link>
        </div>
      }
      filterSlot={({ filters, setFilters }) => (
        <TemplateFilters filters={filters} setFilters={setFilters} />
      )}
    />
  )
}
