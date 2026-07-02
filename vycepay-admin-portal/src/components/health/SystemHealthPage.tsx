'use client'

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { ErrorState, SkeletonTable } from '@/components/ui/States'

export function SystemHealthPage() {
  const { data, isLoading, error } = useQuery({ queryKey: ['system-health'], queryFn: () => apiFetch<Record<string, unknown>>('/system-health') })
  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message="Unable to load system health." />
  const services = (data.services as Record<string, unknown>[]) ?? []
  const choiceBank = data.choiceBank as Record<string, unknown> | undefined
  return (
    <div className="grid">
      <PageHeader title="System Health" description="Internal service and provider reachability." />
      <div className="health-grid">
        {services.map((svc) => (
          <div className="card health-card" key={String(svc.name)}>
            <div className="section-title">{String(svc.name)}</div>
            <div className={svc.status === 'UP' ? 'health-up' : 'health-down'} style={{ fontSize: 24, fontWeight: 800 }}>{String(svc.status)}</div>
            <p className="muted">{String(svc.responseTimeMs ?? '—')} ms</p>
          </div>
        ))}
        {choiceBank ? (
          <div className="card health-card">
            <div className="section-title">Choice Bank</div>
            <div className={choiceBank.reachable ? 'health-up' : 'health-down'} style={{ fontSize: 24, fontWeight: 800 }}>{choiceBank.reachable ? 'REACHABLE' : 'DOWN'}</div>
            <p className="muted">{String(choiceBank.latencyMs ?? '—')} ms</p>
          </div>
        ) : null}
      </div>
    </div>
  )
}
