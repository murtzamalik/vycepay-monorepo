'use client'

import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'id', label: 'ID' },
  { key: 'choiceRequestId', label: 'Request ID', mono: true },
  { key: 'notificationType', label: 'Type' },
  { key: 'processed', label: 'Status', render: (r) => <StatusBadge status={r.processed ? 'COMPLETED' : 'PENDING'} /> },
  { key: 'createdAt', label: 'Received', render: (r) => formatDate(r.createdAt) },
  { key: 'actions', label: '', render: (r) => <EntityLink href={`/callbacks/${r.id}`}><span className="btn secondary btn-sm">View</span></EntityLink> },
]

export default function Page() {
  return <ListPage title="Callbacks" description="Monitor Choice Bank callback delivery and processing." endpoint="/callbacks" columns={columns} showDateRange />
}
