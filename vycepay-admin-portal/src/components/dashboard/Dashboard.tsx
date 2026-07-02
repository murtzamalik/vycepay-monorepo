'use client'

import { Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, buildQuery, defaultDateRange } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { DateRangeFilter } from '@/components/layout/DateRangeFilter'
import { DataTable } from '@/components/ui/DataTable'
import { KpiCard } from '@/components/ui/KpiCard'
import { ChartCard } from '@/components/charts/ChartCard'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatDate, formatKes } from '@/lib/format'
import { SimpleLineChart, SimpleBarChart, SimplePieChart } from '@/components/charts/SimpleCharts'

function DashboardInner() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const defaults = defaultDateRange()
  const fromDate = searchParams.get('fromDate') ?? defaults.fromDate
  const toDate = searchParams.get('toDate') ?? defaults.toDate
  const q = buildQuery({ fromDate, toDate })

  const summary = useQuery({ queryKey: ['dashboard-summary', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>>(`/dashboard/summary${q}`) })
  const volume = useQuery({ queryKey: ['dashboard-volume', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>[]>(`/dashboard/tx-volume-chart${q}`) })
  const donut = useQuery({ queryKey: ['dashboard-donut', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>>(`/dashboard/tx-type-donut${q}`) })
  const kyc = useQuery({ queryKey: ['dashboard-kyc'], queryFn: () => apiFetch<Record<string, unknown>[]>('/dashboard/kyc-status-chart') })
  const alerts = useQuery({ queryKey: ['dashboard-alerts'], queryFn: () => apiFetch<Record<string, unknown>>('/dashboard/alerts') })
  const recent = useQuery({ queryKey: ['dashboard-recent', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>[]>(`/dashboard/recent-transactions${buildQuery({ fromDate, toDate, limit: 10 })}`) })

  function setDates(from: string, to: string) {
    router.push(`?fromDate=${from}&toDate=${to}`)
  }

  const donutData = donut.data ? [
    { name: 'Transfers', value: Number(donut.data.transferCount ?? 0) },
    { name: 'Deposits', value: Number(donut.data.depositCount ?? 0) },
  ] : []

  return (
    <div className="grid">
      <PageHeader title="Dashboard" description="Operational overview, alerts, and recent transaction activity." actions={<DateRangeFilter fromDate={fromDate} toDate={toDate} onChange={setDates} />} />
      {summary.error ? <ErrorState message="Unable to load dashboard." /> : null}
      <div className="kpi-grid">
        {summary.data ? Object.entries(summary.data).map(([key, value]) => (
          <KpiCard key={key} label={key.replace(/([A-Z])/g, ' $1')} value={String(value).match(/^\d/) ? formatKes(value) : String(value)} />
        )) : <SkeletonTable />}
      </div>
      <div className="grid" style={{ gridTemplateColumns: '1fr 340px' }}>
        <ChartCard title="Transaction Volume" subtitle="Daily volume in KES">
          <SimpleLineChart data={volume.data ?? []} xKey="date" lines={[{ key: 'transferAmount', color: '#4f79ff' }, { key: 'depositAmount', color: '#0fd67c' }]} />
        </ChartCard>
        <div className="grid">
          <ChartCard title="Transaction Types" height={160}>
            <SimplePieChart data={donutData} />
          </ChartCard>
          <div className="card">
            <div className="section-title">Alerts</div>
            {alerts.data ? Object.entries(alerts.data).map(([k, v]) => <div key={k} className="kv-row"><span>{k}</span><span className="mono">{String(v)}</span></div>) : null}
          </div>
        </div>
      </div>
      <ChartCard title="KYC Status" height={180}>
        <SimpleBarChart data={kyc.data ?? []} xKey="status" yKey="count" />
      </ChartCard>
      <div className="card">
        <div className="section-title">Recent Transactions</div>
        <DataTable columns={[
          { key: 'externalId', label: 'TX ID', mono: true, render: (r) => <EntityLink href={`/transactions/${r.externalId}`}>{String(r.externalId)}</EntityLink> },
          { key: 'customerExternalId', label: 'Customer', render: (r) => <EntityLink href={`/customers/${r.customerExternalId}`}>{String(r.customerExternalId)}</EntityLink> },
          { key: 'type', label: 'Type', render: (r) => <StatusBadge status={r.type} /> },
          { key: 'amount', label: 'Amount', render: (r) => formatKes(r.amount) },
          { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> },
          { key: 'createdAt', label: 'When', render: (r) => formatDate(r.createdAt) },
        ]} rows={recent.data ?? []} />
      </div>
    </div>
  )
}

export function Dashboard() {
  return <Suspense fallback={<SkeletonTable />}><DashboardInner /></Suspense>
}
