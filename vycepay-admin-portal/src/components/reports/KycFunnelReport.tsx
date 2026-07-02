'use client'

import { Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, buildQuery, defaultDateRange } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { DateRangeFilter } from '@/components/layout/DateRangeFilter'
import { formatPercent } from '@/lib/format'
import { SkeletonTable } from '@/components/ui/States'

const STAGES = [
  { key: 'registered', label: 'Registered', sub: 'Mobile OTP verified', color: '#4f79ff' },
  { key: 'kycSubmitted', label: 'KYC Submitted', sub: 'Documents sent to Choice Bank', color: '#8b5cf6' },
  { key: 'kycApproved', label: 'KYC Approved', sub: 'Account opened at Choice Bank', color: '#0fd67c' },
  { key: 'firstTransaction', label: 'First Transaction', sub: 'Transfer or deposit', color: '#f5a623' },
]

function KycFunnelReportInner() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const defaults = defaultDateRange()
  const fromDate = searchParams.get('fromDate') ?? defaults.fromDate
  const toDate = searchParams.get('toDate') ?? defaults.toDate
  const { data, isLoading } = useQuery({ queryKey: ['report-funnel', fromDate, toDate], queryFn: () => apiFetch<Record<string, unknown>>(`/reports/kyc-funnel${buildQuery({ fromDate, toDate })}`) })

  if (isLoading) return <SkeletonTable />

  return (
    <div className="grid">
      <PageHeader title="KYC Funnel Report" description="Customer onboarding funnel — registrations to first transaction" actions={<DateRangeFilter fromDate={fromDate} toDate={toDate} onChange={(f, t) => router.push(`?fromDate=${f}&toDate=${t}`)} />} />
      <div className="grid" style={{ gridTemplateColumns: '1fr 360px' }}>
        <div className="card">
          <div className="section-title" style={{ marginBottom: 20 }}>Onboarding Funnel</div>
          {STAGES.map((stage, i) => (
            <div key={stage.key}>
              <div className="funnel-step" style={{ background: `${stage.color}18`, borderColor: `${stage.color}33` }}>
                <div><div style={{ fontWeight: 600, color: stage.color }}>{stage.label}</div><div className="muted" style={{ fontSize: 11 }}>{stage.sub}</div></div>
                <div className="mono" style={{ fontSize: 22, fontWeight: 800, color: stage.color }}>{String(data?.[stage.key] ?? 0)}</div>
              </div>
              {i < STAGES.length - 1 ? <div className="funnel-arrow">↓</div> : null}
            </div>
          ))}
        </div>
        <div className="card">
          <div className="section-title" style={{ marginBottom: 14 }}>Funnel Metrics</div>
          {[
            { label: 'Registration → KYC', value: data?.conversionRegisteredToKyc },
            { label: 'KYC → Approved', value: data?.conversionKycToApproved },
            { label: 'Approved → First Tx', value: data?.conversionApprovedToTx },
            { label: 'Overall Conversion', value: data?.overallConversion },
          ].map((m) => (
            <div key={m.label} style={{ marginBottom: 10 }}>
              <div className="muted" style={{ fontSize: 11, marginBottom: 4 }}>{m.label}</div>
              <div className="progress-bar"><div className="progress-fill" style={{ width: `${m.value ?? 0}%`, background: '#4f79ff' }} /></div>
              <span className="mono">{formatPercent(m.value)}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export function KycFunnelReport() {
  return <Suspense fallback={<SkeletonTable />}><KycFunnelReportInner /></Suspense>
}
