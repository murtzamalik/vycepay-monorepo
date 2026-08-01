'use client'

import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, errorMessage } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { KpiCard } from '@/components/ui/KpiCard'
import { ChartCard } from '@/components/charts/ChartCard'
import { SimpleBarChart } from '@/components/charts/SimpleCharts'
import { DataTable } from '@/components/ui/DataTable'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import type { Column } from '@/lib/columns/types'

type StatusCount = { status?: string; count?: number | string }

type NotificationSummary = {
  inboxTotal?: number
  sentToday?: number
  today?: StatusCount[]
  allTime?: StatusCount[]
}

const statusColumns: Column[] = [
  { key: 'status', label: 'Status', render: (r) => <StatusBadge status={String(r.status ?? '')} /> },
  { key: 'count', label: 'Count' },
]

function toChartRows(rows: StatusCount[] | undefined) {
  return (rows ?? []).map((r) => ({
    status: String(r.status ?? 'UNKNOWN'),
    count: Number(r.count ?? 0),
  }))
}

export function NotificationSummaryPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['notification-summary'],
    queryFn: () => apiFetch<NotificationSummary>('/notifications/summary'),
  })

  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message={errorMessage(error, 'Unable to load notification summary.')} />

  const todayRows = toChartRows(data.today)
  const allTimeRows = toChartRows(data.allTime)

  return (
    <div className="grid">
      <PageHeader
        title="Notification summary"
        description="Delivery attempt counts by status (today and all-time) plus inbox size."
        actions={<Link className="btn secondary" href="/notifications">Back to list</Link>}
      />
      <div className="kpi-grid">
        <KpiCard label="Inbox total" value={String(data.inboxTotal ?? 0)} />
        <KpiCard label="Sent today" value={String(data.sentToday ?? 0)} />
        <KpiCard
          label="Attempts today"
          value={String(todayRows.reduce((sum, r) => sum + r.count, 0))}
        />
        <KpiCard
          label="Attempts all-time"
          value={String(allTimeRows.reduce((sum, r) => sum + r.count, 0))}
        />
      </div>
      <div className="grid" style={{ gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <ChartCard title="Today by status" height={200}>
          <SimpleBarChart data={todayRows} xKey="status" yKey="count" />
        </ChartCard>
        <ChartCard title="All-time by status" height={200}>
          <SimpleBarChart data={allTimeRows} xKey="status" yKey="count" />
        </ChartCard>
      </div>
      <div className="grid" style={{ gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <div className="card">
          <div className="section-title">Today</div>
          <DataTable columns={statusColumns} rows={todayRows} rowKey={(r, i) => String(r.status ?? i)} />
        </div>
        <div className="card">
          <div className="section-title">All-time</div>
          <DataTable columns={statusColumns} rows={allTimeRows} rowKey={(r, i) => String(r.status ?? i)} />
        </div>
      </div>
    </div>
  )
}
