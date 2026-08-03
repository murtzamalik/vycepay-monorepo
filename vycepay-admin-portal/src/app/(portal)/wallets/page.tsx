'use client'

import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate, formatKes } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'choiceAccountId', label: 'Account', mono: true, sortable: true },
  { key: 'customerExternalId', label: 'Customer', sortable: true, render: (r) => <EntityLink href={`/customers/${r.customerExternalId}`}>{String(r.customerExternalId)}</EntityLink> },
  { key: 'balance', label: 'Balance', sortable: true, render: (r) => formatKes(r.balance) },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    filter: {
      type: 'select',
      param: 'status',
      options: [
        { value: 'ACTIVE', label: 'Active' },
        { value: 'FROZEN', label: 'Frozen' },
        { value: 'CLOSED', label: 'Closed' },
      ],
    },
    render: (r) => <StatusBadge status={r.status} />,
  },
  { key: 'createdAt', label: 'Created', sortable: true, render: (r) => formatDate(r.createdAt) },
  { key: 'actions', label: '', render: (r) => <EntityLink href={`/wallets/${r.id}`}><span className="btn secondary btn-sm">View</span></EntityLink> },
]

export default function Page() {
  return <ListPage title="Wallets" description="View wallet status, balances, and account mappings." endpoint="/wallets" columns={columns} />
}
