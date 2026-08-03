'use client'

import { ListPage } from '@/components/shared/ListPage'
import { Avatar } from '@/components/ui/Avatar'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EntityLink } from '@/components/ui/EntityLink'
import { formatDate, formatFullName, formatKes, formatKycStatus, formatMobile } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  {
    key: 'name',
    label: 'Customer',
    render: (r) => {
      const fullName = formatFullName(r.firstName, r.middleName, r.lastName)
      const label = fullName === '—' ? formatMobile(r.mobileCountryCode, r.mobile) : fullName
      return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
        <Avatar name={label} size="sm" />
        <div>
          <div>{label}</div>
          <div className="mono muted" style={{ fontSize: 11 }}>{String(r.externalId)}</div>
        </div>
      </div>
    )},
  },
  { key: 'mobile', label: 'Mobile', render: (r) => <span className="mono">{formatMobile(r.mobileCountryCode, r.mobile)}</span> },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    filter: {
      type: 'select',
      param: 'status',
      options: [
        { value: 'ACTIVE', label: 'Active' },
        { value: 'SUSPENDED', label: 'Suspended' },
        { value: 'PENDING', label: 'Pending' },
      ],
    },
    render: (r) => <StatusBadge status={r.status} />,
  },
  { key: 'kycStatus', label: 'KYC', render: (r) => <StatusBadge status={String(r.kycStatusLabel ?? formatKycStatus(r.kycStatus))} /> },
  { key: 'walletBalance', label: 'Balance', sortable: true, render: (r) => formatKes(r.walletBalance) },
  { key: 'createdAt', label: 'Registered', sortable: true, render: (r) => formatDate(r.createdAt) },
  {
    key: 'actions',
    label: '',
    render: (r) => (
      <div className="actions">
        <EntityLink href={`/customers/${r.externalId}`}><span className="btn secondary btn-sm">View</span></EntityLink>
      </div>
    ),
  },
]

export function CustomerList() {
  return (
    <ListPage
      title="Customers"
      description="Search and monitor registered customers."
      endpoint="/customers"
      exportPath="/api/admin/customers/export"
      columns={columns}
      showDateRange
    />
  )
}
