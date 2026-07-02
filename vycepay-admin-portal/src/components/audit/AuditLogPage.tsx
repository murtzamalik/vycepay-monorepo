'use client'

import { Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, asPaginated, buildQuery, defaultDateRange } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { DateRangeFilter } from '@/components/layout/DateRangeFilter'
import { PaginationBar } from '@/components/layout/PaginationBar'
import { DataTable } from '@/components/ui/DataTable'
import { formatDateTime } from '@/lib/format'
import { SkeletonTable } from '@/components/ui/States'

function AuditLogInner() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const defaults = defaultDateRange()
  const source = searchParams.get('source') ?? 'customer'
  const fromDate = searchParams.get('fromDate') ?? defaults.fromDate
  const toDate = searchParams.get('toDate') ?? defaults.toDate
  const page = searchParams.get('page') ?? '0'
  const q = buildQuery({ source, fromDate, toDate, page, size: 20 })
  const { data, isLoading } = useQuery({ queryKey: ['audit', q], queryFn: () => apiFetch<unknown>(`/audit-log${q}`) })
  const paginated = data ? asPaginated(data) : null

  return (
    <div className="grid">
      <PageHeader title="Audit Log" description="Customer and admin activity audit trail." actions={<a className="btn secondary" href={`/api/admin/audit-log/export${buildQuery({ fromDate, toDate })}`}>Export CSV</a>} />
      <div className="tab-bar">
        {['customer', 'admin'].map((s) => (
          <button key={s} type="button" className={`tab-btn ${source === s ? 'active' : ''}`} onClick={() => router.push(`?source=${s}&fromDate=${fromDate}&toDate=${toDate}`)}>
            {s === 'customer' ? 'Customer Activity' : 'Admin Audit'}
          </button>
        ))}
      </div>
      <DateRangeFilter fromDate={fromDate} toDate={toDate} onChange={(f, t) => router.push(`?source=${source}&fromDate=${f}&toDate=${t}`)} />
      {isLoading ? <SkeletonTable /> : null}
      {paginated ? (
        <DataTable columns={source === 'admin' ? [
          { key: 'action', label: 'Action' },
          { key: 'entityType', label: 'Entity' },
          { key: 'adminUsername', label: 'Admin' },
          { key: 'reason', label: 'Reason' },
          { key: 'createdAt', label: 'When', render: (r) => formatDateTime(r.createdAt) },
        ] : [
          { key: 'action', label: 'Action' },
          { key: 'customerId', label: 'Customer ID' },
          { key: 'resourceType', label: 'Resource' },
          { key: 'createdAt', label: 'When', render: (r) => formatDateTime(r.createdAt) },
        ]} rows={paginated.content} />
      ) : null}
      <PaginationBar data={paginated} onPageChange={(p) => router.push(`?source=${source}&fromDate=${fromDate}&toDate=${toDate}&page=${p}`)} />
    </div>
  )
}

export function AuditLogPage() {
  return <Suspense fallback={<SkeletonTable />}><AuditLogInner /></Suspense>
}
