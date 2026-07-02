'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { formatDateTime } from '@/lib/format'
import { DetailLayout } from '@/components/detail/DetailLayout'
import { KeyValueGrid } from '@/components/detail/KeyValueGrid'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EntityLink } from '@/components/ui/EntityLink'
import { ErrorState, SkeletonTable } from '@/components/ui/States'

export function KycDetail() {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({ queryKey: ['kyc', params.id], queryFn: () => apiFetch<Record<string, unknown>>(`/kyc/${params.id}`) })
  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message="Unable to load KYC record." />
  const name = `${data.firstName ?? data.first_name ?? ''} ${data.lastName ?? data.last_name ?? ''}`.trim()
  return (
    <DetailLayout header={<div><h2>KYC — {name || `#${params.id}`}</h2><StatusBadge status={data.status} /></div>}>
      <div className="card">
        <KeyValueGrid items={[
          { label: 'Customer', value: <EntityLink href={`/customers/${data.customerExternalId}`}>{String(data.customerExternalId)}</EntityLink> },
          { label: 'Choice Onboarding ID', value: <span className="mono">{String(data.choice_onboarding_request_id ?? data.choiceOnboardingRequestId ?? '—')}</span> },
          { label: 'Choice User ID', value: <span className="mono">{String(data.choice_user_id ?? '—')}</span> },
          { label: 'ID Type', value: String(data.id_type ?? data.idType ?? '—') },
          { label: 'ID Number', value: String(data.id_number ?? '—') },
          { label: 'Status', value: <StatusBadge status={data.status} /> },
          { label: 'Submitted', value: formatDateTime(data.created_at ?? data.createdAt) },
          { label: 'ID Front', value: data.id_front_url ? <a href={String(data.id_front_url)} target="_blank" rel="noreferrer">View document</a> : '—' },
          { label: 'Selfie', value: data.selfie_url ? <a href={String(data.selfie_url)} target="_blank" rel="noreferrer">View document</a> : '—' },
        ]} />
      </div>
    </DetailLayout>
  )
}
