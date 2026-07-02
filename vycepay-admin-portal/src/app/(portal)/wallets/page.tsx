'use client'

import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate, formatKes } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'choiceAccountId', label: 'Account', mono: true },
  { key: 'customerExternalId', label: 'Customer', render: (r) => <EntityLink href={`/customers/${r.customerExternalId}`}>{String(r.customerExternalId)}</EntityLink> },
  { key: 'balance', label: 'Balance', render: (r) => formatKes(r.balance) },
  { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'actions', label: '', render: (r) => <EntityLink href={`/wallets/${r.id}`}><span className="btn secondary btn-sm">View</span></EntityLink> },
]

export default function Page() {
  return <ListPage title="Wallets" description="View wallet status, balances, and account mappings." endpoint="/wallets" columns={columns} />
}
