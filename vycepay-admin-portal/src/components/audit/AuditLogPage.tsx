'use client'

import { Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, asPaginated, buildQuery, defaultDateRange, errorMessage } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { DateRangeFilter } from '@/components/layout/DateRangeFilter'
import { PaginationBar } from '@/components/layout/PaginationBar'
import { DataTable } from '@/components/ui/DataTable'
import { formatDateTime } from '@/lib/format'
import { ErrorState, SkeletonTable } from '@/components/ui/States'

const SOURCES = [
  { id: 'admin', label: 'Admin Audit' },
  { id: 'auth', label: 'Auth Events' },
  { id: 'customer', label: 'Customer Activity' },
] as const

function AuditLogInner() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const defaults = defaultDateRange()
  const source = searchParams.get('source') ?? 'admin'
  const fromDate = searchParams.get('fromDate') ?? defaults.fromDate
  const toDate = searchParams.get('toDate') ?? defaults.toDate
  const page = searchParams.get('page') ?? '0'
  const sort = searchParams.get('sort') ?? undefined
  const order = searchParams.get('order') ?? undefined
  const q = buildQuery({ source, fromDate, toDate, page, size: 20, sort, order })
  const { data, isLoading, error } = useQuery({ queryKey: ['audit', q], queryFn: () => apiFetch<unknown>(`/audit-log${q}`) })
  const paginated = data ? asPaginated(data) : null

  function push(patch: Record<string, string | undefined>) {
    const next = new URLSearchParams(searchParams.toString())
    Object.entries(patch).forEach(([k, v]) => {
      if (v === undefined || v === '') next.delete(k)
      else next.set(k, v)
    })
    if (!('page' in patch)) next.set('page', '0')
    router.push(`?${next.toString()}`)
  }

  const columns = source === 'auth'
    ? [
        { key: 'eventType', label: 'Event', sortable: true },
        { key: 'outcome', label: 'Outcome', sortable: true },
        { key: 'customerExternalId', label: 'Customer' },
        { key: 'identifierMasked', label: 'Identifier', mono: true },
        { key: 'detail', label: 'Detail' },
        { key: 'createdAt', label: 'When', sortable: true, render: (r: Record<string, unknown>) => formatDateTime(r.createdAt) },
      ]
    : source === 'admin'
      ? [
          { key: 'action', label: 'Action', sortable: true },
          { key: 'entityType', label: 'Entity' },
          { key: 'adminUsername', label: 'Admin', sortable: true },
          { key: 'reason', label: 'Reason' },
          { key: 'createdAt', label: 'When', sortable: true, render: (r: Record<string, unknown>) => formatDateTime(r.createdAt) },
        ]
      : [
          { key: 'action', label: 'Action', sortable: true },
          { key: 'customerId', label: 'Customer ID' },
          { key: 'resourceType', label: 'Resource' },
          { key: 'resourceId', label: 'Resource ID', mono: true },
          { key: 'createdAt', label: 'When', sortable: true, render: (r: Record<string, unknown>) => formatDateTime(r.createdAt) },
        ]

  return (
    <div className="grid">
      <PageHeader
        title="Audit Log"
        description="Customer activity, auth security events, and admin action trails."
        actions={<a className="btn secondary" href={`/api/admin/audit-log/export${buildQuery({ source, fromDate, toDate })}`}>Export CSV</a>}
      />
      <div className="tab-bar">
        {SOURCES.map((s) => (
          <button
            key={s.id}
            type="button"
            className={`tab-btn ${source === s.id ? 'active' : ''}`}
            onClick={() => push({ source: s.id, fromDate, toDate })}
          >
            {s.label}
          </button>
        ))}
      </div>
      <DateRangeFilter fromDate={fromDate} toDate={toDate} onChange={(f, t) => push({ source, fromDate: f, toDate: t })} />
      {isLoading ? <SkeletonTable /> : null}
      {error ? <ErrorState message={errorMessage(error, 'Unable to load audit log.')} /> : null}
      {paginated ? (
        <DataTable
          columns={columns}
          rows={paginated.content}
          sortKey={sort}
          sortOrder={order === 'asc' ? 'asc' : 'desc'}
          onSort={(key, ord) => push({ source, fromDate, toDate, sort: key, order: ord })}
        />
      ) : null}
      <PaginationBar data={paginated} onPageChange={(p) => push({ source, fromDate, toDate, page: String(p), sort, order })} />
    </div>
  )
}

export function AuditLogPage() {
  return <Suspense fallback={<SkeletonTable />}><AuditLogInner /></Suspense>
}
