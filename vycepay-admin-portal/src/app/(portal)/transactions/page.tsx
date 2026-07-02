'use client'

import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate, formatKes } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'externalId', label: 'TX ID', mono: true, render: (r) => <EntityLink href={`/transactions/${r.externalId}`}>{String(r.externalId)}</EntityLink> },
  { key: 'customerExternalId', label: 'Customer', render: (r) => <EntityLink href={`/customers/${r.customerExternalId}`}>{String(r.customerExternalId)}</EntityLink> },
  { key: 'type', label: 'Type', render: (r) => <StatusBadge status={r.type} /> },
  { key: 'amount', label: 'Amount', render: (r) => formatKes(r.amount) },
  { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'createdAt', label: 'Date', render: (r) => formatDate(r.createdAt) },
]

export default function Page() {
  return <ListPage title="Transactions" description="Review all transfer and deposit activity." endpoint="/transactions" exportPath="/api/admin/transactions/export" columns={columns} showDateRange />
}
