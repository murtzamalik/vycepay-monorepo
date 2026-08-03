'use client'

import { Suspense } from 'react'
import Link from 'next/link'
import { useSearchParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, buildQuery, defaultDateRange, errorMessage } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { DateRangeFilter } from '@/components/layout/DateRangeFilter'
import { DataTable } from '@/components/ui/DataTable'
import { KpiCard, type KpiTone } from '@/components/ui/KpiCard'
import { AlertPanel, type AlertItem } from '@/components/ui/AlertPanel'
import type { IconName } from '@/components/ui/DashboardIcon'
import { ChartCard } from '@/components/charts/ChartCard'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { formatCount, formatDate, formatKes, formatPercent } from '@/lib/format'
import { SimpleLineChart, SimpleBarChart, SimplePieChart } from '@/components/charts/SimpleCharts'

type Summary = {
  totalCustomers?: number
  activeWallets?: number
  todayTxVolume?: number
  todayTxCount?: number
  kycApprovalRate?: number
  pendingCallbacks?: number
  stuckTxOver1h?: number
  notificationsSentToday?: number
}

type KpiDef = {
  key: keyof Summary
  label: string
  format: 'count' | 'money' | 'percent'
  icon: IconName
  tone: KpiTone
  href: string
  sub?: string
}

const KPI_DEFS: KpiDef[] = [
  { key: 'totalCustomers', label: 'Customers', format: 'count', icon: 'users', tone: 'brand', href: '/customers', sub: 'Registered in range' },
  { key: 'activeWallets', label: 'Active wallets', format: 'count', icon: 'wallet', tone: 'success', href: '/wallets', sub: 'Status ACTIVE' },
  { key: 'todayTxVolume', label: 'Tx volume', format: 'money', icon: 'volume', tone: 'brand', href: '/transactions', sub: 'Selected period' },
  { key: 'todayTxCount', label: 'Tx count', format: 'count', icon: 'transactions', tone: 'muted', href: '/transactions', sub: 'All types' },
  { key: 'kycApprovalRate', label: 'KYC approval', format: 'percent', icon: 'shield', tone: 'warning', href: '/kyc', sub: 'All-time rate' },
  { key: 'notificationsSentToday', label: 'Pushes today', format: 'count', icon: 'bell', tone: 'muted', href: '/notifications', sub: 'Sent / partial' },
]

function formatKpi(format: KpiDef['format'], value: unknown): string {
  if (format === 'money') return formatKes(value)
  if (format === 'percent') return formatPercent(value)
  return formatCount(value)
}

function DashboardInner() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const defaults = defaultDateRange()
  const fromDate = searchParams.get('fromDate') ?? defaults.fromDate
  const toDate = searchParams.get('toDate') ?? defaults.toDate
  const q = buildQuery({ fromDate, toDate })

  const summary = useQuery({ queryKey: ['dashboard-summary', fromDate, toDate], queryFn: () => apiFetch<Summary>(`/dashboard/summary${q}`) })
  const volume = useQuery({ queryKey: ['dashboard-volume', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>[]>(`/dashboard/tx-volume-chart${q}`) })
  const donut = useQuery({ queryKey: ['dashboard-donut', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>>(`/dashboard/tx-type-donut${q}`) })
  const kyc = useQuery({ queryKey: ['dashboard-kyc'], queryFn: () => apiFetch<Record<string, unknown>[]>('/dashboard/kyc-status-chart') })
  const alerts = useQuery({ queryKey: ['dashboard-alerts'], queryFn: () => apiFetch<Record<string, unknown>>('/dashboard/alerts') })
  const recent = useQuery({ queryKey: ['dashboard-recent', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>[]>(`/dashboard/recent-transactions${buildQuery({ fromDate, toDate, limit: 10 })}`) })

  function setDates(from: string, to: string) {
    router.push(`?fromDate=${from}&toDate=${to}`)
  }

  const donutData = donut.data
    ? [
        { name: 'Transfers', value: Number(donut.data.transferCount ?? 0) },
        { name: 'Deposits', value: Number(donut.data.depositCount ?? 0) },
      ]
    : []

  const alertItems: AlertItem[] = [
    {
      id: 'callbacks',
      label: 'Unprocessed callbacks',
      count: Number(alerts.data?.unprocessedCallbacks ?? summary.data?.pendingCallbacks ?? 0),
      href: '/callbacks?processed=false',
      severity: 'danger',
      icon: 'alert',
    },
    {
      id: 'tx1h',
      label: 'Pending transactions > 1h',
      count: Number(alerts.data?.pendingTxOver1h ?? summary.data?.stuckTxOver1h ?? 0),
      href: '/transactions?status=PENDING',
      severity: 'warning',
      icon: 'clock',
    },
    {
      id: 'tx24h',
      label: 'Pending transactions > 24h',
      count: Number(alerts.data?.pendingTxOver24h ?? 0),
      href: '/transactions?status=PENDING',
      severity: 'danger',
      icon: 'clock',
    },
  ]

  return (
    <div className="dashboard">
      <PageHeader
        title="Operations overview"
        description={`Period ${fromDate} → ${toDate}. Volume, KYC health, and work that needs attention.`}
        actions={<DateRangeFilter fromDate={fromDate} toDate={toDate} onChange={setDates} />}
      />

      {summary.error ? <ErrorState message={errorMessage(summary.error, 'Unable to load dashboard summary.')} /> : null}

      <section className="kpi-grid" aria-label="Key metrics">
        {KPI_DEFS.map((def) => (
          <KpiCard
            key={def.key}
            label={def.label}
            icon={def.icon}
            tone={def.tone}
            href={def.href}
            sub={def.sub}
            loading={summary.isLoading}
            value={summary.data ? formatKpi(def.format, summary.data[def.key]) : undefined}
          />
        ))}
      </section>

      <section className="dashboard-main-grid" aria-label="Charts and alerts">
        <ChartCard
          title="Transaction volume"
          subtitle="Daily transfer vs deposit amount (KES)"
          height={240}
          legend={
            <div className="chart-legend">
              <span className="chart-legend-item"><span className="chart-legend-swatch" style={{ background: '#4f79ff' }} />Transfers</span>
              <span className="chart-legend-item"><span className="chart-legend-swatch" style={{ background: '#0fd67c' }} />Deposits</span>
            </div>
          }
        >
          {volume.isLoading ? <div className="kpi-skeleton" style={{ height: '100%' }} /> : null}
          {volume.error ? <div className="muted">Unable to load volume chart</div> : null}
          {!volume.isLoading && !volume.error ? (
            <SimpleLineChart
              data={volume.data ?? []}
              xKey="date"
              lines={[
                { key: 'transferAmount', color: '#4f79ff' },
                { key: 'depositAmount', color: '#0fd67c' },
              ]}
            />
          ) : null}
        </ChartCard>

        <div className="dashboard-side-stack">
          <ChartCard title="Mix by type" subtitle="Transfer vs deposit count" height={180}>
            {donut.isLoading ? <div className="kpi-skeleton" style={{ height: '100%' }} /> : null}
            {donut.error ? <div className="muted">Unable to load type mix</div> : null}
            {!donut.isLoading && !donut.error ? <SimplePieChart data={donutData} /> : null}
          </ChartCard>
          <AlertPanel items={alertItems} loading={alerts.isLoading && summary.isLoading} />
        </div>
      </section>

      <section className="dashboard-secondary-grid" aria-label="KYC and recent activity">
        <ChartCard title="KYC pipeline" subtitle="Verification counts by status" height={210}>
          {kyc.isLoading ? <div className="kpi-skeleton" style={{ height: '100%' }} /> : null}
          {kyc.error ? <div className="muted">Unable to load KYC chart</div> : null}
          {!kyc.isLoading && !kyc.error ? (
            <SimpleBarChart data={kyc.data ?? []} xKey="status" yKey="count" color="#8b5cf6" />
          ) : null}
        </ChartCard>

        <div className="card recent-tx-card">
          <div className="section-hdr">
            <div>
              <div className="section-title">Recent transactions</div>
              <div className="section-sub muted">Latest activity in the selected range</div>
            </div>
            <Link className="btn secondary btn-sm" href="/transactions">View all</Link>
          </div>
          {recent.isLoading ? <SkeletonTable /> : null}
          {recent.error ? <ErrorState message={errorMessage(recent.error, 'Unable to load recent transactions.')} /> : null}
          {!recent.isLoading && !recent.error ? (
            <DataTable
              columns={[
                { key: 'externalId', label: 'TX ID', mono: true, render: (r) => <EntityLink href={`/transactions/${r.externalId}`}>{String(r.externalId).slice(0, 8)}…</EntityLink> },
                { key: 'customerExternalId', label: 'Customer', render: (r) => <EntityLink href={`/customers/${r.customerExternalId}`}>{String(r.customerExternalId).slice(0, 8)}…</EntityLink> },
                { key: 'type', label: 'Type', render: (r) => <StatusBadge status={r.type} /> },
                { key: 'amount', label: 'Amount', render: (r) => formatKes(r.amount) },
                { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> },
                { key: 'createdAt', label: 'When', render: (r) => formatDate(r.createdAt) },
              ]}
              rows={recent.data ?? []}
            />
          ) : null}
        </div>
      </section>
    </div>
  )
}

export function Dashboard() {
  return (
    <Suspense fallback={<SkeletonTable />}>
      <DashboardInner />
    </Suspense>
  )
}
