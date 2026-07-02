'use client'

import { Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, buildQuery, defaultDateRange } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { DateRangeFilter } from '@/components/layout/DateRangeFilter'
import { KpiCard } from '@/components/ui/KpiCard'
import { ChartCard } from '@/components/charts/ChartCard'
import { formatKes, formatPercent } from '@/lib/format'
import { SimpleLineChart, SimpleBarChart } from '@/components/charts/SimpleCharts'
import { SkeletonTable } from '@/components/ui/States'

function VolumeReportInner() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const defaults = defaultDateRange()
  const fromDate = searchParams.get('fromDate') ?? defaults.fromDate
  const toDate = searchParams.get('toDate') ?? defaults.toDate
  const groupBy = searchParams.get('groupBy') ?? 'day'
  const q = buildQuery({ fromDate, toDate, groupBy })

  const rows = useQuery({ queryKey: ['report-volume', q], queryFn: () => apiFetch<Record<string, unknown>[]>(`/reports/volume${q}`) })
  const summary = useQuery({ queryKey: ['report-volume-summary', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>>(`/reports/volume/summary${buildQuery({ fromDate, toDate })}`) })

  return (
    <div className="grid">
      <PageHeader title="Transaction Volume Report" description="Daily and cumulative transaction volumes in KES" actions={<DateRangeFilter fromDate={fromDate} toDate={toDate} onChange={(f, t) => router.push(`?fromDate=${f}&toDate=${t}&groupBy=${groupBy}`)} />} />
      {summary.data ? (
        <div className="kpi-grid">
          <KpiCard label="Total Volume" value={formatKes(summary.data.totalVolume)} />
          <KpiCard label="Total Transactions" value={String(summary.data.totalTransactions)} />
          <KpiCard label="Avg Daily Volume" value={formatKes(summary.data.avgDailyVolume)} />
          <KpiCard label="Success Rate" value={formatPercent(summary.data.successRate)} />
        </div>
      ) : <SkeletonTable />}
      <div className="grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
        <ChartCard title="Daily Volume (KES)">
          <SimpleLineChart data={rows.data ?? []} xKey="period" lines={[{ key: 'transferVol', color: '#4f79ff' }, { key: 'depositVol', color: '#0fd67c' }]} />
        </ChartCard>
        <ChartCard title="Transaction Count">
          <SimpleBarChart data={rows.data ?? []} xKey="period" yKey="txCount" />
        </ChartCard>
      </div>
    </div>
  )
}

export function VolumeReport() {
  return <Suspense fallback={<SkeletonTable />}><VolumeReportInner /></Suspense>
}
