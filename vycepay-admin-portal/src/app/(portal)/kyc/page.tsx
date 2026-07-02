'use client'

import { ListPage } from '@/components/shared/ListPage'
import { Avatar } from '@/components/ui/Avatar'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'customer', label: 'Customer', render: (r) => <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}><Avatar name={`${r.firstName} ${r.lastName}`} size="sm" /><span>{`${r.firstName ?? ''} ${r.lastName ?? ''}`.trim()}</span></div> },
  { key: 'choiceOnboardingRequestId', label: 'Onboarding ID', mono: true },
  { key: 'idType', label: 'ID Type' },
  { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'createdAt', label: 'Submitted', render: (r) => formatDate(r.createdAt) },
  { key: 'actions', label: '', render: (r) => <EntityLink href={`/kyc/${r.id}`}><span className="btn secondary btn-sm">View</span></EntityLink> },
]

export default function Page() {
  return <ListPage title="KYC Management" description="Monitor onboarding applications and verification state." endpoint="/kyc" columns={columns} showDateRange />
}
