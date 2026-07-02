'use client'

import { Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, buildQuery, defaultDateRange } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { DateRangeFilter } from '@/components/layout/DateRangeFilter'
import { ChartCard } from '@/components/charts/ChartCard'
import { SimpleLineChart } from '@/components/charts/SimpleCharts'
import { SkeletonTable } from '@/components/ui/States'

function GrowthReportInner() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const defaults = defaultDateRange()
  const fromDate = searchParams.get('fromDate') ?? defaults.fromDate
  const toDate = searchParams.get('toDate') ?? defaults.toDate
  const groupBy = searchParams.get('groupBy') ?? 'day'
  const { data, isLoading } = useQuery({ queryKey: ['report-growth', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>[]>(`/reports/customer-growth${buildQuery({ fromDate, toDate, groupBy })}`) })

  if (isLoading) return <SkeletonTable />

  return (
    <div className="grid">
      <PageHeader title="Customer Growth Report" description="Customer acquisition and wallet creation trends" actions={<DateRangeFilter fromDate={fromDate} toDate={toDate} onChange={(f, t) => router.push(`?fromDate=${f}&toDate=${t}&groupBy=${groupBy}`)} />} />
      <ChartCard title="Registrations vs Wallets" subtitle="By period">
        <SimpleLineChart data={data ?? []} xKey="period" lines={[
          { key: 'newCustomers', color: '#4f79ff' },
          { key: 'activeWallets', color: '#0fd67c' },
          { key: 'cumulativeTotal', color: '#8b5cf6' },
        ]} />
      </ChartCard>
    </div>
  )
}

export function GrowthReport() {
  return <Suspense fallback={<SkeletonTable />}><GrowthReportInner /></Suspense>
}
