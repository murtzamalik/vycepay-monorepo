'use client'

import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate, formatKes } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'externalId', label: 'TX ID', mono: true, sortable: true, render: (r) => <EntityLink href={`/transactions/${r.externalId}`}>{String(r.externalId)}</EntityLink> },
  { key: 'customerExternalId', label: 'Customer', sortable: true, render: (r) => <EntityLink href={`/customers/${r.customerExternalId}`}>{String(r.customerExternalId)}</EntityLink> },
  {
    key: 'type',
    label: 'Type',
    sortable: true,
    filter: {
      type: 'select',
      param: 'type',
      options: [
        { value: 'TRANSFER', label: 'Transfer' },
        { value: 'DEPOSIT', label: 'Deposit' },
      ],
    },
    render: (r) => <StatusBadge status={r.type} />,
  },
  { key: 'amount', label: 'Amount', sortable: true, render: (r) => formatKes(r.amount) },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    filter: {
      type: 'select',
      param: 'status',
      options: [
        { value: 'PENDING', label: 'Pending' },
        { value: 'COMPLETED', label: 'Completed' },
        { value: 'FAILED', label: 'Failed' },
      ],
    },
    render: (r) => <StatusBadge status={r.status} />,
  },
  { key: 'createdAt', label: 'Date', sortable: true, render: (r) => formatDate(r.createdAt) },
]

export default function Page() {
  return <ListPage title="Transactions" description="Review all transfer and deposit activity." endpoint="/transactions" exportPath="/api/admin/transactions/export" columns={columns} showDateRange />
}
