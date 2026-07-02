'use client'

import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate, formatKes } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'externalId', label: 'TX ID', mono: true, render: (r) => <EntityLink href={`/transactions/${r.externalId}`}>{String(r.externalId)}</EntityLink> },
  { key: 'type', label: 'Type', render: (r) => <StatusBadge status={r.type} /> },
  { key: 'amount', label: 'Amount', render: (r) => formatKes(r.amount) },
  { key: 'errorCode', label: 'Error', mono: true },
  { key: 'createdAt', label: 'Date', render: (r) => formatDate(r.createdAt) },
]

export default function Page() {
  return (
    <ListPage
      title="Failed Transactions"
      description="Analyze failed transactions and Choice Bank errors."
      endpoint="/transactions/failed"
      columns={columns}
      showDateRange
      filterSlot={({ filters, setFilters }) => (
        <input
          className="input input-sm"
          placeholder="Error code"
          defaultValue={filters.errorCode ?? ''}
          onKeyDown={(e) => {
            if (e.key === 'Enter') setFilters({ errorCode: (e.target as HTMLInputElement).value })
          }}
        />
      )}
    />
  )
}
