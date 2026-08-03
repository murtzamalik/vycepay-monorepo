'use client'

import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'id', label: 'ID', sortable: true },
  { key: 'choiceRequestId', label: 'Request ID', mono: true },
  {
    key: 'notificationType',
    label: 'Type',
    sortable: true,
    filter: { type: 'text', param: 'notificationType', placeholder: 'Notification type' },
  },
  {
    key: 'processed',
    label: 'Status',
    sortable: true,
    filter: {
      type: 'select',
      param: 'processed',
      options: [
        { value: 'true', label: 'Processed' },
        { value: 'false', label: 'Pending' },
      ],
    },
    render: (r) => <StatusBadge status={r.processed === true || r.processed === 'true' || r.processed === 1 ? 'COMPLETED' : 'PENDING'} />,
  },
  { key: 'createdAt', label: 'Received', sortable: true, render: (r) => formatDate(r.createdAt) },
  { key: 'actions', label: '', render: (r) => <EntityLink href={`/callbacks/${r.id}`}><span className="btn secondary btn-sm">View</span></EntityLink> },
]

export default function Page() {
  return <ListPage title="Callbacks" description="Monitor Choice Bank callback delivery and processing." endpoint="/callbacks" columns={columns} showDateRange />
}
